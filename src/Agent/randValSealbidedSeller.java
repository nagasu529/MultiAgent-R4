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

    //General papameter information
    DecimalFormat df = new DecimalFormat("#.##");
    randValue randValue = new randValue();
    agentInfo sellerInfo = new agentInfo("", "seller", randValue.getRandDoubleRange(10,12), randValue.getRandDoubleRange(1300,1500));
    String log = "";
    int FreqCnt = 2;

    ArrayList<Agents> informMessageList = new ArrayList<>();
    int informCnt = 0;

    protected void setup(){
        // Create and show the GUI
        myGui = new randValSealbidedSellerGUI(this);
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
                addBehaviour(new PurchaseOrdersServer());
                // Add the behaviour serving purchase orders from buyer agents
                //addBehaviour(new PurchaseOrdersServer());
            }
        } );
    }

    private class PurchaseOrdersServer extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                myGui.displayUI("\n" + getAID().getLocalName() + "accpted to buy water from" + msg.getSender().getLocalName());
                for(int i= 0; i <= informMessageList.size() - 1;i++){
                    if(informMessageList.get(i).name.equals(msg.getSender().getLocalName())){
                        myGui.displayUI(informMessageList.get(i).toString());
                    }
                }
                informCnt--;
                if (informCnt == 0){
                    myAgent.doSuspend();
                    System.out.println(getAID().getName() + " terminating.");
                }
            }else {
                block();
            }
        }
    }

    private class RequestPerformer extends Behaviour {
        //The list of known water selling agent
        private AID[] bidderAgent;
        private int repliesCnt; // The counter of replies from seller agents
        private MessageTemplate mt; // The template to receive replies
        int countTick;

        ArrayList<Agents> bidderReplyList = new ArrayList<>();


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
                            myGui.displayUI(bidderAgent[i].getName()+ "\n");
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
                    cfp.setContent(sellerInfo.sellingVol.toString());
                    cfp.setConversationId("bidding");
                    cfp.setReplyWith("cfp"+System.currentTimeMillis()); // Unique value
                    myAgent.send(cfp);
                    //myGui.displayUI(cfp.toString());
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
                            double tempVolume = Double.parseDouble(arrOfStr[0]);
                            double tempPrice = Double.parseDouble(arrOfStr[1]);
                            double tempValue = tempPrice * tempVolume;

                            //adding data to dictionary, compairing and storing data.
                            bidderReplyList.add(new Agents(tempVolume,tempPrice, tempValue,reply.getSender().getLocalName()));

                        }

                        if (repliesCnt >= bidderAgent.length) {
                            Collections.sort(bidderReplyList, new SortbyValue());
                            Collections.reverse(bidderReplyList);
                            for(int i = 0; i <= bidderReplyList.size() -1; i++){
                                myGui.displayUI(bidderReplyList.get(i).toString());
                            }
                            if(bidderReplyList.size()==0){
                                step =4;
                                break;
                            }

                            myGui.displayUI("the best value from bidders is   " + bidderReplyList.get(0).toString());
                            myGui.displayUI("\n");

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
                    //Sorted propose message and matching to reply INFORM Message.

                    informMessageList.add(bidderReplyList.get(0));
                    myGui.displayUI("bidder inform list: " + "\n");
                    for(int i = 0; i <= informMessageList.size()-1;i++){
                        myGui.displayUI(informMessageList.get(i).toString() + "\n");
                    }
                    informCnt = informMessageList.size();

                    //Sending PROPOSE message to Seller (only the best option for volume requirement.

                    for (int i = 0; i < bidderAgent.length; i++) {
                        for (int j = 0; j <= informMessageList.size() - 1; j++) {
                            if (bidderAgent[i].getLocalName().equals(informMessageList.get(j).name)) {
                                ACLMessage acceptMsg = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                                //acceptMsg.setContent("500" + "-" + informMessageList.get(j).fivehundredFeq + "-" + informMessageList.get(j).varieVolume + "-" + "1" + "-" + informMessageList.get(i).price);
                                acceptMsg.setConversationId("bidding");
                                acceptMsg.setReplyWith("reply" + System.currentTimeMillis());
                                acceptMsg.addReceiver(bidderAgent[i]);
                                myAgent.send(acceptMsg);
                                System.out.println(acceptMsg);
                            }
                        }
                    }

                    for (int i = 0; i < bidderAgent.length; i++) {
                        for (int j = 0; j <= bidderReplyList.size() - 1; j++) {
                            if (bidderAgent[i].getLocalName().equals(bidderReplyList.get(j).name)) {
                                ACLMessage rejectMsg = new ACLMessage(ACLMessage.REJECT_PROPOSAL);
                                //rejectMsg.setContent("500" + "-" + bidderReplyList.get(j).fivehundredFeq + "-" + bidderReplyList.get(j).varieVolume + "-" + "1");
                                rejectMsg.setConversationId("bidding");
                                rejectMsg.setReplyWith("reply" + System.currentTimeMillis());
                                rejectMsg.addReceiver(bidderAgent[i]);
                                myAgent.send(rejectMsg);
                                System.out.println(rejectMsg);
                            }
                        }
                    }

                    step = 3;
                    break;

                case 3:
                    // Receive the purchase order reply

                    reply = myAgent.receive(mt);
                    //int informCnt = informMessageList.size();
                    if (reply != null) {
                        if (reply.getPerformative() == ACLMessage.INFORM) {
                            for(int i = 0; i <= informMessageList.size() -1; i++){
                                if(informMessageList.get(i).name.equals(reply.getSender().getLocalName())){
                                    String tempFreq = getAID().getLocalName() + "  Selling water to  " + reply.getSender().getLocalName() + "  " + informMessageList.get(i).totalVolume + "\n";
                                    log = log + tempFreq;
                                    sellerInfo.sellingVol = sellerInfo.sellingVol - informMessageList.get(i).totalVolume;
                                    informMessageList.remove(i);
                                }
                            }
                        }
                        if(informMessageList.size() == 0){
                            myGui.displayUI(log);
                            step = 4;
                            myAgent.doSuspend();

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
            if (step == 4 && bidderReplyList.size() == 0) {
                myGui.displayUI("Do not buyer who provide the matching price.");
                myAgent.doSuspend();
                takeDown();

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
        //double varieVolume;
        //int fivehundredFeq;
        double totalVolume;
        double totalValue;
        double price;
        String name;
        //Constructor
        public Agents(double totalVolume, double totalValue, double price, String name){
            //this.varieVolume = varieVolume;
            //this.fivehundredFeq = fivehundredFeq;
            this.totalVolume = totalVolume;
            this.totalValue = totalValue;
            this.price = price;
            this.name = name;
        }
        public String toString(){
            //return this.name + " " + this.varieVolume + " " + this.fivehundredFeq + "  Total Volume: " + this.totalVolume + "  Total Value: " + this.totalValue + " Price: " + this.price;
            return this.name + "   " + "Total Volume: " + this.totalVolume + "  Total Value:  " + this.totalValue + " Price: " + this.price;
        }
    }
}