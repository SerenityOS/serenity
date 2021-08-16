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
 * @bug 4034428
 * @summary Test for leading space in values output from properties
 */

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * This class tests to see if the properties object saves
 * leading space in the value of a property as it should
 * do according to the JLS
 */
public class Save {

    public static void main(String argv[]) {
        int testSucceeded=0;
        FileOutputStream myOutput;

        // create a properties object to save
        Properties myProperties = new Properties();
        String value = "   spacesfirst";
        myProperties.put("atest", value);

        try {
            // save the object and check output
            myOutput = new FileOutputStream("testout");
            myProperties.store(myOutput,"A test");
            myOutput.close();

            //load the properties set
            FileInputStream myIn = new FileInputStream("testout");
            Properties myNewProps = new Properties();
            try {
                myNewProps.load(myIn);
            } finally {
                myIn.close();
            }

            //check the results
            String newValue = (String) myNewProps.get("atest");
            if (!newValue.equals(value))
                throw new RuntimeException(
                    "Properties does not save leading spaces in values correctly.");
         } catch (IOException e) { //do nothing
         }
     }
}
