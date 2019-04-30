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

public class randValSealbidedBidder extends Agent {
    randValue randValue = new randValue();
    DecimalFormat df = new DecimalFormat("#.##");



    agentInfo bidderInfo = new agentInfo("","bidder", randValue.getRandDoubleRange(13,15), randValue.getRandDoubleRange(300,1000),0.0, 0.0, 0);
    double bidderValue = bidderInfo.buyingPrice * bidderInfo.buyingVolumn;

    protected void setup(){
        System.out.println(getAID().getLocalName() + "is Ready");
        bidderInfo.farmerName = getAID().getLocalName();

        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("bidder");
        sd.setName(getAID().getName());
        dfd.addServices(sd);
        try{
            DFService.register(this, dfd);
        }
        catch (FIPAException fe){
            fe.printStackTrace();
        }
        System.out.println("Agent type is  " + sd.getType());
        //addBehaviour(new RejectSameAgentType());

        //Agent activities.

        addBehaviour(new TickerBehaviour(this, 10000) {
            protected void onTick() {
                addBehaviour(new RequestPerformer());
            }
        });
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

        private AID tempName;
        private double tempVolumn;
        private double tempPrice;

        private int step = 0;

        public void action() {
            switch (step) {
                case 0:
                    //update bidder list
                    DFAgentDescription template = new DFAgentDescription();
                    ServiceDescription sd = new ServiceDescription();
                    sd.setType("seller");
                    template.addServices(sd);
                    try {
                        DFAgentDescription[] result = DFService.search(myAgent, template);
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
                        if (bidderAgent[i].getName().equals(bidderInfo.farmerName)== false) {
                            cfp.addReceiver(bidderAgent[i]);
                        }
                    }
                    cfp.setContent(String.valueOf(Double.toString(bidderInfo.buyingVolumn) + "-"
                            + Double.toString((bidderInfo.buyingVolumn))));
                    cfp.setConversationId("Price asking");
                    cfp.setReplyWith("cfp"+System.currentTimeMillis()); // Unique value
                    myAgent.send(cfp);
                    System.out.println("cfp message :" + "\n" + cfp);
                    // Prepare the template to get proposals
                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("Price asking"),
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
                            tempName = reply.getSender();
                            tempVolumn = Double.parseDouble(arrOfStr[1]);
                            tempPrice = Double.parseDouble(arrOfStr[2]);

                            //adding data to dictionary
                            volumnDict.put(tempName.getLocalName(),tempVolumn);
                            priceDict.put(tempName.getLocalName(),tempPrice);
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

    /***
     *  private class RejectSameAgentType extends CyclicBehaviour{
     *         public void action(){
     *             MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
     *             ACLMessage msg = myAgent.receive(mt);
     *             if(msg != null){
     *                 ACLMessage reply = msg.createReply();
     *                 reply.setPerformative(ACLMessage.REFUSE);
     *                 myAgent.send(reply);
     *             }else {
     *                 block();
     *             }
     *         }
     *     }
     *
     *
     *
     * private class RequestPerformers extends Behaviour{
        private int step = 0;
        private double tempVolumn;
        private double tempPrice;
        private double tempValue;
        private String tempAgentstatus;
        private String[] arrOfStr;
        private int repliesCnt;
        private MessageTemplate mt;
        private AID[] agentsList;
        private AID acceptedAgent;
        double bidderValue = bidderInfo.buyingVolumn * bidderInfo.buyingPrice;
        double acceptedValue;

        public void action(){
            //prepairing services, parameters and rules.
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType("agent");
            template.addServices(sd);
            try {
                DFAgentDescription[] result = DFService.search(myAgent, template);
                agentsList = new AID[result.length];
                for(int i=0; i < result.length ;i++){
                    if(result[i].getName().equals(getAID().getName())==false){
                        //System.out.println(result[i].getName() + "            " + i + "          " + result.length);
                        agentsList[i] = result[i].getName();
                    }
                }
            }catch (FIPAException fe){
                fe.printStackTrace();
            }

            //adding parameter and rules.
            switch (step){
                //catagories seller-agent with selling offers.
                case 0:
                    ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
                    for (int i = 0; i < agentsList.length; i++) {
                        cfp.addReceiver(agentsList[i]);
                        System.out.println(agentsList[i] + "\n");
                    }
                    cfp.setConversationId("looking");
                    cfp.setReplyWith("cfp" + System.currentTimeMillis());
                    myAgent.send(cfp);
                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("looking"),MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
                    System.out.println(cfp);

                    //Prepare the template to get proposals
                    step = 1;
                    System.out.println("cvcvcvcvcvcvcvcvcvcvc      " + step);
                    break;
                case 1:
                    ACLMessage reply = myAgent.receive(mt);
                    if (reply != null) {
                        if(reply.getPerformative()==ACLMessage.PROPOSE){
                            System.out.println(reply);
                            arrOfStr = reply.getContent().split("-");
                            tempVolumn = Double.parseDouble(arrOfStr[0]);
                            tempPrice = Double.parseDouble(arrOfStr[1]);
                            tempAgentstatus = arrOfStr[2];
                            tempValue = tempPrice * tempVolumn;
                            System.out.println("\n" + "dddddddddddddddddddddddddddddddddddd  "+ tempAgentstatus +"  " + tempValue);
                            System.out.println("\n" + "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx   "+ (tempAgentstatus.equals("seller")) + "\n");
                            if(tempAgentstatus.equals("seller")){
                                if(acceptedValue == 0 || acceptedValue > tempValue){
                                    bidderInfo.offeredVolumn = tempVolumn;
                                    bidderInfo.offeredPrice = tempPrice;
                                    acceptedValue = tempValue;
                                    acceptedAgent = reply.getSender();
                                    System.out.println("             " + bidderInfo.offeredPrice + bidderInfo.offeredVolumn + acceptedAgent);
                                }
                            }
                        }
                        repliesCnt++;
                        System.out.println("\n" + "sssssssssssssssssssssssssssssssssssssssssss                " + repliesCnt + "          " + agentsList.length);
                        if (repliesCnt >= agentsList.length -1) {
                            System.out.println("Best Result:  " + bidderInfo.offeredVolumn + "   " + bidderInfo.offeredPrice + acceptedAgent.getLocalName() + "    " + repliesCnt + "    " + agentsList.length);
                            //myAgent.doSuspend();
                            // We received all replies
                            step = 2;
                            System.out.println("trueeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee             " + step + agentsList.length);
                        }
                    }
                    else {
                        block();
                    }
                    break;
                case 2:
                    //Sending accept proposal to accepted bidder.
                    ACLMessage accepMsg = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                    accepMsg.addReceiver(acceptedAgent);
                    accepMsg.setConversationId("bidderOffer");
                    accepMsg.setReplyWith("bidderOffer" + System.currentTimeMillis());
                    //Prepare the template to get the purchase order reply.
                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("bidderOffer"),MessageTemplate.MatchInReplyTo(accepMsg.getReplyWith()));
                    System.out.println("\n" + accepMsg.toString() + "\n");

                    step = 3;
                    //myAgent.doSuspend();
                    break;
                case 3:
                    //Receive the purchase order reply
                    reply = myAgent.receive(mt);
                    if(reply != null){
                        if(reply.getPerformative()==ACLMessage.INFORM){
                            System.out.println(getAID().getLocalName() + "  succcessfully purchased from agent " + reply.getSender().getLocalName() + "\n");
                            System.out.println("Volumn:  " + bidderInfo.offeredVolumn + "     " + "Price ($ per mm^3):  " + bidderInfo.offeredPrice);
                        }else {
                            System.out.println("null ssssssssssssssssssssssssssssssss");
                        }
                    }

            }
        }
        public boolean done(){
            if (step == 2 && acceptedAgent == null) {
                System.out.println("Do not have matched seller for price and volumn.");
            }
            return ((step == 2 && acceptedAgent == null) || step == 4);
        }
    }
     ***/

    protected void takeDown(){
        try{
            DFService.deregister(this);
        }catch (FIPAException fe){
            fe.printStackTrace();
        }
        System.out.println(getAID().getLocalName() + " is terminated");
    }

    public class agentInfo{
        String farmerName;
        String agentType;
        Double buyingPrice;
        Double buyingVolumn;
        Double offeredPrice;
        Double offeredVolumn;
        int numSeller;

        agentInfo(String farmerName, String agentType, double buyingPrice, double buyingVolumn, double offeredPrice, double offeredVolumn, int numSeller){
            this.farmerName = farmerName;
            this.agentType = agentType;
            this.buyingPrice = buyingPrice;
            this.buyingVolumn = buyingVolumn;
            this.offeredPrice = offeredPrice;
            this.offeredVolumn = offeredVolumn;
            this.numSeller = numSeller;
        }
    }
}

/***
 private class OfferRequestsServer extends Behaviour {

 private MessageTemplate mt;
 private int step = 0;
 private int repliesCnt;
 private AID acceptedSeller;

 private double tempVolumn;
 private double tempPrice;
 public void action() {
 switch (step) {
 case 0:
 System.out.println("Agent Name: " + bidderInfo.farmerName + "  " + "Buying price: " + bidderInfo.buyingPrice + "  " + "Water volumn need: " + bidderInfo.buyingVolumn);
 DFAgentDescription template = new DFAgentDescription();
 ServiceDescription sd = new ServiceDescription();
 sd.setType("seller");
 template.addServices(sd);
 try {
 DFAgentDescription[] result = DFService.search(myAgent, template);
 sellerAgents = new AID[result.length];
 bidderInfo.numSeller = result.length;
 System.out.println("fffffffffffffffffffffffffff   " + result.length);
 for (int i = 0; i < result.length; ++i) {
 System.out.println(sellerAgents[i].getLocalName() + "\n");
 sellerAgents[i].getName();

 }

 } catch (FIPAException fe) {
 fe.printStackTrace();
 }
 //sending the offer to others (CFP messagge)
 ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
 for (int i = 0; i < sellerAgents.length; i++) {
 cfp.addReceiver(sellerAgents[i]);
 }
 cfp.setContent(bidderInfo.buyingVolumn + "-" + bidderInfo.buyingPrice);
 cfp.setConversationId("looking");
 cfp.setReplyWith("cfp" + System.currentTimeMillis());
 myAgent.send(cfp);
 System.out.println(cfp);

 //Prepare the template to get proposals
 mt = MessageTemplate.and(MessageTemplate.MatchConversationId("looking"), MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
 step = 1;
 break;

 case 1:
 ACLMessage reply = myAgent.receive(mt);
 if (reply != null) {
 //Reply received
 String[] arrOfStr = reply.getContent().split("-");
 tempVolumn = Double.parseDouble(arrOfStr[0]);
 tempPrice = Double.parseDouble(arrOfStr[1]);
 if (reply.getPerformative() == ACLMessage.PROPOSE && bidderInfo.buyingPrice < tempPrice) {
 bidderInfo.offeredVolumn = bidderInfo.buyingVolumn;
 bidderInfo.offeredPrice = bidderInfo.buyingPrice;
 acceptedSeller = reply.getSender();
 }
 repliesCnt++;
 }
 if (repliesCnt >= sellerAgents.length) {
 step = 2;
 } else {
 block();
 }
 case 2:
 //Sending the purchase order to seller who accepted the price.
 ACLMessage order = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
 order.addReceiver(acceptedSeller);
 order.setContent("accepted");
 order.setConversationId("looking");
 order.setReplyWith("order" + System.currentTimeMillis());
 myAgent.send(order);

 //Prepare the template to get the purchase order reply.
 mt = MessageTemplate.and(MessageTemplate.MatchConversationId("looking"), MessageTemplate.MatchInReplyTo(order.getReplyWith()));
 step = 3;
 break;
 case 3:
 //Receive the purchase order reply.
 reply = myAgent.receive(mt);
 if (reply != null) {
 if (reply.getPerformative() == ACLMessage.INFORM) {
 //Purchase successful. We can terminate
 System.out.println(getAID().getLocalName() + " is sucessfully purchased from " + reply.getSender().getLocalName());
 } else {
 System.out.println(getAID().getLocalName() + " is failed for purchese process");
 step = 4;
 }
 } else {
 block();
 }
 break;
 }
 }
 public boolean done(){
 if(step == 2 && acceptedSeller == null){
 System.out.println("Do not have matching price and volumn to buy");
 }
 return ((step == 2 && acceptedSeller == null) || step==4);
 }
 }
 ***/