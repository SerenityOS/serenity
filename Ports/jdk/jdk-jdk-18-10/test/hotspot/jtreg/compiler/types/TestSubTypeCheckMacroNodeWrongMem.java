/*
 * Copyright (c) 2020, Red Hat, Inc. All rights reserved.
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
 * @bug 8239367
 * @summary Wiring of memory in SubTypeCheck macro node causes graph should be schedulable
 *
 * @run main/othervm -Xcomp -XX:CompileOnly=TestSubTypeCheckMacroNodeWrongMem::test -XX:+IgnoreUnrecognizedVMOptions -XX:-DoEscapeAnalysis TestSubTypeCheckMacroNodeWrongMem
 *
 */

public class TestSubTypeCheckMacroNodeWrongMem {
    private static int stop = 100;

    public static void main(String[] args) {
        TestSubTypeCheckMacroNodeWrongMem o = new TestSubTypeCheckMacroNodeWrongMem();
        test();
    }

    private static void test() {
        Object o1 = null;
        for (int i = 0; i < stop; i++) {
            try {
                Object o = new TestSubTypeCheckMacroNodeWrongMem();
                o1.equals(o);
            } catch (NullPointerException npe) {
            } catch (Exception e) {
                throw new RuntimeException();
            }
        }
    }
}
