/*
 * Copyright (c) 2008, 2020, Oracle and/or its affiliates. All rights reserved.
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
 *
 * @summary converted from VM Testbase jit/t/t087.
 * VM Testbase keywords: [jit, quick, jdk, javac]
 *
 * @library /vmTestbase
 *          /test/lib
 * @build ExecDriver
 *
 * @comment build all dependencies
 * @build jit.t.t087.t087
 *
 * @comment make sure foo.class is only in the current directory
 * @clean jit.t.t087.foo
 * @run driver ExecDriver --cmd
 *      ${compile.jdk}/bin/javac
 *      -d .
 *      -cp ${test.class.path}
 *      ${test.src}/t087.java
 *
 * @comment run the test
 * @run driver ExecDriver --java
 *      -cp .${path.separator}${test.class.path}
 *      -Dtest.src=${test.src}
 *      jit.t.t087.t087
 *      -WorkDir ./jit/t/t087
 */

package jit.t.t087;

import nsk.share.GoldChecker;

import java.io.File;

class foo {
    static void bar() {
        t087.goldChecker.println("You shouldn't see this.");
    }
}

class t087 {
    public static final GoldChecker goldChecker = new GoldChecker("t087");

    public static void main(String[] argv) {
        File f;
        if (argv.length < 2 || !argv[0].equals("-WorkDir")) {
            f = new File(".", "foo.class");
        } else {
            f = new File(argv[1], "foo.class");
        }
        if (f.isFile()) {
            f.delete();

            for (int i = 1; i <= 2; i += 1) {
                try {
                    foo.bar();
                } catch (Throwable t) {
                    t087.goldChecker.println("Exception on try" + i);
                }
            }

        } else {
            t087.goldChecker.println("No foo.class in cwd");
        }
        t087.goldChecker.check();
    }
}

