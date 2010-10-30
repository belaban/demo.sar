package org.jboss.client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * @author Bela Ban
 * @version $Id$
 */
public class bla {

    public static void main(String[] args) {
        Frame mainFrame=new JFrame("Chat demo");
        mainFrame.setPreferredSize(new Dimension(600,600));
        mainFrame.setBackground(Color.white);

        Box main_box=Box.createVerticalBox();
        // main_box.setAlignmentX(Component.LEFT_ALIGNMENT);

        main_box.setBackground(Color.white);
        Box input=Box.createHorizontalBox();   // input field
        Box buttons=Box.createHorizontalBox(); // for all the buttons
        mainFrame.add(main_box);

        main_box.add(Box.createVerticalStrut(10));


        JLabel cluster=new JLabel("Cluster: 4 member(s): A, B, C, D", SwingConstants.CENTER);
        // cluster.setAlignmentX(Component.LEFT_ALIGNMENT);
        // cluster.setHorizontalAlignment(SwingConstants.LEFT);
        cluster.setHorizontalTextPosition(SwingConstants.LEFT);

        main_box.add(cluster);
        main_box.add(Box.createVerticalStrut(10));

        TextArea txtArea=new TextArea();
        txtArea.setPreferredSize(new Dimension(550, 500));
        txtArea.setEditable(false);
        txtArea.setBackground(Color.white);
        main_box.add(txtArea);

        main_box.add(Box.createVerticalStrut(10));
        main_box.add(input);
        main_box.add(Box.createVerticalStrut(10));
        main_box.add(buttons);

        JLabel csLabel=new JLabel("Send:");
        csLabel.setPreferredSize(new Dimension(85, 30));
        input.add(csLabel);

        final JTextField txtField=new JTextField();
        txtField.setPreferredSize(new Dimension(200, 30));
        txtField.setBackground(Color.white);
        input.add(txtField);


        JButton leaveButton=new JButton("Leave");
        leaveButton.setPreferredSize(new Dimension(150, 30));
        buttons.add(leaveButton);

        JButton sendButton=new JButton("Send");
        sendButton.setPreferredSize(new Dimension(150, 30));
        buttons.add(sendButton);

        JButton clearButton=new JButton("Clear");
        clearButton.setPreferredSize(new Dimension(150, 30));
        buttons.add(clearButton);

        JLabel status=new JLabel("");
        status.setForeground(Color.red);
        main_box.add(status);

        mainFrame.pack();
        mainFrame.setLocation(15, 25);
        Dimension main_frame_size=mainFrame.getSize();
        txtArea.setPreferredSize(new Dimension((int)(main_frame_size.width * 0.9), (int)(main_frame_size.height * 0.8)));
        mainFrame.setVisible(true);
        txtField.setFocusable(true);
        txtField.requestFocusInWindow();
        txtField.setToolTipText("type and then press enter to send");
        txtField.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                String cmd=e.getActionCommand();
                if(cmd != null && cmd.length() > 0) {
                    System.out.println("cmd = " + cmd);
                    txtField.selectAll();
                }
            }
        });

    }
}

