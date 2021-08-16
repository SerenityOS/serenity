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
 * @bug 4218776
 * @summary Test loading of properties files with blank lines
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * This class tests to see if a properties object correctly handles blank
 * lines in a properties file
 */
public class BlankLines {
    public static void main(String []args)
    {
        try {
            // create test file
            File file = new File("test.properties");

            // write a single space to the test file
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(' ');
            fos.close();

            // test empty properties
            Properties prop1 = new Properties();

            // now load the file we just created, into a
            // properties object.
            // the properties object should have no elements,
            // but due to a bug, it has an empty key/value.
            // key = "", value = ""
            Properties prop2 = new Properties();
            InputStream is = new FileInputStream(file);
            try {
                prop2.load(is);
            } finally {
                is.close();
            }
            if (!prop1.equals(prop2))
                throw new RuntimeException("Incorrect properties loading.");

            // cleanup
            file.delete();
        }
        catch(IOException e) {}
    }
}
