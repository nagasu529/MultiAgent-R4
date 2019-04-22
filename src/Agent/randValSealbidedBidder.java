package Agent;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import Agent.Crop.cropType;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import java.text.DecimalFormat;

public class randValSealbidedBidder extends Agent {
    randValue randValue = new randValue();
    DecimalFormat df = new DecimalFormat("#.##");

    agentInfo bidderInfo = new agentInfo("","bidder", randValue.getRandDoubleRange(13,15), randValue.getRandDoubleRange(300,1000),0.0, 0.0, 0);

    protected void setup(){
        System.out.println(getAID().getLocalName() + "is Ready");
        bidderInfo.farmerName = getAID().getLocalName();
        bidderInfo.acceptedPrice = bidderInfo.buyingPrice;
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("bidder");
        sd.setName(getAID().getName());
        dfd.addServices(sd);
        try{
            DFService.register(this, dfd);
        }
        catch (FIPAException fe){
            fe.printStackTrace();
        }

        //Bidding water process
        addBehaviour(new TickerBehaviour(this, 5000) {
            protected void onTick() {
                System.out.println("Agent Name: " + bidderInfo.farmerName + "  " + "Buying price: " + bidderInfo.buyingPrice + "  " + "Water volumn need: " + bidderInfo.buyingVolumn);
                addBehaviour(new OfferRequestsServer());
                addBehaviour(new PurchaseOrdersServer());
            }
        });
    }

    private class OfferRequestsServer extends Behaviour {
        //The seller list is update and choosing lowest price for seller.
        private AID[] sellerAgents;
        private double tempVolumn;
        private double tempPrice;
        private int repliesCnt;
        private String tempName;

        private int step = 0;

        public void action() {
            switch (step) {
                case 0:
                    DFAgentDescription template = new DFAgentDescription();
                    ServiceDescription sd = new ServiceDescription();
                    sd.setType("Seller");
                    template.addServices(sd);
                    try {
                        DFAgentDescription[] result = DFService.search(myAgent, template);
                        sellerAgents = new AID[result.length];
                        bidderInfo.numSeller = sellerAgents.length;
                        System.out.println("Seller agent NO.     " + sellerAgents.length);
                        for (int i = 0; i < result.length; i++) {
                            sellerAgents[i] = result[i].getName();
                            System.out.println(sellerAgents[i]);
                        }
                    } catch (FIPAException fe) {
                        fe.printStackTrace();
                    }

                    step = 1;
                    System.out.println(step);
                    break;

                case 1:
                    //CFP Message received.
                    MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
                    ACLMessage msg = myAgent.receive(mt);
                    if (msg != null) {
                        repliesCnt++;
                        //Offered price Per MM. and the number of volumn from each seller.
                        String currentOffer = msg.getContent();
                        String[] arrOfstr = currentOffer.split("-");
                        tempVolumn = Double.parseDouble(arrOfstr[0]);
                        tempPrice = Double.parseDouble(arrOfstr[1]);
                        if (tempPrice <= bidderInfo.acceptedPrice && tempVolumn >= bidderInfo.acceptedVolumn) {
                            bidderInfo.acceptedPrice = tempPrice;
                            bidderInfo.acceptedVolumn = tempVolumn;
                            tempName = msg.getSender().getName();
                            System.out.println("dddddddddddddddddddddddddddddddddddddddddddd" + tempName + tempPrice +tempVolumn);
                        }
                        step = 2;
                    }else {
                        block();
                    }
                    break;

                case 2:
                    if (repliesCnt >= sellerAgents.length) {
                        step = 3;
                    }else {
                        step = 0;
                    }
                    break;

                case 3:
                    System.out.println("66666666666666666666666666666666666666666666666666666666666" + tempName + tempVolumn + tempPrice);
                    for (int i = 0; i < sellerAgents.length; i++) {
                        if (tempName.equals(sellerAgents[i])) {
                            ACLMessage acceptedBidding = new ACLMessage((ACLMessage.PROPOSE));
                            acceptedBidding.addReceiver(sellerAgents[i]);
                            acceptedBidding.setConversationId("bidding");
                            String arrOfbidding = bidderInfo.acceptedVolumn + "-" + bidderInfo.acceptedPrice + "-";
                            acceptedBidding.setContent(arrOfbidding);
                            myAgent.send(acceptedBidding);
                            System.out.println("sssssss" + acceptedBidding);
                        }
                        else {
                            //Refuse message prepairing
                            ACLMessage rejectedRequest = new ACLMessage(ACLMessage.REFUSE);
                            rejectedRequest.addReceiver(sellerAgents[i]);
                            //myGui.displayUI(rejectedRequest.toString());
                            myAgent.send(rejectedRequest);
                            System.out.println("yyyyyyy" + rejectedRequest);
                        }
                    }
                    step = 4;
                    System.out.println(step);
                    break;
            }
        }
        public boolean done(){
            if(step == 4){
                System.out.println("\n" + getAID().getLocalName() + " finished bidding process");
            }
            return step == 0;
        }
    }

    private class PurchaseOrdersServer extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                // ACCEPT_PROPOSAL Message received. Process it
                ACLMessage reply = msg.createReply();
                System.out.println(bidderInfo.sellingStatus);
                reply.setPerformative(ACLMessage.INFORM);
                if (bidderInfo.sellingStatus=="avalable") {
                    bidderInfo.sellingStatus = "sold";
                    //System.out.println(getAID().getName()+" sold water to agent "+msg.getSender().getName());
                    System.out.println(getAID().getLocalName()+" sold water to "+msg.getSender().getLocalName());
                    //myGui.displayUI(farmerInfo.sellingStatus.toString());
                    //System.out.println(farmerInfo.sellingStatus);
                    doSuspend();
                } else {
                    // The requested book has been sold to another buyer in the meanwhile.
                    reply.setPerformative(ACLMessage.FAILURE);
                    reply.setContent("not-available for sale");
                    //myGui.displayUI("not avalable to sell");
                }
            }else {
                block();
            }
        }
    }

    protected void takeDown() {
        try {
            DFService.deregister(this);
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }
        // Printout a dismissal message
        System.out.println(getAID().getName()+" terminating.");
    }

    public class agentInfo{
        String farmerName;
        String sellingStatus;
        Double buyingPrice;
        Double buyingVolumn;
        Double acceptedPrice;
        Double acceptedVolumn;
        int numSeller;

        agentInfo(String farmerName, String agentType, double buyingPrice, double buyingVolumn, double acceptedPrice, double acceptedVolumn, int numSeller){
            this.farmerName = farmerName;
            this.sellingStatus = agentType;
            this.buyingPrice = buyingPrice;
            this.buyingVolumn = buyingVolumn;
            this.acceptedPrice = acceptedPrice;
            this.acceptedVolumn = acceptedVolumn;
            this.numSeller = numSeller;
        }
    }
}