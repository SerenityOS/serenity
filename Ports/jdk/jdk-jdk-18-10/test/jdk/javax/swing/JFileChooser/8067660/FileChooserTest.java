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

/*
 * @test
 * @bug 8067660 8178106
 * @summary JFileChooser create new folder fails silently
 * @requires (os.family == "windows")
 * @run main/manual FileChooserTest
 */
import java.awt.Panel;
import java.awt.TextArea;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class FileChooserTest {

    private static boolean theTestPassed;
    private static boolean testGeneratedInterrupt;
    private static Thread mainThread;
    private static int sleepTime = 30000;
    public static  JFileChooser fileChooser;

    private static void init() throws Exception {

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                String[] instructions
                     = {
                       "1) Create a folder with read only permissions by "
                       + "changing security permission through Security tab"
                       + "under Folder->Properties menu to deny write permission"
                       + " to the newly created folder",
                       "2) Click on run test button.It will open a open dialog"
                       + " Navigate to the newly created read only folder",
                       "3) Click on the create new folder button in open dialog",
                       "4) If an error message does not pops up"
                       + "test failed otherwise passed.",
                       "5) Pressing Pass/Fail button will mark test as "
                       + "pass/fail and will shutdown JVM",
                       "6) Newly created folder permissions can now be restored"
                       + " back to default",
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
        if(FileChooserTest.fileChooser != null) {
            FileChooserTest.fileChooser.cancelSelection();
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
                FileChooserTest.fileChooser = new JFileChooser();
                FileChooserTest.fileChooser.showOpenDialog(null);
                passB.setEnabled(true);
                failB.setEnabled(true);
            }
        });

        passB.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                FileChooserTest.pass();
            }
        });

        failB.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                FileChooserTest.fail();
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
