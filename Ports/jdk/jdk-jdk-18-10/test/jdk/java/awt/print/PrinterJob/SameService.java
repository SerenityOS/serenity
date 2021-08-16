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
 * @bug 6446094
 * @summary Don't re-create print services.
 * @run main SameService
 */

import java.awt.*;
import java.awt.print.*;
import javax.print.*;

public class SameService implements Printable {

    public static void main(String args[]) throws Exception {
        PrinterJob job1 = PrinterJob.getPrinterJob();
        job1.setPrintable(new SameService());
        PrintService service1 = job1.getPrintService();
        PrinterJob job2 = PrinterJob.getPrinterJob();
        job2.setPrintable(new SameService());
        PrintService service2 = job2.getPrintService();

        if (service1 != service2) {
           throw new RuntimeException("Duplicate service created");
        }
    }

     public int print(Graphics g, PageFormat pf, int pi)
                       throws PrinterException  {
          return NO_SUCH_PAGE;
     }

}
