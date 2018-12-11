package Database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 *
 * @author Kitti
 */
public class databaseConn {

    public double avgET0 = 0.0;
    public double stdET0 = 7.0;
    public double avgKc = 0.0;
    public double KcValue = 0.0;
    public double KcBased = 0.0;
    public double irrigationRate = 0.0;
    public double grossMaginValue = 0.0;
    public double totalIncome = 0.0;
    public double expanditureValue = 0.0;
    public double tonePerHec = 0.0;

    //Database connect for calculationg ET0
    private Connection connect(){
        //SQlite connietion string

        //String url = "jdbc:sqlite:/Users/kitti.ch/Dropbox/PhD-Lincoln/javaProgram/DBandText/db/FarmDB.sqlite"; //Macbook
        //String url = "jdbc:sqlite:C:/Users/chiewchk/Dropbox/PhD-Lincoln/javaProgram/DBandText/db/FarmDB.sqlite";  //Office
        String url = "jdbc:sqlite:G:/Dropbox/PhD-Lincoln/javaProgram/DBandText/db/FarmDB.sqlite"; //Home PC
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(url);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            System.exit(0);
        }
        return conn;
    }
    /**
     * Select data from ET0 Cable
     *
     */
    public void selectET(){
        avgET0 = 0.0;
        String sql = "SELECT Location, Month, ET0 FROM ET0";

        try(Connection conn = this.connect();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql)){

            //loop through the result set
            while (rs.next()) {
                System.out.println(rs.getInt("Location") + "\t" +
                        rs.getInt("Month") + "\t" +
                        rs.getDouble("ET0"));

            }
        }catch(SQLException e){
            System.out.println(e.getMessage());
        }
    }

    public void ET0Spring(){
        avgET0 = 0.0;
        String sql = "Select ET0 from ET0 where (Month BETWEEN 9 AND 11)";
        try(Connection conn = this.connect();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql)){

            //loop through the result set
            while (rs.next()) {
                avgET0 = avgET0 + rs.getDouble("ET0");
            }
            avgET0 = avgET0/3;
            System.out.println("ET0 Spring is: "+avgET0);
        }catch(SQLException e){
            System.out.println(e.getMessage());
        }
    }

    public void ET0Summer(){
        avgET0 = 0.0;
        String sql = "Select ET0 from ET0 where Month  = 12 or Month BETWEEN 1 AND 2";
        try(Connection conn = this.connect();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql)){

            //loop through the result set
            while (rs.next()) {
                //System.out.println(rs.getDouble("ET0"));
                avgET0 = avgET0 + rs.getDouble("ET0");
            }
            avgET0 = avgET0/3;
            System.out.println("ET0 Summer is:"+avgET0);
        }catch(SQLException e){
            System.out.println(e.getMessage());
        }
    }

    public void ET0Autumn(){
        avgET0 = 0.0;
        String sql = "Select ET0 from ET0 WHERE Month BETWEEN 3 AND 5";
        try(Connection conn = this.connect();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql)){

            //loop through the result set
            while (rs.next()) {
                avgET0 = avgET0 + rs.getDouble("ET0");
            }
            avgET0 = avgET0/3;
            System.out.println("ET0 Autumn is:"+avgET0);
        }catch(SQLException e){
            System.out.println(e.getMessage());
        }
    }

    public void ET0Winter(){
        avgET0 = 0.0;
        String sql = "Select ET0 from ET0 WHERE Month BETWEEN 6 AND 8";
        try(Connection conn = this.connect();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql)){

            //loop through the result set
            while (rs.next()) {
                avgET0 = avgET0 + rs.getDouble("ET0");
            }
            avgET0 = avgET0/3;
            System.out.println("ET0 Winter is: "+avgET0);
        }catch(SQLException e){
            System.out.println(e.getMessage());
        }
    }

    public void KcCalculation(String cropName, String cropPeriod){

        //temp value
        double tmpCal = 0.0;

        String sql = "SELECT Duration.Crop, Duration.Initial, Period.initialDay, Duration.CropDev, Period.CropDevDay, "
                + "Duration.Mid, Period.MidDay, Duration.Late, Period.LateDay, Period.TotalDay FROM Duration INNER JOIN "
                + "Period ON Duration.Crop = period.Crop WHERE Period.Crop = ? AND Period.period = ?;";
        try(Connection conn = this.connect();
            PreparedStatement pstmt = conn.prepareStatement(sql)){

            //set value
            pstmt.setString(1,cropName);
            pstmt.setString(2, cropPeriod);
            ResultSet rs = pstmt.executeQuery();

            //loop through the result set
            while (rs.next()) {
                avgKc = avgKc + (rs.getDouble("Initial")*rs.getDouble("InitialDay")) +
                        (rs.getDouble("CropDev")*rs.getDouble("CropDevDay")) +
                        (rs.getDouble("Mid")*rs.getDouble("MidDay")) +
                        (rs.getDouble("Late")*rs.getDouble("LateDay"));
            }
            System.out.println(avgKc);
        }catch(SQLException e){
            System.out.println(e.getMessage());
        }
    }

    //Irrigation rate from FAO white paper book

    public void getIrrigationTypeValue(String IrrigationType){
        irrigationRate = 0.0;
        String sql = "select FWValue FROM FWIrriRate where IrrigationEvent=?";
        double tmp = 0.0;
        try (Connection conn = this.connect();
             PreparedStatement pstmt  = conn.prepareStatement(sql)){

            // set the value
            pstmt.setString(1,IrrigationType);
            //
            ResultSet rs  = pstmt.executeQuery();

            tmp = rs.getDouble("FWValue");
            irrigationRate = tmp;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    //Gross margin from database
    public void getGrossMarginValue(String cropName){
        grossMaginValue = 0.0;
        String sql = "select GrossMarginHa FROM Duration where cropName=?";
        double tmp = 0.0;
        try (Connection conn = this.connect();
             PreparedStatement pstmt  = conn.prepareStatement(sql)){

            // set the value
            pstmt.setString(1,cropName);
            //
            ResultSet rs  = pstmt.executeQuery();

            tmp = rs.getDouble("GrossMarginHa");
            grossMaginValue = tmp;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public void getTotalIncome(String cropName){
        totalIncome = 0.0;
        String sql = "select TotalIncome FROM Duration where cropName=?";
        double tmp = 0.0;
        try (Connection conn = this.connect();
             PreparedStatement pstmt  = conn.prepareStatement(sql)){

            // set the value
            pstmt.setString(1,cropName);
            //
            ResultSet rs  = pstmt.executeQuery();

            tmp = rs.getDouble("GrossMarginHa");
            totalIncome = tmp;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public void getexpanditureValue(String cropName){
        expanditureValue = 0.0;
        String sql = "select Expanditure FROM Duration where cropName=?";
        double tmp = 0.0;
        try (Connection conn = this.connect();
             PreparedStatement pstmt  = conn.prepareStatement(sql)){

            // set the value
            pstmt.setString(1,cropName);
            //
            ResultSet rs  = pstmt.executeQuery();

            tmp = rs.getDouble("GrossMarginHa");
            expanditureValue = tmp;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public void getTonPerHa(String cropName){
        tonePerHec = 0.0;
        String sql = "select TonePerHectre FROM Duration where cropName=?";
        double tmp = 0.0;
        try (Connection conn = this.connect();
             PreparedStatement pstmt  = conn.prepareStatement(sql)){

            // set the value
            pstmt.setString(1,cropName);
            //
            ResultSet rs  = pstmt.executeQuery();

            tmp = rs.getDouble("GrossMarginHa");
            tonePerHec = tmp;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    //Original KC calculation (without Ke soil water wetting data).

    public void KcStageValue(String cropName, String cropStage, String irrigationType){
        //Temp calculation value
        KcValue = 0.0;
        double KcMax = 0.0;
        double KcMin = 0.0;
        double KcCurrent = 0.0;
        double Fc = 0.0;
        double floweringValue = 0.0;
        double germinationValue = 0.0;
        double developmentValue = 0.0;
        double ripeningValue = 0.0;
        double hight = 0.0;
        //double kWater = 0.0;

        //Phasing parameters
        //kWater = Double.parseDouble(soilWaterRate);


        getIrrigationTypeValue(irrigationType);
        //System.out.println("irrigation type value:" + irrigationRate);


        //cropStage Categories

        if(cropName.equals("Pasture") && cropStage.equals("Initial")){
            cropStage = "Flowering";
        }else if(cropName.equals("Pasture") && cropStage.equals("Late Season")){
            cropStage = "Ripening";
        }

        String sql = "SELECT Flowering, Germination, Development, Ripening, MaxCropHight FROM Duration WHERE Crop=?";
        try(Connection conn = this.connect();
            PreparedStatement pstmt = conn.prepareStatement(sql)){
            //Set the value
            pstmt.setString(1, cropName);
            //
            ResultSet rs = pstmt.executeQuery();
            floweringValue = rs.getDouble("Flowering");
            germinationValue = rs.getDouble("Germination");
            developmentValue = rs.getDouble("Development");
            ripeningValue = rs.getDouble("Ripening");
            hight = rs.getDouble("MaxCropHight");
            KcCurrent = rs.getDouble(cropStage);
            //System.out.println(germinationValue);
            //System.out.println(KcCurrent);
        }catch(SQLException e){
            System.out.println(e.getMessage());
        }

        //Calculating Min and Max Kc
        double[] valueList = {floweringValue, germinationValue, developmentValue, ripeningValue};
        sortAlgor(valueList);
        KcMax = valueList[valueList.length -1];
        KcMin = valueList[0];

        //System.out.println(floweringValue);
        //Calculating Fc
        Fc =  fcCal(KcCurrent, KcMin, KcMax, hight);

        //Finding the minimum of Few
        double[] Fvalue = {1-Fc,irrigationRate};
        sortAlgor(Fvalue);
        Fc = Fvalue[0] * KcMax;

        //Ke calculation
        double Ke = keCal(KcMax, KcCurrent);

        //comparing Ke and Fc
        double temp = 0.0;
        if (Ke <= Fc) {
            temp = Ke;
        } else {
            temp = Fc;
        }

        //result of Kc value which include irrigation system and soil type data
        KcValue = KcCurrent + temp;
        //System.out.println("Kr is " + temp);
        //System.out.println("Kc based current is" + KcCurrent);
        //System.out.println("Kc Stage value is " + KcValue);
    }

//Method session

    //Bouble sort
    static void sortAlgor(double[] arr){
        int n = arr.length;
        double temp = 0.0;
        for (int i = 0; i < arr.length; i++) {
            for(int j = 1; j < (n-i); j++ ){
                if(arr[j-1] > arr [j]){
                    //swap element
                    temp = arr[j-1];
                    arr[j-1] = arr[j];
                    arr[j] = temp;
                }
            }
        }
    }

    static double fcCal(double KcCurrent, double KcMin, double KcMax, double hight ){
        double temp = ((KcCurrent - KcMin)/(KcMax - KcMin));
        temp = Math.pow(temp, (1+(0.5*hight)));
        return temp;
    }

    static double keCal(double KcMax, double KcCurrent){
        double temp = (KcMax - KcCurrent);
        return temp;
    }


}