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
  @bug 4493178
  @summary tests that getNativesForFlavor() synthesizes an encoded String native
           only if there are no mappings for the DataFlavor and the mappings
           were not explicitly removed
  @author das@sparc.spb.su area=datatransfer
  @modules java.datatransfer
  @run main GetNativesForFlavorTest
*/

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.SystemFlavorMap;
import java.util.Iterator;

public class GetNativesForFlavorTest {

    final static SystemFlavorMap fm =
            (SystemFlavorMap) SystemFlavorMap.getDefaultFlavorMap();

    public static void main(String[] args) throws Exception {
        // 1.Check that the encoded native is not added if there are other
        // natives for this DataFlavor.
        test1();

        // 2.Check that the encoded native is not added if all mappings were
        // explicitly removed for this DataFlavor.
        test2();

        // 3.Check that only the encoded native is added for text DataFlavors
        // that has no mappings and that DataFlavor is properly encoded.
        test3();

        // 4.Verifies that the encoded native is added only for DataFlavors
        // that has no mappings and that DataFlavor is properly encoded.
        test4();
    }

    /**
     * Verifies that the encoded native is not added if there are other
     * natives mapped to this DataFlavor.
     */
    public static void test1() throws ClassNotFoundException {
        final DataFlavor flavor =
                new DataFlavor("text/plain-TEST; charset=Unicode");

        final java.util.List natives = fm.getNativesForFlavor(flavor);

        if (natives.size() > 1) {
            for (final Iterator i = natives.iterator(); i.hasNext(); ) {
                String element = (String) i.next();
                if (SystemFlavorMap.isJavaMIMEType(element)) {
                    throw new RuntimeException("getFlavorsForNative() returns: "
                            + natives);
                }
            }
        }
    }

    /**
     * Verifies that the encoded native is not added if all mappings were
     * explicitly removed for this DataFlavor.
     */
    public static void test2() throws ClassNotFoundException {
        final DataFlavor flavor =
                new DataFlavor("text/plain-TEST; charset=Unicode");

        fm.setNativesForFlavor(flavor, new String[0]);

        final java.util.List natives = fm.getNativesForFlavor(flavor);

        if (!natives.isEmpty()) {
            throw new RuntimeException("getFlavorsForNative() returns:" +
                    natives);
        }
    }

    /**
     * Verifies that only the encoded native is added for text DataFlavors
     * that has no mappings and that DataFlavor is properly encoded.
     */
    public static void test3() throws ClassNotFoundException {
        //
        final DataFlavor flavor =
                new DataFlavor("text/plain-TEST-nocharset; class=java.nio.ByteBuffer");

        final java.util.List natives = fm.getNativesForFlavor(flavor);
        boolean encodedNativeFound = false;

        if (natives.size() == 0) {
            throw new RuntimeException("getFlavorsForNative() returns:" +
                    natives);
        }

        if (natives.size() == 1) {
            String element = (String) natives.get(0);
            if (SystemFlavorMap.isJavaMIMEType(element)) {
                final DataFlavor decodedFlavor =
                        SystemFlavorMap.decodeDataFlavor(element);
                if (!flavor.equals(decodedFlavor)) {
                    System.err.println("DataFlavor is not properly incoded:");
                    System.err.println("    encoded flavor: " + flavor);
                    System.err.println("    decoded flavor: " + decodedFlavor);
                    throw new RuntimeException("getFlavorsForNative() returns:"
                            + natives);
                }
            }
        } else {
            for (final Iterator i = natives.iterator(); i.hasNext(); ) {
                String element = (String) i.next();
                if (SystemFlavorMap.isJavaMIMEType(element)) {
                    throw new RuntimeException("getFlavorsForNative() returns:"
                            + natives);
                }
            }
        }
    }

    /**
     * Verifies that the encoded native is added only for DataFlavors
     * that has no mappings and that DataFlavor is properly encoded.
     */
    public static void test4() throws ClassNotFoundException {
        final DataFlavor flavor =
                new DataFlavor("unknown/unknown");

        final java.util.List natives = fm.getNativesForFlavor(flavor);

        if (natives.size() == 1) {
            String element = (String) natives.get(0);
            if (SystemFlavorMap.isJavaMIMEType(element)) {
                final DataFlavor decodedFlavor =
                        SystemFlavorMap.decodeDataFlavor(element);
                if (!flavor.equals(decodedFlavor)) {
                    System.err.println("DataFlavor is not properly incoded:");
                    System.err.println("    encoded flavor: " + flavor);
                    System.err.println("    decoded flavor: " + decodedFlavor);
                    throw new RuntimeException("getFlavorsForNative() returns:"
                            + natives);
                }
            }
        } else {
            throw new RuntimeException("getFlavorsForNative() returns:"
                    + natives);
        }
    }
}
