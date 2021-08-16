/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @key headful
 * @bug 8158230
 * @summary Verify menu item option apple.laf.useScreenMenuBar implementation
 * @requires (os.family=="mac")
 * @run main ClickMenuTestManual
 */

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import static javax.swing.WindowConstants.EXIT_ON_CLOSE;

public class ClickMenuTestManual implements
        ActionListener {

    public static final String TEST_STRING = "STRING";

    private static GridBagLayout layout;
    private static JPanel mainControlPanel;
    private static JPanel instructionPanel;
    private static JPanel testPanel;

    private static JPanel resultButtonPanel;
    private static JPanel controlPanel;
    private static JTextArea instructionTextArea;
    private static JTextArea testTextArea;
    private static JButton passButton;
    private static JButton failButton;
    private static JMenu menu;
    private static JMenuBar menuBar;
    private static JMenuItem menuItem;
    private static JFrame mainFrame;

    public static void main(String[] args) throws Exception {
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        ClickMenuTestManual test
                = new ClickMenuTestManual();
    }

    public ClickMenuTestManual() throws Exception {
        createControlPanelUI();
    }

    public final void createControlPanelUI() throws Exception {
        layout = new GridBagLayout();
        mainControlPanel = new JPanel(layout);
        instructionPanel = new JPanel(layout);
        testPanel = new JPanel(layout);
        resultButtonPanel = new JPanel(layout);
        controlPanel = new JPanel(layout);

        GridBagConstraints gbc = new GridBagConstraints();
        String instructions
                = "1) Click on MENU using mouse "
                + "\n2) Click on MENU ITEM using mouse "
                + "\n3) Check output on textArea if equal to STRING "
                + "\n\n If correct string, press \"Pass\" "
                + "\n Otherwise, press \"Fail\" ";
        instructionTextArea = new JTextArea();
        instructionTextArea.setText(instructions);
        instructionTextArea.setEnabled(false);
        instructionTextArea.setDisabledTextColor(Color.black);
        instructionTextArea.setBackground(Color.white);
        instructionTextArea.setBorder(
                BorderFactory.createLineBorder(Color.black));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        instructionPanel.add(instructionTextArea, gbc);
        testTextArea = new JTextArea();
        testTextArea.setEnabled(true);
        testTextArea.setDisabledTextColor(Color.black);
        testTextArea.setBackground(Color.white);
        testTextArea.setBorder(
                BorderFactory.createLineBorder(Color.black));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        testPanel.add(testTextArea, gbc);
        passButton = new JButton("Pass");
        passButton.setActionCommand("Pass");
        passButton.addActionListener(this);
        failButton = new JButton("Fail");
        failButton.setActionCommand("Fail");
        failButton.addActionListener(this);
        gbc.gridx = 0;
        gbc.gridy = 0;
        resultButtonPanel.add(passButton, gbc);
        gbc.gridx = 1;
        gbc.gridy = 0;
        resultButtonPanel.add(failButton, gbc);
        gbc.gridx = 0;
        gbc.gridy = 0;
        mainControlPanel.add(instructionPanel, gbc);
        gbc.gridx = 0;
        gbc.gridy = 1;
        mainControlPanel.add(testPanel, gbc);
        gbc.gridx = 0;
        gbc.gridy = 2;
        mainControlPanel.add(resultButtonPanel, gbc);
        gbc.gridx = 0;
        gbc.gridy = 3;
        mainControlPanel.add(controlPanel, gbc);
        mainFrame = new JFrame("Control Panel");
        mainFrame.add(mainControlPanel);
        menuBar = new JMenuBar();
        menu = new JMenu("MENU");
        menuItem = new JMenuItem("MENU ITEM");
        menuItem.addActionListener((e) -> {
            testTextArea.setText(TEST_STRING);
        });
        menu.add(menuItem);
        menuBar.add(menu);
        mainFrame.setJMenuBar(menuBar);
        mainFrame.pack();
        mainFrame.setDefaultCloseOperation(EXIT_ON_CLOSE);
        mainFrame.setVisible(true);

    }

    @Override
    public void actionPerformed(ActionEvent evt) {
        if (evt.getSource() instanceof JButton) {
            JButton btn = (JButton) evt.getSource();
            if (btn.getActionCommand().equals("Pass")) {
                try {
                    cleanUp();
                } catch (Exception ex) {
                    Logger.getLogger(ClickMenuTestManual.class
                            .getName()).log(Level.SEVERE, null, ex);
                }
            } else if (btn.getActionCommand().equals("Fail")) {
                try {
                    cleanUp();
                } catch (Exception ex) {
                    Logger.getLogger(ClickMenuTestManual.class
                            .getName()).log(Level.SEVERE, null, ex);
                }
                throw new AssertionError("Test case has failed");

            }
        }
    }

    private static void cleanUp() {
        mainFrame.dispose();
    }

}
