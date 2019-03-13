package Agent;


import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;

/**
 *
 * @author Kitti Chiewchan
 */
public class CombinatorialSellerGUI extends JFrame {
    //Farming agent class
    private CombinatorialSeller myAgent;

    //Creating setter and getter for passing parameters.
    public static String sFileDir;
    public static Double sActualReduc;
    public static int sEtSeason;
    public static String sAgentStatus;
    public static int sEuDecision;


    public void setFileDir(String fileDir){
        sFileDir = fileDir;
    }
    public static String getFileDir(){
        return sFileDir;
    }

    public void setActualReduc(Double actualReduc){
        sActualReduc = actualReduc;
    }

    public static Double getActualReduc(){
        return sActualReduc;
    }

    public void setEtSeason(int etSeason){
        sEtSeason = etSeason;
    }
    public static int getEtSeason(){
        return sEtSeason;
    }

    public void setEuDecision (int euDecision){sEuDecision = euDecision;}
    public static int getEuDecision(){return sEuDecision;}

    public void setAgentStatus(String agentStatus){
        sAgentStatus = agentStatus;
    }
    public static String getAgentStatus(){
        return sAgentStatus;
    }

    //GUI design preferences
    private JTextField actualReducField;
    private JButton calculateButton, textDirButton;
    //private JFileChooser choosingDir;
    private JTextArea log;

    CombinatorialSellerGUI(CombinatorialSeller a) {
        super(a.getLocalName());
        myAgent = a;

        //Combobox ET0 preference and action listerner.
        String[] etListStrings = { "ET0-Spring", "ET0-Summer", "ET0-Autumn", "ET0-Winter"};
        String[] euListStrings = {"Maximun value Selling", "Maximium water usage", "Profit loss reduction", "balancing method"};

        //Open file button and action listerner
        textDirButton = new JButton("Open file");
        textDirButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                JFileChooser chooser = new JFileChooser();
                int returnVal = chooser.showOpenDialog(CombinatorialSellerGUI.this);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File f = chooser.getSelectedFile();
                    String filename = f.getAbsolutePath();
                    displayUI("Farming scheduale uploaded\n");
                    //System.out.println("Farming scheduale uploaded");
                    //System.out.println(filename);
                    setFileDir(filename);
                }
            }
        });
        JPanel controls = new JPanel();
        getContentPane().add(controls, BorderLayout.NORTH);
        controls.add(textDirButton);
        controls.add(new JLabel("actual water reduction (%)"));
        actualReducField = new JTextField(15);
        controls.add(actualReducField);
        controls.setBorder(BorderFactory.createTitledBorder("Farmer input"));
        JComboBox etList = new JComboBox(etListStrings);
        JComboBox euList = new JComboBox(euListStrings);
        controls.add(etList);
        controls.add(euList);
        etList.setSelectedIndex(0);
        etList.setEditable(false);
        euList.setSelectedIndex(0);
        euList.setEditable(false);

        //log area create
        log = new JTextArea(5,20);
        log.setEditable(false);
        getContentPane().add(log, BorderLayout.CENTER);
        log.setMargin(new Insets(5,5,50,5));
        JScrollPane logScrollPane = new JScrollPane(log);
        getContentPane().add(logScrollPane, BorderLayout.CENTER);

        //Calculation button created and action Listener
        calculateButton = new JButton("Calculate");
        controls.add(calculateButton);
        calculateButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                try {
                    String actualReduc = actualReducField.getText().trim();
                    setActualReduc(Double.parseDouble(actualReduc));
                    myAgent.farmerInput(getFileDir(), getActualReduc(),getEtSeason(),getEuDecision());
                    //fileDirField.setText("");
                    actualReducField.setText("");

                }
                catch (Exception e) {
                    JOptionPane.showMessageDialog(CombinatorialSellerGUI.this, "Invalid values. "+e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        } );
        etList.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                if(etList.getSelectedIndex()==0){
                    setEtSeason(0);
                    displayUI("Spring ET0 choosed\n");

                }else if(etList.getSelectedIndex()==1){
                    setEtSeason(1);
                    //System.out.println("Summer ET0 choosed");
                    displayUI("Summer ET0 choosed\n");
                }else if(etList.getSelectedIndex()==2){
                    setEtSeason(2);
                    //System.out.println("Autumn ET0 choosed");
                    displayUI("Autumn ET0 choosed\n");
                }else {
                    setEtSeason(3);
                    //System.out.println("Winter ET0 choosed");
                    displayUI("Winter ET0 choosed\n");
                }
            }
        });

        euList.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                if(euList.getSelectedIndex()==0){
                    setEuDecision(0);
                    displayUI("Decision method: Maximun value Selling\n");

                }else if(euList.getSelectedIndex()==1){
                    setEuDecision(1);
                    displayUI("Decision method: Maximium water usage\n");
                }else if(euList.getSelectedIndex()==2){
                    setEuDecision(2);
                    displayUI("Decision method: Profit loss reduction");
                }else {
                    setEuDecision(3);
                    displayUI("Decision method: Balancing method\n");
                }
            }
        });

        // Make the agent terminate when the user closes
        // the GUI using the button on the upper right corner
        addWindowListener(new	WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                myAgent.doDelete();
            }
        } );
        setResizable(false);
    }

    public void show() {
        pack();
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int centerX = (int)screenSize.getWidth() / 2;
        int centerY = (int)screenSize.getHeight() / 2;
        setLocation(centerX - getWidth() / 2, centerY - getHeight() / 2);
        super.show();
    }

    public void displayUI(String displayUI) {
        log.append(displayUI);
        log.setCaretPosition(log.getDocument().getLength());
    }
}
