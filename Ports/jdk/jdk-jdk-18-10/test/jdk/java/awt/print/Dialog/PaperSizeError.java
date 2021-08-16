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
 * @bug 6360339
 * @summary Test for fp error in paper size calculations.
 * @run main/manual PaperSizeError
 */

import java.awt.print.*;
import javax.print.*;
import javax.print.attribute.*;
import javax.print.attribute.standard.*;

public class PaperSizeError {

  static String[] instructions = {
     "This test assumes and requires that you have a printer installed",
     "Two page dialogs will appear. You must press 'OK' on both.",
     "If the test fails, it will throw an Exception.",
     ""
  };

  public static void main(String[] args) {

      for (int i=0;i<instructions.length;i++) {
         System.out.println(instructions[i]);
      }

      /* First find out if we have a valid test environment:
       * ie print service exists and supports A4.
       */
      PrinterJob job = PrinterJob.getPrinterJob();
      PrintService service = job.getPrintService();
      if (service == null ||
          !service.isAttributeValueSupported(MediaSizeName.ISO_A4,
                                             null, null)) {
         return;
      }

      // Create A4 sized PageFormat.
      MediaSize a4 = MediaSize.ISO.A4;
      double a4w = Math.rint((a4.getX(1) * 72.0) / Size2DSyntax.INCH);
      double a4h = Math.rint((a4.getY(1) * 72.0) / Size2DSyntax.INCH);
      System.out.println("Units = 1/72\" size=" + a4w + "x" + a4h);
      Paper paper = new Paper();
      paper.setSize(a4w, a4h);
      PageFormat pf = new PageFormat();
      pf.setPaper(paper);

      // Test dialog with PF argument
      PageFormat newPF = job.pageDialog(pf);
      if (newPF == null) {
          return; // user cancelled the dialog (and hence the test).
      } else {
          verifyPaper(newPF, a4w, a4h);
      }

      PrintRequestAttributeSet aset = new HashPrintRequestAttributeSet();
      aset.add(OrientationRequested.PORTRAIT);
      aset.add(MediaSizeName.ISO_A4);

      // Test dialog with AttributeSet argument
      newPF = job.pageDialog(aset);
      if (newPF == null) {
          return; // user cancelled the dialog (and hence the test).
      } else {
          verifyPaper(newPF, a4w, a4h);
      }
  }

  static void verifyPaper(PageFormat pf , double a4w, double a4h) {

      double dw1 = pf.getWidth();
      double dh1 = pf.getHeight();
      float fwMM = (float)((dw1 * 25.4) / 72.0);
      float fhMM = (float)((dh1 * 25.4) / 72.0);
      MediaSizeName msn = MediaSize.findMedia(fwMM, fhMM, Size2DSyntax.MM);
      System.out.println("Units = 1/72\" new size=" + dw1 + "x" + dh1);
      System.out.println("Units = MM new size=" + fwMM + "x" + fhMM);
      System.out.println("Media = " + msn);
      if (a4w != Math.rint(dw1) || a4h != Math.rint(dh1)) {
         System.out.println("Got " + Math.rint(dw1) + "x" + Math.rint(dh1) +
                            ". Expected " + a4w + "x" + a4h);
         throw new RuntimeException("Size is not close enough to A4 size");
      }
      // So far as I know, there's no other standard size that is A4.
      // So we should match the right one.
      if (msn != MediaSizeName.ISO_A4) {
          throw new RuntimeException("MediaSizeName is not A4: " + msn);
      }
  }
}
