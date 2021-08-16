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
 * @bug 4509958
 * @summary Tests that the empty areas aren't drawn.
 * @run main EmptyFill
 */

import java.io.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.print.*;
import javax.print.*;
import javax.print.attribute.*;

public class EmptyFill implements Printable {

    public int print(Graphics g, PageFormat pf, int pageIndex) {

        if (pageIndex > 0) {
            return Printable.NO_SUCH_PAGE;
        }

        g.setColor(Color.black);

        int[] xq = { 75, 125, 75 };
        int[] yq = { 140, 140, 140};

        g.fillPolygon( xq, yq, 3 );

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
       pj.setPrintable(new EmptyFill());
       pj.print();

       String outStr = baos.toString("ISO-8859-1");
       if (outStr.indexOf("\nfill\n") > 0) {
           throw new Exception("Expected no fills");
       }
    }
}
