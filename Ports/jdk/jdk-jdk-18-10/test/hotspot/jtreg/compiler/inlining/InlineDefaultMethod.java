/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8026735
 * @summary CHA in C1 should make correct decisions about default methods
 *
 * @run main/othervm -Xcomp -XX:TieredStopAtLevel=1
 *      -XX:CompileCommand=compileonly,compiler.inlining.InlineDefaultMethod::test
 *      compiler.inlining.InlineDefaultMethod
 */

package compiler.inlining;
public class InlineDefaultMethod {
    interface InterfaceWithDefaultMethod0 {
        default public int defaultMethod() {
            return 1;
        }
    }

    interface InterfaceWithDefaultMethod1 extends InterfaceWithDefaultMethod0 { }

    static abstract class Subtype implements InterfaceWithDefaultMethod1 { }

    static class Decoy extends Subtype {
        public int defaultMethod() {
            return 2;
        }
    }

    static class Instance extends Subtype { }

    public static int test(InterfaceWithDefaultMethod1 x) {
        return x.defaultMethod();
    }
    public static void main(String[] args) {
        InterfaceWithDefaultMethod1 a = new Decoy();
        InterfaceWithDefaultMethod1 b = new Instance();
        if (test(a) != 2 ||
            test(b) != 1) {
          System.err.println("FAILED");
          System.exit(97);
        }
        System.err.println("PASSED");
    }
}
