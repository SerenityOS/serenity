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
   @bug 4140693
   @summary Make sure that java.io.File.list*(null) returns an array, not null
 */

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;


public class ListNull {

    static void go(String what, Object[] fs) throws Exception {
        if (fs == null)
            throw new Exception(what + " returned null");
        System.err.println("-- " + what);
        for (int i = 0; i < fs.length; i++)
            System.err.println(fs[i]);
    }

    public static void main(String[] args) throws Exception {
        File d = new File(".");
        go("list()", d.list());
        go("listFiles()", d.listFiles());
        go("list(null)", d.list(null));
        go("listFiles((FilenameFilter)null)", d.listFiles((FilenameFilter)null));
        go("listFiles((FileFilter)null)", d.listFiles((FileFilter)null));
    }

}
