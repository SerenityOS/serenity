/*
 * Copyright (c) 1999, 2015, Oracle and/or its affiliates. All rights reserved.
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
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;

/*
 * @test
 * @summary To test SystemFlavorMap method:
 *          setNativesForFlavor(DataFlavor flav, String[] natives)
 *          with valid natives and DataFlavors. This stress test will
 *          define numerous mappings of valid String natives and
 *          DataFlavors.  The mappings will be verified by examining
 *          that all entries are present, and order is maintained.
 * @author Rick Reynaga (rick.reynaga@eng.sun.com) area=Clipboard
 * @modules java.datatransfer
 * @run main SetDataFlavorsTest
 */

public class SetDataFlavorsTest {

    SystemFlavorMap flavorMap;
    Hashtable hashVerify;

    Map mapFlavors;
    Map mapNatives;

    Hashtable hashFlavors;
    Hashtable hashNatives;

    public static void main (String[] args) {
        new SetDataFlavorsTest().doTest();
    }

    public void doTest() {
        flavorMap = (SystemFlavorMap)SystemFlavorMap.getDefaultFlavorMap();

        // Get SystemFlavorMap Maps of String Natives and DataFlavors
        mapFlavors = flavorMap.getNativesForFlavors(null);
        mapNatives = flavorMap.getFlavorsForNatives(null);

        hashFlavors = new Hashtable(mapFlavors);
        hashNatives = new Hashtable(mapNatives);


        // Test setNativesForFlavor(DataFlavors flav, String[] natives);
        //
        // Enumerate through all the system defined DataFlavors,
        // and for each DataFlavor, define it again to the SystemFlavorMap
        // with a slightly modified MimeType.
        //
        // For verification, a seperate Hashtable will be maintained of the additions.
        DataFlavor key;
        hashVerify = new Hashtable();

        for (Enumeration e = hashFlavors.keys() ; e.hasMoreElements() ;) {
            key = (DataFlavor)e.nextElement();

            java.util.List listNatives = flavorMap.getNativesForFlavor(key);
            Vector vectorNatives = new Vector(listNatives);
            String[] arrayNatives = (String[])vectorNatives.toArray(new String[0]);

            // Construct a new DataFlavor from an existing DataFlavor's MimeType
            // Example:
            // Original MimeType: "text/plain; class=java.io.Reader; charset=Unicode"
            // Modified MimeType: "text/plain-TEST; class=java.io.Reader; charset=Unicode"

            StringBuffer mimeType = new StringBuffer(key.getMimeType());
            mimeType.insert(mimeType.indexOf(";"),"-TEST");

            DataFlavor testFlav = new DataFlavor(mimeType.toString(), "Test DataFlavor");

                                        // define the new String native entry
            flavorMap.setNativesForFlavor(testFlav, arrayNatives);
                                        // keep track of this new native entry
            hashVerify.put(testFlav, vectorNatives);
        }

        // After establishing "new" mappings, verify that the defined
        // Natives can be retrieved and that the List (order) is preserved.
        verifyNewMappings();
    }

    // Verify getNativesForFlavor(DataFlavor flav) is returning the correct list
    // of Natives (for the new mappings).
    public void verifyNewMappings() {
        // Enumerate through all DataFlavors
        for (Enumeration e = hashVerify.keys() ; e.hasMoreElements() ;) {
            DataFlavor key = (DataFlavor)e.nextElement();

            java.util.List listNatives = flavorMap.getNativesForFlavor(key);
            Vector vectorFlavors = new Vector(listNatives);

            // Compare the list of Natives
            if ( !vectorFlavors.equals(hashVerify.get(key))) {
                throw new RuntimeException("\n*** Error in verifyNewMappings()" +
                    "\nmethod1: setNativesForFlavor(DataFlavor flav, String[] natives)" +
                    "\nmethod2: List getNativesForFlavor(DataFlavor flav)" +
                    "\nDataFlavor: " + key.getMimeType() +
                    "\nThe Returned List did not match the original set of Natives.");
            }
        }
        System.out.println("*** DataFlavor size = " + hashVerify.size());
    }
}

