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
  * @bug 4026910 4011163 4077980 4096786 4213537
  * @summary Test for saving and loading encoded keys and values
  */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;

/**
 * This class tests to see if the properties object saves
 * and loads keys and values properly
 */
public class SaveEncoding {

    public static void main(String argv[]) {
        int testSucceeded=0;
        FileOutputStream myOutput;

        // Create a properties object to save
        Properties myProperties = new Properties();
        myProperties.put("signal", "val\u0019");
        myProperties.put("ABC 10", "value0");
        myProperties.put("\uff10test", "value\u0020");
        myProperties.put("key with spaces", "value with spaces");
        myProperties.put(" special#=key ", "value3");

        try {
            // Destroy old test file if any
            File myFile = new File("testout");
            myFile.delete();

            // Save the object and check output
            myOutput = new FileOutputStream("testout");
            myProperties.store(myOutput,"A test");
            myOutput.close();

            // Read properties file and verify \u0019
            FileInputStream inFile = new FileInputStream("testout");
            BufferedReader in = new BufferedReader(
                                new InputStreamReader(inFile));
            String firstLine = "foo";
            while (!firstLine.startsWith("signal"))
                firstLine = in.readLine();
            inFile.close();
            if (firstLine.length() != 16)
                throw new RuntimeException(
                    "Incorrect storage of values < 32.");

            // Load the properties set
            FileInputStream myIn = new FileInputStream("testout");
            Properties myNewProps = new Properties();
            try {
                myNewProps.load(myIn);
            } finally {
                myIn.close();
            }

            // Check the results
            if (!myNewProps.equals(myProperties))
                throw new RuntimeException(
                    "Properties is not character encoding safe.");
        } catch (IOException e) { // Do nothing
        }
    }
}
