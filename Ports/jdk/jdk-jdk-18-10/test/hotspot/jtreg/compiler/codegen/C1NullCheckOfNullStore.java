/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8039043
 * @summary Null check is placed in a wrong place when storing a null to an object field on x64 with compressed oops off
 *
 * @run main/othervm -Xbatch -XX:+IgnoreUnrecognizedVMOptions
 *    -XX:+TieredCompilation -XX:TieredStopAtLevel=1 -XX:-UseCompressedOops
 *    -XX:CompileCommand=compileonly,compiler.codegen.C1NullCheckOfNullStore::test
 *    compiler.codegen.C1NullCheckOfNullStore
 */

package compiler.codegen;

public class C1NullCheckOfNullStore {
    private static class Foo {
        Object bar;
    }

    static private void test(Foo x) {
        x.bar = null;
    }

    static public void main(String args[]) {
        Foo x = new Foo();
        for (int i = 0; i < 10000; i++) {
            test(x);
        }
        boolean gotNPE = false;
        try {
            for (int i = 0; i < 10000; i++) {
                test(null);
            }
        } catch (NullPointerException e) {
            gotNPE = true;
        }
        if (!gotNPE) {
            throw new Error("Expecting a NullPointerException");
        }
    }
}
