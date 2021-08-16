/*
 * Copyright (c) 2016, Red Hat, Inc. All rights reserved.
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
 * @bug 8166836
 * @summary Elimination of clone's ArrayCopyNode may make compilation fail silently
 * @run main/othervm -XX:-BackgroundCompilation -XX:-UseOnStackReplacement -XX:CompileCommand=dontinline,TestEliminatedCloneBadMemEdge::not_inlined TestEliminatedCloneBadMemEdge
 *
 */

public class TestEliminatedCloneBadMemEdge implements Cloneable {

    int f1;
    int f2;
    int f3;
    int f4;
    int f5;
    int f6;
    int f7;
    int f8;
    int f9;

    static void not_inlined() {}

    static void test(TestEliminatedCloneBadMemEdge o1) throws CloneNotSupportedException {
        TestEliminatedCloneBadMemEdge o2 = (TestEliminatedCloneBadMemEdge)o1.clone();
        not_inlined();
        o2.f1 = 0x42;
    }

    static public void main(String[] args) throws CloneNotSupportedException {
        TestEliminatedCloneBadMemEdge o1 = new TestEliminatedCloneBadMemEdge();
        for (int i = 0; i < 20000; i++) {
            test(o1);
        }
    }
}
