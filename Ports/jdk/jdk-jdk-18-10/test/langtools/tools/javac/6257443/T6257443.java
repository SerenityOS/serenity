/*
 * Copyright (c) 2005, 2016, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 6257443 6350124 6357979
 * @summary compiler can produce a .class file in some source output modes
 *
 * @compile package-info.java
 * @run main/othervm T6257443 -yes foo/package-info.class
 *
 * @clean foo.package-info
 *
 * @compile -printsource package-info.java
 * @run main/othervm T6257443 -no foo/package-info.class
 */

import java.net.URL;

public class T6257443
{
    public static void main(String[] args) {
        if (args.length != 2)
            throw new Error("wrong number of args");

        String state = args[0];
        String file = args[1];

        if (state.equals("-no")) {
            URL u = find(file);
            if (u != null)
                throw new Error("file " + file + " found unexpectedly");
        }
        else if (state.equals("-yes")) {
            URL u = find(file);
            if (u == null)
                throw new Error("file " + file + " not found");
        }
        else
            throw new Error("bad args");
    }

    public static URL find(String path) {
        return T6257443.class.getClassLoader().getSystemResource(path);
    }
}
