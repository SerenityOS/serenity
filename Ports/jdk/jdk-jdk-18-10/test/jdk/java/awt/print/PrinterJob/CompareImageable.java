/*
 * Copyright (c) 2007, Oracle and/or its affiliates. All rights reserved.
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
  @bug 4748055
  @summary   PASS if the values are same in both cases (2 and 3) below.
  @run main/manual CompareImageable
*/

/********************************************************************
Testcase for comparing the imageable width and height of the paper
with and without using print dialog.

How to run:

1. Launch the app. You'll find a checkbox and a print button.
2. Click on the print button with the checkbox unselected. Note the
imageable width and height displayed on the console
3. Click on the print button with the checkbox selected. This popus up
the print dialog. Click ok on the dialog. Note the imageable width and
height displayed on the console.

Result:  It's a PASS if the values are same in both cases (2 and 3),
        otherwise not.

*********************************************************************/

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.awt.print.*;


public class CompareImageable implements Printable {


    public int print(Graphics g, PageFormat pgFmt, int pgIndex) {

       //get the printable width and height of the paper.
        int pageHeight = (int)pgFmt.getImageableHeight();
        int pageWidth = (int)pgFmt.getImageableWidth();

        System.out.println("imageable width = " + pageWidth + " height = " + pageHeight);
        return Printable.NO_SUCH_PAGE;
    }


    public static void main(String [] args) {

        final JFrame frame = new JFrame("Print Test");
        final JButton printBtn = new JButton("Print");
        final JCheckBox dialogBtn = new JCheckBox("Native dialog");

        JPanel panel = new JPanel(new FlowLayout());
        panel.add(dialogBtn);
        panel.add(printBtn);
        frame.getContentPane().add(panel);

        printBtn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {

                        CompareImageable test = new CompareImageable();
                        PrinterJob pj = PrinterJob.getPrinterJob();

                        if (dialogBtn.isSelected() && !pj.printDialog()) {
                                //user clicked 'Cancel' button in the print dialog. No printing.
                                return;
                        }

                        if (dialogBtn.isSelected()) {
                                System.out.println("With print dialog...");
                        } else {
                                System.out.println("Without print dialog...");
                        }

                        if (pj == null) {
                                System.out.println("No printer job found...");
                                return;
                        }
                        pj.setPrintable(test);

                        try {
                                pj.print();

                        } catch (Exception ex) {
                                ex.printStackTrace();
                        }
                }
        });


        frame.setDefaultCloseOperation(frame.EXIT_ON_CLOSE);
        frame.setSize(400, 400);
        frame.setVisible(true);

    }
}
