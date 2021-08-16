/*
 * Copyright (c) 2007, 2014, Oracle and/or its affiliates. All rights reserved.
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

import javax.print.PrintService;
import javax.print.attribute.PrintServiceAttributeSet;
import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.print.*;

/*
 * @test
 * @summary Check that PrinterJob constructor and methods do not throw unexpected
 *          exceptions in headless mode
 * @run main/othervm -Djava.awt.headless=true HeadlessPrinterJob
 */

public class HeadlessPrinterJob {

    class testPrintable implements Printable {

        public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) {
            Graphics2D g2 = (Graphics2D) graphics;

            if (pageIndex >= 10) {
                return Printable.NO_SUCH_PAGE;
            }

            int gridWidth = 400 / 6;
            int gridHeight = 300 / 2;

            int rowspacing = 5;
            int columnspacing = 7;
            int rectWidth = gridWidth - columnspacing;
            int rectHeight = gridHeight - rowspacing;

            Color fg3D = Color.lightGray;

            g2.setPaint(fg3D);
            g2.drawRect(80, 80, 400 - 1, 310);
            g2.setPaint(Color.black);

            int x = 85;
            int y = 87;


            // draw Line2D.Double
            g2.draw(new Line2D.Double(x, y + rectHeight - 1, x + rectWidth, y));
            x += gridWidth;

            // draw Rectangle2D.Double
            //g2.setStroke(stroke);
            g2.draw(new Rectangle2D.Double(x, y, rectWidth, rectHeight));
            x += gridWidth;

            // draw  RoundRectangle2D.Double
            //g2.setStroke(dashed);
            g2.draw(new RoundRectangle2D.Double(x, y, rectWidth,
                    rectHeight, 10, 10));
            return Printable.PAGE_EXISTS;
        }
    }

    class testPageable implements Pageable {

        public int getNumberOfPages() {
            return 10;
        }

        public PageFormat getPageFormat(int pageIndex) throws IndexOutOfBoundsException {
            PageFormat pf = null;
            if (pageIndex >= 10) {
                throw new IndexOutOfBoundsException("Wrong page#");
            }
            switch (pageIndex) {
                case 0:
                case 2:
                case 4:
                case 6:
                case 8:
                    pf = new PageFormat();
                    pf.setOrientation(PageFormat.REVERSE_LANDSCAPE);
                    break;
                case 1:
                case 3:
                case 5:
                case 7:
                case 9:
                    pf = new PageFormat();
                    pf.setOrientation(PageFormat.LANDSCAPE);
                    break;
            }
            return pf;
        }

        public Printable getPrintable(int pageIndex) throws IndexOutOfBoundsException {
            if (pageIndex >= 10) {
                throw new IndexOutOfBoundsException("Wrong page#");
            }
            return new testPrintable();
        }
    }

    public static void main(String args[]) throws Exception {
        new HeadlessPrinterJob().doTest();
    }

    void doTest() throws Exception {
        PrinterJob pj = PrinterJob.getPrinterJob();
        for (PrintService psl : pj.lookupPrintServices()) {
            PrintServiceAttributeSet psas = psl.getAttributes();
            pj.setPrintService(psl);
        }
        PrintService ps = pj.getPrintService();
        pj.setPrintable(new testPrintable());

        pj = PrinterJob.getPrinterJob();
        PageFormat pf = new PageFormat();
        pf.setOrientation(PageFormat.REVERSE_LANDSCAPE);
        pj.setPrintable(new testPrintable(), pf);
        pj.setPageable(new testPageable());

        boolean exceptions = false;
        try {
            pj.printDialog();
        } catch (HeadlessException e) {
            exceptions = true;
        }
        if (!exceptions)
            throw new RuntimeException("HeadlessException did not occur when expected");

        exceptions = false;
        try {
            pj = PrinterJob.getPrinterJob();
            pf = new PageFormat();
            pf.setOrientation(PageFormat.REVERSE_LANDSCAPE);
            pf = pj.pageDialog(pf);
        } catch (HeadlessException e) {
            exceptions = true;
        }
        if (!exceptions)
            throw new RuntimeException("HeadlessException did not occur when expected");

        pf = new PageFormat();
        pf.setOrientation(PageFormat.REVERSE_LANDSCAPE);
        pf = pj.defaultPage(pf);
        pf = pj.defaultPage();

        pf = new PageFormat();
        pf.setOrientation(PageFormat.REVERSE_LANDSCAPE);
        pf = pj.validatePage(pf);
        pj.setCopies(10);
        pj.getCopies();
        pj.getUserName();
        pj.setJobName("no-job-name");
        pj.getJobName();
    }
}
