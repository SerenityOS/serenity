/*
 * Copyright (c) 1998, 2017, Oracle and/or its affiliates. All rights reserved.
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

/* @test
   @bug 4152178 8152272
   @summary Check various temp-file prefix/suffix cases */

import java.io.File;
import java.io.IOException;

public class Patterns {

    static File dir = new File(".");

    static void ckn(String prefix, String suffix) throws Exception {
        try {
            File f = File.createTempFile(prefix, suffix, dir);
            f.deleteOnExit();
        } catch (Exception x) {
            if ((x instanceof IOException)
                || (x instanceof NullPointerException)
                || (x instanceof IllegalArgumentException)) {
                System.err.println("\"" + prefix + "\", \"" + suffix
                                   + "\" failed as expected: " + x.getMessage());
                return;
            }
            throw x;
        }
        throw new Exception("\"" + prefix + "\", \"" + suffix
                            + "\" should have failed");
    }

    static void cky(String prefix, String suffix) throws Exception {
        File f = File.createTempFile(prefix, suffix, dir);
        f.deleteOnExit();
        System.err.println("\"" + prefix + "\", \"" + suffix
                           + "\" --> " + f.getPath());
    }

    public static void main(String[] args) throws Exception {
        ckn(null, null);
        ckn("", null);
        ckn("x", null);
        ckn("xx", null);
        cky("xxx", null);
        cky("xxx", "");
        cky("xxx", "y");
        cky("xxx", ".y");
        cky("xyz", "Directory" + System.getProperty("file.separator"));
    }

}
