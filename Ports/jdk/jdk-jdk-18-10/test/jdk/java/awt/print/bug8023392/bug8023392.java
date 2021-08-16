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

/*
  test
  @bug 8023392 8259232
  @summary Swing text components printed with spaces between chars
  @author Anton Nashatyrev
  @run applet/manual=yesno bug8023392.html
*/

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.applet.Applet;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.font.TextAttribute;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;


public class bug8023392 extends Applet {
    static final String[] instructions = {
        "Please select the RadioButton for applet size labeled \"variable\" radiobutton in test harness window.",
        "A Frame containing several pairs of labels ((a) and (b)) is displayed.",
        "Labels of each pair look the same and are left-aligned (with spaces ",
        "between chars).",
        "1. Hit the print button.",
        "2. Select any available printer (printing to file is also fine).",
        "3. Look at the printing result (paper, PDF, PS, etc.):",
        "   The (a) and (b) labels should look almost the same and the (a) labels",
        "   shouldn't appear as if they are stretched along X axis."};

    public void init() {
        this.setLayout(new BorderLayout());
        add(new SimplePrint2(), BorderLayout.CENTER);

        Sysout.createDialogWithInstructions(instructions);

    }

    public static class SimplePrint2 extends JPanel
            implements ActionListener, Printable {
        JLabel label1;
        JLabel label2;
        JButton printButton;


        public SimplePrint2() {
            setLayout(new BorderLayout());
            label1 = new JLabel("2a) a b c d e" +
                    "                         ");
            label2 = new JLabel("2b) a b c d e");

            Box p1 = new Box(BoxLayout.Y_AXIS);
            p1.add(label1);
            p1.add(label2);
            p1.add(new JLabel("wwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwww") {
                String s = "3a) a b c d e                                     ";
                @Override
                protected void paintComponent(Graphics g) {
                    sun.swing.SwingUtilities2.drawChars(this, g, s.toCharArray(),
                            0, s.length(), 0, 15);
                }
            });
            p1.add(new JLabel("wwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwww") {
                String s = "3b) a b c d e";
                @Override
                protected void paintComponent(Graphics g) {
                    sun.swing.SwingUtilities2.drawChars(this, g, s.toCharArray(),
                            0, s.length(), 0, 15);
                }
            });
            p1.add(new JLabel("wwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwww") {
                String s = "4a) a b c d e                                     ";
                AttributedCharacterIterator it;
                {
                    AttributedString as = new AttributedString(s);
                    as.addAttribute(TextAttribute.FONT, getFont());
                    as.addAttribute(TextAttribute.FOREGROUND, Color.RED, 3, 8);
                    it = as.getIterator();
                }
                @Override
                protected void paintComponent(Graphics g) {
                    sun.swing.SwingUtilities2.drawString(this, g, it, 0, 15);
                }
            });

            p1.add(new JLabel("wwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwww") {
                String s = "4b) a b c d e";
                AttributedCharacterIterator it;
                {
                    AttributedString as = new AttributedString(s);
                    as.addAttribute(TextAttribute.FONT, getFont());
                    as.addAttribute(TextAttribute.FOREGROUND, Color.RED, 3, 8);
                    it = as.getIterator();
                }
                @Override
                protected void paintComponent(Graphics g) {
                    sun.swing.SwingUtilities2.drawString(this, g, it, 0, 15);
                }
            });

            JPanel p2 = new JPanel();
            printButton = new JButton("Print");
            printButton.addActionListener(this);
            p2.add(printButton);

            Container c = this;
            c.add(p1, BorderLayout.CENTER);
            c.add(p2, BorderLayout.SOUTH);

            String[] data = {
                    "1a) \u30aa\u30f3\u30e9\u30a4\u30f3\u6d88\u8fbc" +
                    "                                              ",
                    "1b) \u30aa\u30f3\u30e9\u30a4\u30f3\u6d88\u8fbc"
            };
            JList l0 = new JList(data);
            l0.setVisibleRowCount(l0.getModel().getSize());
            JScrollPane jsp = new JScrollPane(l0);
            l0.setBorder(new LineBorder(Color.GRAY));
            c.add(jsp, BorderLayout.NORTH);

            for (Component comp : new Component[]{label1, label2, printButton}) {
                comp.setFont(new Font("Monospaced", 0, 16));
            }
        }

        public void actionPerformed(ActionEvent e) {
            PrinterJob job = PrinterJob.getPrinterJob();
            job.setPrintable(this);
            if (job.printDialog()) {
                try {
                    job.print();
                } catch (PrinterException ex) {
                    ex.printStackTrace();
                }
            }
        }

        public int print(Graphics graphics,
                         PageFormat pageFormat,
                         int pageIndex)
                throws PrinterException {
            if (pageIndex >= 1) {
                return Printable.NO_SUCH_PAGE;
            }
            double imgX = pageFormat.getImageableX();
            double imgY = pageFormat.getImageableY();
            ((Graphics2D)graphics).translate(imgX, imgY);
            this.paint(graphics);
            return Printable.PAGE_EXISTS;
        }
    }
}


/**
 * *************************************************
 * Standard Test Machinery
 * DO NOT modify anything below -- it's a standard
 * chunk of code whose purpose is to make user
 * interaction uniform, and thereby make it simpler
 * to read and understand someone else's test.
 * **************************************************
 */
class Sysout {
    private static TestDialog dialog;

    public static void createDialogWithInstructions(String[] instructions) {
        dialog = new TestDialog(new Frame(), "Instructions");
        dialog.printInstructions(instructions);
        dialog.show();
        println("Any messages for the tester will display here.");
    }

    public static void createDialog() {
        dialog = new TestDialog(new Frame(), "Instructions");
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

}// Sysout  class


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
        add("South", messageText);

        pack();

        show();
    }// TestDialog()

    //DO NOT call this directly, go through Sysout
    public void printInstructions(String[] instructions) {
        //Clear out any current instructions
        instructionsText.setText("");

        //Go down array of instruction strings

        String printStr, remainingStr;
        for (int i = 0; i < instructions.length; i++) {
            //chop up each into pieces maxSringLength long
            remainingStr = instructions[i];
            while (remainingStr.length() > 0) {
                //if longer than max then chop off first max chars to print
                if (remainingStr.length() >= maxStringLength) {
                    //Try to chop on a word boundary
                    int posOfSpace = remainingStr.
                            lastIndexOf(' ', maxStringLength - 1);

                    if (posOfSpace <= 0) posOfSpace = maxStringLength - 1;

                    printStr = remainingStr.substring(0, posOfSpace + 1);
                    remainingStr = remainingStr.substring(posOfSpace + 1);
                }
                //else just print
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
    }

}// TestDialog  class

