/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
  @test
  @bug 6334074 8022536
  @summary test supported text flavors reported properly
  @run main TextFlavorTest
*/

import javax.print.*;
import javax.print.attribute.standard.*;
import javax.print.attribute.*;
import java.io.*;

public class TextFlavorTest {

    public static void main(String[] args) throws Exception {

        PrintService service[] =
            PrintServiceLookup.lookupPrintServices(null, null);

        if (service.length == 0) {
            System.out.println("No print service found.");
            return;
        }

        for (int y = 0; y < service.length; y ++) {
            DocFlavor flavors[] = service[y].getSupportedDocFlavors();
            if (flavors == null) continue;
            for (int x = 0; x < flavors.length; x ++) {
                if (!service[y].isDocFlavorSupported(flavors[x])) {
                    String msg = "DocFlavor " + flavors[x] +
                        " is not supported by service "+ service[y];
                    throw new RuntimeException(msg);
                }
            }
        }
        System.out.println("Test passed.");
    }
}
