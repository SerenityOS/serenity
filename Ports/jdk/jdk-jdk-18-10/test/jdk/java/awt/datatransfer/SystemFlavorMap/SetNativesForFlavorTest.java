/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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
  @bug 4478912
  @summary tests that getNativesForFlavor()/getFlavorsForNative() return the
           same list as was set with setNativesForFlavor()/setFlavorsForNative()
  @author das@sparc.spb.su area=datatransfer
  @modules java.datatransfer
  @run main SetNativesForFlavorTest
*/

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.SystemFlavorMap;

public class SetNativesForFlavorTest  {

    public static void main(String[] args) throws Exception {
            final String nativeString = "NATIVE";

            final SystemFlavorMap fm =
                (SystemFlavorMap)SystemFlavorMap.getDefaultFlavorMap();

            fm.setNativesForFlavor(DataFlavor.plainTextFlavor,
                                   new String[] { nativeString });

            final java.util.List natives =
                fm.getNativesForFlavor(DataFlavor.plainTextFlavor);

            if (natives.size() != 1 || !natives.contains(nativeString)) {
                throw new RuntimeException("getNativesForFlavor() returns:" +
                                           natives);
            }

            final DataFlavor dataFlavor =
                new DataFlavor("text/unknown; class=java.lang.String");

            fm.setFlavorsForNative(nativeString, new DataFlavor[] { dataFlavor });

            final java.util.List flavors = fm.getFlavorsForNative(nativeString);

            if (flavors.size() != 1 || !flavors.contains(dataFlavor)) {
                throw new RuntimeException("getFlavorsForNative() returns:" +
                                           flavors);
            }
    }
}
