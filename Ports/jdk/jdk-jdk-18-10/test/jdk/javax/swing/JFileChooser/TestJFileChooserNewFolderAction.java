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
 * @bug 8231042
 * @requires (os.family == "mac")
 * @summary Verify that JFileChooser's New Folder option works correctly
 *  in AquaLookAndFeel
 * @run main/manual TestJFileChooserNewFolderAction
 */

import javax.swing.JFrame;
import javax.swing.JTextArea;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import java.awt.Color;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class TestJFileChooserNewFolderAction {
    private static final JFrame instructionFrame = new JFrame();
    private static final String lafName = "AquaLookAndFeel";
    private static volatile boolean testPassed = false;
    private static volatile CountDownLatch countDownLatch;

    private static final String INSTRUCTIONS = "INSTRUCTIONS:\n\n" +
            "Make sure there is no folder named \"uninitializedValue\" in\n"+
            "JFileChooser directory, else change JFileChooser directory.\n"+
            "Click on \"New Folder\" button in JFileChooser. \n" +
            "A JOptionPane will be opened to write the name of the folder.\n"+
            "Press \"ESC\" key to close the JOptionPane without creating \n"+
            "new folder. Verify that JFileChooser current directory has not\n"+
            "changed and there is no folder named \"uninitializedValue\"\n"+
            "created in the JFileChooser directory.\n" +
            "If yes, Press Pass, Otherwise, Press Fail.\n";


    public static void main(String[] args) throws Exception{

        if (!System.getProperty("os.name").startsWith("Mac")) {
            System.out.println("This test is meant for Mac platform only");
            return;
        }

        countDownLatch = new CountDownLatch(1);
        for (UIManager.LookAndFeelInfo lookAndFeelInfo :
                UIManager.getInstalledLookAndFeels()) {
            if (lookAndFeelInfo.getClassName().contains(lafName)) {
                try {
                    UIManager.setLookAndFeel(lookAndFeelInfo.getClassName());
                } catch (final UnsupportedLookAndFeelException ignored) {
                    System.out.println("Aqua L&F could not be set, so this " +
                            "test can not be run in this scenario ");
                    return;
                }
            }
        }

        SwingUtilities.invokeAndWait(TestJFileChooserNewFolderAction::createInstructionUI);

        SwingUtilities.invokeAndWait(TestJFileChooserNewFolderAction::createTestUI);

        countDownLatch.await(15, TimeUnit.MINUTES);

        SwingUtilities.invokeAndWait(TestJFileChooserNewFolderAction::disposeUI);
        if (!testPassed) {
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
            testPassed = true;
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
        instructionFrame.setVisible(true);
    }

    private static void createTestUI() {
        new JFileChooser().showSaveDialog(null);
    }

    private static void disposeUI() {
        instructionFrame.dispose();
    }
}
