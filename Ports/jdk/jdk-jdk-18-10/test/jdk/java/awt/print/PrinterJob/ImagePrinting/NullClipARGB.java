/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @key printer
 * @bug 8061392
 * @summary Test no NPE when printing transparency with null clip.
 */

import java.awt.*;
import java.awt.image.*;
import java.awt.print.*;

public class NullClipARGB implements Printable {

    public static void main( String[] args ) {

        try {
            PrinterJob pj = PrinterJob.getPrinterJob();
            pj.setPrintable(new NullClipARGB());
            pj.print();
            } catch (Exception ex) {
             throw new RuntimeException(ex);
        }
    }

    public int print(Graphics g, PageFormat pf, int pageIndex)
               throws PrinterException{

        if (pageIndex != 0) {
            return NO_SUCH_PAGE;
        }
        Graphics2D g2 = (Graphics2D)g;
        System.out.println("original clip="+g2.getClip());
        g2.translate(pf.getImageableX(), pf.getImageableY());
        g2.rotate(0.2);
        g2.setClip(null);
        g2.setColor( Color.BLACK );
        g2.drawString("This text should be visible through the image", 0, 20);
        BufferedImage bi = new BufferedImage(100, 100,
                                              BufferedImage.TYPE_INT_ARGB );
        Graphics ig = bi.createGraphics();
        ig.setColor( new Color( 192, 192, 192, 80 ) );
        ig.fillRect( 0, 0, 100, 100 );
        ig.setColor( Color.BLACK );
        ig.drawRect( 0, 0, 99, 99 );
        ig.dispose();
        g2.drawImage(bi, 10, 0, 90, 90, null );
        g2.translate(100, 100);
        g2.drawString("This text should also be visible through the image", 0, 20);
        g2.drawImage(bi, 10, 0, 90, 90, null );
        return PAGE_EXISTS;
    }
}
