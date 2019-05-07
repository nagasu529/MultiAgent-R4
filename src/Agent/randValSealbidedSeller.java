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

public class randValSealbidedSeller extends Agent {
    randValSealbidedSellerGUI myGui;
    // Create and show the GUI
    randValue randValue = new randValue();
    DecimalFormat df = new DecimalFormat("#.##");

    agentInfo sellerInfo = new agentInfo ("","seller",randValue.getRandDoubleRange(10,12),randValue.getRandDoubleRange(1300,1500),0.0,0.0,"");
    //Double minValue = sellerInfo.sellingVolume * sellerInfo.sellingPrice;
    //Double maxOfferValue = sellerInfo.offeredVolumn * sellerInfo.offeredPrice;

    //Storing all biider data for making decision.
    List<offerOfBidders> bidderList;
    List<Double> order;
    List<offerOfBidders> resultList;

    //Instant best seller for the ACCEPT_PROPOSAL message.
    int cnt = 0;

    protected void setup() {
        myGui = new randValSealbidedSellerGUI(this);
        myGui.show();
        System.out.println(getAID().getLocalName() + "  is ready");

        System.out.println(getAID().getLocalName() + " " + sellerInfo.sellingVolume+ "  " + sellerInfo.sellingPrice + "\n");

        //Start Agent
        // Register the book-selling service in the yellow pages
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sellerInfo.farmerName = getAID().getLocalName();
        sd.setType("bidder");

        sd.setName(getAID().getName());
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        //Bidding process.

        //Add the behaviour serving queries from Water provider about current price.
        addBehaviour(new OfferRequestsServer());
        addBehaviour(new ReceivedOrdersServer());

        //Add the behaviour serving purhase orders from water provider agent.
        addBehaviour(new TickerBehaviour(this, 30000) {
            protected void onTick() {
                addBehaviour(new PurchaseOrderServer);
            }
        });
        //addBehaviour(new PurchaseOrdersServer());
    }

    // Put agent clean-up operations here
    protected void takeDown () {
        // Deregister from the yellow pages
        try {
            DFService.deregister(this);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        System.out.println(getAID().getName() + " terminating.");
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
                double tempValue = tempVol * tempPrice;
                offerOfBidders xx = new offerOfBidders(msg.getSender().getLocalName(), msg.getSender().toString(),tempVol, tempPrice,tempValue);
                bidderList.add(xx);
                reply.setPerformative(ACLMessage.PROPOSE);
                String sendingOffer = String.valueOf(sellerInfo.sellingVolume);
                reply.setContent(sendingOffer);
                myAgent.send(reply);
            } else {
                block();
            }
        }
    }

    private class ReceivedOrdersServer extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                .add(msg.getSender().getLocalName());
            } else {
                block();
            }
        }
    }

    private class PurchaseOrdersServer extends CyclicBehaviour{
        public void action(){
            List<offerOfBidders> prioritizedList;
            Collections.sort(bidderList);
        }
    }
    private class RejectandReset extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REFUSE);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                if (msg.getSender().getLocalName().equals(sellerInfo.offeredName)) {
                    sellerInfo.offeredName = "";
                    sellerInfo.offeredPrice = 0.0;
                    sellerInfo.offeredVolumn = 0.0;
                }
            } else {
                block();
            }
        }
    }

    public class agentInfo {
        String farmerName;
        String agentType;
        Double sellingVolume;
        Double sellingPrice;
        Double offeredPrice;
        Double offeredVolumn;
        String offeredName;

        agentInfo(String farmerName, String agentType, double sellingVolume, double sellingPrice, double offeredPrice, double offeredVolumn, String offeredName) {
            this.farmerName = farmerName;
            this.agentType = agentType;
            this.sellingVolume = sellingVolume;
            this.sellingPrice = sellingPrice;
            this.offeredPrice = offeredPrice;
            this.offeredVolumn = offeredVolumn;
            this.offeredName = offeredName;
        }
    }
    public class offerOfBidders {
        String name;
        String agentAddress;
        double volume;
        double price;
        double value;

        offerOfBidders(String name, String agentAddress, double volume, double price, double value){
            this.name = name;
            this.agentAddress = agentAddress;
            this.volume = volume;
            this.price = price;
            this.value = value;
        }
    }
}

