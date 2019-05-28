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

public class randValCombiBidder extends Agent {
    //The list of farmer who are seller (maps the water volumn to its based price)
    randValue randValue = new randValue();

    DecimalFormat df = new DecimalFormat("#.##");

    //Farmer information on each agent.
    agentInfo farmerInfo = new agentInfo("", randValue.getRandDoubleRange(500, 1200),randValue.getRandDoubleRange(12,16), randValue.getRandDoubleRange(5,12));
    String mornitoringMsg = farmerInfo.farmerName + "-" + farmerInfo.buyingVolumn + "-" + farmerInfo.buyingVolumn + "-" + farmerInfo.profitLossPct;

    //Global bidding parameter
    ArrayList<Agents> sortedListSeller = new ArrayList<Agents>();
    ArrayList<Agents> proposeSortedList = new ArrayList<Agents>();

    protected void setup() {
        System.out.println(getAID().getLocalName()+"  is ready" );

        //Start Agent
        // Register the book-selling service in the yellow pages
        farmerInfo.farmerName = getAID().getLocalName();
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("bidder");
        //sd.setType(farmerInfo.agentType);
        sd.setName(getAID().getName());
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }
        //Mornitoring infomation sendding.
        addBehaviour(new AdverInfoMsg());
        //Bidding process.
        //Add the behaviour serving queries from Water provider about current price.
        addBehaviour(new OfferRequestsServer());

        //Add the behaviour serving purhase orders from water provider agent.
        addBehaviour(new PurchaseOrdersServer());
    }

    // Put agent clean-up operations here
    protected void takeDown() {
        // Deregister from the yellow pages
        try {
            DFService.deregister(this);
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }
        // Close the GUI
        //myGUI.dispose();
        // Printout a dismissal message
        System.out.println(getAID().getName()+" terminating.");
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
                        if (msg.getPerformative() == ACLMessage.CFP && msg.getSender().getLocalName().equals("mornitor")==false) {
                            //System.out.println(msg);
                            replyCnt++;
                            //ACLMessage reply = msg.createReply();
                            //Price Per MM. and the number of volumn to sell from Seller.
                            String currentOffer = msg.getContent();
                            String[] arrOfstr = currentOffer.split("-");
                            double tempVolumn = Double.parseDouble(arrOfstr[0]);

                            System.out.println(msg.getSender().getLocalName() + " OfferVol:  " + tempVolumn);

                            sortedListSeller.add(new Agents(tempVolumn, msg.getSender().getLocalName()));
                        }
                        System.out.println("\n");

                        if (replyCnt >= sellerList.length) {
                            Collections.sort(sortedListSeller, new SortbyTotalVol());
                            System.out.println("start +++++++++++++++++++++++++++++++++++++++++++" + "\n");

                            for(int i = 0; i <= sortedListSeller.size() -1; i++){
                                if(sortedListSeller.get(i).totalVolume > farmerInfo.buyingVolumn){
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
                                reply.setContent(proposeSortedList.get(0).totalVolume + "-" + farmerInfo.buyingPricePerMM + "-" + farmerInfo.profitLossPct);
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
            }else {
                block();
            }
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
            mornitorReply.setContent(farmerInfo.buyingVolumn + "-" + farmerInfo.buyingPricePerMM + "-" + farmerInfo.profitLossPct);
            myAgent.send(mornitorReply);
        }
    }

    public class agentInfo{
        String farmerName;
        //String agentType;
        double buyingVolumn;
        //double currentLookingVolumn;
        double buyingPricePerMM;
        //String sellingStatus;
        //double offeredVolumn;
        //double offeredPrice;
        //String offeredName;
        double profitLossPct;

        agentInfo(String farmerName, double buyingVolumn, double buyingPricePerMM, double profitLossPct){
            this.farmerName = farmerName;
            //this.agentType = agentType;
            this.buyingVolumn = buyingVolumn;
            //this.currentLookingVolumn = currentLookingVolumn;
            this.buyingPricePerMM = buyingPricePerMM;
            //this.sellingStatus = sellingStatus;
            //this.offeredVolumn = offeredVolumn;
            //this.offeredPrice = offeredPrice;
            //this.offeredName = offeredName;
            this.profitLossPct = profitLossPct;
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
    class SortbyTotalVol implements Comparator<Agents> {
        //Used for sorting in ascending order of the volumn.
        public int compare(Agents a, Agents b) {
            return Double.compare(a.totalVolume, b.totalVolume);
        }
    }
}