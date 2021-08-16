/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8216329
 * @summary Verify that JCheckBoxMenuItem is resized properly in Synth L&F
 * @run main/manual TestJCheckBoxMenuItem
 */

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.ByteArrayInputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.synth.SynthLookAndFeel;
import javax.swing.UnsupportedLookAndFeelException;

public class TestJCheckBoxMenuItem {

    private static final JFrame instructionFrame = new JFrame();
    private static final JFrame testFrame = new JFrame();

    private static volatile boolean testResult = false;
    private static volatile CountDownLatch countDownLatch;
    private static final String INSTRUCTIONS = "INSTRUCTIONS:\n\n" +
            "Click on the menu \"Menu\" in Test frame. Verify that the \n" +
            "JCheckBoxMenuItem is resized properly and text \n" +
            "\"JCheckBoxMenuItem Sample Text\" is not truncated.\n" +
            "If yes, Press Pass, Otherwise, Press Fail.";

    private static final String synthStyleXML =
            "<synth>" +
                "<style id=\"Default\">" +
                    "<font name=\"Dialog\" size=\"14\" />" +
                "</style>" +
                "<bind style=\"Default\" type=\"region\" key=\".*\" />" +
                "<style id=\"Check Box Menu Item\">" +
                    "<imageIcon id=\"CheckBoxMenuItem_Check_Icon_Selected\" " +
                        "path=\"Check_Icon.png\" />" +
                    "<insets top=\"4\" left=\"9\" bottom=\"4\" right=\"9\" />"+
                    "<state>" +
                        "<property key=\"CheckBoxMenuItem.checkIcon\" " +
                            "value=\"CheckBoxMenuItem_Check_Icon_Selected\" />"+
                        "<imagePainter method=\"checkBoxMenuItemBackground\" "+
                            "path=\"MenuItem_Selected.png\" " +
                            "sourceInsets=\"2 2 2 2\" paintCenter=\"true\" " +
                            "stretch=\"true\" center=\"false\" />"+
                    "</state>" +
                "</style>" +
                "<bind style=\"Check Box Menu Item\" type=\"region\" " +
                    "key=\"CheckBoxMenuItem\" />" +
            "</synth>";
    public static void main(String[] args) throws Exception {
        countDownLatch = new CountDownLatch(1);

        SwingUtilities.invokeAndWait(TestJCheckBoxMenuItem::createInstructionUI);

        SynthLookAndFeel lookAndFeel = new SynthLookAndFeel();
        lookAndFeel.load(
                new ByteArrayInputStream(synthStyleXML.getBytes("UTF8")),
                TestJCheckBoxMenuItem.class);
        try {
            UIManager.setLookAndFeel(lookAndFeel);
        } catch (final UnsupportedLookAndFeelException ignored) {
            System.out.println("Synth L&F could not be set, so this test can" +
                    "not be run in this scenario ");
            return;
        }
        SwingUtilities.invokeAndWait(TestJCheckBoxMenuItem::createTestUI);

        countDownLatch.await(15, TimeUnit.MINUTES);

        disposeUI();
        if (!testResult) {
            throw new RuntimeException("Test failed!");
        }
    }

    private static void createInstructionUI() {
        GridBagLayout layout = new GridBagLayout();
        JPanel mainControlPanel = new JPanel(layout);
        JPanel resultButtonPanel = new JPanel(layout);

        GridBagConstraints gbc = new GridBagConstraints();

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(5, 15, 5, 15);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextArea instructionTextArea = new JTextArea();
        instructionTextArea.setText(INSTRUCTIONS);
        instructionTextArea.setEditable(false);
        instructionTextArea.setBackground(Color.white);
        mainControlPanel.add(instructionTextArea, gbc);

        JButton passButton = new JButton("Pass");
        passButton.setActionCommand("Pass");
        passButton.addActionListener((ActionEvent e) -> {
            testResult = true;
            countDownLatch.countDown();

        });

        JButton failButton = new JButton("Fail");
        failButton.setActionCommand("Fail");
        failButton.addActionListener(e -> {
            countDownLatch.countDown();
        });

        gbc.gridx = 0;
        gbc.gridy = 0;

        resultButtonPanel.add(passButton, gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        resultButtonPanel.add(failButton, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        mainControlPanel.add(resultButtonPanel, gbc);

        instructionFrame.pack();
        instructionFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        instructionFrame.add(mainControlPanel);
        instructionFrame.pack();

        instructionFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                disposeUI();
                countDownLatch.countDown();
            }
        });
        instructionFrame.setVisible(true);
    }

    private static void createTestUI() {
        JMenuBar menuBar = new JMenuBar();
        JMenu menu = new JMenu("Menu");

        JCheckBoxMenuItem checkBoxMenuItem = new JCheckBoxMenuItem();
        checkBoxMenuItem.setSelected(false);
        checkBoxMenuItem.setMnemonic(KeyEvent.VK_K);
        checkBoxMenuItem.setText("JCheckBoxMenuItem Sample Text");
        checkBoxMenuItem.setHorizontalTextPosition(JCheckBoxMenuItem.LEADING);
        checkBoxMenuItem.setVerticalTextPosition(JCheckBoxMenuItem.CENTER);

        menu.add(checkBoxMenuItem);
        menuBar.add(menu);

        testFrame.setJMenuBar(menuBar);
        testFrame.setLocation(
                instructionFrame.getX() + instructionFrame.getWidth(),
                instructionFrame.getY());
        testFrame.setPreferredSize(new Dimension(instructionFrame.getWidth(),
                instructionFrame.getHeight()));
        testFrame.pack();
        testFrame.setTitle("Test");
        testFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        testFrame.setVisible(true);
    }

    private static void disposeUI() {
        instructionFrame.dispose();
        testFrame.dispose();
    }
}
