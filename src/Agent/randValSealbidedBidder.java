package Agent;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import Agent.Crop.cropType;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import java.text.DecimalFormat;

public class randValSealbidedBidder extends Agent {
    randValue randValue = new randValue();
    DecimalFormat df = new DecimalFormat("#.##");
    AID[] sellerAgents;

    agentInfo bidderInfo = new agentInfo("","agent", randValue.getRandDoubleRange(13,15), randValue.getRandDoubleRange(300,1000),0.0, 0.0, 0);

    protected void setup(){
        System.out.println(getAID().getLocalName() + "is Ready");
        bidderInfo.farmerName = getAID().getLocalName();

        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType(bidderInfo.agentType);
        sd.setName(getAID().getName());
        dfd.addServices(sd);
        try{
            DFService.register(this, dfd);
        }
        catch (FIPAException fe){
            fe.printStackTrace();
        }
        System.out.println("Agent type is  " + sd.getType());

        addBehaviour(new TickerBehaviour(this, 2000) {
            protected void onTick() {
                addBehaviour(new OfferRequestsServer());
            }
        });
    }

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
                    sd.setType("agent");
                    template.addServices(sd);
                    try {
                        DFAgentDescription[] result = DFService.search(myAgent, template);
                        sellerAgents = new AID[result.length];
                        bidderInfo.numSeller = result.length;
                        for (int i = 0; i < result.length; ++i) {
                            sellerAgents[i] = result[i].getName();
                            System.out.println(sellerAgents[i].getLocalName() + "\n");
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