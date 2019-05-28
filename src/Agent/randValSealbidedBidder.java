package Agent;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class randValSealbidedBidder extends Agent {
    randValue randValue = new randValue();
    DecimalFormat df = new DecimalFormat("#.##");
    ArrayList<Agents> sortedListSeller = new ArrayList<Agents>();
    ArrayList<Agents> proposeSortedList = new ArrayList<Agents>();


    agentInfo bidderInfo = new agentInfo("", "bidder", randValue.getRandDoubleRange(10, 16), randValue.getRandDoubleRange(300, 2000));

    //Instant best seller for the ACCEPT_PROPOSAL message.
    int cnt = 0;
    int fiveHundredVol = 500;
    int fiveHundredVolFreq;
    double varieVol;
    int varieVolFreq = 1;

    protected void setup() {
        System.out.println(getAID().getLocalName() + "  is ready");
        //Start Agent
        // Register the service in the yellow pages
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        bidderInfo.farmerName = getAID().getLocalName();
        System.out.println(bidderInfo.farmerName + "  " + bidderInfo.buyingVol + "  " + bidderInfo.buyingPrice + "  " + fiveHundredVolFreq + "  " + varieVol);
        sd.setType("bidder");

        sd.setName(getAID().getName());
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        //Bidding process.

        //Sendding info to mornitor.
        addBehaviour(new AdverInfoMsg());

        //Add the behaviour serving queries from Water provider about current price.
        addBehaviour(new OfferRequestsServer());

        //Add the behaviour serving purhase orders from water provider agent.
        addBehaviour(new PurchaseOrdersServer());

        //addBehaviour(new RejectProposalProcess());

    }

    // Put agent clean-up operations here
    protected void takeDown() {
        // Deregister from the yellow pages
        try {
            DFService.deregister(this);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        System.out.println(getAID().getName() + " terminating.");
    }

    private class OfferRequestsServer extends Behaviour {
        //search agent in DF
        private AID[] sellerList;
        private int replyCnt;
        private MessageTemplate mt;
        private int step = 0;

        public void action() {
            //Search Sellers
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType("seller");
            template.addServices(sd);
            try {
                DFAgentDescription[] result = DFService.search(myAgent, template);
                sellerList = new AID[result.length];
                for (int i = 0; i < result.length; ++i) {
                    sellerList[i] = result[i].getName();
                    //System.out.println(sellerList[i]);

                }
            } catch (FIPAException fe) {
                fe.printStackTrace();
            }

            switch (step) {
                case 0:
                    //receive CFP
                    //MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
                    ACLMessage msg = myAgent.receive(mt);
                    if (msg != null) {
                        if (msg.getPerformative() == ACLMessage.CFP && msg.getSender().getLocalName().equals("monitor")==false) {
                            System.out.println(msg);
                            replyCnt++;
                            //ACLMessage reply = msg.createReply();

                            //Price Per MM. and the number of volumn to sell from Seller.
                            String currentOffer = msg.getContent();
                            String[] arrOfstr = currentOffer.split("-");
                            double tempVolumn = Double.parseDouble(arrOfstr[0]);

                            System.out.println(msg.getSender().getLocalName() + " Offer price and Vol:  " + tempVolumn);

                            sortedListSeller.add(new Agents(tempVolumn, msg.getSender().getLocalName()));

                        }
                        System.out.println("\n");

                        if (replyCnt >= sellerList.length) {
                            Collections.sort(sortedListSeller, new SortbyTotalVol());
                            System.out.println("start +++++++++++++++++++++++++++++++++++++++++++" + "\n");

                            for(int i = 0; i <= sortedListSeller.size() -1; i++){
                                if(sortedListSeller.get(i).totalVolume > bidderInfo.buyingVol){
                                    proposeSortedList.add(sortedListSeller.get(i));
                                    sortedListSeller.remove(i);
                                    break;
                                }
                            }
                            if(proposeSortedList.size()== 0){
                                System.out.println("Do not have enought water to buy.");
                                step = 1;
                            }else {
                                System.out.println("The best option for sendding offer is  " + proposeSortedList.get(0).toString());
                                step = 1;
                            }

                        }

                    } else {
                        block();
                    }
                    break;

                case 1:
                    //Sending PROPOSE message to Seller (only the best option for volume requirement.

                    if(proposeSortedList.size() !=0){
                        for (int i = 0; i < sellerList.length; i++) {
                            if (sellerList[i].getLocalName().equals(proposeSortedList.get(0).name)) {
                                ACLMessage reply = new ACLMessage(ACLMessage.PROPOSE);
                                reply.setContent(proposeSortedList.get(0).totalVolume + "-" + bidderInfo.buyingPrice);
                                reply.setConversationId("bidding");
                                reply.setReplyWith("reply" + System.currentTimeMillis());
                                reply.addReceiver(sellerList[i]);
                                myAgent.send(reply);
                                System.out.println(reply);
                            }
                        }
                    }


                    for (int i = 0; i < sellerList.length; i++) {
                        for (int j = 0; j <= sortedListSeller.size() - 1; j++) {
                            if (sellerList[i].getLocalName().equals(sortedListSeller.get(j).name)) {
                                ACLMessage reply = new ACLMessage(ACLMessage.REFUSE);
                                reply.setConversationId("bidding");
                                reply.setReplyWith("reply" + System.currentTimeMillis());
                                reply.addReceiver(sellerList[i]);
                                myAgent.send(reply);
                                System.out.println(reply);
                            }
                        }
                    }
                    step = 2;
                    break;
            }
        }

        public boolean done() {
            return step == 2;
        }
    }

    private class PurchaseOrdersServer extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                ACLMessage reply = msg.createReply();
                reply.setPerformative(ACLMessage.INFORM);
                System.out.println("");
                System.out.println("\n" + getAID().getLocalName() + "accpted to buy water from" + msg.getSender().getLocalName());
                myAgent.send(reply);
                System.out.println(reply);
                myAgent.doSuspend();
                takeDown();
                System.out.println(getAID().getName() + " terminating.");
            } else {
                block();
            }

            //
            //myGUI.dispose();
            //
        }
    }

    private class AdverInfoMsg extends OneShotBehaviour {
        private AID[] mornitorAgent;
        public void action(){
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType("mornitor");
            template.addServices(sd);
            try {
                DFAgentDescription[] result = DFService.search(myAgent, template);
                //System.out.println("Found acutioneer agents:");
                mornitorAgent = new AID[result.length];
                for (int i = 0; i < result.length; ++i) {
                    mornitorAgent[i] = result[i].getName();
                    //System.out.println(mornitorAgent[i].getName());
                }
            }
            catch (FIPAException fe) {
                fe.printStackTrace();
            }
            // Send the cfp to all sellers (Sending water volumn required to all bidding agent)
            ACLMessage mornitorReply = new ACLMessage(ACLMessage.PROPOSE);
            for (int i = 0; i < mornitorAgent.length; ++i) {
                mornitorReply.addReceiver(mornitorAgent[i]);
            }
            mornitorReply.setContent(bidderInfo.buyingVol + "-" + bidderInfo.buyingPrice + "-" + 0);
            myAgent.send(mornitorReply);
        }
    }

    private class RejectProposalProcess extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REJECT_PROPOSAL);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                addBehaviour(new OfferRequestsServer());
            } else {
                block();
            }
        }
    }

    class agentInfo {
        String farmerName;
        String agentType;
        Double buyingPrice;
        Double buyingVol;

        public agentInfo(String farmerName, String agentType, double buyingPrice, double buyingVol) {
            this.farmerName = farmerName;
            this.agentType = agentType;
            this.buyingPrice = buyingPrice;
            this.buyingVol = buyingVol;
        }

        public String toString() {
            return "Bidder Name: " + this.farmerName + " " + "Buying Volume: " + this.buyingVol + " " + "Price: " + this.buyingPrice;
        }
    }

    class SortbyTotalVol implements Comparator<Agents> {
        //Used for sorting in ascending order of the volumn.
        public int compare(Agents a, Agents b) {
            return Double.compare(a.totalVolume, b.totalVolume);
        }
    }

    //adding new class for sorted seller agent data.
    class Agents {
        //double varieVolume;
        //int fivehundredFeq;
        double totalVolume;
        String name;

        //Constructor
        public Agents(double totalVolume, String name) {
            //this.varieVolume = varieVolume;
            //this.fivehundredFeq = fivehundredFeq;
            this.totalVolume = totalVolume;
            this.name = name;
        }

        public String toString() {
            //return this.name + " " + this.varieVolume + " " + this.fivehundredFeq + "  total Volume: " + (this.varieVolume + (this.fivehundredFeq * 500));
            return this.name + " " + "  total Volume: " + this.totalVolume;
        }
    }
}



