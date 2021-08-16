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
 * @bug 4077980 4011163 4096786 4075955
 * @summary Test properties save and load methods
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
 * can successfully save and load properties
 * using character encoding
 */
public class SaveLoadBasher {

    private static String keyValueSeparators = "=: \t\r\n\f#!\\";

    public static void main(String[] args) throws Exception {

        Properties originalProps = new Properties();
        Properties loadedProps = new Properties();

        // Generate a unicode key and value
        Random generator = new Random();
        int achar=0;
        StringBuffer aKeyBuffer = new StringBuffer(120);
        StringBuffer aValueBuffer = new StringBuffer(120);
        String aKey;
        String aValue;
        for (int x=0; x<300; x++) {
            for(int y=0; y<100; y++) {
                achar = generator.nextInt();
                char test;
                if(achar < 99999) {
                    test = (char)(achar);
                }
                else {
                    int zz = achar % 10;
                    test = keyValueSeparators.charAt(zz);
                }
                aKeyBuffer.append(test);
            }
            aKey = aKeyBuffer.toString();
            for(int y=0; y<100; y++) {
                achar = generator.nextInt();
                char test = (char)(achar);
                aValueBuffer.append(test);
            }
            aValue = aValueBuffer.toString();

            // Attempt to add to original
            try {
                originalProps.put(aKey, aValue);
            }
            catch (IllegalArgumentException e) {
                System.err.println("disallowing...");
            }
            aKeyBuffer.setLength(0);
            aValueBuffer.setLength(0);
        }

        // Destroy old test file if it exists
        File oldTestFile = new File("props3");
        oldTestFile.delete();

        // Save original
        System.out.println("Saving...");
        OutputStream out = new FileOutputStream("props3");
        originalProps.store(out, "test properties");
        out.close();

        // Load in the set
        System.out.println("Loading...");
        InputStream in = new FileInputStream("props3");
        try {
            loadedProps.load(in);
        } finally {
            in.close();
        }

        // Compare results
        if (!originalProps.equals(loadedProps))
           throw new RuntimeException("Properties load and save failed");

    }

}
