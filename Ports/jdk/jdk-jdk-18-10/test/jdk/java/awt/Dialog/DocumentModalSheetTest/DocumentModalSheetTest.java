/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8208543
 * @requires (os.family == "mac")
 * @summary Support for apple.awt.documentModalSheet incomplete in Mac
 * @run main/manual DocumentModalSheetTest
 */

import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.FlowLayout;
import javax.swing.JFrame;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JTextArea;
import javax.swing.Timer;
import javax.swing.SwingUtilities;

public class DocumentModalSheetTest {

    private static JFrame jFrame;
    private static JDialog jDialog;
    private static JFrame instructionFrame;
    private static Timer timer;
    private static final int sleepTime = 300000;
    private static volatile boolean testContinueFlag = true;
    private static final String TEST_INSTRUCTIONS =
    " This is a manual test\n\n" +
    " 1) A Modal dialog with label 'Modal Dialog as Sheet' will be displayed\n" +
    "   i) Press PASS if dialog appears as a sheet\n" +
    "   ii) Press FAIL otherwise\n" +
    " 2) A Modal dialog with label 'Modal Dialog as Window' will be displayed\n" +
    "   i) Press PASS if dialog appears as a Window\n" +
    "   ii) Press FAIL otherwise\n";
    private static String FAIL_MESSAGE = "Modal dialog displayed as a new window";

    private static void createAndShowInstructionFrame() throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                JButton passButton = new JButton("Pass");
                passButton.setEnabled(true);

                JButton failButton = new JButton("Fail");
                failButton.setEnabled(true);

                JTextArea instructions = new JTextArea(5, 60);
                instructions.setText(TEST_INSTRUCTIONS);

                instructionFrame = new JFrame("Test Instructions");
                instructionFrame.add(passButton);
                instructionFrame.add(failButton);
                instructionFrame.add(instructions);
                instructionFrame.setSize(200,200);
                instructionFrame.setLayout(new FlowLayout());
                instructionFrame.pack();
                instructionFrame.setVisible(true);

                passButton.addActionListener(ae -> {
                    jDialog.setVisible(false);
                    timer.stop();
                    dispose();
                });

                failButton.addActionListener(ae -> {
                    jDialog.setVisible(false);
                    timer.stop();
                    dispose() ;
                    testContinueFlag = false;
                    throw new RuntimeException(FAIL_MESSAGE);
                });
            }
        });
    }

    private static void createAndShowModalDialog() throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                try {
                    //Display Modal Dialog as a SHEET
                    jFrame = new JFrame();
                    createAndShowModalSheet(jFrame, "Modal Dialog as Sheet");
                    if (testContinueFlag) {
                        //Display Modal Dialog as a Window
                        FAIL_MESSAGE = "Modal dialog displayed as a Sheet";
                        createAndShowModalSheet(null, "Modal Dialog as Window");
                        testContinueFlag = false;
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Modal dialog creation failed");
                } finally {
                    if (instructionFrame != null) {
                        instructionFrame.dispose();
                    }
                }
            }
        });
    }

    private static void createAndShowModalSheet(JFrame frame, String label) throws Exception {
        jDialog = new JDialog(frame, null, Dialog.ModalityType.DOCUMENT_MODAL);
        jDialog.setSize(200, 200);
        jDialog.getRootPane().putClientProperty("apple.awt.documentModalSheet", Boolean.TRUE);
        JLabel jLabel = new JLabel(label);
        jDialog.add(jLabel);

        timer = new Timer(sleepTime, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jDialog.setVisible(false);
                testContinueFlag = false;
                dispose();
                throw new RuntimeException("Timed out after " +
                                           sleepTime / 1000 + " seconds");
            }
        });
        timer.setRepeats(false);
        timer.start();

        jDialog.pack();
        jDialog.setVisible(true);
    }

    private static void dispose() {
        if (jDialog != null) {
            jDialog.dispose();
        }
        if (jFrame != null) {
            jFrame.dispose();
        }
    }

    public static void main(String[] args) throws Exception {
        createAndShowInstructionFrame();
        createAndShowModalDialog();
    }
}
