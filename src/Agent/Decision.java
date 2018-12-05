package Agent;

public class Decision {

    //Changing price rate calculation
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

    //GDP and margin benefit function.

}
