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
import java.nio.charset.Charset;

/*
 * @test
 * @summary To test SystemFlavorMap method:
 *          addFlavorForUnencodedNative(String nat, DataFlavor flav)
 *          with valid natives and DataFlavors. This stress test will
 *          define numerous mappings of valid String natives and
 *          DataFlavors.  The mappings will be verified by examining
 *          that all entries are present, and order is maintained.
 * @author Rick Reynaga (rick.reynaga@eng.sun.com) area=Clipboard
 * @author dmitriy.ermashov@oracle.com
 * @modules java.datatransfer
 * @run main AddFlavorTest
 */

public class AddFlavorTest {

    SystemFlavorMap flavorMap;
    Hashtable<String, List<DataFlavor>> hashVerify;

    public static void main (String[] args) throws Exception {
        new AddFlavorTest().doTest();
    }

    void doTest() throws Exception {
        flavorMap = (SystemFlavorMap)SystemFlavorMap.getDefaultFlavorMap();

        // Test addFlavorForUnencodedNative(String nat, DataFlavor flav);
        //
        // Enumerate through all the system defined String natives,
        // and for each String native, define it again to the SystemFlavorMap
        // with a slightly modified String native (name).
        //
        // As a list of DataFlavors will be returned for each String native,
        // the method addFlavorForUnencodedNative will be called for each
        // DataFlavor in the list.
        hashVerify = new Hashtable();

        for (String key : flavorMap.getFlavorsForNatives(null).keySet()) {
            Set<DataFlavor> flavorsSet = new HashSet<>(flavorMap.getFlavorsForNative(key));
            // construct a unique String native
            key = key.concat("TEST");

            for (DataFlavor element : flavorsSet) {
                flavorMap.addFlavorForUnencodedNative(key, element);
            }
            hashVerify.put(key, new Vector(flavorsSet));
        }

        // Assertions: After establishing "new" mappings, verify that the defined
        //             DataFlavors can be retrieved and that the List is preserved.
        verifyNewMappings();
    }

    // Verify getFlavorsForNative(String nat) is returning the correct list
    // of DataFlavors (for the new mappings).
    void verifyNewMappings() {
        // Enumerate through all natives
        System.out.println("*** native size = " + hashVerify.size());
        for (Enumeration e = hashVerify.keys() ; e.hasMoreElements() ;) {
            String key = (String)e.nextElement();
            compareFlavors(hashVerify.get(key), flavorMap.getFlavorsForNative(key), key);
            compareFlavors(flavorMap.getFlavorsForNative(key), hashVerify.get(key), key);
        }
    }

    void compareFlavors(List<DataFlavor> flavors1, List<DataFlavor> flavors2, String key){
        for (DataFlavor flavor1 : flavors1) {
            boolean result = false;
            for (DataFlavor flavor2 : flavors2) {
                if (flavor1.equals(flavor2)) result = true;
            }
            if (!result)
                throw new RuntimeException("\n*** Error in verifyNewMappings()" +
                        "\nmethod1: addFlavorForUnencodedNative(String nat, DataFlavor flav)"  +
                        "\nmethod2: List getFlavorsForNative(String nat)" +
                        "\nString native: " + key +
                        "\nAfter adding several mappings with addFlavorForUnencodedNative," +
                        "\nthe returned list did not match the mappings that were added." +
                        "\nEither the mapping was not included in the list, or the order was incorect.");
        }

    }

    Set<DataFlavor> convertMimeTypeToDataFlavors(String baseType) throws Exception {
        Set<DataFlavor> result = new LinkedHashSet<>();

        for (String charset : getStandardEncodings()) {
            for (String txtClass : new String[]{"java.io.InputStream", "java.nio.ByteBuffer", "\"[B\""}) {
                String mimeType = baseType + ";charset=" + charset + ";class=" + txtClass;

                if ("text/html".equals(baseType)) {
                    for (String documentType : new String[]{"all", "selection", "fragment"})
                        result.add(new DataFlavor(mimeType + ";document=" + documentType));
                } else {
                    DataFlavor df = new DataFlavor(mimeType);
                    if (df.equals(DataFlavor.plainTextFlavor))
                        df = DataFlavor.plainTextFlavor;
                    result.add(df);
                }
            }
        }
        return result;
    }

    Set<String> getStandardEncodings() {
        Set<String> tempSet = new HashSet<>();
        tempSet.add("US-ASCII");
        tempSet.add("ISO-8859-1");
        tempSet.add("UTF-8");
        tempSet.add("UTF-16BE");
        tempSet.add("UTF-16LE");
        tempSet.add("UTF-16");
        tempSet.add(Charset.defaultCharset().name());
        return tempSet;
    }
}

