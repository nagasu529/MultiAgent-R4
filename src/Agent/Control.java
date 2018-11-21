package Agent;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.ActionExecutor;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.text.DecimalFormat;
import java.util.HashMap;

/**
 *
 * @author chiewchk
 */
public class Control extends Agent{

    //Calcualting parameter.
    double consentPrice, maxPrice, minPrice;

    //The list of farmer who are seller (maps the water volumn to its based price)
    private ControlGUI myGui;
    Crop calCrops = new Crop();

    DecimalFormat df = new DecimalFormat("#.##");

    //The list of known water selling agent
    private AID[] agentList;

    //Counting list (single negotiation process)
    int countTick;

    //The list of information (buying or selling) from agent which include price and mm^3
    private HashMap catalogue = new HashMap();

    protected void setup(){
        System.out.println(getAID()+" is ready");

        //Creating catalogue and running GUI
        myGui = new ControlGUI(this);
        myGui.show();
        //Start agent

        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("Control");
        sd.setName(getAID().getName());
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

        myGui.displayUI("Control agent is started" + "\n");

        //Add a TickerBehaviour that chooses agent status to buyer or seller.
        addBehaviour(new TickerBehaviour(this, 20000){
            protected void onTick() {
                    /*
                     ** Searching and Calculating equilibrium price (min, max and average)
                     */
                    addBehaviour(new priceEquilibrium());
                    // Add the behaviour serving purchase orders from buyer agents
                    //addBehaviour(new PurchaseOrdersServer());

            }
        } );
    }

    /*
     * 	Request performer
     *
     * 	This behaviour is used by buyer mechanism to request seller agents for water pricing ana selling capacity.
     */
    private class priceEquilibrium extends Behaviour {
        int numOfagent = 0;
        private int step =0;

        public void action(){
            switch (step){
                case 0:
                    //Update agent list (Seller and Buyer)
                    DFAgentDescription template = new DFAgentDescription();
                    ServiceDescription sd = new ServiceDescription();
                    template.addServices(sd);
                    try {
                        DFAgentDescription[] result = DFService.search(myAgent, template);
                        myGui.displayUI("The number of agents is " + result.length + "\n");
                        numOfagent = result.length;
                        agentList = new AID[result.length];
                        for (int i = 0; i < result.length; i++){
                            if(agentList[i].getName().equals(myAgent.getName())==false){
                                agentList[i] = result[i].getName();
                            }
                        }
                    }catch (FIPAException fe){
                        fe.printStackTrace();
                    }
                    break;

                case 1:
                    //Receive all message from all agent for equilibrium estimation
                    MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
                    ACLMessage msg = myAgent.receive(mt);

                    //CFP message received and process
                    String consentInfo = msg.getContent();
                    String[] arrOfstr = consentInfo.split("-");
                    myGui.displayUI("consent info from : " + reply.get);


            }
        }

        public boolean done() {
            if (step == 2 && bestBidder == null) {
                //System.out.println("Attempt failed: "+volumeToBuy+" not available for sale");
                myGui.displayUI("Attempt failed: do not have seller now".toString());
            }
            return ((step == 2 && bestBidder == null) || step == 5);
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
        System.out.println("Control Price agent "+getAID().getName()+" terminating.");
    }
}
