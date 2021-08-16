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
 * @bug 4480930
 * @summary TextLayout prints as filled shapes
 * @author prr
 * @run main/manual PrintTextLayout
 */

/* This is a MANUAL test and must be run on a system with a printer
 * configured.
 */

import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.font.*;
import java.awt.geom.*;
import java.awt.print.*;
import javax.print.attribute.*;
import javax.print.attribute.standard.*;

public class PrintTextLayout implements Printable {
    static String[] fontnames = {
        "SansSerif",
        "Serif",
        "Monospaced",
     };

    static String text =
    "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ 1234567890";

    public static void main(String args[]) {
        PrinterJob pj = PrinterJob.getPrinterJob();

        if (pj != null) {
            PrintRequestAttributeSet aset = new HashPrintRequestAttributeSet();
            aset.add(new Destination((new File("./out.ps")).toURI()));
            PageFormat pf = pj.defaultPage();
            Paper p = pf.getPaper();
            // Extend imageable width to reduce likelihood end of text
            // is clipped as we'd like to see the end of the line.
            p.setImageableArea(p.getImageableX(), p.getImageableY(),
                               p.getWidth()-p.getImageableX(),
                               p.getImageableHeight());
            pf.setPaper(p);
            pj.setPrintable( new PrintTextLayout(), pf);
            try {
                pj.print(aset);
            } catch (PrinterException pe) {
                pe.printStackTrace();
            } finally {
            }
        }
    }

    public int print(Graphics g, PageFormat pgFmt, int pgIndex) {
        if (pgIndex > 0) return Printable.NO_SUCH_PAGE;

        double iw = pgFmt.getImageableWidth();
        double ih = pgFmt.getImageableHeight();
        Graphics2D g2d = (Graphics2D)g;
        g2d.translate(pgFmt.getImageableX(), pgFmt.getImageableY()+50);

        float ypos = 20f;
        for (int f=0; f< fontnames.length; f++) {
            for (int s=0;s<4;s++) {
                Font font = new Font(fontnames[f], s, 12);
                ypos = drawText(g2d, font, ypos);
            }
        }
        return Printable.PAGE_EXISTS;
    }

    float drawText(Graphics2D g2d, Font font, float ypos) {
        int x = 10;
        /* Set the graphics font to something odd before using TL so
         * can be sure it picks up the font from the TL, not the graphics */
        Font f1 = new Font("serif", Font.ITALIC, 1);
        g2d.setFont(f1);
        FontRenderContext frc = new FontRenderContext(null, false, false);
        TextLayout tl = new TextLayout(text ,font, frc);
        float ascent = tl.getAscent();
        int dpos = (int)(ypos+ascent);
        tl.draw(g2d, x, dpos);
        int dpos2 = (int)(ypos+ascent+tl.getDescent());
        g2d.drawLine(x, dpos2, x+(int)tl.getAdvance(), dpos2);
        float tlHeight = tl.getAscent()+tl.getDescent()+tl.getLeading();
        return ypos+tlHeight;
    }
}
