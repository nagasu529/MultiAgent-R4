package Agent;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
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

public class testBidder extends Agent {
    randValue randValue = new randValue();
    DecimalFormat df = new DecimalFormat("#.##");
    ArrayList<Agents> sortedListFiveHundred = new ArrayList<Agents>();
    ArrayList<Agents> sortedListVaries = new ArrayList<Agents>();

    agentInfo bidderInfo = new agentInfo("","bidder", randValue.getRandDoubleRange(10,16), randValue.getRandDoubleRange(300,2000));

    //Instant best seller for the ACCEPT_PROPOSAL message.
    int cnt = 0;
    int fiveHundredVol = 500;
    int fiveHundredVolFreq;
    double varieVol;
    int varieVolFreq = 1;

    protected void setup() {
        System.out.println(getAID().getLocalName()+"  is ready" );
        //Selling volume splited by conditions (each grounp is not over 500 mm^3).
        fiveHundredVolFreq = (int)(bidderInfo.buyingVol/500);
        varieVol = ((bidderInfo.buyingVol/500) - fiveHundredVolFreq) * 500;

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
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }
        //Bidding process.

        //Add the behaviour serving queries from Water provider about current price.
        addBehaviour(new OfferRequestsServer());

        //Add the behaviour serving purhase orders from water provider agent.
        addBehaviour(new PurchaseOrdersServer());

        addBehaviour(new RejectandReset());
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
                        if(msg.getPerformative() == ACLMessage.CFP){
                            replyCnt++;
                            //ACLMessage reply = msg.createReply();

                            //Price Per MM. and the number of volumn to sell from Seller.
                            String currentOffer = msg.getContent();
                            String[] arrOfstr = currentOffer.split("-");
                            double tempFiveHundredVol = Double.parseDouble(arrOfstr[0]);
                            int tempFiveHunderedFreq = Integer.parseInt(arrOfstr[1]);
                            double tempVarieVol = Double.parseDouble(arrOfstr[2]);
                            int tempVarieFreq = Integer.parseInt(arrOfstr[3]);

                            System.out.println(msg.getSender().getLocalName() + " Offer price and Vol:  " + tempFiveHundredVol + "  " + tempFiveHunderedFreq + "   " + tempVarieVol + "  " + tempVarieFreq + "  " + bidderInfo.buyingPrice);

                            //Reply bidding information to sellers
                            //reply.setPerformative(ACLMessage.PROPOSE);
                            //reply.setContent(fiveHundredVol + "-" + fiveHundredVolFreq + "-" + varieVol + "-" + varieVolFreq + "-" + bidderInfo.buyingPrice);
                            //myAgent.send(reply);
                            if(tempVarieVol > varieVol){
                                sortedListVaries.add(new Agents(tempVarieVol, tempFiveHunderedFreq, msg.getSender().getLocalName()));
                            }else {
                                sortedListVaries.add(new Agents(0,tempFiveHunderedFreq,msg.getSender().getLocalName()));
                            }

                        }
                        //Prepairing reply message.
                        if(replyCnt >= sellerList.length){
                            Collections.sort(sortedListVaries, new SortbyVolume());
                            for(int i = 0; i < sortedListVaries.size();i++){
                                System.out.println(sortedListVaries.get(i));
                            }
                            step =1;
                        }

                    } else {
                        block();
                    }
                    break;

                case 1:
                    //
                    ACLMessage reply  = new ACLMessage(ACLMessage.PROPOSE);
                    while (fiveHundredVolFreq > 0){
                        reply.setContent(fiveHundredVol + "-" + fiveHundredVolFreq + "-" + varieVol + "-" + varieVolFreq + "-" + bidderInfo.buyingPrice);
                    }
                    /***
                    for(int i=0; i < sellerList.length; ++i){
                        if(sellerList[i].getLocalName().equals(sortedListVaries.get(0).name) && (sortedListVaries.get(0).fivehundredFeq == fiveHundredVolFreq)){
                            // Send the purchase order to the seller that provided the best offer
                            ACLMessage acceptedRequest = new ACLMessage(ACLMessage.PROPOSE);
                            acceptedRequest.addReceiver(sellerList[i]);
                            acceptedRequest.setConversationId("bidding");
                            acceptedRequest.setReplyWith("acceptedRequest" + System.currentTimeMillis());
                            myAgent.send(acceptedRequest);
                            System.out.println("\n" + acceptedRequest.toString() + "\n");
                            mt = MessageTemplate.and(MessageTemplate.MatchConversationId("bidding"),MessageTemplate.MatchInReplyTo
                                    (acceptedRequest.getReplyWith()));
                        }
                    }

                    /***
                case 1:
                    //receive CFP
                    //MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
                    ACLMessage msg = myAgent.receive(mt);
                    if (msg != null) {
                        if(msg.getPerformative() == ACLMessage.CFP){
                            replyCnt++;
                            ACLMessage reply = msg.createReply();

                            //Price Per MM. and the number of volumn to sell from Seller.
                            String currentOffer = msg.getContent();
                            String[] arrOfstr = currentOffer.split("-");
                            double tempFiveHundredVol = Double.parseDouble(arrOfstr[0]);
                            int tempFiveHunderedFreq = Integer.parseInt(arrOfstr[1]);
                            double tempVarieVol = Double.parseDouble(arrOfstr[2]);
                            int tempVarieFreq = Integer.parseInt(arrOfstr[3]);

                            System.out.println(msg.getSender().getLocalName() + " Offer price and Vol:  " + tempFiveHundredVol + "  " + tempFiveHunderedFreq + "   " + tempVarieVol + "  " + tempVarieFreq + "  " + bidderInfo.buyingPrice);

                            //Reply bidding information to sellers
                            reply.setPerformative(ACLMessage.PROPOSE);
                            //reply.setContent(fiveHundredVol + "-" + fiveHundredVolFreq + "-" + varieVol + "-" + varieVolFreq + "-" + bidderInfo.buyingPrice);
                            //myAgent.send(reply);
                            for(int i = 1; i <= tempFiveHunderedFreq; i++){
                                sortedListAgent.add(new Agents(500,msg.getSender().getLocalName()));
                            }
                            sortedListAgent.add(new Agents(tempVarieVol,msg.getSender().getLocalName()));
                        }

                        if(replyCnt >= sellerList.length){
                            Collections.sort(sortedListAgent, new SortbyVolume());
                            step =2;
                        }
                    } else {
                        block();
                    }
                    break;
                case 2:
                    System.out.println("result sort: \n");
                    for(int i=0; i < sortedListAgent.size();i++){
                        System.out.println(sortedListAgent.get(i));
                    }
                     ***/
            }
        }
        public boolean done(){
            return step ==2;
        }
    }

    private class PurchaseOrdersServer extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                ACLMessage reply = msg.createReply();
                if(msg.getSender().getLocalName().equals(bidderInfo.offeredVarieName) && bidderInfo.offeredVarieName != null) {
                    reply.setPerformative(ACLMessage.INFORM);
                    System.out.println("");
                    System.out.println("\n" + getAID().getLocalName() + "accpted to buy water from" + msg.getSender().getLocalName());
                    bidderInfo.offeredVarieName = null;
                    myAgent.send(reply);
                    System.out.println(reply);
                }
                if(msg.getSender().getLocalName().equals(bidderInfo.offeredNameFiveHundred) && bidderInfo.offeredNameFiveHundred != null) {
                    reply.setPerformative(ACLMessage.INFORM);
                    System.out.println("");
                    System.out.println("\n" + getAID().getLocalName() + "accpted to buy water from" + msg.getSender().getLocalName());
                    bidderInfo.offeredNameFiveHundred = null;
                    myAgent.send(reply);
                    System.out.println(reply);
                } else{
                    reply.setPerformative(ACLMessage.FAILURE);
                    myAgent.send(reply);
                    System.out.println(reply);
                }
            } else {
                block();
            }
            if(bidderInfo.offeredNameFiveHundred == null && bidderInfo.offeredVarieName == null) {
                myAgent.doDelete();
                //myGUI.dispose();
                System.out.println(getAID().getName() + " terminating.");
            }
        }
    }

    private class RejectandReset extends CyclicBehaviour{
        public void action(){
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REFUSE);
            ACLMessage msg = myAgent.receive(mt);
            if(msg != null){

            }else {
                block();
            }
        }
    }

    class agentInfo{
        String farmerName;
        String agentType;
        Double buyingPrice;
        Double buyingVol;
        public agentInfo(String farmerName, String agentType, double buyingPrice, double buyingVol){
            this.farmerName = farmerName;
            this.agentType = agentType;
            this.buyingPrice = buyingPrice;
            this.buyingVol = buyingVol;
        }
        public String toString(){
            return "Bidder Name: " + this.farmerName + " " + "Buying Volume: " + this.buyingVol + " " + "Price: " + this.buyingPrice;
        }
    }

    //Sorted by volumn (descending).
    class SortbyVolume implements Comparator<Agents>{
        //Used for sorting in ascending order of the volumn.
        public int compare(Agents a, Agents b){
            return Double.compare(a.varieVolume, b.varieVolume);
        }
    }

    //adding new class for sorted seller agent data.
    class Agents{
        double varieVolume;
        int fivehundredFeq;
        String name;
        //Constructor
        public Agents(double varieVolume, int fivehundredFeq, String name){
            this.varieVolume = varieVolume;
            this.fivehundredFeq = fivehundredFeq;
            this.name = name;
        }
        public String toString(){
            return this.name + " " + this.varieVolume + " " + this.fivehundredFeq;
        }
    }
}
