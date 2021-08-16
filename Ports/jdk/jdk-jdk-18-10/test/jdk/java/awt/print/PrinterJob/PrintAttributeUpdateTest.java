/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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
  @bug 8042713 8170578
  @summary  Print Dialog does not update attribute set with page range
  @run main/manual PrintAttributeUpdateTest
 */
import java.awt.Component;
import java.awt.Graphics;
import java.awt.print.PageFormat;
import java.awt.print.Pageable;
import java.awt.print.Printable;
import java.awt.print.PrinterJob;
import javax.print.attribute.Attribute;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.standard.DialogTypeSelection;
import javax.print.attribute.standard.PageRanges;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public class PrintAttributeUpdateTest implements Pageable, Printable {

    public static void main(String args[]) throws Exception {
        String[] instructions
                = {
                    "Select Pages Range From instead of All in print dialog. ",
                    "Then select Print"
                };
        SwingUtilities.invokeAndWait(() -> {
            JOptionPane.showMessageDialog((Component) null,
                    instructions, "Instructions",
                    JOptionPane.INFORMATION_MESSAGE);
        });
        HashPrintRequestAttributeSet as = new HashPrintRequestAttributeSet();
        PrinterJob j = PrinterJob.getPrinterJob();
        j.setPageable(new PrintAttributeUpdateTest());
        as.add(DialogTypeSelection.NATIVE);
        j.printDialog(as);
        if (as.containsKey(PageRanges.class) == false) {
            throw new RuntimeException("Print Dialog did not update "
                    + " attribute set with page range");
        }
        Attribute attrs[] = as.toArray();
        for (int i = 0; i < attrs.length; i++) {
            System.out.println("attr " + attrs[i]);
        }
        j.print(as);
    }

    public int getNumberOfPages() {
        return UNKNOWN_NUMBER_OF_PAGES;
    }

    public PageFormat getPageFormat(int pageIndex) {
        PageFormat pf = new PageFormat();
        return pf;
    }

    public Printable getPrintable(int pageIndex) {
        return this;
    }

    public int print(Graphics g, PageFormat pgFmt, int pi) {
        g.drawString("Page : " + (pi + 1), 200, 200);

        return PAGE_EXISTS;
    }

}
