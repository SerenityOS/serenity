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
 * @summary converted from VM Testbase jit/t/t113.
 * VM Testbase keywords: [jit, quick, vm6]
 * VM Testbase readme:
 * Clone of t097.  The pass file changed in JDK 1.2.
 *
 * @library /vmTestbase
 *          /test/lib
 * @build jit.t.t113.t113
 * @comment ExecDriver is used so golden file won't depend on jtreg
 * @run driver ExecDriver --java
 *      -Dtest.src=${test.src}
 *      jit.t.t113.t113
 */

package jit.t.t113;

import nsk.share.GoldChecker;

import java.io.PrintWriter;
import java.io.StringWriter;

// THIS TEST IS LINE NUMBER SENSITIVE

// Exception thrown by checkcast.

class parent {
}

class kid1 extends parent {
    int i;
}

class kid2 extends parent {
    int j;
}

public class t113 {
    public static final GoldChecker goldChecker = new GoldChecker("t113");

    public static void main(String[] argv) throws Throwable {
        parent p;
        kid2 k2;

        p = new kid1();
        try {
            k2 = (kid2) p;
        } catch (Throwable t) {
            StringWriter sr = new StringWriter();
            t.printStackTrace(new PrintWriter(sr));
            // Relaxing the gold checking to allow jigsaw module names
            // appear in ClassCastException message
            t113.goldChecker.print(sr.toString().replace("\t", "        ")
                    .replaceAll(" \\(in module: [ \\w\\.]*\\)", ""));
        }

        t113.goldChecker.check();
    }
}
