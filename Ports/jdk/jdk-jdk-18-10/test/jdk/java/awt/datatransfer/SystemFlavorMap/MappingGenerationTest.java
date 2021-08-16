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

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.SystemFlavorMap;
import java.util.List;

/*
  @test
  @bug 4512530 8027148
  @summary tests that mappings for text flavors are generated properly
  @author das@sparc.spb.su area=datatransfer
  @modules java.datatransfer
*/

public class MappingGenerationTest {

    private static final SystemFlavorMap fm =
        (SystemFlavorMap)SystemFlavorMap.getDefaultFlavorMap();

    public static void main(String[] args)  {
        test1();
        test2();
        test3();
        test4();
        test5();
        test6();
    }

    /**
     * Verifies that Lists returned from getNativesForFlavor() and
     * getFlavorsForNative() are not modified with a subsequent call
     * to addUnencodedNativeForFlavor() and addFlavorForUnencodedNative()
     * respectively.
     */
    public static void test1() {
        DataFlavor df = new DataFlavor("text/plain-test1", null);
        String nat = "native1";

        List<String> natives = fm.getNativesForFlavor(df);
        fm.addUnencodedNativeForFlavor(df, nat);
        List<String> nativesNew = fm.getNativesForFlavor(df);
        if (natives.equals(nativesNew)) {
            System.err.println("orig=" + natives);
            System.err.println("new=" + nativesNew);
            throw new RuntimeException("Test failed");
        }

        List<DataFlavor> flavors = fm.getFlavorsForNative(nat);
        fm.addFlavorForUnencodedNative(nat, df);
        List<DataFlavor> flavorsNew = fm.getFlavorsForNative(nat);
        if (flavors.equals(flavorsNew)) {
            System.err.println("orig=" + flavors);
            System.err.println("new=" + flavorsNew);
            throw new RuntimeException("Test failed");
        }
    }

    /**
     * Verifies that SystemFlavorMap is not affected by modification of
     * the Lists returned from getNativesForFlavor() and
     * getFlavorsForNative().
     */
    public static void test2() {
        DataFlavor df = new DataFlavor("text/plain-test2", null);
        String nat = "native2";
        DataFlavor extraDf = new DataFlavor("text/test", null);

        List<String> natives = fm.getNativesForFlavor(df);
        natives.add("Should not be here");
        java.util.List nativesNew = fm.getNativesForFlavor(df);
        if (natives.equals(nativesNew)) {
            System.err.println("orig=" + natives);
            System.err.println("new=" + nativesNew);
            throw new RuntimeException("Test failed");
        }

        List<DataFlavor> flavors = fm.getFlavorsForNative(nat);
        flavors.add(extraDf);
        java.util.List flavorsNew = fm.getFlavorsForNative(nat);
        if (flavors.equals(flavorsNew)) {
            System.err.println("orig=" + flavors);
            System.err.println("new=" + flavorsNew);
            throw new RuntimeException("Test failed");
        }
    }

    /**
     * Verifies that addUnencodedNativeForFlavor() for a particular text flavor
     * doesn't affect mappings for other flavors.
     */
    public static void test3() {
        DataFlavor df1 = new DataFlavor("text/plain-test3", null);
        DataFlavor df2 = new DataFlavor("text/plain-test3; charset=Unicode; class=java.io.Reader", null);
        String nat = "native3";
        List<String> natives = fm.getNativesForFlavor(df2);
        fm.addUnencodedNativeForFlavor(df1, nat);
        List<String> nativesNew = fm.getNativesForFlavor(df2);
        if (!natives.equals(nativesNew)) {
            System.err.println("orig=" + natives);
            System.err.println("new=" + nativesNew);
            throw new RuntimeException("Test failed");
        }
    }

    /**
     * Verifies that addUnencodedNativeForFlavor() really adds the specified
     * flavor-to-native mapping to the existing mappings.
     */
    public static void test4() {
        DataFlavor df = new DataFlavor("text/plain-test4; charset=Unicode; class=java.io.Reader", null);
        String nat = "native4";
        List<String> natives = fm.getNativesForFlavor(df);
        if (!natives.contains(nat)) {
            fm.addUnencodedNativeForFlavor(df, nat);
            List<String> nativesNew = fm.getNativesForFlavor(df);
            natives.add(nat);
            if (!natives.equals(nativesNew)) {
                System.err.println("orig=" + natives);
                System.err.println("new=" + nativesNew);
                throw new RuntimeException("Test failed");
            }
        }
    }

    /**
     * Verifies that a flavor doesn't have any flavor-to-native mappings after
     * a call to setNativesForFlavor() with this flavor and an empty native
     * array as arguments.
     */
    public static void test5() {
        final DataFlavor flavor =
            new DataFlavor("text/plain-TEST5; charset=Unicode", null);

        fm.getNativesForFlavor(flavor);

        fm.setNativesForFlavor(flavor, new String[0]);

        List<String> natives = fm.getNativesForFlavor(flavor);

        if (!natives.isEmpty()) {
            System.err.println("natives=" + natives);
            throw new RuntimeException("Test failed");
        }
    }

    /**
     * Verifies that a native doesn't have any native-to-flavor mappings after
     * a call to setFlavorsForNative() with this native and an empty flavor
     * array as arguments.
     */
    public static void test6() {
        final String nat = "STRING";
        fm.getFlavorsForNative(nat);
        fm.setFlavorsForNative(nat, new DataFlavor[0]);

        List<DataFlavor> flavors = fm.getFlavorsForNative(nat);

        if (!flavors.isEmpty()) {
            System.err.println("flavors=" + flavors);
            throw new RuntimeException("Test failed");
        }
    }
}
