/*
 * Copyright (c) 2001, 2009, Oracle and/or its affiliates. All rights reserved.
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
 * @test 1.3 01/05/11
 * @bug 4456750
 * @summary Test for supported chromaticity values with null DocFlavor.
 *          No exception should be thrown.
 * @run main Chroma
*/

// Chroma.java
import java.io.*;

import javax.print.*;
import javax.print.attribute.*;
import javax.print.attribute.standard.*;

public class Chroma {

   public static void main(String args[]) {

      StreamPrintServiceFactory []fact =
        StreamPrintServiceFactory.lookupStreamPrintServiceFactories(
              DocFlavor.SERVICE_FORMATTED.PRINTABLE,
              DocFlavor.BYTE_ARRAY.POSTSCRIPT.getMimeType());

      if (fact.length != 0) {
          OutputStream out = new ByteArrayOutputStream();
          StreamPrintService sps = fact[0].getPrintService(out);
          checkChroma(sps);
      }

      PrintService defSvc = PrintServiceLookup.lookupDefaultPrintService();
      if (defSvc != null) {
           checkChroma(defSvc);
      }

   }

    static void checkChroma(PrintService svc) {
       if (svc.isAttributeCategorySupported(Chromaticity.class)) {
            svc.getSupportedAttributeValues(Chromaticity.class,null,null);
       }
    }

}
