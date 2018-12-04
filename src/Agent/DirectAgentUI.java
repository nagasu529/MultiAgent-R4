package Agent;

import static com.sun.java.accessibility.util.AWTEventMonitor.addWindowListener;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.AncestorListener;

/**
 *
 * @author Kitti Chiewchan
 */

public class DirectAgentUI extends JFrame{
    private DirectAgent myAgent;

    //Creating setter and getter for passing parameters.
    public static String sFileDir;
    public static Double sActualReduc;
    public static int sEtSeason;
    public static String sAgentStatus;


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

    public void setAgentStatus(String agentStatus){
        sAgentStatus = agentStatus;
    }
    public static String getAgentStatus(){
        return sAgentStatus;
    }

    //GUI design preferences
    private JTextField actualReducField;
    private JButton calculateButton, textDirButton;
    private JFileChooser choosingDir;
    private JTextArea log;

    DirectAgentUI(DirectAgent a) {
        super(a.getLocalName());
        myAgent = a;

        //Create a file chooser
        choosingDir = new JFileChooser();

        //Combobox Buyer/Seler preferences and action listerner.
        String[] agentWorkStirng = {"Seller","Buyer"};

        //Combobox ET0 preference and action listerner.
        String[] etListStrings = { "ET0-Spring", "ET0-Summer", "ET0-Autumn", "ET0-Winter"};

        //Open file button and action listerner
        textDirButton = new JButton("Open file");
        textDirButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                JFileChooser chooser = new JFileChooser();
                int returnVal = chooser.showOpenDialog(DirectAgentUI.this);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File f = chooser.getSelectedFile();
                    String filename = f.getAbsolutePath();
                    displayUI("Farming scheduale uploaded\n");
                    setFileDir(filename);
                }
            }
        });
        JComboBox stageList = new JComboBox(agentWorkStirng);
        stageList.setSelectedIndex(1);
        stageList.setEditable(false);
        stageList.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                if (stageList.getSelectedIndex()==0) {
                    setAgentStatus("seller");
                    myAgent.farmerInfo.agentType = "seller";
                    displayUI("Agent status updated to seller\n");

                } else {
                    setAgentStatus("buyer");
                    //System.out.println("Agent status updated to buyer");
                    displayUI("Agent status updated to buyer\n");
                    myAgent.farmerInfo.agentType = "buyer";
                }
            }
        });

        JPanel controls = new JPanel();
        getContentPane().add(controls, BorderLayout.NORTH);
        controls.add(stageList);
        controls.add(textDirButton);
        controls.add(new JLabel("actual water reduction (%)"));
        actualReducField = new JTextField(15);
        controls.add(actualReducField);
        controls.setBorder(BorderFactory.createTitledBorder("Farmer input"));
        JComboBox etList = new JComboBox(etListStrings);
        controls.add(etList);
        etList.setSelectedIndex(3);
        etList.setEditable(false);

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
                    myAgent.farmerInput(getFileDir(), getActualReduc(),getEtSeason());
                    //fileDirField.setText("");
                    actualReducField.setText("");

                }
                catch (Exception e) {
                    JOptionPane.showMessageDialog(DirectAgentUI.this, "Invalid values. "+e.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
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
