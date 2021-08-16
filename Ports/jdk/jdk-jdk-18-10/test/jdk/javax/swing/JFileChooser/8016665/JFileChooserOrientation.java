/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/*
 * @test
 * @key headful
 * @bug 8016665
 * @summary verifies different behaviour of JFileChooser changing orientation
 * @run main JFileChooserOrientation
 */
import java.awt.Color;
import java.awt.ComponentOrientation;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

public class JFileChooserOrientation {

    private static JFrame frame;
    private static GridBagLayout layout;
    private static JPanel panel;
    private static JPanel lookAndFeelPanel;
    private static JPanel orientationPanel;
    private static JPanel passFailPanel;
    private static JTextArea instructionsTextArea;
    private static JLabel lookAndFeelLabel;
    private static JLabel orientationLabel;
    private static JComboBox lookAndFeelComboBox;
    private static JComboBox orientationComboBox;

    private static JButton fileChooserButton;
    private static JButton passButton;
    private static JButton failButton;
    private static JFileChooser openChooser;
    private static UIManager.LookAndFeelInfo[] lookAndFeelArray;

    private static final String orientationLTR = " Left to Right";
    private static final String orientationRTL = " Right to Left";
    private static final String fileChooserString = "Show File Chooser";

    public static void main(String[] args) throws Exception {
        createManualTestUI();
    }

    private static void createManualTestUI() throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                layout = new GridBagLayout();
                GridBagConstraints gbc = new GridBagConstraints();
                panel = new JPanel(layout);
                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.gridx = 0;
                gbc.gridy = 0;
                instructionsTextArea = new JTextArea();
                String instructions
                        = "1) Select Look and feel from combobox"
                        + "\n2) Select component orientation"
                        + "\n3) Click on \"Show File Chooser\""
                        + "\n4) Check if orientation is as selected"
                        + "\n5) Press \"Cancel\" on the File Chooser Dialog"
                        + "\n\n Perform steps 1- 4 for all LAFs & orientations"
                        + "\n If all are correct press Pass or else press Fail";
                instructionsTextArea.setText(instructions);
                instructionsTextArea.setBorder(
                        BorderFactory.createLineBorder(Color.black));
                panel.add(instructionsTextArea, gbc);

                lookAndFeelPanel = new JPanel();
                lookAndFeelPanel.setBorder(
                        BorderFactory.createLineBorder(Color.black));
                lookAndFeelLabel = new JLabel("Look And Feel: ");
                gbc.gridx = 0;
                gbc.gridy = 0;
                lookAndFeelPanel.add(lookAndFeelLabel, gbc);

                lookAndFeelComboBox = new JComboBox();
                lookAndFeelArray = UIManager.getInstalledLookAndFeels();
                for (UIManager.LookAndFeelInfo lookAndFeelItem
                        : lookAndFeelArray) {
                    lookAndFeelComboBox.addItem(lookAndFeelItem.getClassName());
                }
                gbc.gridx = 1;
                gbc.gridy = 0;
                lookAndFeelPanel.add(lookAndFeelComboBox, gbc);
                gbc.gridx = 0;
                gbc.gridy = 1;
                panel.add(lookAndFeelPanel, gbc);

                orientationPanel = new JPanel();
                orientationPanel.setBorder(
                        BorderFactory.createLineBorder(Color.black));
                orientationLabel = new JLabel("Orientation: ");
                gbc.gridx = 0;
                gbc.gridy = 0;
                orientationPanel.add(orientationLabel, gbc);

                orientationComboBox = new JComboBox();
                orientationComboBox.addItem(orientationLTR);
                orientationComboBox.addItem(orientationRTL);
                gbc.gridx = 1;
                gbc.gridy = 0;
                orientationPanel.add(orientationComboBox, gbc);
                gbc.gridx = 0;
                gbc.gridy = 2;
                panel.add(orientationPanel, gbc);

                fileChooserButton = new JButton(fileChooserString);
                fileChooserButton.setActionCommand(fileChooserString);

                fileChooserButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {

                        try {
                            showFileChooser();
                        } catch (Exception ex) {
                            Logger.getLogger(JFileChooserOrientation.class
                                    .getName()).log(Level.SEVERE, null, ex);
                        }

                    }
                });
                gbc.gridx = 0;
                gbc.gridy = 3;
                panel.add(fileChooserButton, gbc);

                passFailPanel = new JPanel();
                passFailPanel.setBorder(BorderFactory.createLineBorder(Color.black));
                passButton = new JButton(" Pass ");
                passButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        try {
                            pass();
                        } catch (Exception ex) {
                            Logger.getLogger(JFileChooserOrientation.class
                                    .getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                });
                gbc.gridx = 0;
                gbc.gridy = 0;
                passFailPanel.add(passButton, gbc);
                failButton = new JButton(" Fail ");
                failButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        try {
                            fail();
                        } catch (Exception ex) {
                            Logger.getLogger(JFileChooserOrientation.class
                                    .getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                });
                gbc.gridx = 1;
                gbc.gridy = 0;
                passFailPanel.add(failButton, gbc);
                gbc.gridx = 0;
                gbc.gridy = 4;
                panel.add(passFailPanel, gbc);
                frame = new JFrame();
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setContentPane(panel);
                frame.pack();
                frame.setVisible(true);
            }
        });
    }

    private static void pass() throws Exception
    {

                frame.dispose();

    }

    private static void fail() throws Exception
    {

        frame.dispose();
        System.err.println(lookAndFeelComboBox.getSelectedItem().toString()
     + " : Incorrect Orientation");
    }

    private static void showFileChooser() throws Exception {
        if (tryLookAndFeel(lookAndFeelComboBox.getSelectedItem().toString())) {

            openChooser = new JFileChooser();

            ComponentOrientation orientation
                    = ComponentOrientation.UNKNOWN;

            switch (orientationComboBox.getSelectedItem().toString()) {
                case orientationLTR:
                    orientation = ComponentOrientation.LEFT_TO_RIGHT;
                    break;
                case orientationRTL:
                    orientation = ComponentOrientation.RIGHT_TO_LEFT;
                    break;
            }
            openChooser.setComponentOrientation(orientation);
            openChooser.showOpenDialog(frame);

        }
    }
    private static boolean tryLookAndFeel(String lookAndFeelString)
            throws Exception {
        try {
            UIManager.setLookAndFeel(
                    lookAndFeelString);
        } catch (UnsupportedLookAndFeelException
                | ClassNotFoundException
                | InstantiationException
                | IllegalAccessException e) {
            return false;
        }
        return true;
    }
}
