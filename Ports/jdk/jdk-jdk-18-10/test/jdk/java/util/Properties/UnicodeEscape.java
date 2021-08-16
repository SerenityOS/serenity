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
 * @bug 4792682
 * @summary Test for correct exception with a short unicode escape
*/

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.util.Properties;

public class UnicodeEscape {

    public static void main(String argv[]) throws Exception {
        save();
        load();
    }

    private static void save() throws Exception {
        FileWriter out = new FileWriter("a.properties");
        out.write("a=b\nb=\\u0\n");
        out.close();
    }

    private static void load() throws Exception {
        Properties properties = new Properties();
        InputStream in = new FileInputStream("a.properties");
        try {
            properties.load(in);
        } catch (IllegalArgumentException iae) {
            // Correct result
        } finally {
            in.close();
        }
    }
}
