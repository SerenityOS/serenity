/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6609431
 * @summary Test whether loading of a property value in a file ending with
 *    a backslash works fine.
 */

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

public class Bug6609431 {
    private static final String expected = "backslash";

    public static void main(String[] args) throws IOException {
        try (FileReader fr =
                new FileReader(new File(System.getProperty("test.src", "."),
                                        "Bug6609431.properties"))) {
            Properties p = new Properties();
            p.load(fr);
            p.getProperty("a");
            String val = p.getProperty("b");
            if (!val.equals(expected)) {
                throw new RuntimeException("Value returned from the property" +
                " list was incorrect. Returned: '" + val +
                "', expected: '" + expected + "'");
            }
        }
    }
}
