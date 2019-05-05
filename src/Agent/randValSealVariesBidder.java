package Agent;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

import java.text.DecimalFormat;
import java.util.*;

public class randValSealVariesBidder extends Agent {
    randValue randValue = new randValue();
    DecimalFormat df = new DecimalFormat("#.##");

    agentInfo bidderInfo = new agentInfo("","bidder", randValue.getRandDoubleRange(13,15), randValue.getRandDoubleRange(300,1000),0.0, 0.0, "");

    //Instant best seller for the ACCEPT_PROPOSAL message.
    int cnt = 0;

    protected void setup() {
        System.out.println(getAID().getLocalName()+"  is ready" );

        //Start Agent
        // Register the book-selling service in the yellow pages
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        bidderInfo.farmerName = getAID().getLocalName();
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

        /***
         addBehaviour(new TickerBehaviour(this, 500) {
         public void onTick() {
         System.out.println("bidder name: " + bidderInfo.farmerName + "  " + bidderInfo.buyingVolumn + "  " + bidderInfo.buyingPrice + "\n");
         //Add the behaviour serving queries from Water provider about current price.
         addBehaviour(new OfferRequestsServer());

         //Add the behaviour serving purhase orders from water provider agent.
         addBehaviour(new PurchaseOrdersServer());
         }
         });

         addBehaviour(new TickerBehaviour(this, 30000) {
         protected void onTick() {
         bidderInfo.offeredName = "";
         bidderInfo.offeredPrice = 0.0;
         bidderInfo.offeredVolumn = 0.0;
         }
         });***/
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

    private class OfferRequestsServer extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                ACLMessage reply = msg.createReply();

                //Price Per MM. and the number of volumn to sell from Seller.
                String currentOffer = msg.getContent();
                String[] arrOfstr = currentOffer.split("-");

                double tempVol = Double.parseDouble(arrOfstr[0]);
                double tempPrice = Double.parseDouble(arrOfstr[1]);

                System.out.println("Offer price and Vol:  " + tempVol + "   " + tempPrice);

                //myGUI.displayUI("Price setting up from Seller: " + farmerInfo.waterPriceFromSeller + " per MM" + "\n");
                //myGUI.displayUI("Selling volume from seller:" + farmerInfo.waterVolumnFromSeller + "\n");

                //Auction Process
                if (tempVol >= bidderInfo.buyingVolumn && tempPrice <= bidderInfo.buyingPrice) {
                    reply.setPerformative(ACLMessage.PROPOSE);
                    String sendingOffer = tempVol + "-" + tempPrice;
                    reply.setContent(sendingOffer);
                    double tempValue = tempPrice * tempVol;
                    double tempMax = bidderInfo.offeredPrice * bidderInfo.offeredVolumn;

                    //setting up the best offer from seller.
                    if(tempMax == 0 || tempValue < tempMax ){
                        bidderInfo.offeredVolumn = tempVol;
                        bidderInfo.offeredPrice = tempPrice;
                        bidderInfo.offeredName = msg.getSender().getLocalName();
                    }

                    System.out.print(getAID().getLocalName() + "the best seller option is   " + bidderInfo.offeredName + " " + bidderInfo.offeredPrice + "  " + bidderInfo.offeredVolumn);

                    myAgent.send(reply);
                    System.out.println(reply.toString());
                } else {
                    reply.setPerformative(ACLMessage.REFUSE);
                    myAgent.send(reply);
                    System.out.println(reply.toString());
                    System.out.println(getAID().getName() + " is surrender");
                }
            } else {
                block();
            }
        }
    }

    private class RejectandReset extends CyclicBehaviour{
        public void action(){
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REFUSE);
            ACLMessage msg = myAgent.receive(mt);
            if(msg != null){
                if(msg.getSender().getLocalName().equals(bidderInfo.offeredName)){
                    bidderInfo.offeredName = "";
                    bidderInfo.offeredPrice = 0.0;
                    bidderInfo.offeredVolumn = 0.0;
                }
            }else {
                block();
            }
        }
    }

    private class PurchaseOrdersServer extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                ACLMessage reply = msg.createReply();
                if(msg.getSender().getLocalName().equals(bidderInfo.offeredName) && bidderInfo.offeredName != null){
                    reply.setPerformative(ACLMessage.INFORM);
                    System.out.println("");
                    System.out.println("\n" + getAID().getLocalName() + "accpted to buy water from" + msg.getSender().getLocalName());
                    myAgent.doDelete();
                    //myGUI.dispose();
                    System.out.println(getAID().getName() + " terminating.");
                }else {
                    reply.setPerformative(ACLMessage.FAILURE);
                    myAgent.send(reply);
                }
                myAgent.send(reply);

            } else {
                block();
            }
        }
    }

    public class agentInfo{
        String farmerName;
        String agentType;
        Double buyingPrice;
        Double buyingVolumn;
        Double offeredPrice;
        Double offeredVolumn;
        String offeredName;

        agentInfo(String farmerName, String agentType, double buyingPrice, double buyingVolumn, double offeredPrice, double offeredVolumn, String offeredName){
            this.farmerName = farmerName;
            this.agentType = agentType;
            this.buyingPrice = buyingPrice;
            this.buyingVolumn = buyingVolumn;
            this.offeredPrice = offeredPrice;
            this.offeredVolumn = offeredVolumn;
            this.offeredName = offeredName;
        }
    }
}
