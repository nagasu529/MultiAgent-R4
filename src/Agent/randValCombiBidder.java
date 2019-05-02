package Agent;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.text.DecimalFormat;

public class randValCombiBidder extends Agent {
    //The list of farmer who are seller (maps the water volumn to its based price)
    randValue randValue = new randValue();

    DecimalFormat df = new DecimalFormat("#.##");

    //Farmer information on each agent.
    agentInfo farmerInfo = new agentInfo("", "", randValue.getRandDoubleRange(500, 1200),0.0, randValue.getRandDoubleRange(12,16), "looking", 0.0, 0.0, randValue.getRandDoubleRange(5,12), "");

    //Global bidding parameter

    protected void setup() {
        System.out.println(getAID().getLocalName()+"  is ready" );

        //Start Agent
        // Register the book-selling service in the yellow pages
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        farmerInfo.agentType = "Farmer-auctioneer";
        farmerInfo.farmerName = getAID().getLocalName();
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
        //Bidding process.
        addBehaviour(new TickerBehaviour(this, 3000) {
            public void onTick() {

                if (farmerInfo.sellingStatus=="looking"){
                    //System.out.println("\n");
                    //System.out.println("Name: " + farmerInfo.farmerName + "\n");
                    //System.out.println("Status: " + farmerInfo.agentType + "\n");
                    //System.out.println("Total buying water needed: " + df.format(farmerInfo.buyingVolumn) + "\n");
                    //System.out.println("Water need currently " + df.format(farmerInfo.currentLookingVolumn) + "\n");
                    //System.out.println("Maximum buying price (per MM.) " + df.format(farmerInfo.buyingPricePerMM) + "\n");
                    //System.out.println("Selling / Buying stages " + farmerInfo.sellingStatus + "\n");
                    //System.out.println("Profit loss (%): " + farmerInfo.profitLossPct);
                    //System.out.println("\n");

                    /*
                     ** Bidding water process
                     */
                    //Add the behaviour serving queries from Water provider about current price.
                    addBehaviour(new OfferRequestsServer());

                    //Add the behaviour serving purhase orders from water provider agent.
                    addBehaviour(new PurchaseOrdersServer());
                }
            }
        });
        addBehaviour(new TickerBehaviour(this, 12000) {
            protected void onTick() {
                farmerInfo.offeredPrice = 0.0;
                farmerInfo.offeredVolumn = 0.0;
                farmerInfo.offeredName = "";
            }
        });
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

    private class OfferRequestsServer extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
            ACLMessage msg = myAgent.receive(mt);
            //String log = new String();
            //CFP Message received. Process it.
            if (msg != null) {
                ACLMessage reply = msg.createReply();
                //Price Per MM. and the number of volumn to sell from Seller.
                String currentOffer = msg.getContent();
                String[] arrOfstr = currentOffer.split("-");
                //myGUI.displayUI("Offer from Seller: " + currentOffer + "\n");
                double tempVol = Double.parseDouble(arrOfstr[0]);
                double tempPrice = Double.parseDouble(arrOfstr[1]);

                //myGUI.displayUI("Price setting up from Seller: " + farmerInfo.waterPriceFromSeller + " per MM" + "\n");
                //myGUI.displayUI("Selling volume from seller:" + farmerInfo.waterVolumnFromSeller + "\n");

                //Auction Process
                if (farmerInfo.offeredPrice <= farmerInfo.buyingPricePerMM) {
                    reply.setPerformative(ACLMessage.PROPOSE);
                    String sendingOffer = farmerInfo.farmerName + "-" + farmerInfo.buyingVolumn + "-" + farmerInfo.buyingPricePerMM + "-" + farmerInfo.profitLossPct;
                    //String sendingOffer = farmerInfo.buyingVolumn + "-" + farmerInfo.buyingPricePerMM;
                    reply.setContent(sendingOffer);
                    double tempValue = tempPrice * tempVol;
                    double tempMax = farmerInfo.offeredVolumn * farmerInfo.offeredPrice;

                    if(tempMax == 0 || tempValue < tempMax){
                        farmerInfo.offeredVolumn = tempVol;
                        farmerInfo.offeredPrice = tempPrice;
                        farmerInfo.offeredName = msg.getSender().getLocalName();
                    }

                    myAgent.send(reply);
                    //myGUI.displayUI("Sending Offer : " + reply.getContent() + "\n");
                    //myGUI.displayUI(log + "\n");
                } else {
                    reply.setPerformative(ACLMessage.REFUSE);
                    //reply.setContent(getAID().getName() + " is surrender");
                    myAgent.send(reply);
                    System.out.println(getAID().getName() + " is surrender");
                }
            } else {
                block();
            }
        }
    }

    private class PurchaseOrdersServer extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                //myGUI.displayUI("Accept Proposal Message: " + msg.toString() +"\n");
                // ACCEPT_PROPOSAL Message received. Process it
                ACLMessage reply = msg.createReply();
                reply.setPerformative(ACLMessage.INFORM);
                myAgent.send(reply);
                //water requirement for next round bidding.
                //myAgent.doDelete();
                myAgent.doSuspend();
                //myGUI.dispose();
                System.out.println(getAID().getName() + " terminating.");
            }else {
                block();
            }
        }
    }

    public void bidderInput(final Double buyingPrice, Double volumnToBuy, Double profitLossPct){

        addBehaviour(new OneShotBehaviour() {
            @Override
            public void action() {
                //input bidding information parameter
                farmerInfo.sellingStatus = "looking";
                farmerInfo.buyingPricePerMM = buyingPrice;
                farmerInfo.buyingVolumn = volumnToBuy;
                farmerInfo.profitLossPct = profitLossPct;
            }
        });
    }

    public class agentInfo{
        String farmerName;
        String agentType;
        double buyingVolumn;
        double currentLookingVolumn;
        double buyingPricePerMM;
        String sellingStatus;
        double offeredVolumn;
        double offeredPrice;
        String offeredName;
        double profitLossPct;

        agentInfo(String farmerName, String agentType, double buyingVolumn, double currentLookingVolumn,
                  double buyingPricePerMM, String sellingStatus, double offeredVolumn, double offeredPrice, double profitLossPct, String offeredName){
            this.farmerName = farmerName;
            this.agentType = agentType;
            this.buyingVolumn = buyingVolumn;
            this.currentLookingVolumn = currentLookingVolumn;
            this.buyingPricePerMM = buyingPricePerMM;
            this.sellingStatus = sellingStatus;
            this.offeredVolumn = offeredVolumn;
            this.offeredPrice = offeredPrice;
            this.offeredName = offeredName;
            this.profitLossPct = profitLossPct;
        }
    }
}