package Agent;
import jade.core.AID;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class CombinatorialBidderGUI extends JFrame {
    private CombinatorialBidder bidderAgent;

    //Creating setter and getter for passing parameters.
    public static Double sPrice, sVolumnToBuy;

    public void setPrice(Double Price){
        sPrice = Price;
    }
    public static Double getPrice(){
        return sPrice;
    }
    public void setVolumeToBuy(Double volumeToBuy){
        sVolumnToBuy = volumeToBuy;
    }
    public static Double getVolumnToBuy(){
        return sVolumnToBuy;
    }

    private JTextField buyingPriceField, volumeToBuyField;
    private JTextArea log;

    CombinatorialBidderGUI(CombinatorialBidder a){
        super(a.getLocalName().concat(" Bidding information"));
        bidderAgent = a;

        JPanel p = new JPanel();
        p.setLayout(new GridLayout(2, 2));
        p.add(new JLabel("Buying price:"));
        buyingPriceField = new JTextField(5);
        p.add(buyingPriceField);

        p.add(new JLabel("Volume to buy:"));
        volumeToBuyField = new JTextField(5);
        p.add(volumeToBuyField);


        getContentPane().add(p, BorderLayout.CENTER);

        //log area create
        log = new JTextArea(5,20);
        log.setEditable(false);
        getContentPane().add(log, BorderLayout.CENTER);
        log.setMargin(new Insets(5,5,50,5));
        JScrollPane logScrollPane = new JScrollPane(log);
        getContentPane().add(logScrollPane, BorderLayout.CENTER);

        JButton addButton = new JButton("Bid");
        addButton.addActionListener( new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                try {
                    String buyingPirce = buyingPriceField.getText().trim();
                    setPrice(Double.parseDouble(buyingPirce));
                    String volume = volumeToBuyField.getText().trim();
                    setVolumeToBuy(Double.parseDouble(volume));
                    bidderAgent.bidderInput(getPrice(), getVolumnToBuy());
                    buyingPriceField.setText("");
                    volumeToBuyField.setText("");

                }
                catch (Exception e) {
                    JOptionPane.showMessageDialog(CombinatorialBidderGUI.this, "Invalid values. "+e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        } );
        //p = new JPanel();
        p.add(addButton);
        getContentPane().add(p, BorderLayout.SOUTH);

        // Make the agent terminate when the user closes
        // the GUI using the button on the upper right corner
        addWindowListener(new	WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                bidderAgent.doDelete();
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

