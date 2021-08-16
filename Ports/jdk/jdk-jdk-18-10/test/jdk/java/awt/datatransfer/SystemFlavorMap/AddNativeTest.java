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
import java.util.*;

/*
 * @test
 * @summary To test SystemFlavorMap method:
 *          addUnencodedNativeForFlavor(DataFlavor flav, String nat)
 *          with valid natives and DataFlavors. This stress test will
 *          define numerous mappings of valid String natives and
 *          DataFlavors.  The mappings will be verified by examining
 *          that all entries are present.
 * @author Rick Reynaga (rick.reynaga@eng.sun.com) area=Clipboard
 * @modules java.datatransfer
 * @run main AddNativeTest
 */

public class AddNativeTest {

    SystemFlavorMap flavorMap;
    Hashtable hashVerify;

    Map mapFlavors;
    Map mapNatives;

    Hashtable hashFlavors;
    Hashtable hashNatives;

    public static void main(String[] args) {
        new AddNativeTest().doTest();
    }

    public void doTest() {
        flavorMap = (SystemFlavorMap)SystemFlavorMap.getDefaultFlavorMap();

        // Get SystemFlavorMap Maps of String Natives and DataFlavors
        mapFlavors = flavorMap.getNativesForFlavors(null);
        mapNatives = flavorMap.getFlavorsForNatives(null);

        hashFlavors = new Hashtable(mapFlavors);
        hashNatives = new Hashtable(mapNatives);

        // Test addUnencodedNativeForFlavor(DataFlavor flav, String nat);
        //
        // Enumerate through all the system defined DataFlavors,
        // and for each DataFlavor, define it again to the SystemFlavorMap
        // with a slightly modified Mime Type.
        //
        // As a list of String natives will be returned for each DataFlavor,
        // the method addUnencodedNativeForFlavor will be called for each
        // String native in the list.
        //
        // For verification, a seperate Hashtable (Map) will be maintained with changes.
        DataFlavor key;
        hashVerify = new Hashtable();

        for (Enumeration e = hashFlavors.keys() ; e.hasMoreElements() ;) {
            key = (DataFlavor)e.nextElement();

            java.util.List listNatives = flavorMap.getNativesForFlavor(key);
            Vector vectorNatives = new Vector(listNatives);

            // Construct a new DataFlavor from an existing DataFlavor's MimeType
            // Example:
            // Original MimeType: "text/plain; class=java.io.Reader; charset=Unicode"
            // Modified MimeType: "text/plain-TEST; class=java.io.Reader; charset=Unicode"

            StringBuffer mimeType = new StringBuffer(key.getMimeType());
            mimeType.insert(mimeType.indexOf(";"),"-TEST");

            DataFlavor testFlavor = new DataFlavor(mimeType.toString(), "Test DataFlavor");

            for (ListIterator i = vectorNatives.listIterator() ; i.hasNext() ;) {
                String element = (String)i.next();
                flavorMap.addUnencodedNativeForFlavor(testFlavor, element);
            }

            //Added following 4 lines - Aruna Samji - 07/22/2003
            Vector existingNatives = new Vector(flavorMap.getNativesForFlavor(testFlavor));
            existingNatives.addAll(vectorNatives);
            vectorNatives = existingNatives;
            hashVerify.put(testFlavor, vectorNatives);
        }

        // Assertions: After establishing "new" mappings, verify that the defined
        //             DataFlavors can be retrieved.
        verifyNewMappings();
    }

    // Verify getFlavorsForNative(String nat) is returning the correct list
    // of DataFlavors (for the new mappings).
    public boolean verifyNewMappings() {
        boolean result = true;

        // Enumerate through all DataFlavors
        for (Enumeration e = hashVerify.keys() ; e.hasMoreElements() ;) {
            DataFlavor key = (DataFlavor)e.nextElement();

            java.util.List listNatives = flavorMap.getNativesForFlavor(key);
            Vector vectorNatives = new Vector(listNatives);

            // Compare the list of Natives
            //Next line changed by Kanishk to comply with bug 4696522
            //if ( !vectorNatives.equals((Vector)hashVerify.get(key))) {
            if ( !(vectorNatives.containsAll((Vector)hashVerify.get(key)) && ((Vector)hashVerify.get(key)).containsAll(vectorNatives))) {
                throw new RuntimeException("\n*** Error in verifyNewMappings()" +
                    "\nmethod1: addUnencodedNativeForFlavor(DataFlavor flav, String nat)"  +
                    "\nmethod2: List getNativesForFlavor(DataFlavor flav)" +
                    "\nDataFlavor: " + key.getMimeType() +
                    "\nAfter adding several mappings with addUnencodedNativeForFlavor," +
                    "\nthe returned list did not match the mappings that were added." +
                    "\nThe mapping was not included in the list.");
            }
        }
        System.out.println("*** DataFlavor size = " + hashVerify.size());
        System.out.println("*** verifyNewMappings result: " + result + "\n");
        return result;
    }
}

