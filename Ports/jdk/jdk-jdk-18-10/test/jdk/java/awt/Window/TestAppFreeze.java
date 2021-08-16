/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 *  @test
 *  @bug 8256465
 *  @requires (os.family == "Mac")
 *  @summary Application should not freeze when opening dialog
 *  @key headful
 *  @run main/manual TestAppFreeze
 */

public class TestAppFreeze {
    private static volatile CountDownLatch countDownLatch;
    private static JFrame instructionFrame;
    private static JFrame testFrame;
    private static volatile boolean testPassed = false;

    private static boolean validatePlatform() {
        String osName = System.getProperty("os.name");
        if (osName == null) {
            throw new RuntimeException("Name of the current OS could not be" +
                    " retrieved.");
        }
        return osName.startsWith("Mac");
    }

    private static void createInstructionUI() {
        SwingUtilities.invokeLater(() -> {
            String instruction = "1. This test is only for Mac OS Version 10.12 " +
                    "or later, on other Mac OS just press PASS\n" +
                    "2. Go to System Preference -> General on Mac OS 11 or later\n" +
                    "3. Go to System Preference -> Dock on Mac OS 10.12 to 10.15\n" +
                    "4. Set Prefer tabs when opening documents to Always.\n" +
                    "5. Then click on the click button of the test frame\n" +
                    "6. The dialog should open in new window and Application " +
                    "should not freeze\n" +
                    "7. IF the conditions are met then press PASS else " +
                    "press FAIL";
            instructionFrame = new JFrame("Instruction Frame");
            JTextArea  textArea = new JTextArea(instruction);
            textArea.setEditable(false);
            final JButton passButton = new JButton("PASS");
            passButton.addActionListener((e -> {
                testPassed = true;
                instructionFrame.dispose();
                testFrame.dispose();
                countDownLatch.countDown();
            }));
            final JButton failButton = new JButton("FAIL");
            failButton.addActionListener((e) -> {
                instructionFrame.dispose();
                testFrame.dispose();
                countDownLatch.countDown();
            });

            JPanel mainPanel = new JPanel(new BorderLayout());
            mainPanel.add(textArea, BorderLayout.CENTER);

            JPanel buttonPanel =  new JPanel(new FlowLayout());
            buttonPanel.add(passButton);
            buttonPanel.add(failButton);
            mainPanel.add(buttonPanel, BorderLayout.SOUTH);
            instructionFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            instructionFrame.setBounds(0, 0, 500, 500);
            instructionFrame.add(mainPanel);
            instructionFrame.pack();
            instructionFrame.setVisible(true);
        });
    }

    private static void testApp() {
        SwingUtilities.invokeLater(() ->{
            testFrame = new JFrame("TestFrame");
            testFrame.setBounds(600, 0, 1000, 200);
            testFrame.getContentPane().add(new JButton(new AbstractAction("Click") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    JDialog dlg = new JDialog(testFrame, false);
                    dlg.setSize(500, 500);
                    dlg.getContentPane().add(new JTextArea());
                    dlg.setVisible(true);
                }
            }));
            testFrame.setVisible(true);
        });
    }

    public static void main(String[] args) throws Exception{
        if (!validatePlatform()) {
            System.out.println("This test is only for Mac OS");
            return;
        }
        countDownLatch = new CountDownLatch(1);
        TestAppFreeze testAppFreeze = new TestAppFreeze();
        testAppFreeze.createInstructionUI();
        testAppFreeze.testApp();
        countDownLatch.await(15, TimeUnit.MINUTES);

        if(!testPassed) {
            throw new RuntimeException("Test failed!");
        }
    }
}
