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
  @bug 4493189
  @summary tests that addUnencodedNativeForFlavor()/addFlavorForUnencodedNative()
           do not allow to duplicate mappings
  @author das@sparc.spb.su area=datatransfer
  @modules java.datatransfer
  @run main DuplicateMappingTest
*/

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.SystemFlavorMap;
import java.util.Iterator;

public class DuplicateMappingTest {

    public static void main(String[] args) throws Exception {

        final String nativeString = "NATIVE";
        final DataFlavor dataFlavor = new DataFlavor();

        final SystemFlavorMap fm =
                (SystemFlavorMap) SystemFlavorMap.getDefaultFlavorMap();

        fm.addUnencodedNativeForFlavor(dataFlavor, nativeString);
        fm.addUnencodedNativeForFlavor(dataFlavor, nativeString);

        final java.util.List natives =
                fm.getNativesForFlavor(dataFlavor);
        boolean found = false;

        for (final Iterator i = natives.iterator(); i.hasNext(); ) {
            if (nativeString.equals(i.next())) {
                if (found) {
                    throw new RuntimeException("getNativesForFlavor() returns:" +
                            natives);
                } else {
                    found = true;
                }
            }
        }

        if (!found) {
            throw new RuntimeException("getNativesForFlavor() returns:" +
                    natives);
        }

        fm.addFlavorForUnencodedNative(nativeString, dataFlavor);
        fm.addFlavorForUnencodedNative(nativeString, dataFlavor);

        final java.util.List flavors =
                fm.getFlavorsForNative(nativeString);
        found = false;

        for (final Iterator i = flavors.iterator(); i.hasNext(); ) {
            if (dataFlavor.equals(i.next())) {
                if (found) {
                    throw new RuntimeException("getFlavorsForNative() returns:" +
                            flavors);
                } else {
                    found = true;
                }
            }
        }

        if (!found) {
            throw new RuntimeException("getFlavorsForNative() returns:" +
                    natives);
        }
    }
}
