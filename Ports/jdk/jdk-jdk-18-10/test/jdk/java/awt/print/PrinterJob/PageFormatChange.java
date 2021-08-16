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
 * @bug 6359283
 * @summary pagedialog needs to update based on change of printer.
 * @run main/manual PageFormatChange
 */

import java.awt.print.*;
import javax.print.*;

public class PageFormatChange {

    static String[] text = {
    "This is is a manual test intended to be run on Windows, and you",
    "must have at least two printers installed, and ideally the second",
    "printer should support large paper sizes. When the pageDialog appears",
    "first change printers, then choose a large paper size, then OK",
    "the dialog. The test will throw an Exception if it fails",
    };

    public static void main(String[] args) {
        if (!System.getProperty("os.name","").startsWith("Windows")) {
           System.out.println("Not Windows, so test is not applicable");
           return;
        }
        for (String s : text) {
            System.out.println(s);
        }
        PrinterJob job = PrinterJob.getPrinterJob();
        PrintService service1 = job.getPrintService();
        PageFormat pf1 = job.defaultPage();
        PageFormat pf2 = job.pageDialog(pf1);
        PrintService service2 = job.getPrintService();
        if (service1.equals(service2)) {
           System.err.println("You must select a different printer!");
           System.err.println("Test cannot continue");
        }
        if (pf1.equals(pf2)) {
           System.err.println("You must select a different paper size!");
        }
        /* Assume uniform margins, and test if imageable area matches
         * imageable height.
         */
        int pw = (int)(pf2.getWidth()+0.5);
        int ph = (int)(pf2.getHeight()+0.5);
        int iw = (int)(pf2.getImageableWidth()+0.5);
        int ih = (int)(pf2.getImageableHeight()+0.5);
        int ix = (int)(pf2.getImageableX()+0.5);
        int iy = (int)(pf2.getImageableY()+0.5);
        int expectedWidth = ix*2+iw;
        int expectedHeight = iy*2+ih;
        if (expectedWidth != pw || expectedHeight != ph) {
            throw new RuntimeException("Unexpected size");
        }
        displayPageFormat(pf2);
    }

    public static void displayPageFormat(PageFormat pf)
    {
        System.out.println("------- Page Format -------");
        System.out.println("ImageableX = " + pf.getImageableX());
        System.out.println("ImageableY = " + pf.getImageableY());
        System.out.println("ImageableWidth = " + pf.getImageableWidth());
        System.out.println("ImageableHeight = " + pf.getImageableHeight());
        System.out.println("Width = " + pf.getWidth());
        System.out.println("Height = " + pf.getHeight());
    }

}
