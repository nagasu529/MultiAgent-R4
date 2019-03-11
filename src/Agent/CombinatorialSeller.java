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
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Stream;

/**
 *
 * @author chiewchk
 */
public class CombinatorialSeller extends Agent {

    //The list of farmer who are seller (maps the water volumn to its based price)
    private CombinatorialSellerGUI myGui;
    CropTest calCrops = new CropTest();

    DecimalFormat df = new DecimalFormat("#.##");

    //Counting list (single negotiation process)
    int countTick;

    //Farmer information on each agent.
    agentInfo farmerInfo = new agentInfo("", "", 0.0,0.0, 10.0, 0.0, 0.0,"", 0);

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
                if (farmerInfo.agentType == "owner"||farmerInfo.agentType=="Farmer-owner") {
                    //Register the seller description service on yellow pages.
                    farmerInfo.agentType = "Farmer-owner";
                    farmerInfo.sellingPrice = 10;
                    sd.setType(farmerInfo.agentType);
                    sd.setName(getAID().getName());
                    farmerInfo.farmerName = getAID().getLocalName();

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
        //The list of known water selling agent
        private AID[] bidderAgent;
        private int repliesCnt; // The counter of replies from seller agents
        private MessageTemplate mt; // The template to receive replies
        ArrayList<String> bidderList = new ArrayList<String>();  //sorted list follows maximumprice factor.
        ArrayList<combinatorialList> buyerList = new ArrayList<combinatorialList>();    //result list for selling process reference.
        ArrayList<String> maxVolumnList = new ArrayList<String>();
        ArrayList<String> maxPriceList = new ArrayList<String>();
        //Creating dictionary for buyer volume and pricing
        Dictionary<String, Double> volumnDict = new Hashtable<String, Double>();
        Dictionary<String, Double> priceDict = new Hashtable<String, Double>();
        Object[] maxVolumnSell, maxPriceSell;
        Double maxVolumn = 0.0, maxPrice = 0.0;


        private String agentName;
        private double waterVolFromBidder;
        private double biddedPriceFromBidder;
        private int negotiateCnt;

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
                    cfp.setContent(String.valueOf(Double.toString(farmerInfo.sellingVolume) + "-"
                            + Double.toString((farmerInfo.sellingPrice))));
                    cfp.setConversationId("bidding");
                    cfp.setReplyWith("cfp"+System.currentTimeMillis()); // Unique value
                    myAgent.send(cfp);
                    System.out.println("cfp message :" + "\n" + cfp);
                    // Prepare the template to get proposals
                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("bidding"),
                            MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
                    step = 1;
                    break;

                case 1:
                    // Receive all proposals/refusals from bidder agents
                    //Sorted all offers based on price Per mm.
                    ACLMessage reply = myAgent.receive(mt);
                    if (reply != null) {
                        repliesCnt++;
                        // Reply received
                        if (reply.getPerformative() == ACLMessage.PROPOSE) {
                            System.out.println("Receive message: " + reply);
                            //Count number of bidder that is propose message for water price bidding.
                            // This is an offer
                            String biddedFromAcutioneer = reply.getContent();
                            String[] arrOfStr = biddedFromAcutioneer.split("-");
                            agentName = arrOfStr[0];
                            waterVolFromBidder = Double.parseDouble(arrOfStr[1]);
                            biddedPriceFromBidder = Double.parseDouble(arrOfStr[2]);
                            //adding data to dictionary
                            volumnDict.put(agentName,waterVolFromBidder);
                            priceDict.put(agentName,biddedPriceFromBidder);
                        }

                        if (repliesCnt >= bidderAgent.length) {

                            // We received all replies
                            for(Enumeration e = volumnDict.keys();e.hasMoreElements();){
                                String temp = e.nextElement().toString();
                                bidderList.add(temp);
                            }
                            String[] tempBidderList = GetStringArray(bidderList);

                            int index = tempBidderList.length - 1;
                            ArrayList<ArrayList<String> > powersetResult = getSubset(tempBidderList, index);
                            System.out.println(powersetResult);

                            //Loop and result calculation
                            for(int i=0; i < powersetResult.size();i++){
                                String xx = powersetResult.get(i).toString();
                                Double tempMaxVolumn = 0.0;
                                Double tempMaxPrice = 0.0;
                                for(int j=0; j< powersetResult.get(i).size();j++){
                                    tempMaxVolumn = tempMaxVolumn + volumnDict.get(powersetResult.get(i).get(j));
                                    double tempPrice = priceDict.get(powersetResult.get(i).get(j)) * volumnDict.get(powersetResult.get(i).get(j));
                                    tempMaxPrice = tempMaxPrice + tempPrice;
                                }

                                //Storing maximum volumn and price.
                                if(tempMaxVolumn > maxVolumn && tempMaxVolumn <= farmerInfo.sellingVolume){
                                    maxVolumn = tempMaxVolumn;
                                    maxVolumnSell = new String[]{xx,tempMaxVolumn.toString(),tempMaxPrice.toString()};
                                    maxVolumnList = powersetResult.get(i);
                                }
                                if(tempMaxPrice > maxPrice && tempMaxVolumn <= farmerInfo.sellingVolume){
                                    maxPrice = tempMaxPrice;
                                    maxPriceSell = new String[]{xx, tempMaxVolumn.toString(),tempMaxPrice.toString()};
                                    maxPriceList = powersetResult.get(i);
                                }
                                System.out.println("\n" + "result set is : " + powersetResult.get(i).toString()+"\n"
                                        +  "Volumn to sell is  " + tempMaxVolumn + "\n" + "Income is  " + tempMaxPrice);
                            }

                            step = 2;
                        }
                    }else {
                        block();
                    }
                    break;
                case 2:
                    /*
                     * calulating and adding accepted water volumn for bidder based on highest price.
                     * Sendding message to bidders wiht two types (Accept proposal or Refuse) based on
                     * accepted water volumn to sell.
                     */

                    myGui.displayUI("\n" + "Best solution for each case:"+"\n"+"Max volumn selling:  " + Arrays.toString(maxVolumnSell)+ "\n");
                    myGui.displayUI("\n" + "Best solution for each case:"+"\n"+"Max price selling:  " + Arrays.toString(maxPriceSell) + "\n");
                    myGui.displayUI("List of bidder for selling water based on offering price" + "\n");
                    for(int i=0; i < bidderAgent.length; ++i){
                        for (String e: maxVolumnList
                             ) {
                            if(bidderAgent[i].getLocalName().equals(e)){
                                // Send the purchase order to the seller that provided the best offer
                                ACLMessage acceptedRequest = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                                acceptedRequest.addReceiver(bidderAgent[i]);
                                acceptedRequest.setConversationId("bidding");
                                acceptedRequest.setReplyWith("acceptedRequest" + System.currentTimeMillis());
                                //myGui.displayUI(acceptedRequest.toString());
                                myAgent.send(acceptedRequest);
                                mt = MessageTemplate.and(MessageTemplate.MatchConversationId("bidding"),MessageTemplate.MatchInReplyTo
                                        (acceptedRequest.getReplyWith()));
                            }else {
                                //Refuse message prepairing
                                ACLMessage rejectedRequest = new ACLMessage(ACLMessage.REFUSE);
                                rejectedRequest.addReceiver(bidderAgent[i]);
                                //myGui.displayUI(rejectedRequest.toString());
                                myAgent.send(rejectedRequest);
                            }
                        }
                    }

                    step = 3;
                    break;

                case 3:
                    // Receive the purchase order reply
                    reply = myAgent.receive(mt);
                    if (reply != null) {
                        double soldVolumn = 0;
                        //System.out.println("\n" + "Reply message:" + reply.toString());
                        //myGui.displayUI("\n" + "Reply message:" + reply.toString());
                        // Purchase order reply received
                        if (reply.getPerformative() == ACLMessage.INFORM) {
                            System.out.println("accepted volumn from seller" + reply.getSender().getLocalName());
                            farmerInfo.sellingVolume = farmerInfo.sellingVolume - soldVolumn;
                            System.out.println("Water volumn left :  " + farmerInfo.sellingVolume);
                            // Purchase successful. We can terminate
                            //System.out.println(farmerInfo.farmerName +" successfully purchased from agent "+reply.getSender().getName() + "\n");
                            //System.out.println("Price = "+farmerInfo.currentPricePerMM);
                            //myGui.displayUI("\n" + farmerInfo.farmerName +" successfully purchased from agent "+reply.getSender().getName() +"\n");
                            //myGui.displayUI("Price = " + farmerInfo.currentPricePerMM);
                            myAgent.doSuspend();
                            //myGui.dispose();
                        }
                        else {
                            System.out.println("Attempt failed: requested water volumn already sold." + "\n");
                            myGui.displayUI("Attempt failed: requested water volumn already sold." + "\n");
                        }
                    }
                    else {
                        block();
                    }
                    break;
            }
        }
        public boolean done() {
            if (step == 4) {
                myGui.displayUI("\n" + getAID().getLocalName() + "sold all water" + "\n");
                myGui.displayUI(getAID().getLocalName() + "is Terminated");
                myAgent.doSuspend();

                //myGui.dispose();
                //myGui.displayUI("Attempt failed: do not have bidder now" + "\n");
            }
            return step == 0 ;
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
                    // The requested book has been sold to another buyer in the meanwhile.
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
        System.out.println(getAID().getName()+" terminating.");
    }

    public class agentInfo{
        String farmerName;
        String agentType;
        double buyingPricePerMM;
        double buyingVolumn;
        double sellingPrice;
        double sellingVolume;
        double currentLookingVolumn;
        String sellingStatus;
        double numBidder;

        agentInfo(String farmerName, String agentType,double buyingPricePerMM, double buyingVolumn, double sellingPrice, double sellingVolume, double currentLookingVolumn,
                  String sellingStatus, double numBidder){
            this.farmerName = farmerName;
            this.agentType = agentType;
            this.buyingPricePerMM = buyingPricePerMM;
            this.buyingVolumn = buyingVolumn;
            this.sellingPrice = sellingPrice;
            this.sellingVolume = sellingVolume;
            this.currentLookingVolumn = currentLookingVolumn;
            this.sellingStatus = sellingStatus;
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

    public void powerSet(double set[]){
        int n = set.length;

        // Run a loop for printing all 2^n
        // subsets one by obe
        for (int i = 0; i < (1<<n); i++)
        {
            System.out.print("{ ");

            // Print current subset
            for (int j = 0; j < n; j++)

                // (1<<j) is a number with j th bit 1
                // so when we 'and' them with the
                // subset number we get which numbers
                // are present in the subset and which
                // are not
                if ((i & (1 << j)) > 0)
                    System.out.print(set[j] + " ");

            System.out.println("}");
        }
    }

    public void xorSum(int arr[], int n) {

        int bits = 0;

        // Finding bitwise OR of all elements
        for (int i = 0; i < n; ++i)
            bits |= arr[i];

        int ans = bits * (int)Math.pow(2, n-1);
    }

    static ArrayList<ArrayList<String> > getSubset(String[] set, int index) {
        ArrayList<ArrayList<String> > allSubsets;
        if (index < 0) {
            allSubsets = new ArrayList<ArrayList<String> >();
            allSubsets.add(new ArrayList<String>());
        }

        else {
            allSubsets = getSubset(set, index - 1);
            String item = set[index];
            ArrayList<ArrayList<String> > moreSubsets
                    = new ArrayList<ArrayList<String> >();

            for (ArrayList<String> subset : allSubsets) {
                ArrayList<String> newSubset = new ArrayList<String>();
                newSubset.addAll(subset);
                newSubset.add(item);
                moreSubsets.add(newSubset);
            }
            allSubsets.addAll(moreSubsets);
        }
        return allSubsets;
    }

    // Function to convert ArrayList<String> to String[]
    public static String[] GetStringArray(ArrayList<String> arr)
    {
        // declaration and initialise String Array
        String str[] = new String[arr.size()];
        // ArrayList to Array Conversion
        for (int j = 0; j < arr.size(); j++) {
            // Assign each value to String array
            str[j] = arr.get(j);
        }
        return str;
    }
}
