/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.Component;
import java.awt.print.PrinterJob;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.DialogTypeSelection;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * @test
 * @bug 8067059
 * @run main/manual PageDlgApp
 * @summary Test if cancelling dialog returns null when
 *          PrinterJob.pageDialog() with DialogSelectionType.NATIVE is called
 */
public class PageDlgApp {

    public static void main(String[] args) throws Exception {

        String[] instructions =
         {
             "Visual inspection of the dialog is needed. ",
             "It should be a Printer Job Setup Dialog",
             "Do nothing except Cancel",
             "You must NOT press OK",
         };
        SwingUtilities.invokeAndWait(() -> {
            JOptionPane.showMessageDialog(
                    (Component) null,
                    instructions,
                    "information", JOptionPane.INFORMATION_MESSAGE);
        });
        PrinterJob pj = PrinterJob.getPrinterJob();
        PrintRequestAttributeSet pSet = new HashPrintRequestAttributeSet();
        pSet.add(DialogTypeSelection.NATIVE);
        if ((pj.pageDialog(pSet)) != null) {
            throw
            new RuntimeException("PrinterJob.pageDialog(PrintRequestAttributeSet)"
                        + " does not return null when dialog is cancelled");
        }
    }
}

