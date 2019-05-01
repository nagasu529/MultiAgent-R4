package Agent;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.text.DecimalFormat;
import java.util.*;

public class randValSealbidedSeller extends Agent {
    randValSealbidedSellerGUI myGui;

    //General papameter information
    DecimalFormat df = new DecimalFormat("#.##");
    randValue randValue = new randValue();
    agentInfo sellerInfo = new agentInfo("", "seller", randValue.getRandDoubleRange(10,12), randValue.getRandDoubleRange(5000,13000), 0, 0.0, "", "looking");
    //double minSellingValue = sellerInfo.sellingVolumn * sellerInfo.sellingPrice;
    double minSellingValue = 0;
    //Instant papameter for AID[]
    //Create list of offer.
    HashMap<String, Double> sortHm = new HashMap<String, Double>();



    protected void setup(){
        // Create and show the GUI
        myGui = new randValSealbidedSellerGUI(this);
        myGui.show();
        System.out.println(getAID().getLocalName() + " is ready");
        //Start agent
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("seller");
        sd.setName(getAID().getName());
        sellerInfo.farmerName = getAID().getName();
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

        System.out.println(sellerInfo.farmerName + "  is ready" + "\n" + "Stage is" + sellerInfo.agentType + "\n");

        //Add a TickerBehaviour that chooses agent status to buyer or seller.
        addBehaviour(new TickerBehaviour(this, 5000){
            protected void onTick() {
                myGui.displayUI("Name: " + sellerInfo.farmerName + "\n");
                myGui.displayUI("Status: " + sellerInfo.agentType + "\n");
                myGui.displayUI("Volumn to sell: " + sellerInfo.sellingVolumn + "\n");
                myGui.displayUI("Selling price: " + sellerInfo.sellingPrice + "\n");
                myGui.displayUI("Providing price" + "\n");
                myGui.displayUI("\n");

                /*
                 ** Selling water process
                 */
                addBehaviour(new RequestPerformer());
                // Add the behaviour serving purchase orders from buyer agents
                //addBehaviour(new PurchaseOrdersServer());
            }
        } );
    }

    private class RequestPerformer extends Behaviour {
        //The list of known water selling agent
        private AID[] bidderAgent;
        private int repliesCnt; // The counter of replies from seller agents
        private MessageTemplate mt; // The template to receive replies
        ArrayList<String> bidderList = new ArrayList<String>();  //sorted list follows maximumprice factor.
        //ArrayList<combinatorialList> buyerList = new ArrayList<combinatorialList>();    //result list for selling process reference.

        //Creating dictionary for buyer volume and pricing
        Dictionary<String, Double> volumnDict = new Hashtable<String, Double>();
        Dictionary<String, Double> priceDict = new Hashtable<String, Double>();
        Dictionary<String, Double> profitLossDict = new Hashtable<>();
        Object[] maxEuObj;
        Double maxEuValue = 0.0;
        ArrayList<String> maxEuList = new ArrayList<String>();

        private String agentName;
        private double waterVolFromBidder;
        private double biddedPriceFromBidder;
        private double profitLossPct;
        int countTick;
        int decisionRules = 0;

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
                            //System.out.println("tick time:" + countTick);
                        }
                    }
                    catch (FIPAException fe) {
                        fe.printStackTrace();
                    }
                    // Send the cfp to all sellers (Sending water volumn required to all bidding agent)
                    ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
                    for (int i = 0; i < bidderAgent.length; ++i) {
                        if (bidderAgent[i].getName().equals(sellerInfo.farmerName)== false) {
                            cfp.addReceiver(bidderAgent[i]);
                        }
                    }
                    cfp.setContent(String.valueOf(Double.toString(sellerInfo.sellingVolumn) + "-"
                            + Double.toString((sellerInfo.sellingPrice))));
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
                            profitLossPct = Double.parseDouble(arrOfStr[3]);
                            //adding data to dictionary
                            volumnDict.put(agentName,waterVolFromBidder);
                            priceDict.put(agentName,biddedPriceFromBidder);
                            profitLossDict.put(agentName, profitLossPct);
                        }

                        if (repliesCnt >= bidderAgent.length) {

                            // We received all replies
                            for(Enumeration e = volumnDict.keys(); e.hasMoreElements();){
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
                                Double tempMaxEuValue = 0.0;
                                Double tempMaxProfitLoss = 0.0;
                                for(int j=0; j< powersetResult.get(i).size();j++){
                                    tempMaxVolumn = tempMaxVolumn + volumnDict.get(powersetResult.get(i).get(j));
                                    tempMaxProfitLoss = tempMaxProfitLoss + profitLossDict.get(powersetResult.get(i).get(j));
                                    double tempPrice = priceDict.get(powersetResult.get(i).get(j)) * volumnDict.get(powersetResult.get(i).get(j));
                                    tempMaxPrice = tempMaxPrice + tempPrice;
                                }
                                if(decisionRules == 0){
                                    tempMaxEuValue = (0 * tempMaxVolumn) + (0.5 * tempMaxPrice) + (0 * tempMaxProfitLoss);
                                    if(tempMaxEuValue > maxEuValue && tempMaxVolumn <= sellerInfo.sellingVolumn){
                                        maxEuValue = tempMaxEuValue;
                                        maxEuObj = new String[]{xx, tempMaxVolumn.toString(),tempMaxPrice.toString(), tempMaxProfitLoss.toString()};
                                        maxEuList = powersetResult.get(i);
                                    }
                                }else if(decisionRules == 1){
                                    tempMaxEuValue = (0.5 * tempMaxVolumn) + (0 * tempMaxPrice) + (0 * tempMaxProfitLoss);
                                    if(tempMaxEuValue > maxEuValue && tempMaxVolumn <= sellerInfo.sellingVolumn){
                                        maxEuValue = tempMaxEuValue;
                                        maxEuObj = new String[]{xx, tempMaxVolumn.toString(),tempMaxPrice.toString(),tempMaxProfitLoss.toString()};
                                        maxEuList = powersetResult.get(i);
                                    }
                                }else if(decisionRules ==2){
                                    tempMaxEuValue = (0 * tempMaxVolumn) + (0 * tempMaxPrice) + (0.5 * tempMaxProfitLoss);
                                    if(tempMaxEuValue > maxEuValue && tempMaxVolumn <= sellerInfo.sellingVolumn){
                                        maxEuValue = tempMaxEuValue;
                                        maxEuObj = new String[]{xx, tempMaxVolumn.toString(),tempMaxPrice.toString(), tempMaxProfitLoss.toString()};
                                        maxEuList = powersetResult.get(i);
                                    }
                                }
                                else {
                                    tempMaxEuValue = (0.5 * tempMaxVolumn) + (0.5 * tempMaxPrice) + (0 * tempMaxProfitLoss);
                                    if(tempMaxEuValue > maxEuValue && tempMaxVolumn <= sellerInfo.sellingVolumn){
                                        maxEuValue = tempMaxEuValue;
                                        maxEuObj = new String[]{xx, tempMaxVolumn.toString(),tempMaxPrice.toString(), tempMaxProfitLoss.toString()};
                                        maxEuList = powersetResult.get(i);
                                    }
                                }

                                System.out.println("\n" + "result set is : " + powersetResult.get(i).toString()+"\n"
                                        +  "Volumn to sell is  " + tempMaxVolumn + "\n" + "Income is  " + tempMaxPrice + "\n" + "Total profit loss: " + tempMaxProfitLoss + "\n" );
                                System.out.println(decisionRules);
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
                    if(decisionRules == 0){
                        myGui.displayUI("\n" + "Best solution for each case:"+"\n"+"Max price selling:  " + Arrays.toString(maxEuObj) + "\n");
                    }else if(decisionRules == 1) {
                        myGui.displayUI("\n" + "Best solution for each case:"+"\n"+"Max volumn selling:  " + Arrays.toString(maxEuObj)+ "\n");
                    }else if(decisionRules == 2){
                        myGui.displayUI("\n" + "Best solution for each case:"+"\n"+"Max profit loss protection:  " + Arrays.toString(maxEuObj)+ "\n");
                    }else{
                        myGui.displayUI("\n" + "Best solution for each case:"+"\n"+"balancing between volumn and price:  " + Arrays.toString(maxEuObj)+ "\n");
                    }
                    System.out.println("\n" + "Best solution for each case:"+"\n"+"Max price selling:  " + Arrays.toString(maxEuObj) + "\n");

                    for(int i=0; i < bidderAgent.length; ++i){
                        for (String e: maxEuList
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
                            sellerInfo.sellingVolumn = sellerInfo.sellingVolumn - soldVolumn;
                            System.out.println("Water volumn left :  " + sellerInfo.sellingVolumn);
                            // Purchase successful. We can terminate
                            //System.out.println(farmerInfo.farmerName +" successfully purchased from agent "+reply.getSender().getName() + "\n");
                            //System.out.println("Price = "+farmerInfo.currentPricePerMM);
                            //myGui.displayUI("\n" + farmerInfo.farmerName +" successfully purchased from agent "+reply.getSender().getName() +"\n");
                            //myGui.displayUI("Price = " + farmerInfo.currentPricePerMM);
                            myAgent.doSuspend();
                            //myAgent.doDelete();
                            //myGui.dispose();
                        }
                        else {
                            System.out.println("Attempt failed: requested water volumn already sold." + "\n");
                            //myGui.displayUI("Attempt failed: requested water volumn already sold." + "\n");
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
                System.out.println("\n" + getAID().getLocalName() + "sold all water" + "\n");
                System.out.println(getAID().getLocalName() + "is Terminated");
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
                //myGui.displayUI(msg.toString());
                System.out.println(sellerInfo.sellingStatus);
                reply.setPerformative(ACLMessage.INFORM);
                if (sellerInfo.sellingStatus=="avalable") {
                    sellerInfo.sellingStatus = "sold";
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

    //All parameters and method for combinatorial auction process.
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


    // function to sort hashmap by values
    public static HashMap<String, Double> sortByValue(HashMap<String, Double> hm)
    {
        // Create a list from elements of HashMap
        List<Map.Entry<String, Double> > list =
                new LinkedList<Map.Entry<String, Double> >(hm.entrySet());

        // Sort the list
        Collections.sort(list, new Comparator<Map.Entry<String, Double> >() {
            public int compare(Map.Entry<String, Double> o1,
                               Map.Entry<String, Double> o2)
            {
                return (o1.getValue()).compareTo(o2.getValue());
            }
        });

        // put data from sorted list to hashmap
        HashMap<String, Double> temp = new LinkedHashMap<String, Double>();
        for (Map.Entry<String, Double> aa : list) {
            temp.put(aa.getKey(), aa.getValue());
        }
        return temp;
    }


    public class agentInfo{
        String farmerName;
        String agentType;
        Double sellingPrice;
        Double sellingVolumn;
        Double acceptedPrice;
        Double acceptedVolumn;
        String acceptedName;
        String sellingStatus;

        agentInfo(String farmerName, String agentType, double sellingPrice, double sellingVolumn, double acceptedPrice, double acceptedVolumn, String acceptedName, String sellingStatus){
            this.farmerName = farmerName;
            this.agentType = agentType;
            this.sellingPrice = sellingPrice;
            this.sellingVolumn = sellingVolumn;
            this.acceptedPrice = acceptedPrice;
            this.acceptedVolumn = acceptedVolumn;
            this.acceptedName = acceptedName;
            this.sellingStatus = sellingStatus;

        }
    }
}

/***
    //Advertise service reply
    private class AdvertiseServiceReply extends CyclicBehaviour{
        public void action(){
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                // CFP Message received. Process it
                String title = msg.getContent();
                ACLMessage reply = msg.createReply();
                reply.setPerformative(ACLMessage.PROPOSE);
                reply.setContent(sellerInfo.sellingVolumn + "-" + sellerInfo.sellingPrice + "-" + sellerInfo.agentType);
                myAgent.send(reply);
            }
            else {
                block();
            }
        }
    }

    private class PrioritizedOffers extends CyclicBehaviour{
        public void action(){
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
            ACLMessage msg = myAgent.receive(mt);
            if(msg != null){
                myGui.displayUI(msg.toString());
                String[] arrOfStr = msg.getContent().split("-");
                double tempVolumn = Double.parseDouble(arrOfStr[0]);
                double tempPrice = Double.parseDouble(arrOfStr[1]);
                String tempAgentStatus = arrOfStr[2];
                double tempValue = tempPrice * tempVolumn;
                if(tempValue > acceptedValue){
                    sellerInfo.sellingVolumn = tempVolumn;
                    sellerInfo.acceptedPrice = tempPrice;
                    acceptedBidder = msg.getSender();
                    myGui.displayUI(msg.getSender().getLocalName() + " proposed the highest Value offer now" + "\n");
                }
            }else {
                myGui.displayUI("null \n");
                block();
            }
        }
    }

    private class PurchaseOrdersServer extends CyclicBehaviour{
        public void action(){
            //Sending Inform and failure message to others.
            if(acceptedBidder != null){
                myGui.displayUI("xxxxxxxxxxxxxxxxxxxxxxxxxxx");
                for(int i = 0; i < agentList.length; i++){
                    if(agentList[i].getName().equals(acceptedBidder.getName()) == true){
                        ACLMessage reply = new ACLMessage(ACLMessage.INFORM);
                        reply.setContent("Accept");
                        myAgent.send(reply);
                    }else {
                        ACLMessage reply = new ACLMessage(ACLMessage.FAILURE);
                        reply.setContent("Reject");
                        myAgent.send(reply);
                    }
                }
            }else {
                myGui.displayUI("accepted Agent is null \n");
                block();
            }
        }
    }

***/

