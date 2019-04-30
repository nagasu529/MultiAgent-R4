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
    double acceptedValue;
    AID acceptedBidder;
    AID[] agentList;
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
        addBehaviour(new AdvertiseServiceReply());
        addBehaviour(new PrioritizedOffers());

        addBehaviour(new TickerBehaviour(this, 20000) {
            protected void onTick() {
                //Search no of agents in an environment.
                DFAgentDescription template = new DFAgentDescription();
                ServiceDescription sd = new ServiceDescription();
                sd.setType("agent");
                template.addServices(sd);
                try {
                    DFAgentDescription[] result = DFService.search(myAgent, template);
                    agentList = new AID[result.length];
                    myGui.displayUI("Bidder List:" + "\n");
                    for(int i = 0; i < result.length; i++){
                        if(result[i].getName().equals(getAID().getName())==false){
                            agentList[i] = result[i].getName();
                            myGui.displayUI(result[i].getName().toString());
                        }
                    }

                }catch (FIPAException fe){
                    fe.printStackTrace();
                }
                addBehaviour(new PrioritizedOffers());
                addBehaviour(new PurchaseOrdersServer());
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

    //Advertise service reply
    private class AdvertiseServiceReply extends CyclicBehaviour{
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

    private class PrioritizedOffers extends CyclicBehaviour{
        public void action(){
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
            ACLMessage msg = myAgent.receive(mt);
            if(msg != null){
                myGui.displayUI(msg.toString());
                String[] arrOfStr = msg.getContent().split("-");
                double tempVolumn = Double.parseDouble(arrOfStr[0]);
                double tempPrice = Double.parseDouble(arrOfStr[1]);
                String tempAgentStatus = arrOfStr[2];
                double tempValue = tempPrice * tempVolumn;
                if(tempValue > acceptedValue){
                    sellerInfo.sellingVolumn = tempVolumn;
                    sellerInfo.acceptedPrice = tempPrice;
                    acceptedBidder = msg.getSender();
                    myGui.displayUI(msg.getSender().getLocalName() + " proposed the highest Value offer now" + "\n");
                }
            }else {
                myGui.displayUI("null \n");
                block();
            }
        }
    }

    private class PurchaseOrdersServer extends CyclicBehaviour{
        public void action(){
            //Sending Inform and failure message to others.
            if(acceptedBidder != null){
                myGui.displayUI("xxxxxxxxxxxxxxxxxxxxxxxxxxx");
                for(int i = 0; i < agentList.length; i++){
                    if(agentList[i].getName().equals(acceptedBidder.getName()) == true){
                        ACLMessage reply = new ACLMessage(ACLMessage.INFORM);
                        reply.setContent("Accept");
                        myAgent.send(reply);
                    }else {
                        ACLMessage reply = new ACLMessage(ACLMessage.FAILURE);
                        reply.setContent("Reject");
                        myAgent.send(reply);
                    }
                }
            }else {
                myGui.displayUI("accepted Agent is null \n");
                block();
            }
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
