/*
 * Copyright (c) 2007, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6365992 6379599 8137137
 * @summary REG: Showing and disposing a native print dialog makes the main
 *  frame inactive, Win32
 * @run main/manual RestoreActiveWindowTest
 */
import java.awt.Frame;
import java.awt.Button;
import java.awt.GridBagLayout;
import java.awt.Panel;
import java.awt.TextArea;
import java.awt.GridLayout;
import java.awt.GridBagConstraints;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.print.PrinterJob;
import java.awt.print.PageFormat;

public class RestoreActiveWindowTest implements ActionListener {

    private static Frame mainFrame;
    private static Button printDialogButton;
    private static Button pageDialogButton;
    private static Frame instructionFrame;
    private static GridBagLayout layout;
    private static Panel mainControlPanel;
    private static Panel resultButtonPanel;
    private static TextArea instructionTextArea;
    private static Button passButton;
    private static Button failButton;
    private static Thread mainThread = null;
    private static boolean testPassed = false;
    private static boolean isInterrupted = false;
    private static final int testTimeOut = 300000;
    private static String testFailMessage;

    public void createAndShowGUI() {
        mainFrame = new Frame("Test");
        mainFrame.setSize(200, 200);
        mainFrame.setLocationRelativeTo(null);
        mainFrame.setLayout(new GridLayout(2, 1));

        printDialogButton = new Button("show a native print dialog");
        pageDialogButton = new Button("show a native page dialog");
        printDialogButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                PrinterJob.getPrinterJob().printDialog();
                setButtonEnable(true);
                testFailMessage = "Print dialog test failed.";
            }
        });
        pageDialogButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                PrinterJob.getPrinterJob().pageDialog(new PageFormat());
                setButtonEnable(true);
                testFailMessage = "Page dialog test failed.";
            }
        });

        mainFrame.add(printDialogButton);
        mainFrame.add(pageDialogButton);
        mainFrame.setVisible(true);

       mainFrame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
                cleanUp();
                throw new RuntimeException("User has closed the test window "
                        + "without clicking Pass or Fail.");
            }
        });
    }

    private void createInstructionUI() {
        instructionFrame = new Frame("Native Print Dialog and Page Dialog");
        layout = new GridBagLayout();
        mainControlPanel = new Panel(layout);
        resultButtonPanel = new Panel(layout);

        GridBagConstraints gbc = new GridBagConstraints();
        String instructions
                = "\nINSTRUCTIONS:\n"
                + "\n   1. Click on the 'show a native print dialog' button. A "
                + "native print dialog will come up."
                + "\n   2. Click on the 'Cancel' button on Mac OS X or "
                + "'close'(X) on other paltforms. Dialog will be closed."
                + "\n   3. After the dialog is closed another window should "
                + "become the active window."
                + "\n   4. If there no any active window then the test has "
                + "failed. Click on 'Fail' Button."
                + "\n   5. Click on the 'show a native page dialog' button. A "
                + "native page dialog will come up."
                + "\n   6. Click on the 'Cancel' button on Mac OS X or "
                + "'close'(X) on other paltforms. Dialog will be closed."
                + "\n   7. After the dialog is closed another window should "
                + "become the active window."
                + "\n   8. If there no any active window then the test has "
                + "failed. Click on 'Fail' Button."
                + "\n   9. Test Passed. Click on 'Pass' Button.";

        instructionTextArea = new TextArea(13, 80);
        instructionTextArea.setText(instructions);
        instructionTextArea.setEnabled(false);
        instructionTextArea.setBackground(Color.white);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.5;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        mainControlPanel.add(instructionTextArea, gbc);

        passButton = new Button("Pass");
        passButton.setName("Pass");
        passButton.addActionListener((ActionListener) this);

        failButton = new Button("Fail");
        failButton.setName("Fail");
        failButton.addActionListener((ActionListener) this);

        setButtonEnable(false);

        gbc.gridx = 0;
        gbc.gridy = 0;
        resultButtonPanel.add(passButton, gbc);
        gbc.gridx = 1;
        gbc.gridy = 0;
        resultButtonPanel.add(failButton, gbc);
        gbc.gridx = 0;
        gbc.gridy = 1;
        mainControlPanel.add(resultButtonPanel, gbc);

        instructionFrame.add(mainControlPanel);
        instructionFrame.pack();
        instructionFrame.setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        if (ae.getSource() instanceof Button) {
            Button btn = (Button) ae.getSource();
            switch (btn.getName()) {
                case "Pass":
                    testPassed = true;
                    isInterrupted = true;
                    mainThread.interrupt();
                    break;
                case "Fail":
                    testPassed = false;
                    isInterrupted = true;
                    mainThread.interrupt();
                    break;
            }
        }
    }

    private static void setButtonEnable(boolean status) {
        passButton.setEnabled(status);
        failButton.setEnabled(status);
    }

    private static void cleanUp() {
        mainFrame.dispose();
        instructionFrame.dispose();
    }

    public static void main(String args[]) {
        RestoreActiveWindowTest printDialogs = new RestoreActiveWindowTest();
        printDialogs.createInstructionUI();
        printDialogs.createAndShowGUI();

        mainThread = Thread.currentThread();
        try {
            mainThread.sleep(testTimeOut);
        } catch (InterruptedException ex) {
            if (!testPassed) {
                throw new RuntimeException(testFailMessage);
            }
        } finally {
            cleanUp();
        }

        if (!isInterrupted) {
            throw new RuntimeException("Test Timed out after "
                    + testTimeOut / 1000 + " seconds");
        }
    }
}
