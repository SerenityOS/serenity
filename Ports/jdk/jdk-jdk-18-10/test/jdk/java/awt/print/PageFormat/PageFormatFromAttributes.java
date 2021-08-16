/*
 * Copyright (c) 2009, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4500750 6848799 8028584
 * @summary Tests creating page format from attributes
 * @run main PageFormatFromAttributes
 */
import java.awt.print.*;
import javax.print.*;
import javax.print.attribute.*;
import javax.print.attribute.standard.*;

public class PageFormatFromAttributes {

    public static void main(String args[]) {
        PrinterJob job = PrinterJob.getPrinterJob();
        PrintService service = job.getPrintService();
        if (service == null) {
            return; // No printers
        }
        PrintRequestAttributeSet aset = new HashPrintRequestAttributeSet();
        test(job, MediaSizeName.ISO_A4, OrientationRequested.PORTRAIT);
        test(job, MediaSizeName.ISO_A4, OrientationRequested.LANDSCAPE);
        test(job, MediaSizeName.ISO_A4,
             OrientationRequested.REVERSE_LANDSCAPE);
        test(job, MediaSizeName.ISO_A3, OrientationRequested.PORTRAIT);
        test(job, MediaSizeName.NA_LETTER, OrientationRequested.PORTRAIT);
        test(job, MediaSizeName.NA_LETTER, OrientationRequested.LANDSCAPE);
        test(job, MediaSizeName.NA_LEGAL, OrientationRequested.PORTRAIT);
    }

    static void test(PrinterJob job,
                     MediaSizeName media, OrientationRequested orient) {

        PrintRequestAttributeSet aset = new HashPrintRequestAttributeSet();
        aset.add(media);
        aset.add(orient);

        PrintService service = job.getPrintService();
        if (!service.isAttributeValueSupported(media, null, aset) ||
            !service.isAttributeValueSupported(orient, null, aset)) {
            return; // Can't test this case.
        }
        PageFormat pf = job.getPageFormat(aset);
        boolean ok = true;
        switch (pf.getOrientation()) {
        case PageFormat.PORTRAIT :
            ok = orient == OrientationRequested.PORTRAIT;
            break;
        case PageFormat.LANDSCAPE :
            ok = orient == OrientationRequested.LANDSCAPE;
            break;
        case PageFormat.REVERSE_LANDSCAPE :
            ok = orient == OrientationRequested.REVERSE_LANDSCAPE;
            break;
        }
        if (!ok) {
            throw new RuntimeException("orientation not as specified");
        }
        MediaSize mediaSize = MediaSize.getMediaSizeForName(media);
        if (mediaSize == null) {
            throw new RuntimeException("expected a media size");
        }
        double units = Size2DSyntax.INCH/72.0;
        double w = mediaSize.getX(1) / units;
        double h = mediaSize.getY(1) / units;
        Paper paper = pf.getPaper();
        double pw = paper.getWidth();
        double ph = paper.getHeight();
        if (Math.round(pw) != Math.round(w) ||
            Math.round(ph) != Math.round(h)) {
            throw new RuntimeException("size not as specified");
        }
    }
}
