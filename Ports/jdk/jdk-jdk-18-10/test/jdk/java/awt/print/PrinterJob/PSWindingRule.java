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

/*
 * @test
 * @bug 4423489
 * @summary Tests that the postscript renders using the appropriate
 * winding rule. Runs as "main" as can't run in sandbox.
 * @run main PSWindingRule
 */

import java.io.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.print.*;
import javax.print.*;
import javax.print.attribute.*;

public class PSWindingRule implements Printable {

    public int print(Graphics g, PageFormat pf, int pageIndex) {

        if (pageIndex > 0) {
            return Printable.NO_SUCH_PAGE;
        }

        Graphics2D g2 = (Graphics2D)g;

        GeneralPath path1 = new GeneralPath(PathIterator.WIND_EVEN_ODD);
        GeneralPath path2 = new GeneralPath(PathIterator.WIND_EVEN_ODD);
        GeneralPath path3 = new GeneralPath(PathIterator.WIND_EVEN_ODD);
        path1.append(new Ellipse2D.Double(100, 100, 100, 100), false);
        path1.append(new Ellipse2D.Double(120, 120, 60, 60), false);
        path1.append(new Ellipse2D.Double(140, 140, 20, 20), false);

        path2.append(new Ellipse2D.Double(150, 100, 100, 100), false);
        path2.append(new Ellipse2D.Double(170, 120, 60, 60), false);
        path2.append(new Ellipse2D.Double(190, 140, 20, 20), false);

        path3.append(new Ellipse2D.Double(-50, -50, 100, 100), false);
        path3.append(new Ellipse2D.Double(-30, -30, 60, 60), false);
        path3.append(new Ellipse2D.Double(-10, -10, 20, 20), false);

        Rectangle clip = new Rectangle();
        g2.getClipBounds(clip);

        g2.setColor(Color.white);
        g2.fillRect(clip.x, clip.y, clip.width, clip.height);

        g2.setColor(Color.red);
        g2.fill(path1);

        g2.setColor(Color.black);
        g2.fill(path2);

        g2.translate(150, 400);
        g2.setColor(Color.red);
        g2.fill(path3);

        g2.translate(50, 0);
        g2.setColor(Color.black);
        g2.fill(path3);

        return Printable.PAGE_EXISTS;
    }

    public static void main(String arg[]) throws Exception {

       DocFlavor psFlavor = new DocFlavor("application/postscript",
                                          "java.io.OutputStream");

       StreamPrintServiceFactory[] spfs =
              PrinterJob.lookupStreamPrintServices("application/postscript");

       if (spfs.length == 0) {
           return;
       }
       ByteArrayOutputStream baos = new ByteArrayOutputStream(4096);
       StreamPrintService svc = spfs[0].getPrintService(baos);

       PrinterJob pj = PrinterJob.getPrinterJob();
       if (svc == null) {
           return;
       }
       pj.setPrintService(svc);
       pj.setPrintable(new PSWindingRule());
       pj.print();

       String outStr = baos.toString("ISO-8859-1");
       int eofillCnt = 0;
       int index = 0;
       String ls = System.getProperty ("line.separator");

       while (index >= 0) {
           index = outStr.indexOf(ls+"EF"+ls, index+1);
           if (index >=0 ) {
               eofillCnt++;
           }
       }
       if (eofillCnt != 4) {
           throw new Exception("Expected 4 eofill's, got: " + eofillCnt);
       }
    }
}
