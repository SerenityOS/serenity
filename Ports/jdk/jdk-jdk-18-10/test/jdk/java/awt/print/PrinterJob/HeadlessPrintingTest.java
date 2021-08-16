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
 * @bug 4936867
 * @key printer
 * @summary Printing crashes in headless mode.
 * @run main/othervm HeadlessPrintingTest
 */


import java.awt.*;
import javax.print.*;
import javax.print.attribute.*;
import javax.print.attribute.standard.*;
import java.awt.print.*;
import java.io.*;

public class HeadlessPrintingTest {

    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");
        PrinterJob pj = PrinterJob.getPrinterJob();
        pj.setPrintable(new Printable() {
            public int print(Graphics g, PageFormat pg, int pageIndex) {
                Graphics2D g2d = (Graphics2D)g;
                if (pageIndex > 2) {
                    return Printable.NO_SUCH_PAGE;
                } else {
                    g2d.translate(pg.getImageableX(), pg.getImageableY());
                    g2d.setColor(Color.RED);
                    g2d.drawString("page " + pageIndex, 100, 100);
                    return Printable.PAGE_EXISTS;
                }
            }
        });

        try {
            HashPrintRequestAttributeSet attr = new HashPrintRequestAttributeSet();
            File f = File.createTempFile("out", "ps");
            f.deleteOnExit();
            Destination dest = new Destination(f.toURI());
            attr.add(dest);
            pj.print(attr);
        } catch (Exception e) {
        }
    }
}
