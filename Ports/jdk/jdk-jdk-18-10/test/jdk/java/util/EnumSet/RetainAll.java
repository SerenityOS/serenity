/*
 * Copyright (c) 2006, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6377356
 * @summary Test EnumSet.retainAll
 * @author Josh Bloch, Martin Buchholz
 */

import java.util.*;

public class RetainAll {
    enum RegularA { A0, A2 }
    enum RegularB { B0, B1 }
    enum JumboA {
        A00, A01, A02, A03, A04, A05, A06, A07, A08, A09,
        A10, A11, A12, A13, A14, A15, A16, A17, A18, A19,
        A20, A21, A22, A23, A24, A25, A26, A27, A28, A29,
        A30, A31, A32, A33, A34, A35, A36, A37, A38, A39,
        A40, A41, A42, A43, A44, A45, A46, A47, A48, A49,
        A50, A51, A52, A53, A54, A55, A56, A57, A58, A59,
        A60, A61, A62, A63, A64, A65, A66, A67, A68, A69,
    }
    enum JumboB {
        B00, B01, B02, B03, B04, B05, B06, B07, B08, B09,
        B10, B11, B12, B13, B14, B15, B16, B17, B18, B19,
        B20, B21, B22, B23, B24, B25, B26, B27, B28, B29,
        B30, B31, B32, B33, B34, B35, B36, B37, B38, B39,
        B40, B41, B42, B43, B44, B45, B46, B47, B48, B49,
        B50, B51, B52, B53, B54, B55, B56, B57, B58, B59,
        B60, B61, B62, B63, B64, B65, B66, B67, B68, B69,
    }

    private static void realMain(String[] args) throws Throwable {
        Set<RegularA> ras = EnumSet.noneOf(RegularA.class);
        Set<RegularB> rbs = EnumSet.noneOf(RegularB.class);
        Set<JumboA>   jas = EnumSet.noneOf(JumboA.class);
        Set<JumboB>   jbs = EnumSet.noneOf(JumboB.class);
        check(ras.getClass().getName().matches(".*Regular.*"));
        check(jas.getClass().getName().matches(".*Jumbo.*"));
        check(! ras.retainAll(ras));
        check(! ras.retainAll(rbs));
        check(! jas.retainAll(jas));
        check(! jas.retainAll(jbs));
        check(! ras.retainAll(jas));
        check(! jas.retainAll(ras));
    }

    //--------------------- Infrastructure ---------------------------
    static volatile int passed = 0, failed = 0;
    static void pass() {passed++;}
    static void fail() {failed++; Thread.dumpStack();}
    static void fail(String msg) {System.out.println(msg); fail();}
    static void unexpected(Throwable t) {failed++; t.printStackTrace();}
    static void check(boolean cond) {if (cond) pass(); else fail();}
    static void equal(Object x, Object y) {
        if (x == null ? y == null : x.equals(y)) pass();
        else fail(x + " not equal to " + y);}
    public static void main(String[] args) throws Throwable {
        try {realMain(args);} catch (Throwable t) {unexpected(t);}
        System.out.printf("%nPassed = %d, failed = %d%n%n", passed, failed);
        if (failed > 0) throw new AssertionError("Some tests failed");}
}
