package Agent;

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

public class RandValueAgent extends Agent{
    Crop calCrops = new Crop();
    DecimalFormat df = new DecimalFormat("#.##");


    public class agentInfo{
        String farmerName;
        String agentType;
        double buyingVolumn;
        double currentLookingVolumn;
        double buyingPricePerMM;
        String sellingStatus;
        double waterVolumnFromSeller;
        double waterPriceFromSeller;
        double profitLossPct;

        agentInfo(String farmerName, String agentType, double buyingVolumn, double currentLookingVolumn,
                  double buyingPricePerMM, String sellingStatus, double waterVolumnFromSeller, double waterPriceFromSeller, double profitLossPct){
            this.farmerName = farmerName;
            this.agentType = agentType;
            this.buyingVolumn = buyingVolumn;
            this.currentLookingVolumn = currentLookingVolumn;
            this.buyingPricePerMM = buyingPricePerMM;
            this.sellingStatus = sellingStatus;
            this.waterVolumnFromSeller = waterVolumnFromSeller;
            this.waterPriceFromSeller = waterPriceFromSeller;
            this.profitLossPct = profitLossPct;
        }
    }
}
