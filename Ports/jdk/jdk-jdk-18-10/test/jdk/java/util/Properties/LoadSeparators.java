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
 * @bug 4192678
 * @summary Test loading of values that are key value separators
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * This class tests to see if a properties object can successfully save and
 * load properties with a non-escaped value that is also a key value separator
 *
 */
public class LoadSeparators {
    public static void main(String[] argv) throws Exception {
        try {
            // Destroy old test file if any
            // Create a properties file
            File propFile = new File("testout");
            propFile.delete();

            // Create a properties file
            FileOutputStream myOut = new FileOutputStream(propFile);
            String testProperty = "Test3==";
            myOut.write(testProperty.getBytes());
            myOut.close();

            // Load the properties set
            FileInputStream myIn = new FileInputStream("testout");
            Properties myNewProps = new Properties();
            try {
                myNewProps.load(myIn);
            } finally {
                myIn.close();
            }

            // Read in the test property
            String equalSign = myNewProps.getProperty("Test3");

            // Clean up
            propFile.delete();

            if (!equalSign.equals("="))
                throw new Exception("Cannot read key-value separators.");
        } catch (IOException e) {
            System.err.println(e);
        }
    }
}
