package Agent;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class randValue{
    Random rand = new Random();

    //Prepairing random data
    List<String> farmerNameGen = Arrays.asList("John", "Mark", "Dave", "Morgan", "Steve", "Anna", "Heather", "Nick", "Toby", "Rob");
    List<String> cropNameGen = Arrays.asList("Wheat", "Barley", "Pea(fresh)", "Maize(sweet)", "Tomato", "Bean(green)");
    List<String> irrigationTypeGen = Arrays.asList("Sprinkler", "Basin", "Border", "Furrow", "Trickle");
    List<String> cropStageGenText = Arrays.asList("Flowering", "Germination", "Development", "Ripening");

    public Double getRandElementDouble(List<Double> list)
    {
        return list.get(rand.nextInt(list.size()));
    }

    public String getRandElementString(List<String> list)
    {
        return list.get(rand.nextInt(list.size()));
    }

    public int getRandIntRange(int min, int max){
        if(min >= max){
            throw new IllegalArgumentException("max number must be more than min number");
        }
        return rand.nextInt((max - min) + 1) + min;
    }

    public double getRandDoubleRange(double min, double max){
        if(min >= max){
            throw new IllegalArgumentException("max number must be more than min number");
        }
        return rand.nextDouble() * (max - min) + min;
    }

    public List<String> getRandomNoRepetiString(List<String> list, int totalItems)
    {
        //Create temporary arrayList for randomized method without original list edited.
        List<String> tempOriList = list;
        // create a temporary list for storing
        // selected element
        List<String> newList = new ArrayList<>();
        for (int i = 0; i < totalItems; i++) {

            // take a raundom index between 0 to size
            // of given List
            int randomIndex = rand.nextInt(tempOriList.size());

            // add element in temporary list
            newList.add(list.get(randomIndex));

            // Remove selected element from orginal list
            list.remove(tempOriList);
        }
        return newList;
    }
}
