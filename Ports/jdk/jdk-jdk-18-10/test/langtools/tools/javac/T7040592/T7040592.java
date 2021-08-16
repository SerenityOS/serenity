/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7040592
 * @summary Verify that null can be assigned freely to array types without a checkcast
 * @modules jdk.compiler
 *          jdk.jdeps/com.sun.tools.javap
 */

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Paths;

public class T7040592 {

    private static final String assertionErrorMsg =
            "null should be assignable to array type without a checkcast";

    public static void main(String[] args) {
        new T7040592().run();
    }

    void run() {
        check("-c", Paths.get(System.getProperty("test.classes"),
                "T7040592_01.class").toString());
    }

    void check(String... params) {
        StringWriter s;
        String out;
        try (PrintWriter pw = new PrintWriter(s = new StringWriter())) {
            com.sun.tools.javap.Main.run(params, pw);
            out = s.toString();
        }
        if (out.contains("checkcast")) {
            throw new AssertionError(assertionErrorMsg);
        }
    }

}

class T7040592_01 {
    static void handleArrays(Object [] a, Object [][] b, Object [][][] c) {
    }
    public static void main(String[] args) {
        Object a[];
        Object o = (a = null)[0];
        Object b[][];
        o = (b = null)[0][0];
        Object c[][][];
        o = (c = null)[0][0][0];
        handleArrays(null, null, null);
    }
}
