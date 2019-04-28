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

import java.text.DecimalFormat;
import java.util.*;

public class randValSealbidedSeller extends Agent {
    randValSealbidedSellerGUI myGui;

    //General papameter information
    DecimalFormat df = new DecimalFormat("#.##");
    randValue randValue = new randValue();
    agentInfo sellerInfo = new agentInfo("", "seller", randValue.getRandDoubleRange(10,12), randValue.getRandDoubleRange(5000,13000), 0, 0.0, "", 0);

    //Instant papameter for AID[]

    protected void setup(){
        myGui = new randValSealbidedSellerGUI(this);
        myGui.show();
        myGui.displayUI(getAID().getLocalName() + " is active" + "\n");

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
        myGui.displayUI("Agent type is  " + sd.getType());
        addBehaviour( new AdvertiseService());

        addBehaviour(new TickerBehaviour(this, 10000) {
            protected void onTick() {
                addBehaviour(new RequestPerformer());
                //addBehaviour(new PurchaseOrdersServer());
            }
        });
    }

    protected void takeDown(){
        try{
            DFService.deregister(this);
        }catch (FIPAException fe){
            fe.printStackTrace();
        }
        //myGui.dispose();
        myGui.displayUI(getAID().getLocalName() + " is terminated");
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
                reply.setContent(sellerInfo.sellingVolumn + "-" + sellerInfo.sellingPrice + "-" + sellerInfo.agentType);
                myAgent.send(reply);
            }
            else {
                block();
            }
        }
    }

    private class RequestPerformer extends Behaviour {
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
        double sellerValue = sellerInfo.sellingVolumn * sellerInfo.sellingPrice;
        double acceptedValue;

        public void action() {
            //prepairing services, parameters and rules.
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType("agent");
            template.addServices(sd);
            try {
                DFAgentDescription[] result = DFService.search(myAgent, template);
                agentsList = new AID[result.length - 1];
                for (int i = 0; i < result.length; i++) {
                    if (result[i].getName().equals(getAID().getName()) == false) {
                        agentsList[i] = result[i].getName();
                    }
                }
            } catch (FIPAException fe) {
                fe.printStackTrace();
            }

            //adding parameter and rules.
            switch (step) {
                //catagories seller-agent with selling offers.
                case 0:
                    ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
                    for (int i = 0; i < agentsList.length; i++) {
                        cfp.addReceiver(agentsList[i]);
                    }
                    cfp.setContent(0 + "-" + 0 + "-" + sellerInfo.agentType);
                    //cfp.setConversationId("looking");
                    //cfp.setReplyWith("cfp" + System.currentTimeMillis());
                    myAgent.send(cfp);
                    myGui.displayUI(cfp.toString());

                    //Prepare the template to get proposals
                    //mt = MessageTemplate.and(MessageTemplate.MatchConversationId("looking"), MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
                    myGui.displayUI("\n" + "qqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqq   " + step);
                    step = 1;
                    break;
                case 1:
                    ACLMessage reply = myAgent.receive(mt);
                    if (reply != null) {
                        if(reply.getPerformative()==ACLMessage.PROPOSE){
                            arrOfStr = reply.getContent().split("-");
                            tempVolumn = Double.parseDouble(arrOfStr[0]);
                            tempPrice = Double.parseDouble(arrOfStr[1]);
                            tempAgentstatus = arrOfStr[2];
                            tempValue = tempPrice * tempVolumn;
                            myGui.displayUI("\n" + "dddddddddddddddddddddddddddddddddddd"+ tempAgentstatus + tempValue);
                            myGui.displayUI("\n" + "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"+ (tempAgentstatus.equals("bidder")) + "\n");
                            if(tempAgentstatus.equals("bidder")){
                                if(acceptedValue < tempValue){
                                    sellerInfo.acceptedVolumn = tempVolumn;
                                    sellerInfo.acceptedPrice = tempPrice;
                                    acceptedValue = tempValue;
                                    acceptedAgent = reply.getSender();
                                }
                            }
                        }
                        repliesCnt++;
                        if (repliesCnt >= agentsList.length) {
                            myGui.displayUI("Best Result:  " + sellerInfo.acceptedVolumn + "   " + sellerInfo.acceptedPrice + acceptedAgent.getLocalName() + "    " + repliesCnt + "    " + agentsList.length);
                            //myAgent.doSuspend();
                            // We received all replies
                            step = 2;
                            myGui.displayUI("trueeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee             " + step + agentsList.length);
                        }
                    }
                    else {
                        block();
                    }
                    break;
                case 2:
                    //Receiving Message Proposal
                    ACLMessage replyProposal = myAgent.receive(mt);
                    System.out.println("reply is    " + replyProposal.toString());
                    if(replyProposal != null){
                        if(replyProposal.getPerformative() == ACLMessage.ACCEPT_PROPOSAL){
                            arrOfStr = replyProposal.getContent().split("-");
                            tempVolumn = Double.parseDouble(arrOfStr[0]);
                            tempPrice = Double.parseDouble(arrOfStr[1]);
                            tempAgentstatus = arrOfStr[2];
                            tempValue = tempPrice * tempVolumn;
                            System.out.println("\n" + "dddddddddddddddddddddddddddddddddddd"+ tempAgentstatus + tempValue);
                            System.out.println("\n" + "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"+ (tempAgentstatus.equals("bidder")) + "\n");
                            if(tempAgentstatus.equals("bidder")){
                                if(acceptedValue < tempValue){
                                    sellerInfo.acceptedVolumn = tempVolumn;
                                    sellerInfo.acceptedPrice = tempPrice;
                                    acceptedValue = tempValue;
                                    acceptedAgent = replyProposal.getSender();
                                }
                            }
                            System.out.println("cvcvcvcvcvcvcvcvcvcvc      " + replyProposal.toString());
                        }
                        repliesCnt++;
                        if(repliesCnt >= agentsList.length){
                            System.out.println("Best Result:  " + sellerInfo.acceptedVolumn + "   " + sellerInfo.acceptedPrice + acceptedAgent.getLocalName() + "    " + repliesCnt + "    " + agentsList.length);
                            //myAgent.doSuspend();
                            // We received all replies
                            step = 3;
                        }
                    }else {
                        block();
                    }
                    break;

                case 3:
                    MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
                    ACLMessage msg = myAgent.receive(mt);
                    if (msg != null) {
                        myGui.displayUI( getAID().getLocalName() + " sold water to agent "+msg.getSender().getName());
                        myAgent.doSuspend();
                    }
                    else {
                        block();
                    }
                    step = 4;
                    break;
            }
        }
        public boolean done(){
            if(step == 2 && acceptedAgent == null){
                myGui.displayUI("Do not hav matching bidder for this time" + "    " + step);
            }
            return ((step == 2 && acceptedAgent == null) || step == 4);
        }
    }

    private class PurchaseOrdersServer extends CyclicBehaviour {
        public void action() {
            myGui.displayUI("yyyyyyy");
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
            ACLMessage msg = myAgent.receive(mt);
            ACLMessage reply = msg.createReply();
            if (msg != null) {
                // ACCEPT_PROPOSAL Message received. Process it
                reply.setPerformative(ACLMessage.INFORM);
                myGui.displayUI( getAID().getLocalName() + " sold water to agent "+msg.getSender().getName());
            }
            else {
                block();
            }
            myAgent.send(reply);
        }
    }

    public class agentInfo{
        String farmerName;
        String agentType;
        Double sellingPrice;
        Double sellingVolumn;
        Double acceptedPrice;
        Double acceptedVolumn;
        String acceptedName;
        int numSeller;

        agentInfo(String farmerName, String agentType, double sellingPrice, double sellingVolumn, double acceptedPrice, double acceptedVolumn, String acceptedName, int numSeller){
            this.farmerName = farmerName;
            this.agentType = agentType;
            this.sellingPrice = sellingPrice;
            this.sellingVolumn = sellingVolumn;
            this.acceptedPrice = acceptedPrice;
            this.acceptedVolumn = acceptedVolumn;
            this.acceptedName = acceptedName;
            this.numSeller = numSeller;

        }
    }
}
