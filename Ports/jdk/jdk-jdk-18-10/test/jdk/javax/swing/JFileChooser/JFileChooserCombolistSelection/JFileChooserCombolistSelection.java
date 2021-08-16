/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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
   @test
   @bug 4674075 8233584
   @summary Checks if keyboard traversal of JFileChooser's "Look in" combobox is broken
   @modules java.desktop
   @requires (os.family == "windows")
   @key headful
   @run main/manual JFileChooserCombolistSelection
 */
import java.awt.Panel;
import java.awt.TextArea;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.JFrame;
import javax.swing.JDialog;
import javax.swing.JButton;

public class JFileChooserCombolistSelection {

    private static boolean theTestPassed;
    private static boolean testGeneratedInterrupt;
    private static Thread mainThread;
    private static int sleepTime = 90000;
    public static  JFileChooser fileChooser;

    private static void init() throws Exception {

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                String[] instructions
                        = {
                        "Activate the \"Look in\" combobox's popup by "
                                +"clicking on its arrow button."
                                + "Then navigate in it using keyboard "
                                +"(ie UP and DOWN arrow keys)."
                                + "When navigating, take a notice, "
                                +"if the contents of the file list changes."
                                + "If yes, then press \"Fail\", "
                                +"otherwise press \"Passed\"."
                };
                Sysout.createDialogWithInstructions(instructions);
                Sysout.printInstructions(instructions);
            }
        });
    }

    /**
     * ***************************************************
     * Standard Test Machinery Section DO NOT modify anything in this section --
     * it's a standard chunk of code which has all of the synchronisation
     * necessary for the test harness. By keeping it the same in all tests, it
     * is easier to read and understand someone else's test, as well as insuring
     * that all tests behave correctly with the test harness. There is a section
     * following this for test-defined classes
     */
    public static void main(String args[]) throws Exception {
        String osName = System.getProperty("os.name");
        if(!osName.toLowerCase().contains("win")) {
            System.out.println("The test was skipped because it is sensible only for Windows.");
            return;
        }
        try {
            UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
        } catch(Exception ex) {
            return;
        }
        mainThread = Thread.currentThread();
        try {
            init();
        } catch (Exception ex) {
            return;
        }
        try {
            mainThread.sleep(sleepTime);
        } catch (InterruptedException ex) {
            Sysout.dispose();
            if (!theTestPassed && testGeneratedInterrupt) {
                throw new RuntimeException("Test Failed");
            }
        }
        if (!testGeneratedInterrupt) {
            Sysout.dispose();
            throw new RuntimeException("Test Failed");
        }
    }

    public static synchronized void pass() {
        theTestPassed = true;
        testGeneratedInterrupt = true;
        mainThread.interrupt();
    }

    public static synchronized void fail() {
        theTestPassed = false;
        testGeneratedInterrupt = true;
        mainThread.interrupt();
    }
}

/**
 * This is part of the standard test machinery. It creates a dialog (with the
 * instructions), and is the interface for sending text messages to the user. To
 * print the instructions, send an array of strings to Sysout.createDialog
 * WithInstructions method. Put one line of instructions per array entry. To
 * display a message for the tester to see, simply call Sysout.println with the
 * string to be displayed. This mimics System.out.println but works within the
 * test harness as well as standalone.
 */
class Sysout {

    private static TestDialog dialog;
    private static JFrame frame;

    public static void createDialogWithInstructions(String[] instructions) {
        frame = new JFrame();
        dialog = new TestDialog(frame, "Instructions");
        dialog.printInstructions(instructions);
        dialog.setVisible(true);
        println("Any messages for the tester will display here.");
    }

    public static void printInstructions(String[] instructions) {
        dialog.printInstructions(instructions);
    }

    public static void println(String messageIn) {
        dialog.displayMessage(messageIn);
    }

    public static void dispose() {
        Sysout.println("Shutting down the Java process..");
        if(JFileChooserCombolistSelection.fileChooser != null) {
            JFileChooserCombolistSelection.fileChooser.cancelSelection();
        }
        frame.dispose();
        dialog.dispose();
    }
}

/**
 * This is part of the standard test machinery. It provides a place for the test
 * instructions to be displayed, and a place for interactive messages to the
 * user to be displayed. To have the test instructions displayed, see Sysout. To
 * have a message to the user be displayed, see Sysout. Do not call anything in
 * this dialog directly.
 */
class TestDialog extends JDialog {

    private TextArea instructionsText;
    private TextArea messageText;
    private int maxStringLength = 80;
    private Panel buttonP = new Panel();
    private JButton run = new JButton("Run");
    private JButton passB = new JButton("Pass");
    private JButton failB = new JButton("Fail");

    public TestDialog(JFrame frame, String name) {
        super(frame, name);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        int scrollBoth = TextArea.SCROLLBARS_BOTH;
        instructionsText = new TextArea("", 15, maxStringLength, scrollBoth);
        add("North", instructionsText);

        messageText = new TextArea("", 5, maxStringLength, scrollBoth);
        add("Center", messageText);

        buttonP.add("East", run);
        buttonP.add("East", passB);
        buttonP.add("West", failB);
        passB.setEnabled(false);
        failB.setEnabled(false);
        add("South", buttonP);

        run.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                JFileChooserCombolistSelection.fileChooser = new JFileChooser();
                JFileChooserCombolistSelection.fileChooser.showOpenDialog(null);
                passB.setEnabled(true);
                failB.setEnabled(true);
            }
        });

        passB.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                JFileChooserCombolistSelection.pass();
            }
        });

        failB.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                JFileChooserCombolistSelection.fail();
            }
        });
        pack();

        setVisible(true);
    }

    public void printInstructions(String[] instructions) {
        instructionsText.setText("");

        String printStr, remainingStr;
        for (String instruction : instructions) {
            remainingStr = instruction;
            while (remainingStr.length() > 0) {
                if (remainingStr.length() >= maxStringLength) {
                    int posOfSpace = remainingStr.
                            lastIndexOf(' ', maxStringLength - 1);

                    if (posOfSpace <= 0) {
                        posOfSpace = maxStringLength - 1;
                    }

                    printStr = remainingStr.substring(0, posOfSpace + 1);
                    remainingStr = remainingStr.substring(posOfSpace + 1);
                } else {
                    printStr = remainingStr;
                    remainingStr = "";
                }
                instructionsText.append(printStr + "\n");
            }
        }

    }

    public void displayMessage(String messageIn) {
        messageText.append(messageIn + "\n");
    }
}
