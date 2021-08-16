/*
 * Copyright (c) 2001, 2015, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Vector;

/*
 * @test
 * @summary To test SystemFlavorMap method
 *          addUnencodedNativeForFlavor(DataFlavor flav, String nat)
 *          with valid natives and DataFlavors. Specifically test for
 *          adding new mappings, one-way and two-way, and to update
 *          existing mappings.
 * @author Rick Reynaga (rick.reynaga@eng.sun.com) area=Clipboard
 * @modules java.datatransfer
 * @run main AddNativeForFlavorTest
 */

public class AddNativeForFlavorTest {

    SystemFlavorMap flavorMap;
    Vector comp1, comp2, comp3;
    Hashtable hash;
    int hashSize;

    String test_native1, test_native2, test_native3, test_native4;
    String[] test_natives_set1, test_natives_set2;
    DataFlavor test_flav;
    DataFlavor[] test_flavors_set;

    public static void main(String[] args) throws Exception {
        new AddNativeForFlavorTest().doTest();
    }

    public void doTest() throws Exception {
        // Initialize DataFlavors and arrays used for test data
        initMappings();

        flavorMap = (SystemFlavorMap)SystemFlavorMap.getDefaultFlavorMap();

        // Get all the native strings and preferred DataFlavor mappings
        hash = new Hashtable(flavorMap.getFlavorsForNatives(null));
        hashSize = hash.size();

        // Setup One-way Mappings
        System.out.println("One-way Mappings Test");
        flavorMap.addUnencodedNativeForFlavor(test_flav, test_native1);
        flavorMap.addUnencodedNativeForFlavor(test_flav, test_native2);

        // Confirm mapping with getNativesForFlavor
        comp1 = new Vector(Arrays.asList(test_natives_set1));
        comp2 = new Vector(flavorMap.getNativesForFlavor(test_flav));

        if ( !comp1.equals(comp2)) {
            throw new RuntimeException("\n*** After setting up one-way mapping" +
                "\nwith addUnencodedNativeForFlavor(DataFlavor flav, String nat)" +
                "\nthe mappings returned from getNativesForFlavor() do not match" +
                "\noriginal mappings.");
        }
        else
           System.out.println("One-way: Test Passes");

        // Setup Two-way Mapping
        System.out.println("Two-way Mappings Test");
        flavorMap.addFlavorForUnencodedNative(test_native1, test_flav);
        flavorMap.addFlavorForUnencodedNative(test_native2, test_flav);

        // Confirm mapping with getFlavorsForNative
        comp1 = new Vector(Arrays.asList(test_flavors_set));
        comp2 = new Vector(flavorMap.getFlavorsForNative(test_native1));
        comp3 = new Vector(flavorMap.getFlavorsForNative(test_native2));

        if ( !(comp1.equals(comp2)) || !(comp1.equals(comp3))) {
            throw new RuntimeException("\n*** After setting up two-way mapping" +
                "\nwith addFlavorForUnencodedNative(String nat, DataFlavor flav)" +
                "\nthe mappings returned from getFlavorsForNative() do not match" +
                "\noriginal mappings.");
        }
        else
           System.out.println("Two-way (DataFlavor): Test Passes");

        // Check first native mapping
        comp1 = new Vector(Arrays.asList(test_natives_set1));
        comp2 = new Vector(flavorMap.getNativesForFlavor(test_flav));

        if ( !comp1.equals(comp2)) {
            throw new RuntimeException("\n*** After setting up two-way mapping" +
                "\nwith addUnencodedNativeForFlavor(DataFlavor flav, String nat)" +
                "\nthe mappings returned from getNativesForFlavor() do not match" +
                "\noriginal mappings.");
        }
        else
           System.out.println("Two-way (String native): Test Passes");

        // Modify an existing mapping test
        System.out.println("Modify Existing Mappings Test");
        flavorMap.addUnencodedNativeForFlavor(test_flav, test_native3);
        flavorMap.addUnencodedNativeForFlavor(test_flav, test_native4);

        // Confirm mapping with getNativesForFlavor
        comp1 = new Vector(Arrays.asList(test_natives_set2));
        comp2 = new Vector(flavorMap.getNativesForFlavor(test_flav));

        if ( !comp1.equals(comp2)) {
            throw new RuntimeException("\n*** After modifying an existing mapping" +
                "\nwith addUnencodedNativeForFlavor(DataFlavor flav, String nat)" +
                "\nthe mappings returned from getNativesForFlavor() do not match" +
                "\nupdated mappings.");
        }
        else
           System.out.println("Modify Existing Mappings: Test Passes");

    }

    public void initMappings() throws Exception {
      //create String natives
      test_native1 = "TEST1";
      test_native2 = "TEST2";
      test_native3 = "TEST3";
      test_native4 = "TEST4";

      test_flav = new DataFlavor(Class.forName("java.awt.Label"), "test1");

      //create and initialize DataFlavor arrays
      test_flavors_set = new DataFlavor[] {test_flav};

      //create and initialize String native arrays
      test_natives_set1 = new String[] {test_native1, test_native2};
      test_natives_set2 = new String[] {test_native1, test_native2, test_native3, test_native4};
    }
}

