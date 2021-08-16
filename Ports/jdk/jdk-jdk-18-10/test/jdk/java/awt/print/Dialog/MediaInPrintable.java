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

/**
 * @test
 * @bug 4869575 6361766
 * @summary Setting orientation in the page format does not have any effect on the printout. To test 6361766, the application must exit.
 * @run main/manual MediaInPrintable
 */
import java.awt.*;
import java.awt.print.*;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;

public class MediaInPrintable implements Printable {
        private static Font fnt = new Font("Helvetica",Font.PLAIN,24);
        public static void main(String[] args) {

            System.out.println("arguments : native1 | native2\nExpected output :\n\tnative1 -  Landscape orientation.\n\tnative2 - Legal paper is selected.");
            if (args.length == 0) {
                return;
            }


            // Get a PrinterJob
            PrinterJob job = PrinterJob.getPrinterJob();
            PageFormat pf = new PageFormat();

            if (args[0].equals("native1")) {
                pf.setOrientation(PageFormat.LANDSCAPE);
                job.setPrintable(new MediaInPrintable(), pf);
                if (job.printDialog()) {
                        // Print the job if the user didn't cancel printing
                        try {
                                job.print();
                        } catch (Exception e) {
                                e.printStackTrace();
                        }
                }
            } else if (args[0].equals("native2")) {
                Paper p = new Paper();
                p.setSize(612.0, 1008.0);
                p.setImageableArea(72.0, 72.0, 468.0, 864.0);
                pf.setPaper(p);

                job.setPrintable(new MediaInPrintable(), pf);
                if (job.printDialog()) {
                        // Print the job if the user didn't cancel printing
                        try {
                                job.print();
                        } catch (Exception e) {
                                e.printStackTrace();
                        }
                }

            }

                //System.exit(0);
        }

        public int print(Graphics g, PageFormat pf, int pageIndex) throws PrinterException {
        if (pageIndex > 0) {
                return Printable.NO_SUCH_PAGE;
        }
        g.setFont(fnt);
        g.setColor(Color.green);
        g.drawString("Page " + (pageIndex+1), 100, 100);
        return Printable.PAGE_EXISTS;
        }
}
