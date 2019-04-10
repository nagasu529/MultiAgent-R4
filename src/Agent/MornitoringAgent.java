package Agent;

import jade.core.Agent;
import jade.core.behaviours.*;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import java.text.DecimalFormat;
import java.util.*;

public class MornitoringAgent extends Agent {
    private MornitoringAgentGUI myGui;
    DecimalFormat df = new DecimalFormat("#.##");
    ArrayList<String> resultList = new ArrayList<>();

    //Farmer information on each agent.
    //agentInfoMornitor agentInfo = new agentInfoMornitor("", "",0.0, 0.0, "", 0.0);

    protected void setup(){
        //Create and show GUI
        myGui = new MornitoringAgentGUI(this);
        myGui.show();
        myGui.displayUI(getAID().getLocalName() + " Monitoring agent is active");
        //Start agent
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("Farmer");
        sd.setName(getAID().getName());
        //farmerInfo.farmerName = getAID().getName();
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

        //Adding the TickBehaviour for service moritoring.
        addBehaviour(new TickerBehaviour(this, 50000) {
            protected void onTick() {
                addBehaviour(new AgentMornitoring());
            }
        });
    }

    private class AgentMornitoring extends Behaviour {
        //List of all agents in the environment.
        private AID[] bidderAgent;
        private MessageTemplate mt;
        private int repliesCnt = 0;
        String bidderName;
        double buyingVolumn;
        double pricePerMM;
        double profitLostPct;
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
                        myGui.displayUI("Found acutioneer agents:");
                        bidderAgent = new AID[result.length];
                        for (int i = 0; i < result.length; ++i) {
                            bidderAgent[i] = result[i].getName();
                            myGui.displayUI(bidderAgent[i].getName() + "\n");
                        }
                    } catch (FIPAException fe) {
                        fe.printStackTrace();
                    }
                    // Send the cfp to all sellers (Sending water volumn required to all bidding agent)
                    ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
                    for (int i = 0; i < bidderAgent.length; ++i) {
                        if (bidderAgent[i].getName().equals(getAID().getName()) == false) {
                            cfp.addReceiver(bidderAgent[i]);
                        }
                    }
                    cfp.setContent(0.0 + "-" + 0.0);
                    cfp.setConversationId("bidding");
                    cfp.setReplyWith("cfp" + System.currentTimeMillis()); // Unique value
                    myAgent.send(cfp);
                    myGui.displayUI("cfp message :" + "\n" + cfp);
                    // Prepare the template to get proposals
                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("bidding"),
                            MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
                    step = 1;
                    myGui.displayUI("\n" + step);
                    break;

                case 1:
                    //Receive all agent proposals.
                    ACLMessage reply = myAgent.receive(mt);
                    if (reply != null) {
                        repliesCnt++;
                        //Reply received
                        if (reply.getPerformative() == ACLMessage.PROPOSE) {
                            String bidderMessage = reply.getContent();
                            String[] arrOfStr = bidderMessage.split("-");
                            bidderName = arrOfStr[0];
                            buyingVolumn = Double.parseDouble(arrOfStr[1]);
                            pricePerMM = Double.parseDouble(arrOfStr[2]);
                            profitLostPct = Double.parseDouble(arrOfStr[3]);
                            Obj[] xx =
                            resultList.add(bidderName, buyingVolumn, pricePerMM, profitLostPct);
                            //agentInfoMornitor xx = new agentInfoMornitor(bidderName, buyingVolumn, pricePerMM, profitLostPct);
                            //myGui.displayUI(bidderName + "  " + buyingVolumn + "  " + pricePerMM + "  " + profitLostPct);
                            resultList.add(xx);
                        }
                        if (repliesCnt >= bidderAgent.length) {
                            step = 2;
                            //Iterator itrR=resultList.iterator();
                            //while (itrR.hasNext()){
                            //    agentInfoMornitor ct = (agentInfoMornitor)itrR.next();
                                //System.out.print(ct.farmerName + "  " + df.format(ct.buyingVolumn) + "  " + df.format(ct.buyingPricePerMM) + "  " + df.format(ct.buyingPricePerMM + "\n"));
                            }
                    } else {
                        block();
                    }
                    break;

                case 2:
                    if (bidderAgent.length > 0) {
                        step = 0;
                    } else {
                        step = 3;
                    }
            }
        }

        public boolean done() {
            if (step == 3) {
                myGui.displayUI("Process is done" + "\n");
                myAgent.doSuspend();
            }
            return step == 0;
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

    class agentInfoMornitor{
        String farmerName;
        double buyingVolumn;
        double buyingPricePerMM;
        double profitLossPct;

        agentInfoMornitor(String farmerName, double buyingVolumn, double buyingPricePerMM, double profitLossPct){
            this.farmerName = farmerName;
            this.buyingVolumn = buyingVolumn;
            this.buyingPricePerMM = buyingPricePerMM;
            this.profitLossPct = profitLossPct;
        }
    }
}
