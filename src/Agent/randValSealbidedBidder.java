package Agent;

import com.sun.xml.internal.bind.marshaller.MinimumEscapeHandler;
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

    agentInfo bidderInfo = new agentInfo("","bidder", randValue.getRandDoubleRange(13,15), randValue.getRandDoubleRange(300,1000),0.0, 0.0, "");

    //Instant best seller for the ACCEPT_PROPOSAL message.
    int cnt = 0;

    protected void setup() {
        System.out.println(getAID().getLocalName()+"  is ready" );

        //Start Agent
        // Register the book-selling service in the yellow pages
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        bidderInfo.farmerName = getAID().getLocalName();
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

        /***
        addBehaviour(new TickerBehaviour(this, 500) {
            public void onTick() {
                System.out.println("bidder name: " + bidderInfo.farmerName + "  " + bidderInfo.buyingVolumn + "  " + bidderInfo.buyingPrice + "\n");
                //Add the behaviour serving queries from Water provider about current price.
                addBehaviour(new OfferRequestsServer());

                //Add the behaviour serving purhase orders from water provider agent.
                addBehaviour(new PurchaseOrdersServer());
            }
        });

        addBehaviour(new TickerBehaviour(this, 30000) {
            protected void onTick() {
                bidderInfo.offeredName = "";
                bidderInfo.offeredPrice = 0.0;
                bidderInfo.offeredVolumn = 0.0;
            }
        });***/
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

                double tempVol = Double.parseDouble(arrOfstr[0]);
                double tempPrice = Double.parseDouble(arrOfstr[1]);

                System.out.println("Offer price and Vol:  " + tempVol + "   " + tempPrice);

                //myGUI.displayUI("Price setting up from Seller: " + farmerInfo.waterPriceFromSeller + " per MM" + "\n");
                //myGUI.displayUI("Selling volume from seller:" + farmerInfo.waterVolumnFromSeller + "\n");

                //Auction Process
                if (tempVol >= bidderInfo.buyingVolumn && tempPrice <= bidderInfo.buyingPrice) {
                    reply.setPerformative(ACLMessage.PROPOSE);
                    String sendingOffer = tempVol + "-" + tempPrice;
                    reply.setContent(sendingOffer);
                    double tempValue = tempPrice * tempVol;
                    double tempMax = bidderInfo.offeredPrice * bidderInfo.offeredVolumn;

                    //setting up the best offer from seller.
                    if(tempMax == 0 || tempValue < tempMax ){
                        bidderInfo.offeredVolumn = tempVol;
                        bidderInfo.offeredPrice = tempPrice;
                        bidderInfo.offeredName = msg.getSender().getLocalName();
                    }

                    System.out.print(getAID().getLocalName() + "the best seller option is   " + bidderInfo.offeredName + " " + bidderInfo.offeredPrice + "  " + bidderInfo.offeredVolumn);

                    myAgent.send(reply);
                    System.out.println(reply.toString());
                } else {
                    reply.setPerformative(ACLMessage.REFUSE);
                    myAgent.send(reply);
                    System.out.println(reply.toString());
                    System.out.println(getAID().getName() + " is surrender");
                }
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
                if(msg.getSender().getLocalName().equals(bidderInfo.offeredName)){
                    bidderInfo.offeredName = "";
                    bidderInfo.offeredPrice = 0.0;
                    bidderInfo.offeredVolumn = 0.0;
                }
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
                if(msg.getSender().getLocalName().equals(bidderInfo.offeredName) && bidderInfo.offeredName != null){
                    reply.setPerformative(ACLMessage.INFORM);
                    System.out.println("");
                    System.out.println("\n" + getAID().getLocalName() + "accpted to buy water from" + msg.getSender().getLocalName());
                    myAgent.doDelete();
                    //myGUI.dispose();
                    System.out.println(getAID().getName() + " terminating.");
                }else {
                    reply.setPerformative(ACLMessage.FAILURE);
                    myAgent.send(reply);
                }
                myAgent.send(reply);

            } else {
                block();
            }
        }
    }

    public class agentInfo{
        String farmerName;
        String agentType;
        Double buyingPrice;
        Double buyingVolumn;
        Double offeredPrice;
        Double offeredVolumn;
        String offeredName;

        agentInfo(String farmerName, String agentType, double buyingPrice, double buyingVolumn, double offeredPrice, double offeredVolumn, String offeredName){
            this.farmerName = farmerName;
            this.agentType = agentType;
            this.buyingPrice = buyingPrice;
            this.buyingVolumn = buyingVolumn;
            this.offeredPrice = offeredPrice;
            this.offeredVolumn = offeredVolumn;
            this.offeredName = offeredName;
        }
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