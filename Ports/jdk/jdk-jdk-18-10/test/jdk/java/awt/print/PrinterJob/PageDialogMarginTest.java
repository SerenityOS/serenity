/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug      6801613
 * @summary  Verifies if cross-platform pageDialog and printDialog top margin
 *           entry is working
 * @run      main/manual PageDialogMarginTest
 */
import java.awt.Component;
import java.awt.print.PageFormat;
import java.awt.print.PrinterJob;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.MediaPrintableArea;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public class PageDialogMarginTest {

    public static void main(String args[]) throws Exception {
        String[] instructions
                = {
                    "Page Dialog will be shown.",
                    "Change top(in) margin value from 1.0 to 2.0",
                    "Then select OK."
                };
        SwingUtilities.invokeAndWait(() -> {
            JOptionPane.showMessageDialog((Component) null,
                    instructions, "Instructions",
                    JOptionPane.INFORMATION_MESSAGE);
        });
        PrinterJob pj = PrinterJob.getPrinterJob();
        try {
            HashPrintRequestAttributeSet aset = new HashPrintRequestAttributeSet();
            PageFormat pf;
            pf = pj.pageDialog(aset);
            double left = pf.getImageableX();
            double top = pf.getImageableY();
            System.out.println("pageDialog - left/top from pageFormat: " + left / 72
                    + " " + top / 72);
            System.out.println("pageDialog - left/top from attribute set: "
                    + getPrintableXFromASet(aset) + " "
                    + getPrintableYFromASet(aset));
            if (top / 72 != 2.0f || getPrintableYFromASet(aset) != 2.0f) {
                throw new RuntimeException("Top margin value not updated");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static double getPrintableXFromASet(PrintRequestAttributeSet aset) {
        try {
            return ((MediaPrintableArea) aset.get(
                    MediaPrintableArea.class)).getX(MediaPrintableArea.INCH);
        } catch (Exception e) {
            return -1.0;
        }
    }

    static double getPrintableYFromASet(PrintRequestAttributeSet aset) {
        try {
            return ((MediaPrintableArea) aset.get(
                    MediaPrintableArea.class)).getY(MediaPrintableArea.INCH);
        } catch (Exception e) {
            return -1.0;
        }
    }

}
