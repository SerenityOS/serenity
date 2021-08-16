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
 *
 * @bug 4276227
 * @summary Checks that the PrinterGraphics is for a Printer GraphicsDevice.
 * Test doesn't run unless there's a printer on the system.
 * @author prr
 * @run main/othervm PrinterDevice
 */

import java.awt.*;
import java.awt.geom.*;
import java.awt.print.*;
import java.io.*;
import javax.print.attribute.*;
import javax.print.attribute.standard.*;

public class PrinterDevice implements Printable {

    public static void main(String args[]) throws PrinterException {
        System.setProperty("java.awt.headless", "true");

        PrinterJob pj = PrinterJob.getPrinterJob();
        if (pj.getPrintService() == null) {
            return; /* Need a printer to run this test */
        }

        PrintRequestAttributeSet aset = new HashPrintRequestAttributeSet();
        File f = new File("./out.prn");
        f.deleteOnExit();
        aset.add(new Destination(f.toURI()));
        aset.add(OrientationRequested.LANDSCAPE);
        pj.setPrintable(new PrinterDevice());
        pj.print(aset);
    }

    public int print(Graphics g, PageFormat pf, int pageIndex) {
         if (pageIndex > 0 ) {
             return Printable.NO_SUCH_PAGE;
         }

         /* Make sure calls to get DeviceConfig, its transforms,
          * etc all work without exceptions and as expected */
         Graphics2D g2 = (Graphics2D)g;
         GraphicsConfiguration gConfig = g2.getDeviceConfiguration();
         AffineTransform dt = gConfig.getDefaultTransform();
         AffineTransform nt = gConfig.getNormalizingTransform();
         AffineTransform gt = g2.getTransform();

         System.out.println("Graphics2D transform = " + gt);
         System.out.println("Default transform = " + dt);
         System.out.println("Normalizing transform = " + nt);

         Rectangle bounds = gConfig.getBounds();
         System.out.println("Bounds = " + bounds);
         if (!nt.isIdentity()) {
             throw new RuntimeException("Expected Identity transdform");
         }

         /* Make sure that device really is TYPE_PRINTER */
         GraphicsDevice gd = gConfig.getDevice();
         System.out.println("Printer Device ID = " + gd.getIDstring());
         if (!(gd.getType() == GraphicsDevice.TYPE_PRINTER)) {
             throw new RuntimeException("Expected printer device");
         }
         System.out.println(" *** ");
         System.out.println("");
         return Printable.PAGE_EXISTS;
    }
}
