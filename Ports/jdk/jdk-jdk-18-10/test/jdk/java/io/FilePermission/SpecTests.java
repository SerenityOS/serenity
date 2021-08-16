/*
 * Copyright (c) 2005, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4955804 8230407
 * @summary Tests for FilePermission constructor spec for null,
 *      empty and misformated String parameters
 */

import java.io.*;

public class SpecTests {

    public static void main(String args[]) throws Exception {
        String ILE = "java.lang.IllegalArgumentException";
        String NPE = "java.lang.NullPointerException";

        String names[] =   {"", null, "foo", "foo", "foo", "foo", "foo"};
        String actions[] = {"read", "read", "", null, "junk",
                            "read,write,execute,delete,rename",
                            ",read"};
        String exps[] = { null, NPE, ILE, ILE, ILE, ILE, ILE };

        FilePermission permit;
        for (int i = 0; i < names.length; i++) {
            try {
                permit = new FilePermission(names[i], actions[i]);
            } catch (Exception e) {
                if (exps[i] == null) {
                    throw e;
                } else if (!((e.getClass().getName()).equals(exps[i]))) {
                    throw new Exception("Expecting: " + exps[i] +
                                        " for name:" + names[i] +
                                        " actions:" + actions[i]);
                } else {
                    System.out.println(names[i] + ", [" + actions[i] + "] " +
                            "resulted in " + exps[i] + " as Expected");
                    continue;
                }
            }
            if (exps[i] == null) {
                System.out.println(names[i] + ", [" + actions[i] + "] " +
                        "resulted in No Exception as Expected");
            } else {
                throw new Exception("Expecting: " + exps[i] +
                                    " for name:" + names[i] +
                                    " actions:" + actions[i]);
            }
        }
    }
}
