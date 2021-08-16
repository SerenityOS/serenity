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
 * @bug 4184565
 * @summary Confirm that the default foreground color on a printer
 *          graphics object is black so that rendering will appear
 *          without having to execute setColor first.
 * @run applet/manual=yesno InitToBlack.html
 */

import java.awt.*;
import java.awt.print.*;
import java.applet.Applet;

public class InitToBlack extends Applet implements Printable {

    public void init() {
        PrinterJob pjob = PrinterJob.getPrinterJob();

        Book book = new Book();
        book.append(this, pjob.defaultPage());
        pjob.setPageable(book);

        try {
            pjob.print();
        } catch (PrinterException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public int print(Graphics g, PageFormat pf, int pageIndex) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.translate(pf.getImageableX(), pf.getImageableY());

        g.drawString("Test Passes", 200, 200);

        return PAGE_EXISTS;
    }

    public static void main(String[] args) {
        new InitToBlack().init();
        System.exit(0);
    }
}
