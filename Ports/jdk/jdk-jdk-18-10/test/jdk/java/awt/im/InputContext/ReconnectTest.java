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

 /*
 * @test
 * @key headful
 * @bug 8213183
 * @summary verifies InputMethod reconnection
 * @run main/manual ReconnectTest
 */
import java.awt.*;
import java.awt.event.*;

public class ReconnectTest {

    private static void init() throws Exception {
        String[] instructions
            = {
                "This test supports Linux and Japanese/Korean/Chinese",
                "input method(IM). Under other environment, press Pass.",
                "Test steps:",
                "1.Turn on IM and input some Asian characters to TestWindow.",
                "2.kill IM frontend by OS, and restart it.",
                "  e.g. for ibus, $ ibus-daemon -r --xim &",
                "3.Change focus to this Instructions window and back to TestWindow.",
                "4.Confirm IM can be activated."
              };

        Sysout.createDialog();
        Sysout.printInstructions(instructions);
    }

    static Frame mainFrame;

    public static void initTestWindow() {
        mainFrame = new Frame();
        mainFrame.setTitle("TestWindow");
        mainFrame.setLayout(new BorderLayout());
        Panel p0 = new Panel();
        p0.setLayout(new FlowLayout());
        mainFrame.add(p0, BorderLayout.CENTER);
        TextArea text = new TextArea();
        p0.add(text);

        mainFrame.pack();
        mainFrame.setLocation(700, 10);
        mainFrame.setVisible(true);
    }

    public static void dispose() {
        Sysout.dispose();
        mainFrame.dispose();
    }

    /**
     * ***************************************************
     * Standard Test Machinery Section DO NOT modify anything in this section --
     * it's a standard chunk of code which has all of the synchronisation
     * necessary for the test harness. By keeping it the same in all tests, it
     * is easier to read and understand someone else's test, as well as insuring
     * that all tests behave correctly with the test harness. There is a section
     * following this for test-defined classes
     * ****************************************************
     */
    private static boolean theTestPassed = false;
    private static boolean testGeneratedInterrupt = false;
    private static String failureMessage = "";
    private static Thread mainThread = null;
    final private static int sleepTime = 300000;

    public static void main(String args[]) throws Exception {
        mainThread = Thread.currentThread();
        try {
            init();
            initTestWindow();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            mainThread.sleep(sleepTime);
        } catch (InterruptedException e) {
            dispose();
            if (testGeneratedInterrupt && !theTestPassed) {
                throw new Exception(failureMessage);
            }
        }
        if (!testGeneratedInterrupt) {
            dispose();
            throw new RuntimeException("Timed out after " + sleepTime / 1000
                    + " seconds");
        }
    }

    public static synchronized void pass() {
        theTestPassed = true;
        testGeneratedInterrupt = true;
        mainThread.interrupt();
    }

    public static synchronized void fail(String whyFailed) {
        theTestPassed = false;
        testGeneratedInterrupt = true;
        failureMessage = whyFailed;
        mainThread.interrupt();
    }
}

// *********** End Standard Test Machinery Section **********
/**
 * **************************************************
 * Standard Test Machinery DO NOT modify anything below -- it's a standard chunk
 * of code whose purpose is to make user interaction uniform, and thereby make
 * it simpler to read and understand someone else's test.
 * **************************************************
 */
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
    private static Frame frame;

    public static void createDialog() {
        frame = new Frame();
        dialog = new TestDialog(frame, "Instructions");
        String[] defInstr = {"Instructions will appear here. ", ""};
        dialog.printInstructions(defInstr);
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
        dialog.dispose();
        frame.dispose();
    }
}

/**
 * This is part of the standard test machinery. It provides a place for the test
 * instructions to be displayed, and a place for interactive messages to the
 * user to be displayed. To have the test instructions displayed, see Sysout. To
 * have a message to the user be displayed, see Sysout. Do not call anything in
 * this dialog directly.
 */
class TestDialog extends Dialog implements ActionListener {

    TextArea instructionsText;
    TextArea messageText;
    int maxStringLength = 80;
    Panel buttonP;
    Button failB;
    Button passB;

    // DO NOT call this directly, go through Sysout
    public TestDialog(Frame frame, String name) {
        super(frame, name);
        int scrollBoth = TextArea.SCROLLBARS_BOTH;
        instructionsText = new TextArea("", 15, maxStringLength, scrollBoth);
        add("North", instructionsText);

        messageText = new TextArea("", 5, maxStringLength, scrollBoth);
        add("Center", messageText);

        buttonP = new Panel();
        failB = new Button("Fail");
        failB.setActionCommand("fail");
        failB.addActionListener(this);
        passB = new Button("Pass");
        buttonP.add(passB);
        buttonP.add(failB);
        passB.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                ReconnectTest.pass();
            }
        });

        add("South", buttonP);
        pack();
        setVisible(true);
    }

    // DO NOT call this directly, go through Sysout
    public void printInstructions(String[] instructions) {
        instructionsText.setText("");
        String printStr, remainingStr;
        for (int i = 0; i < instructions.length; i++) {
            remainingStr = instructions[i];
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

    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand() == "fail") {
            ReconnectTest.fail("User Clicked Fail");
        }
    }
}
