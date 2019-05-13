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
import jade.tools.introspector.gui.MyDialog;

import java.text.DecimalFormat;
import java.util.*;

public class randValSealVariesSeller extends Agent {
    randValSealVariesSellerGUI myGui;

    //General papameter information
    DecimalFormat df = new DecimalFormat("#.##");
    randValue randValue = new randValue();
    agentInfo sellerInfo = new agentInfo("", "seller", randValue.getRandDoubleRange(10,12), randValue.getRandDoubleRange(1300,1500), 0, 0.0, "", "looking");
    double minSellingValue = 0;
    LinkedList<agentInfo> totalSellerRound = new LinkedList<agentInfo>();
    double MaxSellingVolumn = sellerInfo.sellingVolumn;
    String log = "";
    int roundCnt = 2;

    int fiveHundredVolRound;
    double varieVol;
    int varieVolRound;

    protected void setup(){
        // Create and show the GUI
        myGui = new randValSealVariesSellerGUI(this);
        myGui.show();
        //sellerInfo.sellingVolumn = sellerInfo.sellingVolumn/2;

        //Selling volume splited by conditions (each grounp is not over 500 mm^3).
        if(sellerInfo.sellingVolumn > 1000 && sellerInfo.sellingVolumn <= 1500){
            fiveHundredVolRound = 2;
            varieVol = sellerInfo.sellingVolumn - 1000;
            varieVolRound = 1;
        }

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
                myGui.displayUI("Volumn to sell: " + sellerInfo.sellingVolumn + "\n");
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
        ArrayList<String> bidderList = new ArrayList<String>();  //sorted list follows maximumprice factor.
        //ArrayList<combinatorialList> buyerList = new ArrayList<combinatorialList>();    //result list for selling process reference.

        //Creating dictionary for buyer volume and pricing
        Dictionary<String, Double> volumnDict = new Hashtable<String, Double>();
        Dictionary<String, Double> priceDict = new Hashtable<String, Double>();
        Object[] maxEuObj;
        Double maxEuValue = 0.0;
        ArrayList<String> maxEuList = new ArrayList<String>();
        private double waterVolFromBidder;
        private double biddedPriceFromBidder;
        private String acceptedName;
        private double acceptedValue;
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
                    cfp.setContent("500" + "-" + fiveHundredVolRound + "-" + varieVol + "-" + varieVolRound);
                    cfp.setConversationId("bidding");
                    cfp.setReplyWith("cfp"+System.currentTimeMillis()); // Unique value
                    myAgent.send(cfp);
                    //System.out.println("cfp message :" + "\n" + cfp);
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
                            //myGui.displayUI("Receive message: \n" + reply);
                            //Count number of bidder that is propose message for water price bidding.
                            // This is an offer
                            String biddedFromAcutioneer = reply.getContent();
                            String[] arrOfStr = biddedFromAcutioneer.split("-");
                            waterVolFromBidder = Double.parseDouble(arrOfStr[0]);
                            biddedPriceFromBidder = Double.parseDouble(arrOfStr[1]);
                            //adding data to dictionary
                            if(sellerInfo.sellingVolumn >= waterVolFromBidder && (waterVolFromBidder * biddedPriceFromBidder) > acceptedValue){
                                sellerInfo.acceptedVolumn = waterVolFromBidder;
                                sellerInfo.acceptedPrice = biddedPriceFromBidder;
                                acceptedName = reply.getSender().getLocalName();
                                acceptedValue  = waterVolFromBidder * biddedPriceFromBidder;
                            }
                        }

                        if (repliesCnt >= bidderAgent.length) {

                            // We received all replies
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
                        myGui.displayUI("\n" + "Best value is from :"+ acceptedName + "\n" + "Volumn to sell: " + sellerInfo.acceptedVolumn + "\n" + "Price per mm^3: " + sellerInfo.acceptedPrice);
                    }else if(decisionRules == 1) {
                        myGui.displayUI("\n" + "Best solution for each case:"+"\n"+"Max volumn selling:  " + Arrays.toString(maxEuObj)+ "\n");
                    }else if(decisionRules == 2){
                        myGui.displayUI("\n" + "Best solution for each case:"+"\n"+"Max profit loss protection:  " + Arrays.toString(maxEuObj)+ "\n");
                    }else{
                        myGui.displayUI("\n" + "Best solution for each case:"+"\n"+"balancing between volumn and price:  " + Arrays.toString(maxEuObj)+ "\n");
                    }
                    System.out.println("\n" + "Best solution for each case:"+"\n"+"Max price selling:  " + Arrays.toString(maxEuObj) + "\n");

                    for(int i=0; i < bidderAgent.length; ++i){
                        if(bidderAgent[i].getLocalName().equals(acceptedName)){
                            // Send the purchase order to the seller that provided the best offer
                            ACLMessage acceptedRequest = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                            acceptedRequest.addReceiver(bidderAgent[i]);
                            acceptedRequest.setConversationId("bidding");
                            acceptedRequest.setReplyWith("acceptedRequest" + System.currentTimeMillis());
                            //myGui.displayUI(acceptedRequest.toString());
                            myAgent.send(acceptedRequest);
                            //myGui.displayUI("\n" + acceptedRequest.toString() + "\n");
                            mt = MessageTemplate.and(MessageTemplate.MatchConversationId("bidding"),MessageTemplate.MatchInReplyTo
                                    (acceptedRequest.getReplyWith()));
                        }else {
                            //Refuse message prepairing
                            ACLMessage rejectedRequest = new ACLMessage(ACLMessage.REFUSE);
                            rejectedRequest.addReceiver(bidderAgent[i]);
                            //myGui.displayUI(rejectedRequest.toString());
                            myAgent.send(rejectedRequest);
                            //myGui.displayUI("\n" + rejectedRequest.toString() + "\n");
                        }
                    }

                    step = 3;
                    break;

                case 3:
                    // Receive the purchase order reply
                    reply = myAgent.receive(mt);
                    if (reply != null) {
                        //System.out.println("\n" + "Reply message:" + reply.toString());
                        //myGui.displayUI("\n" + "Reply message:" + reply.toString());
                        // Purchase order reply received

                        if (reply.getPerformative() == ACLMessage.INFORM) {
                            MaxSellingVolumn = MaxSellingVolumn - sellerInfo.sellingVolumn;
                            if(MaxSellingVolumn > 0){
                                String tempRound = "First round: " + getAID().getLocalName() + "  Selling water to  " + reply.getSender().getLocalName() +"\n";
                                log = log + tempRound;
                                sellerInfo.acceptedName = "";
                                sellerInfo.acceptedVolumn = 0.0;
                                sellerInfo.acceptedPrice = 0.0;
                            }else {
                                String tempRound = "Second round: " + getAID().getLocalName() + "  Selling water to  " + reply.getSender().getLocalName() +"\n";
                                log = log + tempRound;
                                myGui.displayUI("\n");
                                myGui.displayUI(log);
                                myAgent.doSuspend();
                            }
                            //System.out.println("Water volumn left :  " + sellerInfo.sellingVolumn);
                            // Purchase successful. We can terminate
                            //System.out.println(farmerInfo.farmerName +" successfully purchased from agent "+reply.getSender().getName() + "\n");
                            //System.out.println("Price = "+farmerInfo.currentPricePerMM);
                            //myGui.displayUI("\n" + farmerInfo.farmerName +" successfully purchased from agent "+reply.getSender().getName() +"\n");
                            //myGui.displayUI("Price = " + farmerInfo.currentPricePerMM);
                            //myAgent.doSuspend();
                            //myAgent.doDelete();
                            //myGui.dispose();
                        }
                        else {
                            System.out.println("Attempt failed: requested water volumn already sold." + "\n");
                            //myGui.displayUI("Attempt failed: requested water volumn already sold." + "\n");
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
            if (step == 2 && acceptedName == null) {
                myGui.displayUI("Do not buyer who provide the matching price.");
                //myAgent.doSuspend();

                //myGui.dispose();
                //myGui.displayUI("Attempt failed: do not have bidder now" + "\n");
            }
            return step ==0;
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
