/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @test
 * @bug 4749531 5015114 5055738
 * @summary Test properties XML save and load methods
 * @key randomness
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;
import java.util.Random;

/**
 * This class tests to see if a properties object
 * can successfully save and load properties in XML
 */
public class XMLSaveLoadBasher {

    private static final int MAX_KEY_SIZE = 120;
    private static final int MIN_KEY_SIZE = 1;
    private static final int MAX_VALUE_SIZE = 120;
    private static final int MIN_VALUE_SIZE = 0;

    public static void main(String[] args) throws Exception {
        testSaveLoad("UTF-8", "test save");
        testSaveLoad("UTF-8", null);
        testSaveLoad("ISO-8859-1", "test save");
        testSaveLoad("KOI8-R", "test save");
    }

    private static void testSaveLoad(String encoding, String comment)
        throws Exception
    {
        Properties originalProps = new Properties();
        Properties loadedProps = new Properties();

        // Generate a unicode key and value
        Random generator = new Random();

        for (int x=0; x<10; x++) {
            String aKey;
            String aValue;

            // Assumes MAX_KEY_SIZE >> MIN_KEY_SIZE
            int keyLen = generator.nextInt(MAX_KEY_SIZE - MIN_KEY_SIZE + 1) +
                         MIN_KEY_SIZE;
            int valLen = generator.nextInt(
                MAX_VALUE_SIZE - MIN_VALUE_SIZE + 1) + MIN_VALUE_SIZE;

            StringBuffer aKeyBuffer = new StringBuffer(keyLen);
            StringBuffer aValueBuffer = new StringBuffer(valLen);

            for(int y=0; y<keyLen; y++) {
                char test = (char)(generator.nextInt(6527) + 32);
                aKeyBuffer.append(test);
            }
            aKey = aKeyBuffer.toString();

            for(int y=0; y<valLen; y++) {
                char test = (char)(generator.nextInt(6527) + 32);
                aValueBuffer.append(test);
            }
            aValue = aValueBuffer.toString();

            // Attempt to add to original
            try {
                originalProps.setProperty(aKey, aValue);
            } catch (IllegalArgumentException e) {
                System.err.println("disallowing...");
            }
        }

        //originalProps.put("squid", "kraken");
        //originalProps.put("demon", "furnace");

        // Destroy old test file if it exists
        File oldTestFile = new File("props3");
        oldTestFile.delete();

        // Save original
        System.err.println("Saving...");
        try (OutputStream out = new FileOutputStream("props3")) {
            originalProps.storeToXML(out, comment, encoding);
        }

        // Load in the set
        System.err.println("Loading...");
        try (InputStream in = new FileInputStream("props3")) {
            loadedProps.loadFromXML(in);
        }

        // Compare results
        if (!originalProps.equals(loadedProps))
           throw new RuntimeException("Properties load and save failed");

    }
}
