/*
 * Copyright (c) 2004, 2012, Oracle and/or its affiliates. All rights reserved.
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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import static java.nio.file.Files.*;
import static java.nio.file.StandardOpenOption.*;

public class DefaultLocaleTest {

    static final String setting =
            "language:"   + System.getProperty("user.language") + "_" +
            "country:"    + System.getProperty("user.country")  + "_" +
            "encoding:"   + System.getProperty("file.encoding") + "_" +
            "jnuEncoding:"+ System.getProperty("sun.jnu.encoding");

    public static void main(String[] args) throws IOException {
        if (args != null && args.length > 1) {
            File f = new File(args[1]);
            switch (args[0]) {
                case "-r":
                    System.out.println("reading file: " + args[1]);
                    String str = null;
                    try (BufferedReader in = newBufferedReader(f.toPath(),
                                    Charset.defaultCharset())) {
                        str = in.readLine().trim();
                    }
                    if (setting.equals(str)) {
                        System.out.println("Compared ok");
                    } else {
                        System.out.println("Compare fails");
                        System.out.println("EXPECTED: " + setting);
                        System.out.println("OBTAINED: " + str);
                        throw new RuntimeException("Test fails: compare failed");
                    }
                    break;
                case "-w":
                    System.out.println("writing file: " + args[1]);
                    try (BufferedWriter out = newBufferedWriter(f.toPath(),
                                    Charset.defaultCharset(), CREATE_NEW)) {
                        out.write(setting);
                    }
                    break;
                default:
                    throw new RuntimeException("ERROR: invalid arguments");
            }
        } else {
            throw new RuntimeException("ERROR: invalid arguments");
        }
    }
}
