package Agent;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import Agent.Crop.cropType;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

import java.text.DecimalFormat;
import java.util.*;
/**
 *
 * @author chiewchk
 */

public class DirectAgent extends Agent {
    private DirectAgentUI myGui;
    Crop calCrops = new Crop();

    //Ser agent status after calculating water reduction on farm.
    String agentStatus;
    double volumeToSell;
    double volumeToBuy;
    double sellingPrice;
    double buyingPrice;
    DecimalFormat df = new DecimalFormat("#,##");

    //The list of known water selling agent
    private AID[] sellerAgent;

    //Farmer information on each agent.
    agentInfo farmerInfo = new agentInfo("", "", 0.0, 0.0, "avalable",0.0,0.0,
            0.0,0.0,0.0,0);

    //The list of information (buying or selling) from agent which include price and mm^3
    private HashMap catalogue = new HashMap();

    protected void setup() {
        System.out.println(getAID() + " is ready");

        //Creating catalogue and running GUI
        myGui = new DirectAgentUI(this);
        myGui.show();

        //Start agent and register all service.
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        farmerInfo.agentType = "Farmer";
        sd.setType(farmerInfo.agentType);
        sd.setName(getAID().getName());
        farmerInfo.farmerName = getAID().getName();
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

        myGui.displayUI("Hello " + getAID().getName() + "\n" + "Stage is " + sd.getType() + "\n");

        addBehaviour(new TickerBehaviour(this, 10000) {
            protected void onTick() {

                myGui.displayUI("Agent status is " + farmerInfo.agentType + "\n");
                if (farmerInfo.agentType=="seller"||farmerInfo.agentType=="Farmer-seller") {
                    //Register the seller description service on yellow pages.
                    farmerInfo.agentType = "Farmer-seller";
                    sd.setType(farmerInfo.agentType);
                    farmerInfo.pricePerMM = 300;
                    //farmerInfo.sellingStatus = "available";
                    dfd.addServices(sd);
                    myGui.displayUI("\n");
                    myGui.displayUI("Name: " + farmerInfo.farmerName + "\n");
                    myGui.displayUI("Status: " + farmerInfo.agentType + "\n");
                    myGui.displayUI("Volumn to sell: " + farmerInfo.waterVolumn + "\n");
                    myGui.displayUI("Selling price: " + farmerInfo.pricePerMM + "\n");
                    myGui.displayUI("Selling status: " + farmerInfo.sellingStatus + "\n");
                    myGui.displayUI("Preparing to sell" + "\n");
                    myGui.displayUI("\n");

                    /*
                     ** Selling water process
                     */

                    addBehaviour(new OfferRequestsServer());

                    // Add the behaviour serving purchase orders from buyer agents
                    addBehaviour(new PurchaseOrdersServer());

                } else if(farmerInfo.agentType=="buyer"||farmerInfo.agentType=="Farmer-buyer"){
                    farmerInfo.agentType = "Farmer-buyer";
                    sd.setType(farmerInfo.agentType);
                    farmerInfo.pricePerMM = 300;
                    farmerInfo.waterVolumn = 3935.868;
                    farmerInfo.sellingStatus = "unknown";
                    myGui.displayUI("\n");
                    myGui.displayUI("Name: " + farmerInfo.farmerName + "\n");
                    myGui.displayUI("Status: " + farmerInfo.agentType + "\n");
                    myGui.displayUI("Volumn to sell: " + farmerInfo.waterVolumn + "\n");
                    myGui.displayUI("Selling price: " + farmerInfo.pricePerMM + "\n");
                    myGui.displayUI("Selling status: " + farmerInfo.sellingStatus + "\n");
                    myGui.displayUI("Looking to buy water" + "\n");
                    myGui.displayUI("\n");

                    /*
                     ** Buying water process
                     */

                    //update seller list
                    DFAgentDescription template = new DFAgentDescription();
                    ServiceDescription sd = new ServiceDescription();
                    sd.setType("Farmer");
                    template.addServices(sd);
                    try {
                        DFAgentDescription[] result = DFService.search(myAgent, template);
                        System.out.println("Found the following seller agents:");
                        sellerAgent = new AID[result.length];
                        for (int i = 0; i < result.length; ++i) {
                            sellerAgent[i] = result[i].getName();
                            System.out.println(sellerAgent[i].getName());
                        }
                    }
                    catch (FIPAException fe) {
                        fe.printStackTrace();
                    }
                    addBehaviour(new RequestPerformer());
                }
            }
        } );
    }

    public void farmerInput(final String filenameGlob, final Double actualRate, final int etSeason) {
        StringBuilder resultCal = new StringBuilder();

        addBehaviour(new OneShotBehaviour() {
            public void action() {

                //Input parameters from GUI
                calCrops.readText(filenameGlob);
                double totalWaterReductionPctg = actualRate/100;
                //Choosing ET0 from database.
                switch(etSeason){
                    case 0:
                        calCrops.ET0Spring();

                        break;
                    case 1:
                        calCrops.ET0Summer();

                        break;
                    case 2:
                        calCrops.ET0Autumn();

                        break;
                    default:
                        calCrops.ET0Winter();

                }
                calCrops.ET = calCrops.avgET0;
                calCrops.farmFactorValues();
                double actualReduction = calCrops.calcWaterReduction(totalWaterReductionPctg);
                resultCal.append("\n");
                resultCal.append("Water reduction result:\n");
                resultCal.append("\n");
                resultCal.append("Actual reducion is:" + actualReduction + "\n");
                //myGui.displayUI(xx.toString());

                //Result calculation
                Iterator itrR=calCrops.resultList.iterator();
                while (itrR.hasNext()) {
                    cropType st = (cropType)itrR.next();
                    /*System.out.println(st.cropName + " " + st.cropStage +
                        " " + st.droubhtSensitivity + " " + st.dsValue + " " + st.stValue + " " + st.cvValue +
                        " " + st.literPerSecHec + " " + st.waterReq + " " + st.cropCoefficient + " " + st.waterReduction);*/
                    resultCal.append(st.cropName + " " + st.cropStage +
                            " " + st.droubhtSensitivity + " " + df.format(st.dsValue) + " " + df.format(st.stValue) + " " + df.format(st.cvValue) +
                            " " + df.format(st.literPerSecHec) + " " + df.format(st.waterReq) + " " + df.format(st.soilWaterContainValue) + " " + df.format(st.waterReqWithSoil) +
                            " " + df.format(st.cropCoefficient) + " " + df.format(st.waterReduction) + " " + df.format(st.productValueLost) + "\n");
                }
                //System.out.println("Actual reduction is: " + actualReduction);
                resultCal.append("Actual reduction is: " + actualReduction + "\n");
                resultCal.append("\n");

                if (actualReduction >= (calCrops.totalWaterReq*totalWaterReductionPctg)) {
                    farmerInfo.agentType = "seller";
                    farmerInfo.waterVolumn = actualReduction;
                    myGui.displayUI(resultCal.toString());
                    //Clean parameter
                    calCrops.resultList.clear();
                    calCrops.calList.clear();
                    calCrops.cropT.clear();
                    calCrops.cv.clear();
                    calCrops.ds.clear();
                    calCrops.order.clear();
                    calCrops.st.clear();
                }
            }
        } );
    }

    /*
     *	OfferRequestsSerer
     *	This behaviour is used b Seller mechanism for water buying request form other agent.
     *	If the requested water capacity and price match with buyer, the seller replies with a PROPOSE message specifying the price.
     *	Otherwise a REFUSE message is send back.
     * and PurchaseOrderServer is required by agent when the agent status is "Seller"
     */
    private class OfferRequestsServer extends CyclicBehaviour {
        public void action() {

            //Register service to DFDAgent

            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
            ACLMessage msg = myAgent.receive(mt);
            String log = new String();
            if (msg != null) {
                // CFP Message received. Process it
                //String title = msg.getContent();
                ACLMessage reply = msg.createReply();

                //String sellingStatus = (String) farmerInfo.sellingStatus;

                if (farmerInfo.sellingStatus== "avalable") {
                    // The requested water is available for sale. Reply with the price
                    reply.setPerformative(ACLMessage.PROPOSE);
                    reply.setContent(String.valueOf(farmerInfo.waterVolumn));
                } else {
                    // The requested water is NOT available for sale.
                    reply.setPerformative(ACLMessage.REFUSE);
                    reply.setContent("do not water for sale");
                }
                myAgent.send(reply);
                myGui.displayUI(log + "\n");
            }else {
                block();
            }
        }
    }

    /*
     * 	PurchaseOrderServer
     * 	This behaviour is used by Seller agent to serve incoming offer acceptances (purchase orders) from buyer.
     * 	The seller agent will remove selling list and replies with an INFORM message to notify the buyer that purchase has been
     * 	successfully complete.
     */

    private class PurchaseOrdersServer extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                // ACCEPT_PROPOSAL Message received. Process it
                ACLMessage reply = msg.createReply();
                reply.setPerformative(ACLMessage.INFORM);
                if (farmerInfo.sellingStatus=="avalable") {
                    farmerInfo.sellingStatus = "sold";
                    //System.out.println(getAID().getName()+" sold water to agent "+msg.getSender().getName());
                    myGui.displayUI(getAID().getLocalName()+" sold water to agent "+msg.getSender().getLocalName());
                    //myGui.displayUI(farmerInfo.sellingStatus.toString());
                    //System.out.println(farmerInfo.sellingStatus);
                    //doSuspend();
                } else {
                    // The requested book has been sold to another buyer in the meanwhile .
                    reply.setPerformative(ACLMessage.FAILURE);
                    reply.setContent("not-available for sale");
                    myGui.displayUI("not avalable to sell");
                }
                myAgent.send(reply);
                doSuspend();
            }else {
                block();
            }
        }
    }

    /*
     * 	Request performer
     *
     * 	This behaviour is used by buyer mechanism to request seller agents for water pricing ana selling capacity.
     */
    private class RequestPerformer extends Behaviour {
        private AID bestSeller; // The agent who provides the best offer
        private double bestPrice;  // The best offered price
        private int repliesCnt = 0; // The counter of replies from seller agents
        private MessageTemplate mt; // The template to receive replies
        private int step = 0;

        public void action() {
            switch (step) {
                case 0:
                    // Send the cfp to all sellers
                    ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
                    for (int i = 0; i < sellerAgent.length; ++i) {
                        cfp.addReceiver(sellerAgent[i]);
                    }
                    cfp.setContent(String.valueOf(farmerInfo.waterVolumn));
                    cfp.setConversationId("water-trade");
                    cfp.setReplyWith("cfp"+System.currentTimeMillis()); // Unique value
                    myAgent.send(cfp);
                    // Prepare the template to get proposals
                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("water-trade"),
                            MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
                    step = 1;
                    System.out.println(step);
                    break;
                case 1:
                    // Receive all proposals/refusals from seller agents
                    ACLMessage reply = myAgent.receive(mt);
                    if (reply != null) {
                        // Reply received
                        if (reply.getPerformative() == ACLMessage.PROPOSE) {
                            // This is an offer
                            double volumn = Double.parseDouble(reply.getContent());
                            if (bestSeller == null || volumn < bestPrice) {

                                // This is the best offer at present
                                bestPrice = volumn;
                                //System.out.println(volumn);
                                bestSeller = reply.getSender();
                            }
                        }
                        repliesCnt++;
                        System.out.println("Best seller is " + bestSeller);
                        System.out.println("Volumn to sell is :" + bestPrice);
                        if (repliesCnt >= sellerAgent.length-1) {
                            // We received all replies

                            step = 2;
                            System.out.println(step);
                        }
                    }else {
                        block();
                    }
                    break;
                case 2:
                    // Send the purchase order to the seller that provided the best offer
                    ACLMessage order = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                    order.addReceiver(bestSeller);
                    order.setContent(String.valueOf(farmerInfo.pricePerMM));
                    order.setConversationId("water-trade");
                    order.setReplyWith("order"+System.currentTimeMillis());
                    myAgent.send(order);
                    // Prepare the template to get the purchase order reply
                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("water-trade"),
                            MessageTemplate.MatchInReplyTo(order.getReplyWith()));

                    step = 3;
                    System.out.println(step);
                    break;
                case 3:
                    // Receive the purchase order reply
                    reply = myAgent.receive(mt);
                    if (reply != null) {
                        // Purchase order reply received
                        if (reply.getPerformative() == ACLMessage.INFORM) {
                            // Purchase successful. We can terminate
                            System.out.println(farmerInfo.farmerName +" successfully purchased from agent "+reply.getSender().getName());
                            System.out.println("Price = "+bestPrice);
                            myGui.displayUI(farmerInfo.farmerName +" successfully purchased from agent "+reply.getSender().getName().toString());
                            myGui.displayUI("Price = " + bestPrice);
                            doSuspend();
                            //myAgent.doDelete();
                        }
                        else {
                            System.out.println("Attempt failed: requested water volumn already sold.");
                            myGui.displayUI("Attempt failed: requested water volumn already sold.");
                        }

                        step = 4;
                    }
                    else {
                        block();
                    }
                    break;
            }
        }

        public boolean done() {
            if (step == 2 && bestSeller == null) {
                System.out.println("Attempt failed: "+volumeToBuy+" not available for sale");
                myGui.displayUI("Attempt failed: "+ volumeToBuy +" not available for sale".toString());
            }
            return ((step == 2 && bestSeller == null) || step == 4);
        }
    }

    protected void takeDown(){
        try {
            DFService.deregister(this);
        }catch (FIPAException fe){
            fe.printStackTrace();
        }
    }

    public class agentInfo{
        String farmerName;
        String agentType;
        double waterVolumn;
        double pricePerMM;
        String sellingStatus;
        double minPricePerMM;
        double maxPricePerMM;
        double currentPricePerMM;
        double bidedPrice;
        double previousPrice;
        int numBidder;

        agentInfo(String farmerName, String agentType, double waterVolumn, double pricePerMM, String sellingStatus, double minPricePerMM, double maxPricePerMM,
                  double currentPricePerMM, double biddedPrice, double previousPrice, int numBidder){
            this.farmerName = farmerName;
            this.agentType = agentType;
            this.waterVolumn = waterVolumn;
            this.pricePerMM = pricePerMM;
            this.sellingStatus = sellingStatus;
            this.minPricePerMM = minPricePerMM;
            this.maxPricePerMM = maxPricePerMM;
            this.currentPricePerMM = currentPricePerMM;
            this.bidedPrice = biddedPrice;
            this.previousPrice = previousPrice;
            this.numBidder = numBidder;
        }
    }
}
