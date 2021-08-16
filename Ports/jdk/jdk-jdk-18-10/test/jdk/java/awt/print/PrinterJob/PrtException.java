/*
 * Copyright (c) 2001, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4429544
 * @summary This test should not throw a printer exception. Test has been modified to correspond with the behavior of 1.5 and above.
 * @run main PrtException
 */

import java.awt.*;
import java.awt.print.*;
import javax.print.*;

public class PrtException implements Printable {
    PrinterJob pj;

    public PrtException() {

        try{
            PrintService[] svc;
            PrintService defService = PrintServiceLookup.lookupDefaultPrintService();
            if (defService == null) {
                svc = PrintServiceLookup.lookupPrintServices(DocFlavor.SERVICE_FORMATTED.PRINTABLE, null);
                if (svc.length == 0) {
                    throw new RuntimeException("Printer is required for this test.  TEST ABORTED");
                }
                defService = svc[0];
            }

            System.out.println("PrintService found : "+defService);
            pj = PrinterJob.getPrinterJob();;
            pj.setPrintService(defService);
            //pj.setPrintable(this); // commenting this line should not result in PrinterException
            pj.print();
        } catch(PrinterException e ) {
            e.printStackTrace();
            throw new RuntimeException(" PrinterException should not be thrown. TEST FAILED");
        }
        System.out.println("TEST PASSED");
    }


    public int print(Graphics g,PageFormat pf,int pageIndex) {
        Graphics2D g2= (Graphics2D)g;
        if(pageIndex>=1){
            return Printable.NO_SUCH_PAGE;
        }
        g2.translate(pf.getImageableX(), pf.getImageableY());
        g2.setColor(Color.black);
        g2.drawString("Hello world.", 10, 10);

        return Printable.PAGE_EXISTS;
    }

    public static void main(String arg[]) {
        PrtException sp = new PrtException();
    }
}
