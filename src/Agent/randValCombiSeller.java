package Agent;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.text.DecimalFormat;
import java.util.*;

public class randValCombiSeller extends Agent{
    randValCombiSellerGUI myGUI;


    //General arameters prepairation.
    DecimalFormat df = new DecimalFormat("#.##");
    private int decisionRule;
    randValue randValue = new randValue();
    agentInfo farmerInfo = new agentInfo("", randValue.getRandDoubleRange(10,12), randValue.getRandDoubleRange(1300,1500));
    int countTick;
    int decisionRules = 1;

    //Seting up and starting agent.
    protected void setup(){
        // Create and show the GUI

        myGUI = new randValCombiSellerGUI(this);
        myGUI.show();
        System.out.println(getAID().getLocalName() + " is ready");
        //Start agent
        DFAgentDescription dfd = new DFAgentDescription();
        farmerInfo.farmerName = getAID().getLocalName();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("seller");
        sd.setName(getAID().getName());
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

        System.out.println(farmerInfo.farmerName + "  is ready" + "\n");

        //Add a TickerBehaviour that chooses agent status to buyer or seller.
        addBehaviour(new TickerBehaviour(this, 15000){
            protected void onTick() {
                myGUI.displayUI("Name: " + farmerInfo.farmerName + "\n");
                //myGUI.displayUI("Status: " + farmerInfo.agentType + "\n");
                myGUI.displayUI("Volumn to sell: " + farmerInfo.sellingVolume + "\n");
                myGUI.displayUI("Selling price: " + farmerInfo.sellingPrice + "\n");
                myGUI.displayUI("\n");

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
        private int refuseCnt;
        private int repliesCnt; // The counter of replies from seller agents
        private MessageTemplate mt; // The template to receive replies
        ArrayList<String> bidderList = new ArrayList<String>();  //sorted list follows maximumprice factor.
        ArrayList<Agents> bidderInfo = new ArrayList<>();
        //ArrayList<combinatorialList> buyerList = new ArrayList<combinatorialList>();    //result list for selling process reference.

        //Creating dictionary for buyer volume and pricing
        Dictionary<String, Double> volumnDict = new Hashtable<String, Double>();
        Dictionary<String, Double> priceDict = new Hashtable<String, Double>();
        Dictionary<String, Double> profitLossDict = new Hashtable<>();
        Object[] maxEuObj;
        Double maxEuValue = 0.0;
        ArrayList<String> maxEuList = new ArrayList<String>();

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
                        if (bidderAgent[i].getName().equals(farmerInfo.farmerName)== false) {
                            cfp.addReceiver(bidderAgent[i]);
                        }
                    }
                    cfp.setContent(String.valueOf(Double.toString(farmerInfo.sellingVolume)));
                    cfp.setConversationId("bidding");
                    cfp.setReplyWith("cfp"+System.currentTimeMillis()); // Unique value
                    myAgent.send(cfp);
                    //myGUI.displayUI("cfp message :" + "\n" + cfp);
                    // Prepare the template to get proposals
                    step = 1;
                    break;

                case 1:
                    // Receive all proposals/refusals from bidder agents
                    //Sorted all offers based on price Per mm.
                    ACLMessage reply = myAgent.receive(mt);
                    if (reply != null) {
                        //myGUI.displayUI(reply.toString());
                        repliesCnt++;
                        // Reply received
                        if (reply.getPerformative() == ACLMessage.PROPOSE) {
                            System.out.println("Receive message: " + reply);
                            //Count number of bidder that is propose message for water price bidding.
                            // This is an offer
                            String biddedFromAcutioneer = reply.getContent();
                            String[] arrOfStr = biddedFromAcutioneer.split("-");
                            String tempName = reply.getSender().getLocalName();
                            double tempVolume = Double.parseDouble(arrOfStr[0]);
                            double tempPrice = Double.parseDouble(arrOfStr[1]);
                            double tempProfitLossPct = Double.parseDouble(arrOfStr[2]);
                            double tempValue = tempPrice * tempVolume;

                            bidderInfo.add(new Agents(tempVolume,tempPrice, tempValue,tempName));
                            //myGUI.displayUI(reply.toString());

                            //adding data to dictionary
                            volumnDict.put(reply.getSender().getLocalName(),tempVolume);
                            priceDict.put(reply.getSender().getLocalName(),tempPrice);
                            profitLossDict.put(reply.getSender().getLocalName(), tempProfitLossPct);
                        }else if(reply.getPerformative() == ACLMessage.REFUSE) {
                            refuseCnt++;
                        }

                        if(refuseCnt == bidderAgent.length){
                            step = 2;
                            break;
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
                            //System.out.println(powersetResult);

                            //Loop and result calculation
                            for(int i=0; i <= powersetResult.size() -1;i++){
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
                                    if(tempMaxEuValue > maxEuValue && tempMaxVolumn <= farmerInfo.sellingVolume){
                                        maxEuValue = tempMaxEuValue;
                                        maxEuObj = new String[]{xx, tempMaxVolumn.toString(),tempMaxPrice.toString(), tempMaxProfitLoss.toString()};
                                        maxEuList = powersetResult.get(i);
                                    }
                                }else if(decisionRules == 1){
                                    tempMaxEuValue = (0.5 * tempMaxVolumn) + (0 * tempMaxPrice) + (0 * tempMaxProfitLoss);
                                    if(tempMaxEuValue > maxEuValue && tempMaxVolumn <= farmerInfo.sellingVolume){
                                        maxEuValue = tempMaxEuValue;
                                        maxEuObj = new String[]{xx, tempMaxVolumn.toString(),tempMaxPrice.toString(),tempMaxProfitLoss.toString()};
                                        maxEuList = powersetResult.get(i);
                                    }
                                }else if(decisionRules ==2){
                                    tempMaxEuValue = (0 * tempMaxVolumn) + (0 * tempMaxPrice) + (0.5 * tempMaxProfitLoss);
                                    if(tempMaxEuValue > maxEuValue && tempMaxVolumn <= farmerInfo.sellingVolume){
                                        maxEuValue = tempMaxEuValue;
                                        maxEuObj = new String[]{xx, tempMaxVolumn.toString(),tempMaxPrice.toString(), tempMaxProfitLoss.toString()};
                                        maxEuList = powersetResult.get(i);
                                    }
                                }
                                else {
                                    tempMaxEuValue = (0.5 * tempMaxVolumn) + (0.5 * tempMaxPrice) + (0 * tempMaxProfitLoss);
                                    if(tempMaxEuValue > maxEuValue && tempMaxVolumn <= farmerInfo.sellingVolume){
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
                    myGUI.displayUI(getAID().getLocalName() + "    voldict  " + volumnDict.size() + "price dict  " + priceDict.size());
                    if(decisionRules == 0){
                        myGUI.displayUI("\n" + "Best solution for each case:"+"\n"+"Max price selling:  " + Arrays.toString(maxEuObj) + "\n");
                    }else if(decisionRules == 1) {
                        myGUI.displayUI("\n" + "Best solution for each case:"+"\n"+"Max volumn selling:  " + Arrays.toString(maxEuObj)+ "\n");
                    }else if(decisionRules == 2){
                        myGUI.displayUI("\n" + "Best solution for each case:"+"\n"+"Max profit loss protection:  " + Arrays.toString(maxEuObj)+ "\n");
                    }else{
                        myGUI.displayUI("\n" + "Best solution for each case:"+"\n"+"balancing between volumn and price:  " + Arrays.toString(maxEuObj)+ "\n");
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
                                myGUI.displayUI(acceptedRequest.toString());
                                mt = MessageTemplate.and(MessageTemplate.MatchConversationId("bidding"),MessageTemplate.MatchInReplyTo
                                        (acceptedRequest.getReplyWith()));
                            }else {
                                //Refuse message prepairing
                                ACLMessage rejectedRequest = new ACLMessage(ACLMessage.REJECT_PROPOSAL);
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
                    //int informCnt = informMessageList.size();
                    String log = "";
                    String tempInfo = "";
                    if (reply != null) {
                        if (reply.getPerformative() == ACLMessage.INFORM) {
                            myGUI.displayUI(reply.toString());
                            for(int i = 0; i <= maxEuList.size() -1; i++){
                                if(maxEuList.get(i).equals(reply.getSender().getLocalName())){
                                    String tempFreq = getAID().getLocalName() + "  Selling water to  " + reply.getSender().getLocalName() + "\n" + "Price:  " + priceDict.get(reply.getSender().getLocalName()) + "Volume: " + volumnDict.get(reply.getSender().getLocalName());
                                    for(int j = 0; i <= bidderInfo.size() -1; i++){
                                        if(reply.getSender().getLocalName().equals(bidderInfo.get(j).name)){
                                            myGUI.displayUI(bidderInfo.get(j).toString() + "\n");
                                        }
                                    }
                                    log = log + tempFreq;
                                    myGUI.displayUI(log);
                                    myGUI.displayUI(tempInfo);
                                    myAgent.doSuspend();
                                }
                            }

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
            if (step == 2 && maxEuList.size() == 0) {
                myGUI.displayUI("Do not buyer who provide the matching price");
                myAgent.doSuspend();

            }
            return step == 0 ;
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
        //String agentType;
        double sellingPrice;
        double sellingVolume;
        //double sellingPrice;
        //double sellingVolume;
        //double currentLookingVolumn;
        //String sellingStatus;
        //double numBidder;

        agentInfo(String farmerName,double sellingPrice, double sellingVolume){
            this.farmerName = farmerName;
            //this.agentType = agentType;
            this.sellingPrice = sellingPrice;
            this.sellingVolume = sellingVolume;
            //this.sellingPrice = sellingPrice;
            //this.sellingVolume = sellingVolume;
            //this.currentLookingVolumn = currentLookingVolumn;
            //this.sellingStatus = sellingStatus;
            //this.numBidder = numBidder;
        }
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

    //adding new class for sorted seller agent data.
    class Agents{
        //double varieVolume;
        //int fivehundredFeq;
        double totalVolume;
        double price;
        double totalValue;
        String name;
        //Constructor
        public Agents(double totalVolume, double price, double totalValue, String name){
            this.totalVolume = totalVolume;
            this.price = price;
            this.totalValue = totalValue;
            this.name = name;
        }
        public String toString(){
            //return this.name + " " + this.varieVolume + " " + this.fivehundredFeq + "  Total Volume: " + this.totalVolume + "  Total Value: " + this.totalValue + " Price: " + this.price;
            return this.name + "   " + "Total Volume: " + this.totalVolume + " Price: " + this.price + "  Total Value:  " + this.totalValue;
        }
    }
}
