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
 * @bug 4694495
 * @summary Check that the dialog shows copies = 3.
 * @run main/manual Test
 */
import java.awt.print.*;
import javax.print.*;
import javax.print.attribute.*;
import javax.print.attribute.standard.*;

public class Test {
        static public void main(String args[]) {
                DocFlavor flavor = DocFlavor.SERVICE_FORMATTED.PAGEABLE;
                PrintRequestAttributeSet aSet
                    = new HashPrintRequestAttributeSet();
                PrintService[] services
                    = PrintServiceLookup.lookupPrintServices(flavor, aSet);

                PrinterJob pj = PrinterJob.getPrinterJob();

                for (int i=0; i<services.length; i++)
                    System.out.println(services[i].getName());
                try { pj.setPrintService(services[services.length-1]); }
                catch (Exception e) { e.printStackTrace(); }
                pj.setCopies(3);
                pj.printDialog();
        }
}
