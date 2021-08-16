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
 * @bug 6217355 6324057
 * @summary Tests that '?' prints with postscript fonts
 * @run main PSQuestionMark
 */

import java.io.*;
import java.awt.*;
import java.awt.print.*;
import javax.print.*;
import javax.print.attribute.*;

public class PSQuestionMark implements Printable {

    public int print(Graphics g, PageFormat pf, int pageIndex) {

        if (pageIndex > 0) {
            return Printable.NO_SUCH_PAGE;
        }

        g.setColor(Color.black);
        g.setFont(new Font("Serif", Font.PLAIN, 12));
        g.drawString("?", 100, 150);

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
       //FileOutputStream baos = new FileOutputStream("q.ps");
       StreamPrintService svc = spfs[0].getPrintService(baos);

       PrinterJob pj = PrinterJob.getPrinterJob();
       if (svc == null) {
           return;
       }
       pj.setPrintService(svc);
       pj.setPrintable(new PSQuestionMark());
       pj.print();
       //baos.close();

       /* Expect to see the PS we generate for setting 12 pt Times Roman
        * and the hex value of '?'
        * "12.0 12 F"
        * "<3f> 6.72 100.0 150.0 S"
        * This test will need to be updated if the postscript generation
        * changes.
        */
       String outStr = baos.toString("ISO-8859-1");
       String ls = System.getProperty("line.separator");
       if (outStr.indexOf("12.0 32 F"+ls+"<3f>") < 0) {
           throw new Exception("PS font not used");
       }
    }
}
