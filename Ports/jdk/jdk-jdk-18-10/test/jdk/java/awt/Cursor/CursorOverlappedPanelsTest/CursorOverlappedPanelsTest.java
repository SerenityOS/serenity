/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.*;
//import java.applet.Applet;
import javax.swing.BorderFactory;
import javax.swing.JApplet;
import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;

/**
 * @test
 * @bug 8007155
 * @summary [macosx] Disabled panel takes mouse input in JLayeredPane
 * @author Alexander Scherbatiy: area=java.awt.Cursor
 * @run applet/manual=yesno CursorOverlappedPanelsTest.html
 */
public class CursorOverlappedPanelsTest extends JApplet {
    //Declare things used in the test, like buttons and labels here

    public void init() {
        //Create instructions for the user here, as well as set up
        // the environment -- set the layout manager, add buttons,
        // etc.
        this.setLayout(new BorderLayout());

        String[] instructions = {
            "Verify that the Crosshair cursor from enabled panel"
            + " is displayed on the panels intersection",
            "1) Move the mosue cursor on the Enabled and Disabled panels"
            + " intersection",
            "2) Check that the Crosshair cursor is displayed ",
            "If so, press PASS, else press FAIL."
        };
        Sysout.createDialogWithInstructions(instructions);

    }//End  init()

    public void start() {
        //Get things going.  Request focus, set size, et cetera
        setSize(200, 200);
        setVisible(true);
        validate();
        try {
            EventQueue.invokeAndWait(new Runnable() {
                public void run() {
                    createAndShowGUI();
                }
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }// start()

    //The rest of this class is the actions which perform the test...
    //Use Sysout.println to communicate with the user NOT System.out!!
    //Sysout.println ("Something Happened!");
    private static JPanel createPanel(Point location, boolean enabled) {
        final JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setEnabled(enabled);
        panel.setSize(new Dimension(200, 200));
        panel.setLocation(location);
        panel.setBorder(BorderFactory.createTitledBorder(
                enabled ? "Enabled" : "Disabled"));
        panel.setCursor(Cursor.getPredefinedCursor(
                enabled ? Cursor.CROSSHAIR_CURSOR : Cursor.DEFAULT_CURSOR));
        System.out.println("cursor: " + Cursor.getPredefinedCursor(enabled ? Cursor.CROSSHAIR_CURSOR : Cursor.DEFAULT_CURSOR));
        return panel;
    }

    private static void createAndShowGUI() {
        final JFrame frame = new JFrame("Test");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JLayeredPane layeredPane = new JLayeredPane();
        layeredPane.setPreferredSize(new Dimension(400, 400));
        JPanel enabledPanel = createPanel(new Point(10, 10), true);
        JPanel disabledPanel = createPanel(new Point(100, 100), false);
        layeredPane.add(disabledPanel, JLayeredPane.PALETTE_LAYER);
        layeredPane.add(enabledPanel, JLayeredPane.DEFAULT_LAYER);

        frame.getContentPane().add(layeredPane);
        frame.pack();
        frame.setVisible(true);
    }
}// class BlockedWindowTest

/* Place other classes related to the test after this line */
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

    public static void createDialogWithInstructions(String[] instructions) {
        dialog = new TestDialog(new Frame(), "Instructions");
        dialog.printInstructions(instructions);
        dialog.setVisible(true);
        println("Any messages for the tester will display here.");
    }

    public static void createDialog() {
        dialog = new TestDialog(new Frame(), "Instructions");
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
}// Sysout  class

/**
 * This is part of the standard test machinery. It provides a place for the test
 * instructions to be displayed, and a place for interactive messages to the
 * user to be displayed. To have the test instructions displayed, see Sysout. To
 * have a message to the user be displayed, see Sysout. Do not call anything in
 * this dialog directly.
 */
class TestDialog extends Dialog {

    TextArea instructionsText;
    TextArea messageText;
    int maxStringLength = 80;

    //DO NOT call this directly, go through Sysout
    public TestDialog(Frame frame, String name) {
        super(frame, name);
        int scrollBoth = TextArea.SCROLLBARS_BOTH;
        instructionsText = new TextArea("", 15, maxStringLength, scrollBoth);
        add("North", instructionsText);

        messageText = new TextArea("", 5, maxStringLength, scrollBoth);
        add("Center", messageText);

        pack();

        setVisible(true);
    }// TestDialog()

    //DO NOT call this directly, go through Sysout
    public void printInstructions(String[] instructions) {
        //Clear out any current instructions
        instructionsText.setText("");

        //Go down array of instruction strings

        String printStr, remainingStr;
        for (int i = 0; i < instructions.length; i++) {
            //chop up each into pieces maxSringLength long
            remainingStr = instructions[ i];
            while (remainingStr.length() > 0) {
                //if longer than max then chop off first max chars to print
                if (remainingStr.length() >= maxStringLength) {
                    //Try to chop on a word boundary
                    int posOfSpace = remainingStr.
                            lastIndexOf(' ', maxStringLength - 1);

                    if (posOfSpace <= 0) {
                        posOfSpace = maxStringLength - 1;
                    }

                    printStr = remainingStr.substring(0, posOfSpace + 1);
                    remainingStr = remainingStr.substring(posOfSpace + 1);
                } //else just print
                else {
                    printStr = remainingStr;
                    remainingStr = "";
                }

                instructionsText.append(printStr + "\n");

            }// while

        }// for

    }//printInstructions()

    //DO NOT call this directly, go through Sysout
    public void displayMessage(String messageIn) {
        messageText.append(messageIn + "\n");
        System.out.println(messageIn);
    }
}// TestDialog  class
