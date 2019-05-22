package Agent;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
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

public class testBidder extends Agent {
    randValue randValue = new randValue();
    DecimalFormat df = new DecimalFormat("#.##");
    ArrayList<Agents> sortedListSeller = new ArrayList<Agents>();
    ArrayList<Agents> proposeSortedList = new ArrayList<Agents>();


    agentInfo bidderInfo = new agentInfo("","bidder", randValue.getRandDoubleRange(10,16), randValue.getRandDoubleRange(300,2000));

    //Instant best seller for the ACCEPT_PROPOSAL message.
    int cnt = 0;
    int fiveHundredVol = 500;
    int fiveHundredVolFreq;
    double varieVol;
    int varieVolFreq = 1;

    protected void setup() {
        System.out.println(getAID().getLocalName()+"  is ready" );
        //Selling volume splited by conditions (each grounp is not over 500 mm^3).
        fiveHundredVolFreq = (int)(bidderInfo.buyingVol/500);
        varieVol = ((bidderInfo.buyingVol/500) - fiveHundredVolFreq) * 500;

        //Start Agent
        // Register the service in the yellow pages
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        bidderInfo.farmerName = getAID().getLocalName();
        System.out.println(bidderInfo.farmerName + "  " + bidderInfo.buyingVol + "  " + bidderInfo.buyingPrice + "  " + fiveHundredVolFreq + "  " + varieVol);
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

        //addBehaviour(new RejectandReset());
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

    private class OfferRequestsServer extends Behaviour {
        //search agent in DF
        private AID[] sellerList;
        private int replyCnt;
        private MessageTemplate mt;
        private int step = 0;

        public void action() {
            //Search Sellers
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType("seller");
            template.addServices(sd);
            try {
                DFAgentDescription[] result = DFService.search(myAgent, template);
                sellerList = new AID[result.length];
                for (int i = 0; i < result.length; ++i) {
                    sellerList[i] = result[i].getName();

                }
            } catch (FIPAException fe) {
                fe.printStackTrace();
            }
            switch (step) {
                case 0:
                    //receive CFP
                    //MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
                    ACLMessage msg = myAgent.receive(mt);
                    if (msg != null) {
                        if(msg.getPerformative() == ACLMessage.CFP){
                            replyCnt++;
                            //ACLMessage reply = msg.createReply();

                            //Price Per MM. and the number of volumn to sell from Seller.
                            String currentOffer = msg.getContent();
                            String[] arrOfstr = currentOffer.split("-");
                            double tempFiveHundredVol = Double.parseDouble(arrOfstr[0]);
                            int tempFiveHunderedFreq = Integer.parseInt(arrOfstr[1]);
                            double tempVarieVol = Double.parseDouble(arrOfstr[2]);
                            int tempVarieFreq = Integer.parseInt(arrOfstr[3]);
                            double totalSelling = (tempFiveHunderedFreq * 500) + tempVarieVol;

                            System.out.println(msg.getSender().getLocalName() + " Offer price and Vol:  " + tempFiveHundredVol + "  " + tempFiveHunderedFreq + "   " + tempVarieVol + "  " + tempVarieFreq);
                            //Reply bidding information to sellers
                            //reply.setPerformative(ACLMessage.PROPOSE);
                            //reply.setContent(fiveHundredVol + "-" + fiveHundredVolFreq + "-" + varieVol + "-" + varieVolFreq + "-" + bidderInfo.buyingPrice);
                            //myAgent.send(reply);
                            /***
                            if(tempVarieVol > varieVol){
                                sortedListSeller.add(new Agents(tempVarieVol, tempFiveHunderedFreq, totalSelling, msg.getSender().getLocalName()));
                            }else {
                                sortedListSeller.add(new Agents(0,tempFiveHunderedFreq,totalSelling - tempVarieVol, msg.getSender().getLocalName()));
                            }
                             ***/
                            sortedListSeller.add(new Agents(tempVarieVol, tempFiveHunderedFreq, totalSelling, msg.getSender().getLocalName()));

                        }
                        //Prepairing reply message.
                        if(replyCnt >= sellerList.length){
                            Collections.sort(sortedListSeller, new SortbyTotalVol());

                            for(int i = 0; i <= sortedListSeller.size() - 1; i++){
                                System.out.println(sortedListSeller.get(i));
                            }

                            System.out.println("\n");

                            //very specific case for not over 500 and do not have match for any varies value.
                            double buyingVolumeTotal = bidderInfo.buyingVol;

                            while (fiveHundredVolFreq > 0 || varieVol > 0){
                                double tempVarie = sortedListSeller.get(0).varieVolume;
                                int tempFiveHundredFirst = sortedListSeller.get(0).fivehundredFeq;
                                double tempTotalSelling = sortedListSeller.get(0).totalVolume;

                                //Varie Decision.
                                double tempInputVarie;
                                if(varieVol < tempVarie){
                                    tempInputVarie = tempVarie;
                                    varieVol = 0;
                                    //fiveHundredVolFreq = fiveHundredVolFreq +1;
                                }else {
                                    tempInputVarie = 0;
                                }
                                varieVol = varieVol - tempVarie;

                                //FiveH decision.
                                int tempInputFiveH;
                                if(fiveHundredVolFreq > 0 && fiveHundredVolFreq - tempFiveHundredFirst >= 0){
                                    tempInputFiveH = tempFiveHundredFirst;
                                }else {
                                    if(buyingVolumeTotal - tempInputVarie <=0){
                                        tempInputFiveH = 0;
                                        fiveHundredVolFreq = 0;
                                    }else {
                                        tempInputFiveH = fiveHundredVolFreq;
                                        fiveHundredVolFreq = 0;
                                    }

                                }
                                fiveHundredVolFreq = fiveHundredVolFreq - tempInputFiveH;

                                String name = sortedListSeller.get(0).name;
                                sortedListSeller.remove(0);

                                proposeSortedList.add(new Agents(tempInputVarie, tempInputFiveH, (tempInputVarie + (tempInputFiveH * 500)), name));
                                buyingVolumeTotal = buyingVolumeTotal - (tempInputVarie + (tempInputFiveH * 500));

                            }

                            /***
                            //Finding to fill the FivehundredFrequency.
                            while (fiveHundredVolFreq >0){
                                if(sortedListSeller.get(0).fivehundredFeq >= fiveHundredVolFreq){
                                    proposeSortedList.add(new Agents(0,sortedListSeller.get(0).fivehundredFeq, sortedListSeller.get(0).name));
                                    fiveHundredVolFreq = fiveHundredVolFreq - sortedListSeller.get(0).fivehundredFeq;
                                }else if(fiveHundredVolFreq < sortedListSeller.get(0).fivehundredFeq){
                                    proposeSortedList.add(new Agents(0,fiveHundredVolFreq, sortedListSeller.get(0).name));
                                    fiveHundredVolFreq = fiveHundredVolFreq - sortedListSeller.get(0).fivehundredFeq;
                                }
                                if(fiveHundredVolFreq < 0){
                                    fiveHundredVolFreq =0;
                                }
                                sortedListSeller.remove(0);
                            }***/
                            step =1;
                            for(int i=0; i <= proposeSortedList.size() - 1;i++){
                                System.out.println( getAID().getLocalName() + "  xxxx  " + proposeSortedList.get(i));

                            }
                        }

                    } else {
                        block();
                    }
                    break;

                case 1:
                    //Sending PROPOSE message to Seller (only the best option for volume requirement)

                    for(int i = 0; i < sellerList.length; i++){
                        for(int j = 0; j < proposeSortedList.size(); j++){
                            if(sellerList[i].getLocalName().equals(proposeSortedList.get(j).name)){
                                ACLMessage reply  = new ACLMessage(ACLMessage.PROPOSE);
                                reply.setContent("500" + "-" + proposeSortedList.get(j).fivehundredFeq + "-" + proposeSortedList.get(j).varieVolume + "-" + "1");
                                reply.setConversationId("bidding");
                                reply.setReplyWith("reply" + System.currentTimeMillis());
                                myAgent.send(reply);
                                //System.out.println(reply);
                            }else {
                                ACLMessage reply = new ACLMessage(ACLMessage.REFUSE);
                                myAgent.send(reply);
                                //System.out.println(reply);
                            }
                        }
                    }
            }
        }
        public boolean done(){
            return step == 2;
        }
    }

    private class PurchaseOrdersServer extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                ACLMessage reply = msg.createReply();
                    reply.setPerformative(ACLMessage.INFORM);
                    System.out.println("");
                    System.out.println("\n" + getAID().getLocalName() + "accpted to buy water from" + msg.getSender().getLocalName());
                    myAgent.send(reply);
                    System.out.println(reply);
                    myAgent.doDelete();
                    System.out.println(getAID().getName() + " terminating.");
            } else {
                block();
            }

            //
                //myGUI.dispose();
            //
            }
    }
}
/***
    private class RejectandReset extends CyclicBehaviour{
        public void action(){
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REFUSE);
            ACLMessage msg = myAgent.receive(mt);
            if(msg != null){

            }else {
                block();
            }
        }
    }
**/
    class agentInfo{
        String farmerName;
        String agentType;
        Double buyingPrice;
        Double buyingVol;
        public agentInfo(String farmerName, String agentType, double buyingPrice, double buyingVol){
            this.farmerName = farmerName;
            this.agentType = agentType;
            this.buyingPrice = buyingPrice;
            this.buyingVol = buyingVol;
        }
        public String toString(){
            return "Bidder Name: " + this.farmerName + " " + "Buying Volume: " + this.buyingVol + " " + "Price: " + this.buyingPrice;
        }
    }

    //Sorted by volumn (descending).
    class SortbyVolume implements Comparator<Agents>{
        //Used for sorting in ascending order of the volumn.
        public int compare(Agents a, Agents b){
            return Double.compare(a.varieVolume, b.varieVolume);
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
        String name;
        //Constructor
        public Agents(double varieVolume, int fivehundredFeq, double totalVolume, String name){
            this.varieVolume = varieVolume;
            this.fivehundredFeq = fivehundredFeq;
            this.totalVolume = totalVolume;
            this.name = name;
        }
        public String toString(){
            return this.name + " " + this.varieVolume + " " + this.fivehundredFeq + "  total Volume: " + this.totalVolume;
        }
    }
