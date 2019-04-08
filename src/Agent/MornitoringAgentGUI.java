package Agent;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class MornitoringAgentGUI extends JFrame{
    //Agent class
    private MornitoringAgent myAgent;

    private JTextArea sellerLog;
    private JTextArea bidderLog;

    MornitoringAgentGUI(MornitoringAgent a){
        super(a.getLocalName() + " Monitoring Agent");
        myAgent = a;

        //sellerLog area with logscrollPane
        sellerLog = new JTextArea(5,20);
        sellerLog.setEditable(false);
        //getContentPane().add(sellerLog, BorderLayout.CENTER);
        //sellerLog.setMargin(new Insets(5,5,50,5));
        //JScrollPane sellerlogScrollPane = new JScrollPane(sellerLog);
        //getContentPane().add(sellerlogScrollPane, BorderLayout.CENTER);

        //bidderLog area with logscrollPane
        bidderLog = new JTextArea(5,20);
        bidderLog.setEditable(false);
        getContentPane().add(bidderLog, BorderLayout.CENTER);
        bidderLog.setMargin(new Insets(5,5,50,5));
        JScrollPane bidderLogScrollPane = new JScrollPane(bidderLog);
        getContentPane().add(bidderLogScrollPane, BorderLayout.CENTER);

        //Labeling (seller and bidder)
        JLabel sellerLabel = new JLabel("seller side");
        JLabel bidderLabel = new JLabel("bidder side");

        //setup container
        Container cp = this.getContentPane();
        cp.setLayout(new BorderLayout(100,100));
        cp.add(sellerLabel, BorderLayout.WEST);
        cp.add(bidderLabel, BorderLayout.EAST);
        cp.add(sellerLog, BorderLayout.WEST);
        cp.add(bidderLog, BorderLayout.EAST);


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

    public void displaysellerUI(String displayUI) {
        sellerLog.append(displayUI);
        sellerLog.setCaretPosition(sellerLog.getDocument().getLength());
    }

    public void displaybidderUI(String displayUI){
        bidderLog.append(displayUI);
        bidderLog.setCaretPosition(bidderLog.getDocument().getLength());
    }
}
