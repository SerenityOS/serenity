/*
 * Copyright (c) 2005, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4302718 6753165 8177386
 * @summary verifies that undo does not work in a disabled TextField on windows
 * @requires (os.family == "windows")
 * @run main/manual DisabledUndoTest
 */
import java.awt.Button;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Panel;
import java.awt.TextArea;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class DisabledUndoTest {

    private static void init() throws Exception {
        String[] instructions
            = {
                "1.Type a few symbols in the textfield.",
                "2.Right-click on the textfield to invoke context menu and select \"Undo\". Make sure the typed symbol is undone.",
                "3.Then click the button Disable textfield to disable the textfield.",
                "4.Right-click on the textfield to invoke context menu.Verify that Undo option is disabled in context menu and you can't undo the text",
                "5.If they are not, press Pass, else press Fail."
                };

        Sysout.createDialog();
        Sysout.printInstructions(instructions);
    }

    static Frame mainFrame;
    static Button bt;
    static TextField tf;
    static Panel p1;

    public static void initTestWindow() {
        mainFrame = new Frame();
        p1 = new Panel();
        mainFrame.setTitle("TestWindow");
        mainFrame.setBounds(700, 10, 400, 100);

        tf = new TextField(20);
        tf.select(0, 10);
        bt = new Button("Disable textfield");
        p1.add(tf);
        p1.add(bt);
        mainFrame.add(p1);
        bt.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                tf.setEditable(false);
            }
        });
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
        dialog.show();
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
                DisabledUndoTest.pass();
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
            DisabledUndoTest.fail("User Clicked Fail");
        }
    }
}
