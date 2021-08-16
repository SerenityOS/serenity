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
 * @bug 4115936 4385195 4972297
 * @summary checks for processing errors in properties.load
 */

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

public class LoadParsing {
    public static void main(String[] argv) throws Exception {
        File f = new File(System.getProperty("test.src", "."), "input.txt");
        FileInputStream myIn = new FileInputStream(f);
        Properties myProps = new Properties();
        int size = 0;
        try {
            myProps.load(myIn);
        } finally {
            myIn.close();
        }

        if (!myProps.getProperty("key1").equals("value1"))
            throw new RuntimeException("Bad comment parsing");
        if (!myProps.getProperty("key2").equals("abc\\defg\\"))
            throw new RuntimeException("Bad slash parsing");
        if (!myProps.getProperty("key3").equals("value3"))
            throw new RuntimeException("Adds spaces to key");
        if (!myProps.getProperty("key4").equals(":value4"))
            throw new RuntimeException("Bad separator parsing");
        if (myProps.getProperty("#") != null)
            throw new RuntimeException(
                "Does not recognize comments with leading spaces");
        if ((size=myProps.size()) != 4)
            throw new RuntimeException(
                 "Wrong number of keys in Properties; expected 4, got " +
                size + ".");
    }
}
