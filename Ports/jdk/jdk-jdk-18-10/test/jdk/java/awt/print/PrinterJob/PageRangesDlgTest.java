/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8061267
 * @summary The specified page range should be displayed in the dialog
 * @run main/manual=yesno PageRangesDlgTest
 */

import javax.print.*;
import javax.print.attribute.*;
import javax.print.attribute.standard.*;
import java.awt.*;
import java.awt.print.*;

public class PageRangesDlgTest implements Printable {

    static String[] instr = {
     "This test is to check that the print dialog displays the specified",
     "page ranges. You must have a printer installed for this test.",
     "It is valid only on dialogs which support page ranges",
     "In each dialog, check that a page range of 2 to 3 is requested",
     "Optionally press Print instead of Cancel, and verify that the",
     "correct number/set of pages is printed",
    };

    public static void main(String args[]) throws Exception {
        for (int i=0;i<instr.length;i++) {
            System.out.println(instr[i]);
        }
        PrinterJob job = PrinterJob.getPrinterJob();
        if (job.getPrintService() == null) {
           System.out.println("No printers. Test cannot continue.");
           return;
        }
        job.setPrintable(new PageRangesDlgTest());
        PrintRequestAttributeSet aset = new HashPrintRequestAttributeSet();
        aset.add(new PageRanges(2,3));
        if (job.printDialog(aset)) {
           job.print(aset);
        }

        job = PrinterJob.getPrinterJob();
        job.setPrintable(new PageRangesDlgTest());
        aset.add(DialogTypeSelection.NATIVE);
        if (job.printDialog()) {
            job.print();
        }
    }

    public int print(Graphics g, PageFormat pf, int pi)
                     throws PrinterException  {

        System.out.println("pi="+pi);
        if (pi >= 5) {
            return NO_SUCH_PAGE;
        }

        g.drawString("Page : " + (pi+1), 200, 200);

        return PAGE_EXISTS;
    }
}
