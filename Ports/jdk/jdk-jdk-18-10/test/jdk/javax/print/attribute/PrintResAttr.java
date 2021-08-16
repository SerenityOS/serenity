/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8048328
 * @summary CUPS Printing does not report supported printer resolutions.
 * @run main PrintResAttr
 */

/*
 * Since there is no guarantee you have any printers that support
 * resolution this test can't verify that resolution is being
 * reported when supported. But when they are it should test that
 * the code behaves reasonably.
 */
import javax.print.*;
import javax.print.attribute.*;
import javax.print.attribute.standard.*;

public class PrintResAttr {

   public static void main(String args[]) throws Exception {

      PrintService[] services =
            PrintServiceLookup.lookupPrintServices(null,null);
      for (int i=0; i<services.length; i++) {
          if (services[i].isAttributeCategorySupported(PrinterResolution.class)) {
              System.out.println("Testing " + services[i]);
              PrinterResolution[] res = (PrinterResolution[])
            services[i].getSupportedAttributeValues(PrinterResolution.class,
                                                      null,null);
              System.out.println("# supp res= " + res.length);
              for (int r=0;r<res.length;r++) System.out.println(res[r]);
          }
      }
   }
}
