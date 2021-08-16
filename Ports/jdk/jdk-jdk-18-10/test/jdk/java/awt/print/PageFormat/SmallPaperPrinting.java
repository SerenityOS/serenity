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

   import java.awt.*;
   import java.awt.print.*;

   public class SmallPaperPrinting
   {
      public static void main(String args[])
      {
        System.out.println("----------------- Instructions --------------------");
        System.out.println("Arguments: (none)  - paper width=1,     height=.0001");
        System.out.println("              1    - paper width=.0001, height=1");
        System.out.println("              2    - paper width=-1,    height=1");
        System.out.println("A passing test should catch a PrinterException");
        System.out.println("and should display \"Print error: (exception msg)\".");
        System.out.println("---------------------------------------------------\n");
         PrinterJob job = PrinterJob.getPrinterJob();
         PageFormat format = job.defaultPage();
         Paper paper = format.getPaper();

         double w = 1, h = .0001;  // Generates ArithmeticException: / by zero.
         if(args.length > 0 && args[0].equals("1")) {
            w = .0001;  h = 1; }  // Generates IllegalArgumentException.
         else if(args.length > 0 && args[0].equals("2")) {
            w = -1;  h = 1; }  // Generates NegativeArraySizeException.
         paper.setSize(w, h);
         paper.setImageableArea(0, 0, w, h);
         format.setPaper(paper);
         job.setPrintable(
               new Printable() {
                  public int print(Graphics g, PageFormat page_format, int page) {
                     return NO_SUCH_PAGE;
                  }
               }, format);

         try {
            job.print(); }
            catch(PrinterException e) {
               System.err.println("Print error:\n" + e.getMessage()); // Passing test!
            }
      }
   }
