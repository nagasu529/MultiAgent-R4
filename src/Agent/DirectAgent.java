package Agent;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import Agent.Crop.cropType;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

import java.text.DecimalFormat;
import java.util.*;
/**
 *
 * @author chiewchk
 */

public class DirectAgent extends Agent {
    private DirectAgentUI myGui;
    Crop calCrops = new Crop();

    //Ser agent status after calculating water reduction on farm.
    String agentStatus;
    double volumeToSell;
    double volumeToBuy;
    double sellingPrice;
    double buyingPrice;
    DecimalFormat df = new DecimalFormat("#,##");

    //The list of known water selling agent
    private AID[] sellerAgent;

    protected void takeDonw(){
        try {
            DFService.deregister(this);
        }catch (FIPAException fe){
            fe.printStackTrace();
        }
    }

    public class agentInfo{
        String farmerName;
        String agentType;
        double waterVolumn;
        double pricePerMM;
        String sellingStatus;
        double minPricePerMM;
        double maxPricePerMM;
        double currentPricePerMM;
        double bidedPrice;
        double previousPrice;
        int numBidder;

        agentInfo(String farmerName, String agentType, double waterVolumn, double pricePerMM, String sellingStatus, double minPricePerMM, double maxPricePerMM,
                  double currentPricePerMM, double biddedPrice, double previousPrice, int numBidder){
            this.farmerName = farmerName;
            this.agentType = agentType;
            this.waterVolumn = waterVolumn;
            this.pricePerMM = pricePerMM;
            this.sellingStatus = sellingStatus;
            this.minPricePerMM = minPricePerMM;
            this.maxPricePerMM = maxPricePerMM;
            this.currentPricePerMM = currentPricePerMM;
            this.bidedPrice = biddedPrice;
            this.previousPrice = previousPrice;
            this.numBidder = numBidder;
        }
    }
}
