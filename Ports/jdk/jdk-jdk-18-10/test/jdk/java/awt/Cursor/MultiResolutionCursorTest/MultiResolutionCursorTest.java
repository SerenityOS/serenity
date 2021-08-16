/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Label;
import java.awt.Point;
import java.awt.TextArea;
import java.awt.Toolkit;
import java.awt.image.BaseMultiResolutionImage;
import java.awt.image.BufferedImage;
import javax.swing.JApplet;

import jdk.test.lib.Platform;

/**
 * @test
 * @bug 8028212
 * @summary [macosx] Custom Cursor HiDPI support
 * @author Alexander Scherbatiy
 * @library /test/lib
 * @modules java.desktop/sun.awt.image
 * @build jdk.test.lib.Platform
 * @run applet/manual=yesno MultiResolutionCursorTest.html
 */
public class MultiResolutionCursorTest extends JApplet {
    //Declare things used in the test, like buttons and labels here

    static final int sizes[] = {8, 16, 32, 128};
    static final Color colors[] = {Color.WHITE, Color.RED, Color.GREEN, Color.BLUE};

    public void init() {
        //Create instructions for the user here, as well as set up
        // the environment -- set the layout manager, add buttons,
        // etc.
        this.setLayout(new BorderLayout());

        if (Platform.isOSX()) {
            String[] instructions = {
                "Verify that high resolution custom cursor is used"
                + " on HiDPI displays.",
                "1) Run the test on Retina display or enable the Quartz Debug"
                + " and select the screen resolution with (HiDPI) label",
                "2) Move the cursor to the Test Frame",
                "3) Check that cursor has red, green or blue color",
                "If so, press PASS, else press FAIL."
            };
            Sysout.createDialogWithInstructions(instructions);

        } else {
            String[] instructions = {
                "This test is not applicable to the current platform. Press PASS."
            };
            Sysout.createDialogWithInstructions(instructions);
        }
    }//End  init()

    public void start() {
        //Get things going.  Request focus, set size, et cetera
        setSize(200, 200);
        setVisible(true);
        validate();

        final Image image = new BaseMultiResolutionImage(
                createResolutionVariant(0),
                createResolutionVariant(1),
                createResolutionVariant(2),
                createResolutionVariant(3)
        );

        int center = sizes[0] / 2;
        Cursor cursor = Toolkit.getDefaultToolkit().createCustomCursor(
                image, new Point(center, center), "multi-resolution cursor");

        Frame frame = new Frame("Test Frame");
        frame.setSize(300, 300);
        frame.setLocation(300, 50);
        frame.add(new Label("Move cursor here"));
        frame.setCursor(cursor);
        frame.setVisible(true);
    }// start()

    static BufferedImage createResolutionVariant(int i) {
        BufferedImage resolutionVariant = new BufferedImage(sizes[i], sizes[i],
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = resolutionVariant.createGraphics();
        g2.setColor(colors[i]);
        g2.fillRect(0, 0, sizes[i], sizes[i]);
        g2.dispose();
        return resolutionVariant;
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
                    int posOfSpace = remainingStr.lastIndexOf(' ', maxStringLength - 1);

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
}// Te
