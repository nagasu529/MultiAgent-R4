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
import java.util.ArrayList;
import java.util.Comparator;

public class randValSealVariesBidder extends Agent {
    randValue randValue = new randValue();
    DecimalFormat df = new DecimalFormat("#.##");
    ArrayList<Agent> sortedListAgent = new ArrayList<Agent>();

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
        double volBeforeSplit = bidderInfo.buyingVol/500;
        fiveHundredVol = 500;
        fiveHundredVolFreq = (int)(bidderInfo.buyingVol/500);
        varieVol = ((bidderInfo.buyingVol/500) - fiveHundredVolFreq) * 500;
        varieVolFreq =1;

        //Start Agent
        // Register the book-selling service in the yellow pages
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        bidderInfo.farmerName = getAID().getLocalName();
        System.out.println(bidderInfo.farmerName + "  " + bidderInfo.buyingVol + "  " + bidderInfo.buyingPrice);
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
                int tempFiveHunderedFreq = Integer.parseInt(arrOfstr[1]);
                double tempVarieVol = Double.parseDouble(arrOfstr[2]);
                int tempVarieFreq = Integer.parseInt(arrOfstr[3]);

                System.out.println(msg.getSender().getLocalName() + " Offer price and Vol:  " + tempFiveHundredVol + "  " + tempFiveHunderedFreq + "   " + tempVarieVol + "  " + tempVarieFreq + "  " + bidderInfo.buyingPrice);

                //Reply bidding information to sellers
                reply.setPerformative(ACLMessage.PROPOSE);
                //reply.setContent(fiveHundredVol + "-" + fiveHundredVolFreq + "-" + varieVol + "-" + varieVolFreq + "-" + bidderInfo.buyingPrice);
                //myAgent.send(reply);
                for(int i = 1; i <= tempFiveHunderedFreq; i++){
                    sortedListAgent.add(new Agent(500,msg.getSender().getLocalName()));
                }
                sortedListAgent.add(new Agent(tempVarieVol, msg.getSender().getLocalName()));

                /***
                 //Auction process.
                 if(tempVarieVol >= varieVol && ((bidderInfo.offeredVarieVol > tempVarieVol) || (bidderInfo.offeredVarieVol == 0))){
                 bidderInfo.offeredVarieVol = tempVarieVol;
                 bidderInfo.offeredVarieName = msg.getSender().getLocalName();
                 if(tempFiveHunderedFreq == fiveHundredVolFreq){
                 bidderInfo.offeredVolFiveHundred = tempFiveHundredVol;
                 bidderInfo.offeredNameFiveHundred = msg.getSender().getName();
                 fiveHundredVolFreq = 0;
                 }
                 }
                 if(fiveHundredVolFreq > 0){
                 if (tempFiveHunderedFreq > fiveHundredVolFreq){
                 bidderInfo.offeredVolFiveHundred = fiveHundredVol;
                 bidderInfo.offeredNameFiveHundred = msg.getSender().getLocalName();
                 }else {
                 bidderInfo.offeredPriceFiveHundred = tempFiveHundredVol;
                 bidderInfo.offeredNameFiveHundred = msg.getSender().getLocalName();
                 }
                 }

                 reply.setContent(fiveHundredVol + "-" + fiveHundredVolFreq + "-" + bidderInfo.offeredVarieVol + "-" + varieVolFreq + "-" + bidderInfo.buyingPrice);
                 myAgent.send(reply);
                 /***
                 if((tempFiveHundredVol == 500 && tempFiveHunderedFreq == 2)&& tempVarieVol > varieVol){
                 reply.setPerformative(ACLMessage.PROPOSE);
                 reply.setContent(fiveHundredVol + "-" + fiveHundredVolFreq + "-" + varieVol + "-" + varieVolFreq + "-" + bidderInfo.buyingPrice);
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
                 ***/
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
                bidderInfo.offeredVarieName = "";
                bidderInfo.offeredVariePrice = 0.0;
                bidderInfo.offeredVarieVol = 0.0;
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
                if(msg.getSender().getLocalName().equals(bidderInfo.offeredVarieName) && bidderInfo.offeredVarieName != null) {
                    reply.setPerformative(ACLMessage.INFORM);
                    System.out.println("");
                    System.out.println("\n" + getAID().getLocalName() + "accpted to buy water from" + msg.getSender().getLocalName());
                    bidderInfo.offeredVarieName = null;
                    myAgent.send(reply);
                    System.out.println(reply);
                }
                if(msg.getSender().getLocalName().equals(bidderInfo.offeredNameFiveHundred) && bidderInfo.offeredNameFiveHundred != null) {
                    reply.setPerformative(ACLMessage.INFORM);
                    System.out.println("");
                    System.out.println("\n" + getAID().getLocalName() + "accpted to buy water from" + msg.getSender().getLocalName());
                    bidderInfo.offeredNameFiveHundred = null;
                    myAgent.send(reply);
                    System.out.println(reply);
                } else{
                    reply.setPerformative(ACLMessage.FAILURE);
                    myAgent.send(reply);
                    System.out.println(reply);
                }
            } else {
                block();
            }
            if(bidderInfo.offeredNameFiveHundred == null && bidderInfo.offeredVarieName == null) {
                myAgent.doDelete();
                //myGUI.dispose();
                System.out.println(getAID().getName() + " terminating.");
            }
        }
    }

    class agentInfo{
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


        public agentInfo(String farmerName, String agentType, double buyingPrice, double buyingVol, double offeredPriceFiveHundred, double offeredVolFiveHundred, String offerNameFiveHundred, double offeredVariePrice, double offeredVarieVol, String offeredVarieName){
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

        public String toString(){
            return this.farmerName + " Buying Volume: " + this.buyingVol + " Buying Price: " + this.buyingPrice + "\n" +
                    "Best offer For 500 Vol : " + this.offeredNameFiveHundred + "\n" +
                    "Best offer For varies :" + this.offeredVarieName;
        }
    }

    //Sorted by volumn.
    class SortbyVolume implements Comparator<Agent>{
        //Used for sorting in ascending order of the volumn.
        public int compare(Agent a, Agent b){
            return Double.compare(a.volume, b.volume);
        }
    }

    //adding new class for sorted seller agent data.
    class Agent{
        double volume;
        String name;
        //Constructor
        public Agent(double volume, String name){
            this.volume = volume;
            this.name = name;
        }
        public String toString(){
            return this.volume + " " + this.name;
        }
    }
}
