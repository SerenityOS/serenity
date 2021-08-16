/*
 * Copyright (c) 2002, 2021, Oracle and/or its affiliates. All rights reserved.
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
  @key printer
  @bug 4722616 8263439
  @summary Verify no exception enumerating all attributes
*/

import javax.print.PrintService;
import javax.print.PrintServiceLookup;

public class PrintValues {

    public static void main(String[] args) {

        PrintService printer = PrintServiceLookup.lookupDefaultPrintService();
        if (printer == null) {
            System.out.println("No default printer configured. Cannot continue");
            return;
        }
        Class[] categories = printer.getSupportedAttributeCategories();

        for (int i=0; i<categories.length; i++) {
            System.out.print("Class "+categories[i]);
            System.out.print(' ');
            Object value =
                printer.getSupportedAttributeValues (categories[i], null, null);
            if ((value != null) && (value.getClass().isArray())) {
                Object[] v = (Object[])value;
                for (int j=0; j<v.length; j++) {
                    if (j > 0) {
                        System.out.print(", ");
                    }
                    System.out.print(v[j]);
                }
            } else {
                System.out.print(value);
            }
            System.out.println();
        }
    }
}
