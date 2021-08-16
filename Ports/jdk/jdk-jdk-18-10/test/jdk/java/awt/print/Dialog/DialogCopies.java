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
 * @bug 6357858
 * @summary Job must reports the number of copies set in the dialog.
 * @run main/manual DialogCopies
 */

import java.awt.print.*;

public class DialogCopies {

  static String[] instructions = {
     "This test assumes and requires that you have a printer installed",
     "When the dialog appears, increment the number of copies then press OK.",
     "The test will throw an exception if you fail to do this, since",
     "it cannot distinguish that from a failure",
     ""
  };

  public static void main(String[] args) {

      for (int i=0;i<instructions.length;i++) {
         System.out.println(instructions[i]);
      }

      PrinterJob job = PrinterJob.getPrinterJob();
      if (job.getPrintService() == null || !job.printDialog()) {
         return;
      }

      System.out.println("Job copies is " + job.getCopies());

      if (job.getCopies() == 1) {
            throw new RuntimeException("Copies not incremented");
      }
   }
}
