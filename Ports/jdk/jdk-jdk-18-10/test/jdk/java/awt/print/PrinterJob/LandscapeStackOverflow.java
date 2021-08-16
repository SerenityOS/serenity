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
 * @key headful printer
 * @bug 6842011 8158758
 * @summary Test if StackOverflowError occurs during printing landscape with
 *          scale and transform.
 * @run main LandscapeStackOverflow
 */
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.OrientationRequested;

public class LandscapeStackOverflow {

    public static final void main( String[] parameters ) {
        PrinterJob printjob = PrinterJob.getPrinterJob();
        printjob.setJobName( "Test Print Job" );

        PrintRequestAttributeSet attributes = new HashPrintRequestAttributeSet();
        attributes.add( OrientationRequested.LANDSCAPE );

        try {
            printjob.setPrintable( new Painter() );
            printjob.print( attributes );
        } catch( PrinterException exception ) {
            exception.printStackTrace();
        }
    }
}

/**
 * Paints a 2 inch by 2 inch rectangle in the center of the page.
 */
class Painter implements Printable {

    public int print( Graphics graphics, PageFormat format, int index ) {
        Graphics2D g2d = (Graphics2D)graphics;

        double scalex = g2d.getTransform().getScaleX();
        double scaley = g2d.getTransform().getScaleY();

        double centerx = ( format.getImageableX() +
                         ( format.getImageableWidth() / 2 ) ) * scalex;
        double centery = ( format.getImageableY() +
                         ( format.getImageableHeight() / 2 ) ) * scaley;

        // The following 2 lines cause an error when printing in landscape.
        g2d.scale( 1 / scalex, 1 / scaley );
        g2d.translate( centerx, centery );

        Path2D.Double path = new Path2D.Double();
        path.moveTo( -scalex * 72, -scaley * 72 );
        path.lineTo( -scalex * 72, scaley * 72 );
        path.lineTo( scalex * 72, scaley * 72 );
        path.lineTo( scalex * 72, -scaley * 72 );
        path.closePath();

        g2d.draw( path );

        return index == 0 ? PAGE_EXISTS : NO_SUCH_PAGE;
    }

}
