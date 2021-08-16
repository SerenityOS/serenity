/*
 * Copyright (c) 1998, Oracle and/or its affiliates. All rights reserved.
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
   @bug 4166902
   @summary Ensure that File.toURL does not append a slash to root directories
 */

import java.io.File;
import java.net.URL;


public class ToURL {

    static void go(String fn) throws Exception {
        File f = new File(fn);
        URL u = f.toURL();
        String ufn = u.getFile();
        if (!ufn.endsWith("/"))
            throw new Exception(u + " does not end with slash");
        if (ufn.endsWith("//"))
            throw new Exception(u + " ends with two slashes");
    }

    public static void main(String[] args) throws Exception {
        if (File.separatorChar == '/') {
            go("/");
        } else if (File.separatorChar == '\\') {
            go("\\");
            go("c:\\");
        }
    }

}
