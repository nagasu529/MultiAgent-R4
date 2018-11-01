package Agent;

public class Auction {
    public double changedPriceRate(String incORdec, double changePerct, double pricePerMM){
        double tempPriceRate;
        if(incORdec=="inc"){
            tempPriceRate = (pricePerMM + (changePerct/100)*pricePerMM);

        }else if (incORdec == "dec"){
            tempPriceRate = (pricePerMM - (changePerct/100)*pricePerMM);
        }else {
            tempPriceRate = 0;
        }
        return tempPriceRate;
    }
}
