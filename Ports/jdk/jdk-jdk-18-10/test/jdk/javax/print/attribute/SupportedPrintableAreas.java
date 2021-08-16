/*
 * Copyright (c) 2003, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4762773 6289206 6324049 6362765
 * @summary Tests that get non-null return list of printable areas.
 * @run main SupportedPrintableAreas
 */


import javax.print.*;
import javax.print.event.*;
import javax.print.attribute.*;
import javax.print.attribute.standard.*;

public class SupportedPrintableAreas {

  public static void main(String[] args) {
     PrintService[] svc;
     PrintService printer = PrintServiceLookup.lookupDefaultPrintService();
     if (printer == null) {
         svc = PrintServiceLookup.lookupPrintServices(DocFlavor.SERVICE_FORMATTED.PRINTABLE, null);
         if (svc.length == 0) {
             throw new RuntimeException("Printer is required for this test.  TEST ABORTED");
         }
         printer = svc[0];
     }
     System.out.println("PrintService found : "+printer);

     if (!printer.isAttributeCategorySupported(MediaPrintableArea.class)) {
         return;
     }
     Object value = printer.getSupportedAttributeValues(
                    MediaPrintableArea.class, null, null);
     if (!value.getClass().isArray()) {
         throw new RuntimeException("unexpected value");
      }

     PrintRequestAttributeSet aset = new HashPrintRequestAttributeSet();
     value = printer.getSupportedAttributeValues(
                    MediaPrintableArea.class, null, aset);
     if (!value.getClass().isArray()) {
         throw new RuntimeException("unexpected value");
      }

     Media media = (Media)printer.getDefaultAttributeValue(Media.class);
     aset.add(media);
     value = printer.getSupportedAttributeValues(
                    MediaPrintableArea.class, null, aset);
     if (!value.getClass().isArray()) {
         throw new RuntimeException("unexpected value");
     }

     // test for 6289206
     aset.add(MediaTray.MANUAL);
     value = printer.getSupportedAttributeValues(
                    MediaPrintableArea.class, null, aset);
     if ((value != null) && !value.getClass().isArray()) {
         throw new RuntimeException("unexpected value");
     }
  }
}
