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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class testSeller extends Agent {
    testSellerGUI myGui;

    //General papameter information
    DecimalFormat df = new DecimalFormat("#.##");
    randValue randValue = new randValue();
    agentInfo sellerInfo = new agentInfo("", "seller", randValue.getRandDoubleRange(10,12), randValue.getRandDoubleRange(1300,1500));
    String log = "";
    int FreqCnt = 2;

    protected void setup(){
        // Create and show the GUI
        myGui = new testSellerGUI(this);
        myGui.show();
        //sellerInfo.sellingVolumn = sellerInfo.sellingVolumn/2;



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

        System.out.println(sellerInfo.farmerName + "  is ready" + "\n" + "Stage is  " + sellerInfo.agentType + "\n");

        //Add a TickerBehaviour that chooses agent status to buyer or seller.
        addBehaviour(new TickerBehaviour(this, 15000){
            protected void onTick() {
                myGui.displayUI("Name: " + sellerInfo.farmerName + "\n");
                myGui.displayUI("Status: " + sellerInfo.agentType + "\n");
                myGui.displayUI("Volumn to sell: " + sellerInfo.sellingVol + "\n");
                myGui.displayUI("Selling price: " + sellerInfo.sellingPrice + "\n");
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
        int countTick;

        //Selling volume splited by conditions (each grounp is not over 500 mm^3).
        double volBeforeSplit = sellerInfo.sellingVol/500;
        double fiveHundredVol = 500;
        int fiveHundredVolFreq = (int)(volBeforeSplit);
        double varieVol = (volBeforeSplit - fiveHundredVolFreq) * 500;
        int varieVolFreq =1;
        double fiveHundredValue = fiveHundredVol * (fiveHundredVolFreq * sellerInfo.sellingPrice);
        double varieValue = varieVolFreq * sellerInfo.sellingPrice;

        //List of reply instance.
        ArrayList<Agents> bidderReplyList = new ArrayList<>();
        ArrayList<Agents> informMessageList = new ArrayList<>();

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
                        myGui.displayUI("Found acutioneer agents:\n");
                        bidderAgent = new AID[result.length];
                        for (int i = 0; i < result.length; ++i) {
                            bidderAgent[i] = result[i].getName();
                            //myGui.displayUI(bidderAgent[i].getName()+ "\n");
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
                    //cfp.setContent(String.valueOf(Double.toString(sellerInfo.sellingVolumn) + "-" + Double.toString((sellerInfo.sellingPrice))));
                    cfp.setContent(fiveHundredVol + "-" + fiveHundredVolFreq + "-" + varieVol + "-" + varieVolFreq);
                    cfp.setConversationId("bidding");
                    cfp.setReplyWith("cfp"+System.currentTimeMillis()); // Unique value
                    myAgent.send(cfp);
                    //System.out.println("cfp message :" + "\n" + cfp);
                    // Prepare the template to get proposals
                    //mt = MessageTemplate.and(MessageTemplate.MatchConversationId("bidding"),MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
                    step = 1;
                    break;

                case 1:
                    // Receive all proposals/refusals from bidder agents
                    //Sorted all offers based on price Per mm.
                    //ACLMessage reply = myAgent.receive(mt);
                    //mt = MessageTemplate.MatchPerformative(ACLMessage.PROPOSE);
                    ACLMessage reply = myAgent.receive(mt);
                    if (reply != null) {
                        repliesCnt++;
                        // Reply received
                        if (reply.getPerformative() == ACLMessage.PROPOSE) {
                            //myGui.displayUI("Receive message: \n" + reply + "\n");
                            //Count number of bidder that is propose message for water price bidding.
                            // This is an offer
                            String biddedFromAcutioneer = reply.getContent();
                            String[] arrOfStr = biddedFromAcutioneer.split("-");
                            double tempFiveHundredVol = Double.parseDouble(arrOfStr[0]);
                            int tempFiveHundredFreq = Integer.parseInt(arrOfStr[1]);
                            double tempVarieVol = Double.parseDouble(arrOfStr[2]);
                            int tempVarieFreq = Integer.parseInt(arrOfStr[3]);
                            double tempPrice = Double.parseDouble(arrOfStr[4]);
                            double tempVolume = (tempVarieVol + (500 * tempFiveHundredFreq));
                            double tempValue = tempVolume * tempPrice;

                            //adding data to dictionary, compairing and storing data.
                            bidderReplyList.add(new Agents(tempVarieVol,tempFiveHundredFreq,tempVolume, tempValue, tempPrice, reply.getSender().getLocalName()));
                            //FiveHundred condition.

                            //Varie Value condition.

                        }

                        if (repliesCnt >= bidderAgent.length) {
                            Collections.sort(bidderReplyList, new SortbyValue());
                            Collections.reverse(bidderReplyList);

                            for(int i = 0; i <= bidderReplyList.size() -1; i++){
                                myGui.displayUI(bidderReplyList.get(i).toString() + "\n");
                            }
                            myAgent.doSuspend();

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


                    /***
                    while (fiveHundredVolFreq != 0 || varieVol != 0){
                        double tempInformVarie = bidderReplyList.get(0).varieVolume;
                        int temmpInformFiveFreq = bidderReplyList.get(0).fivehundredFeq;

                        fiveHundredVolFreq = fiveHundredVolFreq - temmpInformFiveFreq;
                        varieVol = varieVol - tempInformVarie;
                        if(fiveHundredVolFreq >= 0 && varieVol >= 0){
                            informMessageList.add(bidderReplyList.get(0));
                            bidderReplyList.remove(0);
                        }else if (fiveHundredVolFreq < 0 || varieVol < 0){
                            fiveHundredVolFreq = 0;
                            varieVol = 0;
                        }
                    }
                    ***/
                    myGui.displayUI("All inform agent list contact:" +"\n");
                    if(fiveHundredVolFreq >0 || varieVol > 0){
                        myGui.displayUI("Next roundddddddddddddddddddddddddddddddddddddddddddddddddd");
                    }
                    for (int i = 0; i < informMessageList.size() ; i++){
                        myGui.displayUI(informMessageList.get(i) + "\n");
                    }

                    step = 3;
                    break;

                case 3:
                    // Receive the purchase order reply


                    reply = myAgent.receive(mt);
                    if (reply != null) {
                        /***
                        //System.out.println("\n" + "Reply message:" + reply.toString());
                        //myGui.displayUI("\n" + "Reply message:" + reply.toString());
                        // Purchase order reply received

                        if (reply.getPerformative() == ACLMessage.INFORM && reply.getSender().getLocalName().equals(sellerInfo.acceptedVarieName)) {
                            String tempFreq = getAID().getLocalName() + "  Selling water to  " + reply.getSender().getLocalName() + "  " + sellerInfo.acceptedVarieVol + "\n";
                            log = log + tempFreq;
                            sellerInfo.sellingVolumn = sellerInfo.sellingVolumn - sellerInfo.acceptedVarieVol;
                        }
                        if(reply.getPerformative() == ACLMessage.INFORM && reply.getSender().getLocalName().equals(sellerInfo.acceptedFiveHundredName)) {
                            String tempFreq = getAID().getLocalName() + "  Selling water to  " + reply.getSender().getLocalName() + "  " + sellerInfo.acceptedFiveHundredVol + "\n";
                            log = log + tempFreq;
                            sellerInfo.sellingVolumn = sellerInfo.sellingVolumn - sellerInfo.acceptedFiveHundredVol;
                        }

                        if (reply.getPerformative() == ACLMessage.REFUSE && reply.getSender().getLocalName().equals(sellerInfo.acceptedVarieName)) {
                            sellerInfo.acceptedVarieName = "";
                            sellerInfo.acceptedVariePrice = 0.0;
                        }
                        if(reply.getPerformative() == ACLMessage.REFUSE && reply.getSender().getLocalName().equals(sellerInfo.acceptedFiveHundredName)) {
                            sellerInfo.acceptedFiveHundredName = "";
                            sellerInfo.acceptedFiveHundredPrice = 0.0;
                        }
                        myGui.displayUI("xxxxxxxxxxxxxxxxxxxxx" + sellerInfo.sellingVolumn);
                        if(sellerInfo.sellingVolumn <= 0) {
                            myGui.displayUI("\n");
                            myGui.displayUI(log);
                            myAgent.doSuspend();
                        }
                        else {
                            System.out.println("Attempt failed: requested water volumn already sold." + "\n");
                            //myGui.displayUI("Attempt failed: requested water volumn already sold." + "\n");
                        }***/
                        step = 4;
                    }
                    else {
                        block();
                    }
                    break;

            }
        }
        public boolean done() {
            if (step == 2 && bidderReplyList.size() == 0) {
                myGui.displayUI("Do not buyer who provide the matching price.");
                //myAgent.doSuspend();

                //myGui.dispose();
                //myGui.displayUI("Attempt failed: do not have bidder now" + "\n");
            }
            return step == 0;
            //return ((step == 2 && acceptedName == null) || step == 4) ;
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

    // function to sort hashmap by values
    class agentInfo{
        String farmerName;
        String agentType;
        Double sellingPrice;
        Double sellingVol;
        public agentInfo(String farmerName, String agentType, double sellingPrice, double sellingVol){
            this.farmerName = farmerName;
            this.agentType = agentType;
            this.sellingPrice = sellingPrice;
            this.sellingVol = sellingVol;
        }
        public String toString(){
            return "Bidder Name: " + this.farmerName + " " + "Buying Volume: " + this.sellingVol + " " + "Price: " + this.sellingPrice;
        }
    }

    //Sorted by volumn (descending).
    class SortbyValue implements Comparator<Agents> {
        //Used for sorting in ascending order of the volumn.
        public int compare(Agents a, Agents b){
            return Double.compare(a.totalValue, b.totalValue);
        }
    }

    class SortbyTotalVol implements Comparator<Agents>{
        //Used for sorting in ascending order of the volumn.
        public int compare(Agents a, Agents b){
            return Double.compare(a.totalVolume, b.totalVolume);
        }
    }

    //adding new class for sorted seller agent data.
    class Agents{
        double varieVolume;
        int fivehundredFeq;
        double totalVolume;
        double totalValue;
        double price;
        String name;
        //Constructor
        public Agents(double varieVolume, int fivehundredFeq, double totalVolume, double totalValue, double price, String name){
            this.varieVolume = varieVolume;
            this.fivehundredFeq = fivehundredFeq;
            this.totalVolume = totalVolume;
            this.totalValue = totalValue;
            this.price = price;
            this.name = name;
        }
        public String toString(){
            return this.name + " " + this.varieVolume + " " + this.fivehundredFeq + "  Total Volume: " + this.totalVolume + "  Total Value: " + this.totalValue + " Price: " + this.price;
        }
    }

}


