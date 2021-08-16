/*
 * Copyright (c) 2002, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4329886
 * @summary Clone() on arrays compiled incorrectly
 * @author gafter jjg
 * @modules jdk.compiler
 *          jdk.jdeps/com.sun.tools.javap
 */

import java.io.*;

/** The qualifying type in the code for array.clone() should be the array type. */
public class ArrayClone {
    public static void main(String[] args) {
        new ArrayClone().run();
    }

    public void run() {
        String[] args = { "-classpath", System.getProperty("test.classes", "."), "-v", "Test" };
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        int rc = com.sun.tools.javap.Main.run(args, pw);
        if (rc != 0)
            throw new Error("javap failed; exit " + rc);

        String out = sw.toString();
        System.out.println(out);

        for (String line: out.split("(\\n|\\r\\n?)")) {
            String match = "[ \t]+[0-9]+:[ \t]+invokevirtual[ \t]+#[0-9]+[ \t]+// Method \"\\[Ljava/lang/String;\".clone:\\(\\)Ljava/lang/Object;";
            if (line.matches(match))
                return;
        }
        throw new Error("expected string not found in javap output");
    }
}

class Test {
    public static void main(String[] args) {
        args.clone();
    }
}
