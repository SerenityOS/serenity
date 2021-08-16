/*
 * Copyright (c) 2003, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4904236
 * @summary You would see a cross-platform print dialog being popped up. Check whether orientation is shown as LANDSCAPE. Click 'OK'. 'streamexample.ps' will be created in the same dir where this application was executed. Pass if the orientation in the ps file is landscape.
 * @run main/manual StreamPrintingOrientation
 */

import java.awt.*;
import java.awt.print.*;
import javax.print.*;
import javax.print.attribute.standard.*;
import javax.print.attribute.*;
import java.io.FileOutputStream;
import java.io.File;
import java.util.Locale;

class StreamPrintingOrientation implements Printable {
        /**
         * Constructor
         */
         public StreamPrintingOrientation() {
                super();
        }
        /**
         * Starts the application.
         */
        public static void main(java.lang.String[] args) {
                StreamPrintingOrientation pd = new StreamPrintingOrientation();
                PrinterJob pj = PrinterJob.getPrinterJob();
                HashPrintRequestAttributeSet prSet = new HashPrintRequestAttributeSet();
                PrintService service = null;

                FileOutputStream fos = null;
                File f = null, f1 = null;
                String mType = "application/postscript";

                try {
                        f = new File("streamexample.ps");
                        fos = new FileOutputStream(f);
                        StreamPrintServiceFactory[] factories = PrinterJob.lookupStreamPrintServices(mType);
                        if (factories.length > 0)
                                service = factories[0].getPrintService(fos);

                        if (service != null) {
                                System.out.println("Stream Print Service "+service);
                                pj.setPrintService(service);
                        } else {
                                throw new RuntimeException("No stream Print Service available.");
                        }
                } catch (Exception e) {
                        e.printStackTrace();
                }

                pj.setPrintable(pd);
                prSet.add(OrientationRequested.LANDSCAPE);
                prSet.add(new Copies(3));
                prSet.add(new JobName("orientation test", null));
                System.out.println("open PrintDialog..");
                if (pj.printDialog(prSet)) {
                        try {
                                System.out.println("\nValues in attr set passed to print method");
                                Attribute attr[] = prSet.toArray();
                                for (int x = 0; x < attr.length; x ++) {
                                        System.out.println("Name "+attr[x].getName()+"  "+attr[x]);
                                }
                                System.out.println("About to print the data ...");
                                if (service != null) {
                                        System.out.println("TEST: calling Print");
                                        pj.print(prSet);
                                        System.out.println("TEST: Printed");
                                }
                        }
                        catch (PrinterException pe) {
                                pe.printStackTrace();
                        }
                }

        }

        //printable interface
        public int print(Graphics g, PageFormat pf, int pi) throws PrinterException {

                if (pi > 0) {
                        return Printable.NO_SUCH_PAGE;
                }
                // Simply draw two rectangles
                Graphics2D g2 = (Graphics2D)g;
                g2.setColor(Color.black);
                g2.translate(pf.getImageableX(), pf.getImageableY());
                System.out.println("StreamPrinting Test Width "+pf.getWidth()+" Height "+pf.getHeight());
                g2.drawRect(1,1,200,300);
                g2.drawRect(1,1,25,25);
                return Printable.PAGE_EXISTS;
        }
}
