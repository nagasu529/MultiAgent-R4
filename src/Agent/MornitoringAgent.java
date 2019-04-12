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
    ArrayList<agentInfoMornitor> resultList = new ArrayList<agentInfoMornitor>();

    //Summarizing results
    private int maxBiderAgents;
    private double maxBuyingVol;
    private double maxProfitLoss;
    private double maxPctProfitReduction;

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
        private String bidderName;
        private double buyingVolumn;
        private double pricePerMM;
        private double profitLostPct;

        private double currentNumBidderAgent;
        private double currentBuyingVol;
        private double currentProfitLoss;
        private double currentPctProfitReduction;

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
                        int tempNumBidderAgent = bidderAgent.length;
                        if(tempNumBidderAgent > maxBiderAgents){
                            maxBiderAgents = tempNumBidderAgent;
                        }
                        currentNumBidderAgent = tempNumBidderAgent;
                        for (int i = 0; i < currentNumBidderAgent; ++i) {
                            bidderAgent[i] = result[i].getName();
                            //myGui.displayUI(bidderAgent[i].getName() + "\n");
                        }
                    } catch (FIPAException fe) {
                        fe.printStackTrace();
                    }
                    // Send the cfp to all sellers (Sending water volumn required to all bidding agent)
                    ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
                    for (int i = 0; i < currentNumBidderAgent; ++i) {
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
                            agentInfoMornitor xx = new agentInfoMornitor(bidderName, buyingVolumn, pricePerMM, profitLostPct);
                            currentBuyingVol = currentBuyingVol + buyingVolumn;
                            currentProfitLoss = currentProfitLoss + profitLostPct;
                            resultList.add(xx);
                        }
                        if (repliesCnt >= currentNumBidderAgent) {
                            //myGui.displayUI("\n" + "Max bidder number are " + currentNumBidderAgent + "\n");
                            myGui.displayUI("max" + maxBiderAgents + "     " + currentNumBidderAgent);
                            if(maxBiderAgents <= currentNumBidderAgent){
                                maxBuyingVol = currentBuyingVol;
                                maxProfitLoss = currentProfitLoss;
                                myGui.displayUI("\n" + "Total bidder numbers is " + maxBiderAgents + "\n");
                                myGui.displayUI("Total water request from the groups of buyers:  " + maxBuyingVol + "  " + ((maxBuyingVol*100)/(maxBuyingVol)) + "%" + "\n");
                                myGui.displayUI("Total profit loss from group of buyers:  " + (100*maxProfitLoss)/(maxProfitLoss) + "\n");
                            }else {
                                myGui.displayUI("Current bidder numbers is:  " + currentNumBidderAgent + "  " + (maxBiderAgents - currentNumBidderAgent) + " bidders leave" + "\n");
                                myGui.displayUI("Total water need currently: " + currentBuyingVol + "  " + df.format(100 - (((currentBuyingVol*100)/(maxBuyingVol)))) + "% of reduction" + "\n");
                                myGui.displayUI("Total profit loss reduction currlently: " + df.format(100 - (currentProfitLoss*100)/(maxProfitLoss)) + "\n");
                            }
                            /***Iterator itrR=resultList.iterator();
                            while (itrR.hasNext()){
                                agentInfoMornitor ct = (agentInfoMornitor)itrR.next();
                                myGui.displayUI(ct.farmerName + "     " + df.format(ct.buyingVolumn) + "     " + df.format(ct.buyingPricePerMM) + "     " + df.format(ct.profitLossPct) + "\n");
                            }***/
                            step = 2;
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
