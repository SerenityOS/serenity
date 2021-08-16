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
 * @bug 6568874
 * @summary Verify the native dialog works with attribute sets.
 * @run main/manual=yesno DialogType
 */

import java.awt.print.*;
import javax.print.attribute.*;
import javax.print.attribute.standard.*;

public class DialogType {

  static String[] instructions = {
     "This test assumes and requires that you have a printer installed",
     "It verifies that the dialogs behave properly when using new API",
     "to optionally select a native dialog where one is present.",
     "Two dialogs are shown in succession.",
     "The test passes as long as no exceptions are thrown, *AND*",
     "if running on Windows only, the first dialog is a native windows",
     "control which differs in appearance from the second dialog",
     ""
  };

  public static void main(String[] args) {

      for (int i=0;i<instructions.length;i++) {
         System.out.println(instructions[i]);
      }

      PrinterJob job = PrinterJob.getPrinterJob();
      if (job.getPrintService() == null) {
         return;
      }

      PrintRequestAttributeSet aset = new HashPrintRequestAttributeSet();
      aset.add(DialogTypeSelection.NATIVE);
      job.printDialog(aset);
      Attribute[] attrs = aset.toArray();
      for (int i=0;i<attrs.length;i++) {
          System.out.println(attrs[i]);
      }
      aset.add(DialogTypeSelection.COMMON);
      job.printDialog(aset);
      attrs = aset.toArray();
      for (int i=0;i<attrs.length;i++) {
          System.out.println(attrs[i]);
      }
   }
}
