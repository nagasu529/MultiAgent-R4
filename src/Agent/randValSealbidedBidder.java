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



    agentInfo bidderInfo = new agentInfo("","bidder", randValue.getRandDoubleRange(13,15), randValue.getRandDoubleRange(300,1000),0.0, 0.0, 0);
    double bidderValue = bidderInfo.buyingPrice * bidderInfo.buyingVolumn;

    protected void setup(){
        System.out.println(getAID().getLocalName() + "is Ready");
        bidderInfo.farmerName = getAID().getLocalName();

        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("agent");
        sd.setName(getAID().getName());
        dfd.addServices(sd);
        try{
            DFService.register(this, dfd);
        }
        catch (FIPAException fe){
            fe.printStackTrace();
        }
        System.out.println("Agent type is  " + sd.getType());
        addBehaviour(new AdvertiseService());
        addBehaviour(new TickerBehaviour(this, 1000) {
            protected void onTick() {
                addBehaviour(new FindingAgentsService());
            }
        });
    }

    private class AdvertiseService extends CyclicBehaviour{
        public void action(){
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                // CFP Message received. Process it
                String title = msg.getContent();
                ACLMessage reply = msg.createReply();
                reply.setPerformative(ACLMessage.PROPOSE);
                reply.setContent(bidderInfo.buyingVolumn + "-" + bidderInfo.buyingPrice + "-" + bidderInfo.agentType);
                myAgent.send(reply);
                System.out.println(reply);
            }
            else {
                block();
            }
        }
    }

    private class FindingAgentsService extends Behaviour{
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
        double acceptedValue = bidderValue;

        public void action(){
            //prepairing services, parameters and rules.
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType("agent");
            template.addServices(sd);
            try {
                DFAgentDescription[] result = DFService.search(myAgent, template);
                agentsList = new AID[result.length - 1];
                for(int i=0; i < result.length -1;i++){
                    if(result[i].getName().equals(getAID().getName())==false){
                        System.out.println(result[i].getName() + "            " + i + "          " + result.length);
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
                    cfp.setContent(0 + "-" + 0 + "-" + bidderInfo.agentType);
                    cfp.setConversationId("looking");
                    //cfp.setReplyWith("cfp" + System.currentTimeMillis());
                    myAgent.send(cfp);
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
                            System.out.println("\n" + "dddddddddddddddddddddddddddddddddddd"+ tempAgentstatus + tempValue);
                            System.out.println("\n" + "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"+ (tempAgentstatus.equals("seller")) + "\n");
                            if(tempAgentstatus.equals("seller")){
                                if(acceptedValue == 0 || acceptedValue > tempValue){
                                    bidderInfo.offeredVolumn = tempVolumn;
                                    bidderInfo.offeredPrice = tempPrice;
                                    acceptedValue = tempValue;
                                    acceptedAgent = reply.getSender();
                                }
                            }
                        }
                        repliesCnt++;
                        if (repliesCnt >= agentsList.length) {
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
                    ACLMessage refuseMsg = new ACLMessage(ACLMessage.REFUSE);
                    for(int i = 0; i < agentsList.length; i++){
                        System.out.println("\n" + agentsList[i].toString() + "   " + "no = " + i + "\n");
                        if(acceptedAgent.equals(agentsList[i])==true){
                            accepMsg.addReceiver(acceptedAgent);
                            accepMsg.setContent(bidderInfo.offeredVolumn + "-" + bidderInfo.offeredPrice + "-" + bidderInfo.agentType);
                            System.out.println("\n" + accepMsg.toString() + "\n");
                        }else {
                            refuseMsg.addReceiver(agentsList[i]);
                            System.out.println(refuseMsg.toString() + "\n");
                        }
                    }
                    step = 3;
                    //myAgent.doSuspend();
                    break;

                case 3:
                    //Received purchesed order reply.
                    reply = myAgent.receive(mt);
                    if (reply != null) {
                        // Purchase order reply received
                        if (reply.getPerformative() == ACLMessage.INFORM) {
                            // Purchase successful. We can terminate
                            System.out.println(getAID().getLocalName() + " successfully purchased from agent "+reply.getSender().getLocalName() + "\n");
                            System.out.println("Volumn = "+bidderInfo.offeredVolumn + "   " + "Price = " + bidderInfo.offeredPrice);
                            myAgent.doSuspend();
                        }
                        else {
                            System.out.println("Attempt failed: requested water already sold to others agent.");
                        }

                        step = 4;
                    }
                    else {
                        block();
                    }
                    break;
            }
        }
        public boolean done(){
            if (step == 2 && acceptedAgent == null) {
                System.out.println("Do not have matched seller for price and volumn.");
            }
            return ((step == 2 && acceptedAgent == null) || step == 4);
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