/*
 * Copyright (c) 2007, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @key headful printer
 * @bug 6543815 6601097 8160888
 * @summary Image should be sent to printer, no exceptions thrown.
 *    The 3 printouts should have a rectangle which is the minimum
 *    possible margins ie, the margins should be hardware margins
 *    and not java default 1 inch margins.
 * @run main Margins
 */

import java.awt.print.PrinterJob;
import java.awt.print.Printable;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.PrinterException;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.Robot;
import java.awt.event.KeyEvent;

public class Margins implements Printable {

    public static void main(String args[]) throws Exception {
        Robot robot = new Robot();
        PrinterJob job = PrinterJob.getPrinterJob();
        if (job.getPrintService() == null) {
            System.out.println("No printers. Test cannot continue");
            return;
        }
        PrintService psrv = PrintServiceLookup.lookupDefaultPrintService();
        System.out.println("PrintService " + psrv.getName());

        PageFormat pageFormat = job.defaultPage();
        Paper paper = pageFormat.getPaper();
        double wid = paper.getWidth();
        double hgt = paper.getHeight();
        paper.setImageableArea(0, -10, wid, hgt);

        Thread t1 = new Thread(() -> {
            robot.delay(2000);
            robot.keyPress(KeyEvent.VK_ENTER);
            robot.keyRelease(KeyEvent.VK_ENTER);
        });
        t1.start();

        pageFormat = job.pageDialog(pageFormat);
        pageFormat.setPaper(paper);
        job.setPrintable(new Margins(), pageFormat);
        try {
            job.print();
        } catch (PrinterException e) {
        }

        paper.setImageableArea(0, 0, wid, hgt + 72);

        Thread t2 = new Thread(() -> {
            robot.delay(2000);
            robot.keyPress(KeyEvent.VK_ENTER);
            robot.keyRelease(KeyEvent.VK_ENTER);
        });
        t2.start();

        pageFormat = job.pageDialog(pageFormat);
        pageFormat.setPaper(paper);

        job.setPrintable(new Margins(), pageFormat);
        try {
           job.print();
        } catch (PrinterException e) {
        }

        pageFormat = job.defaultPage();
        paper = pageFormat.getPaper();
        wid = paper.getWidth();
        hgt = paper.getHeight();

        paper.setImageableArea(0, -10, -wid, hgt);

        Thread t3 = new Thread(() -> {
            robot.delay(2000);
            robot.keyPress(KeyEvent.VK_ENTER);
            robot.keyRelease(KeyEvent.VK_ENTER);
        });
        t3.start();

        pageFormat = job.pageDialog(pageFormat);
        pageFormat.setPaper(paper);

        job.setPrintable(new Margins(), pageFormat);
        try {
           job.print();
        } catch (PrinterException e) {
        }
    }

   public int print(Graphics g, PageFormat pf, int page)
       throws PrinterException {

       if (page > 0) {
           return NO_SUCH_PAGE;
       }
       int ix = (int)pf.getImageableX();
       int iy = (int)pf.getImageableY();
       int iw = (int)pf.getImageableWidth();
       int ih = (int)pf.getImageableHeight();
       System.out.println("ix="+ix+" iy="+iy+" iw="+iw+" ih="+ih);
       if ((ix < 0) || (iy < 0)) {
           throw new RuntimeException("Imageable x or y is a negative value.");
       }


       Paper paper = pf.getPaper();
       int wid = (int)paper.getWidth();
       int hgt = (int)paper.getHeight();
       System.out.println("wid="+wid+" hgt="+hgt);
       /*
        * If imageable width/height is -ve, then print was done with 1" margin
        * e.g. ix=72 iy=72 iw=451 ih=697 and paper wid=595
        * but with fix, we get print with hardware margin e.g.
        * ix=12, iy=12, iw=571, ih=817
        */
       if ((wid - iw > 72) || (hgt - ih > 72)) {
           throw new RuntimeException("Imageable width or height is negative value");
       }
       if ((ix+iw > wid) || (iy+ih > hgt)) {
           throw new RuntimeException("Printable width or height "
                   + "exceeds paper width or height.");
       }
       // runtime checking to see if the margins/printable area
       // correspond to the entire size of the paper, for now, make it pass
       // as for linux, the hwmargin is not taken into account - bug6574279
       if (ix == 0 && iy == 0 && (ix+iw == wid) && (iy+ih == hgt)) {
           return PAGE_EXISTS;
       }

       Graphics2D g2d = (Graphics2D)g;
       g2d.translate(ix, iy);
       g2d.setColor(Color.black);
       g2d.drawRect(1, 1, iw-2, ih-2);

       return PAGE_EXISTS;
   }
}
