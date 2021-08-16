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
 * @bug 4869502 4869539
 * @summary Confirm that ToPage is populated for argument =2. Range is disabled for argument = 0.
 * @run main/manual PrintDlgPageable
 */
import java.awt.*;
import java.awt.print.*;
import java.util.Locale;

import javax.print.*;

class PrintDlgPageable implements Printable {
    public static int arg;
        /**
         * Constructor
         */
         public PrintDlgPageable() {
                super();
        }
        /**
         * Starts the application.
         */
        public static void main(java.lang.String[] args) {
            if (args.length < 1) {
                System.out.println("usage: java PrintDlgPageable { 0 | 2}");
                return;
            }
            arg = Integer.parseInt(args[0]);
                PrintDlgPageable pd = new PrintDlgPageable();
                PrinterJob pj = PrinterJob.getPrinterJob();
                PageableHandler handler = new PageableHandler();
                pj.setPageable(handler);

                System.out.println("open PrintDialog..");
                if (pj.printDialog()) {
                        try {
                                System.out.println("About to print the data ...");
                                pj.print();
                                System.out.println("Printed");
                        }
                        catch (PrinterException pe) {
                                pe.printStackTrace();
                        }
                }

        }

        //printable interface
        public int print(Graphics g, PageFormat pf, int pi) throws
PrinterException {

                /*if (pi > 0) {
                        System.out.println("pi is greater than 0");
                        return Printable.NO_SUCH_PAGE;
                }*/
                // Simply draw two rectangles
                Graphics2D g2 = (Graphics2D)g;
                g2.setColor(Color.black);
                g2.translate(pf.getImageableX(), pf.getImageableY());
                g2.drawRect(1,1,200,300);
                g2.drawRect(1,1,25,25);
                System.out.println("print method called "+pi + " Orientation "+pf.getOrientation());
                return Printable.PAGE_EXISTS;
        }
}

class PageableHandler implements Pageable {

        PageFormat pf = new PageFormat();

        public int getNumberOfPages() {
                return PrintDlgPageable.arg;
                //return 0;
        }

        public Printable getPrintable(int pageIndex) {
                return new PrintDlgPageable();
        }

        public PageFormat getPageFormat(int pageIndex) {
                System.out.println("getPageFormat called "+pageIndex);
                if (pageIndex == 0) {
                        pf.setOrientation(PageFormat.PORTRAIT);
                        System.out.println("Orientation returned from Pageable "+findOrientation(pf.getOrientation()));
                        return pf;
                } else {
                        pf.setOrientation(PageFormat.LANDSCAPE);
                        System.out.println("Orientation returned from Pageable "+findOrientation(pf.getOrientation()));
                        return pf;
                }
        }

        public String findOrientation(int orient) {
                if (orient == PageFormat.LANDSCAPE) {
                        return "LANDSCAPE";
                }else if (orient == PageFormat.PORTRAIT) {
                        return "PORTRAIT";
                } else if (orient == PageFormat.REVERSE_LANDSCAPE) {
                        return "REVERSE LANDSCAPE";
                } else {
                        return null;
                }
        }
}
