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
 * @bug 4186119: setting orientation does not affect printer
 * @summary Confirm that the clip and transform of the Graphics2D is
 *          affected by the landscape orientation of the PageFormat.
 * @run applet/manual=yesno SetOrient.html
 */

import java.awt.*;
import java.awt.geom.*;
import java.awt.print.*;
import java.applet.Applet;

public class SetOrient extends Applet implements Printable {
    PrinterJob pjob;

    public void init() {
        pjob = PrinterJob.getPrinterJob();

        Book book = new Book();
        PageFormat pf = pjob.defaultPage();
        pf.setOrientation(PageFormat.PORTRAIT);
        book.append(this, pf);
        pf = pjob.defaultPage();
        pf.setOrientation(PageFormat.LANDSCAPE);
        book.append(this, pf);
        pjob.setPageable(book);

        try {
            pjob.print();
        } catch (PrinterException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public int print(Graphics g, PageFormat pf, int pageIndex) {
        Graphics2D g2d = (Graphics2D)g;
        drawGraphics(g2d, pf);
        return Printable.PAGE_EXISTS;
    }

    void drawGraphics(Graphics2D g, PageFormat pf) {
        double ix = pf.getImageableX();
        double iy = pf.getImageableY();
        double iw = pf.getImageableWidth();
        double ih = pf.getImageableHeight();

        g.setColor(Color.black);
        g.drawString(((pf.getOrientation() == PageFormat.PORTRAIT)
                      ? "Portrait" : "Landscape"),
                     (int) (ix+iw/2), (int) (iy+ih/2));
        g.draw(new Ellipse2D.Double(ix, iy, iw, ih));
    }
}
