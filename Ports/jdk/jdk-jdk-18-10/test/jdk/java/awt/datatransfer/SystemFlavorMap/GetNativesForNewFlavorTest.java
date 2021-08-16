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
 *          getNativesForFlavor(DataFlavor flav)
 *          with unknown DataFlavor.  Specifically test for
 *          passing an unknown DataFlavor where two-way mapping
 *          should be established.
 * @author Rick Reynaga (rick.reynaga@eng.sun.com) area=Clipboard
 * @modules java.datatransfer
 * @run main GetNativesForNewFlavorTest
 */

public class GetNativesForNewFlavorTest {

    SystemFlavorMap flavorMap;
    Vector comp1, comp2, comp3, comp4;
    Hashtable hash;
    int hashSize;

    String test_encoded;
    DataFlavor test_flavor1, test_flavor2;
    String[] test_natives_set;
    DataFlavor[] test_flavors_set;

    public static void main (String[] args) throws Exception {
        new GetNativesForNewFlavorTest().doTest();
    }

    public void doTest() throws Exception {
        // Initialize DataFlavors and arrays used for test data
        initMappings();

        boolean passed = true;
        flavorMap = (SystemFlavorMap)SystemFlavorMap.getDefaultFlavorMap();

        // Get all the native strings and preferred DataFlavor mappings
        hash = new Hashtable(flavorMap.getFlavorsForNatives(null));
        hashSize = hash.size();

        // GetNativesForFlavor using unknown DataFlavor (verify 2-way mapping)
        //
        // If a new DataFlavor is specified, the method should establish a mapping
        // in both directions between specified DataFlavor and an encoded
        // version of its MIME type as its native.
        System.out.println("GetNativesForFlavor using new DataFlavor");

        comp1 = new Vector(Arrays.asList(test_natives_set));
        comp2 = new Vector(flavorMap.getNativesForFlavor(test_flavor1));

        comp3 = new Vector(Arrays.asList(test_flavors_set));
        comp4 = new Vector(flavorMap.getFlavorsForNative(test_encoded));

        if ( !comp1.equals(comp2) || !comp3.equals(comp4) ) {
            throw new RuntimeException("\n*** After passing a new DataFlavor" +
                "\nwith getNativesForFlavor(DataFlavor flav)" +
                "\nthe mapping in both directions was not established.");
        }
        else
           System.out.println("GetNativesForFlavor using new DataFlavor: Test Passes");
    }

    public void initMappings() throws Exception {
        //initialize mapping variables used in this test
        //create a DataFlavor from Button class
        test_flavor1 = new DataFlavor(Class.forName("java.awt.Button"), "Button");

        //create an Encoded String native using Button class MIME Type
        String buttonMIME = test_flavor1.getMimeType();
        test_encoded = SystemFlavorMap.encodeJavaMIMEType(buttonMIME);

        //create a DataFlavor from the Encoded String native
        test_flavor2 = SystemFlavorMap.decodeDataFlavor(test_encoded);

        //create and initialize DataFlavor arrays
        test_flavors_set = new DataFlavor[] {test_flavor1};

        //create and initialize String native arrays
        test_natives_set = new String[] {test_encoded};
    }
}

