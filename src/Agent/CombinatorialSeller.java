package Agent;

import Agent.CropTest.cropType;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.lang.reflect.Array;
import java.text.DecimalFormat;
import java.util.*;

/**
 *
 * @author chiewchk
 */
public class CombinatorialSeller extends Agent {

    //The list of farmer who are seller (maps the water volumn to its based price)
    private CombinatorialSellerGUI myGui;
    CropTest calCrops = new CropTest();

    DecimalFormat df = new DecimalFormat("#.##");

    //The list of known water selling agent
    private AID[] bidderAgent;

    //Counting list (single negotiation process)
    int countTick;

    //Farmer information on each agent.
    agentInfo farmerInfo = new agentInfo("", "", 0.0,10.0, 0.0, 0.0, 0.0,"", 0.0, 0.0, 0);

    //The list of information (buying or selling) from agent which include price and mm^3
    private HashMap catalogue = new HashMap();

    protected void setup(){
        System.out.println(getAID()+" is ready");

        //Creating catalogue and running GUI
        myGui = new CombinatorialSellerGUI(this);
        myGui.show();
        //Start agent

        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        farmerInfo.agentType = "Farmer";
        ServiceDescription sd = new ServiceDescription();
        sd.setType(farmerInfo.agentType);
        sd.setName(getAID().getName());
        farmerInfo.farmerName = getAID().getName();
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

        myGui.displayUI("Hello "+ farmerInfo.farmerName + "\n" + "Stage is " + farmerInfo.agentType + "\n");

        //Add a TickerBehaviour that chooses agent status to buyer or seller.
        addBehaviour(new TickerBehaviour(this, 50000){
            protected void onTick() {

                myGui.displayUI("Agent status is " + farmerInfo.agentType + "\n");
                if (farmerInfo.agentType=="owner"||farmerInfo.agentType=="Farmer-owner") {
                    //Register the seller description service on yellow pages.
                    farmerInfo.agentType = "Farmer-owner";
                    //farmerInfo.pricePerMM = 10;
                    sd.setType(farmerInfo.agentType);
                    sd.setName(getAID().getName());
                    farmerInfo.farmerName = getAID().getName();

                    myGui.displayUI("\n");
                    myGui.displayUI("Name: " + farmerInfo.farmerName + "\n");
                    myGui.displayUI("Status: " + farmerInfo.agentType + "\n");
                    myGui.displayUI("Volumn to sell: " + farmerInfo.sellingVolume + "\n");
                    myGui.displayUI("Selling price: " + farmerInfo.sellingPrice + "\n");
                    myGui.displayUI("Selling status: " + farmerInfo.sellingStatus + "\n");
                    myGui.displayUI("Providing price" + "\n");
                    myGui.displayUI("\n");
                    /*
                     ** Selling water process
                     */
                    addBehaviour(new RequestPerformer());
                    // Add the behaviour serving purchase orders from buyer agents
                    //addBehaviour(new PurchaseOrdersServer());
                }
            }
        } );
    }

    //Update input data from GUI which include water allocation on single farm.
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
                calCrops.calcWaterReduction(totalWaterReductionPctg);
                resultCal.append("\n");
                resultCal.append("Result:\n");
                resultCal.append("\n");

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
                resultCal.append("Total water requirement on farm: " + calCrops.totalWaterReq + "\n");
                resultCal.append("The water reduction requirement (%): " + actualRate + "\n");
                resultCal.append("Water volume to reduction: " + calCrops.totalWaterReductionReq + "\n");
                resultCal.append("Actual reduction on farm:" + calCrops.totalReduction + "\n");
                resultCal.append("Actual reducion (%):" + calCrops.resultReductionPct + "\n");


                if (calCrops.resultReductionPct >= actualRate) {
                    farmerInfo.agentType = "owner";
                    resultCal.append("Selling water volumn: " + calCrops.waterVolToMarket);
                    farmerInfo.sellingVolume = calCrops.waterVolToMarket;
                }else {
                    farmerInfo.agentType = "owner";
                    resultCal.append("Buying water volumn: " + calCrops.waterVolToMarket);
                    farmerInfo.buyingVolumn = calCrops.waterVolToMarket;
                }

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
        } );
    }
    /*
     * 	Request performer
     *
     * 	This behaviour is used by buyer mechanism to request seller agents for water pricing ana selling capacity.
     */
    private class RequestPerformer extends Behaviour {
        private int repliesCnt; // The counter of replies from seller agents
        private MessageTemplate mt; // The template to receive replies
        ArrayList<Double> order = new ArrayList<Double>();  //sorted list follows maximumprice factor.
        ArrayList<combinatorialList> buyerList = new ArrayList<combinatorialList>();    //result list for selling process reference.
        private String agentName;
        private double waterVolFromBidder;
        private double biddedPriceFromBidder;
        int proposeCnt, refuseCnt;


        private int step = 0;

        public void action() {
            switch (step) {
                case 0:

                    //update bidder list
                    DFAgentDescription template = new DFAgentDescription();
                    ServiceDescription sd = new ServiceDescription();
                    sd.setType("bidder");
                    template.addServices(sd);
                    try {
                        DFAgentDescription[] result = DFService.search(myAgent, template);
                        if(result.length > 1){
                            countTick = countTick+1;
                        }
                        System.out.println("Found acutioneer agents:");
                        bidderAgent = new AID[result.length];
                        for (int i = 0; i < result.length; ++i) {
                            bidderAgent[i] = result[i].getName();
                            System.out.println(bidderAgent[i].getName());
                            System.out.println("tick time:" + countTick);
                        }
                    }
                    catch (FIPAException fe) {
                        fe.printStackTrace();
                    }

                    // Send the cfp to all sellers (Sending water volumn required to all bidding agent)
                    ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
                    for (int i = 0; i < bidderAgent.length; ++i) {
                        if (bidderAgent[i].getName().equals(farmerInfo.farmerName)== false) {
                            cfp.addReceiver(bidderAgent[i]);
                        }
                    }
                    cfp.setContent(String.valueOf(Double.toString(farmerInfo.waterVolumnFromSeller) + "-"
                            + Double.toString((farmerInfo.waterPriceFromSeller))));
                    cfp.setConversationId("bidding");
                    cfp.setReplyWith("cfp"+System.currentTimeMillis()); // Unique value
                    myAgent.send(cfp);
                    System.out.println("cfp message :" + "\n" + cfp);
                    // Prepare the template to get proposals
                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("bidding"),
                            MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
                    step = 1;
                    System.out.println(step);
                    break;

                case 1:
                    // Receive all proposals/refusals from bidder agents
                    //Sorted all offers based on pricePermm.
                    ACLMessage reply = myAgent.receive(mt);
                    if (reply != null) {
                        repliesCnt++;
                        // Reply received
                        if (reply.getPerformative() == ACLMessage.PROPOSE) {
                            proposeCnt++;
                            System.out.println("Receive message: " + reply);
                            //Count number of bidder that is propose message for water price bidding.
                            // This is an offer
                            String biddedFromAcutioneer = reply.getContent();
                            String[] arrOfStr = biddedFromAcutioneer.split("-");
                            agentName = arrOfStr[0];
                            waterVolFromBidder = Double.parseDouble(arrOfStr[1]);
                            biddedPriceFromBidder = Double.parseDouble(arrOfStr[2]);

                            //adding price to sort and sorted agent offers with price
                            order.add(biddedPriceFromBidder);
                            Collections.sort(order);
                            int x = order.indexOf(biddedPriceFromBidder);
                            combinatorialList xx = new combinatorialList(agentName, waterVolFromBidder, biddedPriceFromBidder, 0);
                            buyerList.add(x,xx);
                        }

                        if (repliesCnt >= bidderAgent.length) {
                            // We received all replies
                            step = 2;
                            System.out.println(step);
                        }
                    }else {
                        block();
                    }
                    break;
                case 2:
                    //compare the price and cheking with water valumn to sell
                    //Prepairing the selling list (top list for sell water) and preparing accept proposal message

                    // Send the purchase order to the seller that provided the best offer
                    ACLMessage acceptedRequest = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);

                    //Refuse message prepairing
                    ACLMessage rejectedRequest = new ACLMessage(ACLMessage.REFUSE);

                    Iterator itr = buyerList.iterator();
                    while (itr.hasNext()){
                        combinatorialList bl = (combinatorialList) itr.next();
                        bl.receivedWaterFromSeller = farmerInfo.sellingVolume - farmerInfo.buyingVolumn;
                        if(bl.receivedWaterFromSeller < 0){
                            bl.receivedWaterFromSeller = bl.waterVolume + bl.receivedWaterFromSeller;
                            farmerInfo.sellingVolume = 0;
                        }
                        for(int i=0; i < bidderAgent.length; ++i){
                            if(bl.receivedWaterFromSeller > 0 && bidderAgent[i].getName().equals(bl.agentName)){
                                acceptedRequest.addReceiver(bidderAgent[i]);
                                acceptedRequest.setContent(String.valueOf(bl.receivedWaterFromSeller));
                                acceptedRequest.setConversationId("bidding");
                                acceptedRequest.setReplyWith("acceptedRequest" + System.currentTimeMillis());
                                myAgent.send(acceptedRequest);
                            }else {
                                rejectedRequest.addReceiver(bidderAgent[i]);
                            }
                        }
                        myGui.displayUI(bl.agentName + " " + bl.pricePerMM + " " + bl.receivedWaterFromSeller);

                    }

                    step = 3;
                    break;

                case 3:

                    break;
                case 4:

                    break;
            }
        }

        public boolean done() {
            if (step == 2 && bestBidder == null) {
                //System.out.println("Attempt failed: "+volumeToBuy+" not available for sale");
                myGui.displayUI("Attempt failed: do not have bidder now".toString());
            }
            return ((step == 2 && bestBidder == null) || step == 5);
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
                myGui.displayUI(msg.toString());
                System.out.println(farmerInfo.sellingStatus);
                reply.setPerformative(ACLMessage.INFORM);
                if (farmerInfo.sellingStatus=="avalable") {
                    farmerInfo.sellingStatus = "sold";
                    //System.out.println(getAID().getName()+" sold water to agent "+msg.getSender().getName());
                    myGui.displayUI(getAID().getLocalName()+" sold water to "+msg.getSender().getLocalName());
                    //myGui.displayUI(farmerInfo.sellingStatus.toString());
                    //System.out.println(farmerInfo.sellingStatus);
                    doSuspend();
                } else {
                    // The requested book has been sold to another buyer in the meanwhile .
                    reply.setPerformative(ACLMessage.FAILURE);
                    reply.setContent("not-available for sale");
                    myGui.displayUI("not avalable to sell");
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
        System.out.println("Seller-agent "+getAID().getName()+" terminating.");
    }

    public class agentInfo{
        String farmerName;
        String agentType;
        double buyingVolumn;
        double sellingPrice;
        double sellingVolume;
        double currentLookingVolumn;
        double buyingPricePerMM;
        String sellingStatus;
        double waterVolumnFromSeller;
        double waterPriceFromSeller;
        double numBidder;

        agentInfo(String farmerName, String agentType, double buyingVolumn, double sellingPrice, double sellingVolume, double currentLookingVolumn,
                  double buyingPricePerMM, String sellingStatus, double waterVolumnFromSeller, double waterPriceFromSeller, double numBidder){
            this.farmerName = farmerName;
            this.agentType = agentType;
            this.buyingVolumn = buyingVolumn;
            this.sellingPrice = sellingPrice;
            this.sellingVolume = sellingVolume;
            this.currentLookingVolumn = currentLookingVolumn;
            this.buyingPricePerMM = buyingPricePerMM;
            this.sellingStatus = sellingStatus;
            this.waterVolumnFromSeller = waterVolumnFromSeller;
            this.waterPriceFromSeller = waterPriceFromSeller;
            this.numBidder = numBidder;
        }
    }

    public class combinatorialList{
        String agentName;
        double waterVolume;
        double pricePerMM;
        double receivedWaterFromSeller;

        combinatorialList(String agentName, double waterVolume, double pricePerMM, double receivedWaterFromSeller){
            this.agentName = agentName;
            this.waterVolume = waterVolume;
            this.pricePerMM = pricePerMM;
            this.receivedWaterFromSeller = receivedWaterFromSeller;
        }
    }
}
