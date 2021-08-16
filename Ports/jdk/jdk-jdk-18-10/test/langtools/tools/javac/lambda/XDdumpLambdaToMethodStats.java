/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8143217
 * @summary javac throws NPE when printing diagnostics for Lambda expressions
 * @compile XDdumpLambdaToMethodStats.java --debug=dumpLambdaToMethodStats
 *
 */

/*
 * Note: No golden file since the crash is reproducible only when raw
 *       diagnostics are not in effect.
*/

public class XDdumpLambdaToMethodStats {
    public static void main(String... a) {
        new XDdumpLambdaToMethodStats().run();
    }

    public void run() {
        printHash(this::m);
        printHash(XDdumpLambdaToMethodStats::sm);
        printHash(() -> { o = new Object(); });
        printHash(() -> { s = new Object(); });
    }

    private void printHash(Runnable function) {
        System.out.println(function + "; hash=" + function.hashCode());
    }

    private static void sm() {
    }

    private void m() {
    }

    private Object o;
    private static Object s;
}
