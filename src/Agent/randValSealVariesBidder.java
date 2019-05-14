package Agent;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.text.DecimalFormat;

public class randValSealVariesBidder extends Agent {
    randValue randValue = new randValue();
    DecimalFormat df = new DecimalFormat("#.##");

    agentInfo bidderInfo = new agentInfo("","bidder", randValue.getRandDoubleRange(10,16), randValue.getRandDoubleRange(300,2000),0.0, 0.0, "", 0, 0, "");

    //Instant best seller for the ACCEPT_PROPOSAL message.
    int cnt = 0;

    double fiveHundredVol;
    int fiveHundredVolFreq;
    double varieVol;
    int varieVolFreq;

    protected void setup() {
        System.out.println(getAID().getLocalName()+"  is ready" );

        //Selling volume splited by conditions (each grounp is not over 500 mm^3).
        if(bidderInfo.buyingVol > 1000 && bidderInfo.buyingVol <= 1500){
            fiveHundredVol = 500;
            fiveHundredVolFreq = 2;
            varieVol = bidderInfo.buyingVol - 1000;
            varieVolFreq = 1;
        }

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

                double tempFiveHundredVol = Double.parseDouble(arrOfstr[0]);
                double tempFiveHunderedFreq = Double.parseDouble(arrOfstr[1]);
                double tempVarieVol = Double.parseDouble(arrOfstr[2]);
                double tempVarieFreq = Double.parseDouble(arrOfstr[3]);

                System.out.println("Offer price and Vol:  " + tempFiveHundredVol + "  " + tempFiveHunderedFreq + "   " + tempVarieVol + "  " + tempVarieFreq + "  " + bidderInfo.buyingPrice);

                //myGUI.displayUI("Price setting up from Seller: " + farmerInfo.waterPriceFromSeller + " per MM" + "\n");
                //myGUI.displayUI("Selling volume from seller:" + farmerInfo.waterVolumnFromSeller + "\n");

                //Auction Process
                if((tempFiveHundredVol == 500 && tempFiveHunderedFreq == 2)&& tempVarieVol > varieVol){
                    reply.setPerformative(ACLMessage.PROPOSE);
                    reply.setContent(fiveHundredVol + "-" + fiveHundredVolFreq + "-" + varieVol + "-" + varieVolFreq + bidderInfo.buyingPrice);
                    if(bidderInfo.offeredVarieVol == 0 || bidderInfo.offeredVarieVol > tempVarieVol){
                        bidderInfo.offeredVarieVol = tempVarieVol;
                        bidderInfo.offeredVarieName = msg.getSender().getLocalName();
                        System.out.println("the best option is changed  " + bidderInfo.offeredVarieName + "  " + bidderInfo.offeredVarieVol);
                    }
                    myAgent.send(reply);
                }else {
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
                if(msg.getSender().getLocalName().equals(bidderInfo.offeredVarieName)){
                    bidderInfo.offeredVarieName = "";
                    bidderInfo.offeredVariePrice = 0.0;
                    bidderInfo.offeredVarieVol = 0.0;
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
                if(msg.getSender().getLocalName().equals(bidderInfo.offeredVarieName) && bidderInfo.offeredVarieName != null){
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
        Double buyingVol;
        Double offeredPriceFiveHundred;
        Double offeredVolFiveHundred;
        String offeredNameFiveHundred;
        Double offeredVariePrice;
        Double offeredVarieVol;
        String offeredVarieName;


        agentInfo(String farmerName, String agentType, double buyingPrice, double buyingVol, double offeredPriceFiveHundred, double offeredVolFiveHundred, String offerNameFiveHundred, double offeredVariePrice, double offeredVarieVol, String offeredVarieName){
            this.farmerName = farmerName;
            this.agentType = agentType;
            this.buyingPrice = buyingPrice;
            this.buyingVol = buyingVol;
            this.offeredPriceFiveHundred = offeredPriceFiveHundred;
            this.offeredVolFiveHundred = offeredVolFiveHundred;
            this.offeredNameFiveHundred = offerNameFiveHundred;
            this.offeredVariePrice = offeredVariePrice;
            this.offeredVarieVol = offeredVarieVol;
            this.offeredVarieName = offeredVarieName;
        }
    }
}
