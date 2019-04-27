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
    AID[] agentsList;

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
        System.out.println("Agent type is  " + sd.getType());

        addBehaviour(new TickerBehaviour(this, 2000) {
            protected void onTick() {
                addBehaviour(new RequestPerformer());
            }
        });
    }

    protected void takeDown(){
        try{
            DFService.deregister(this);
        }catch (FIPAException fe){
            fe.printStackTrace();
        }
        myGui.dispose();
        System.out.println(getAID().getLocalName() + " is terminated");
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

        public void action() {
            //prepairing services, parameters and rules.
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType("agent");
            template.addServices(sd);
            try {
                DFAgentDescription[] result = DFService.search(myAgent, template);
                agentsList = new AID[result.length];
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
            }
        }
    }

/***
    private class RequestPerformer extends Behaviour {
        private MessageTemplate mt; // The template to receive replies
        private int step = 0;
        private int repliesCnt = 0;
        String tempMsg;
        Double tempVolumn;
        Double tempPrice;

        public void action() {
            switch (step) {
                case 0:
                    myGui.displayUI("Selling price ($): " + sellerInfo.sellingPrice + "   " + "Volumn to sell: " + sellerInfo.sellingVolumn + "\n");
                    DFAgentDescription template = new DFAgentDescription();
                    ServiceDescription sdSearch = new ServiceDescription();
                    sdSearch.setType("agent");
                    template.addServices(sdSearch);
                    try {
                        DFAgentDescription[] result = DFService.search(myAgent, template);
                        myGui.displayUI("Found the following bidder agents:" + "\n");
                        bidderAgents = new AID[result.length];
                        for (int i = 0; i < result.length; ++i) {
                            if (result[i].getName().equals(getAID().getName())==false) {
                                bidderAgents[i] = result[i].getName();
                                myGui.displayUI(bidderAgents[i].getName()+ "\n");
                            }

                        }
                    }
                    catch (FIPAException fe) {
                        fe.printStackTrace();
                    }
                    //perform the request.
                    step =1;
                    break;

                case 1:
                    //Receied all bidder mesasge.
                    MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
                    ACLMessage msg = myAgent.receive(mt);
                    if (msg != null) {
                        //received CFP messages and process.
                        tempMsg = msg.getContent();
                        myGui.displayUI(tempMsg + "\n");
                        String[] arrOfStr = tempMsg.split("-");
                        tempVolumn = Double.parseDouble(arrOfStr[0]);
                        tempPrice = Double.parseDouble(arrOfStr[1]);
                        if (sellerInfo.sellingVolumn >= tempVolumn && tempPrice > sellerInfo.acceptedPrice) {
                            sellerInfo.acceptedVolumn = tempVolumn;
                            sellerInfo.acceptedPrice = tempPrice;
                            sellerInfo.acceptedName = msg.getSender().getLocalName();
                        }
                        repliesCnt++;
                        if (repliesCnt >= bidderAgents.length) {
                            step = 2;
                        }
                    } else {
                        block();
                    }
                    break;

                case 2:

                    for (int i = 0; i < bidderAgents.length; i++) {
                        if (bidderAgents[i].getLocalName().equals(sellerInfo.acceptedName)) {
                            ACLMessage accptedOffer = new ACLMessage(ACLMessage.PROPOSE);
                            accptedOffer.addReceiver(bidderAgents[i]);
                            accptedOffer.setContent(sellerInfo.acceptedVolumn + "-" + sellerInfo.acceptedPrice);
                            myAgent.send(accptedOffer);
                        } else {
                            ACLMessage rejectedOffer = new ACLMessage(ACLMessage.REFUSE);
                            rejectedOffer.addReceiver(bidderAgents[i]);
                            rejectedOffer.setContent("sold");
                        }
                    }
                    step = 3;
                    break;
            }
        }
        public boolean done(){
            if (step == 2 && sellerInfo.acceptedName == null) {
                myGui.displayUI("Attempt failed: do not have biider to match with a office price");
            }
            return ((step == 2 && sellerInfo.acceptedName == null) || step == 3);
        }
    }
***/
    /**
     Inner class PurchaseOrdersServer.
     This is the behaviour used by Book-seller agents to serve incoming
     offer acceptances (i.e. purchase orders) from buyer agents.
     The seller agent removes the purchased book from its catalogue
     and replies with an INFORM message to notify the buyer that the
     purchase has been successfully completed.
     */
    private class PurchaseOrdersServer extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
            ACLMessage msg = myAgent.receive(mt);
            ACLMessage reply = msg.createReply();
            if (msg != null) {
                // ACCEPT_PROPOSAL Message received. Process it
                reply.setPerformative(ACLMessage.INFORM);
                System.out.println( getAID().getLocalName() + " sold water to agent "+msg.getSender().getName());
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
