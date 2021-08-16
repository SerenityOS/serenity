/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8200696
 * @summary Initial capacity of Class.enumConstantDirectory is not optimal
 * @library /test/lib
 * @modules java.base/java.lang:open
 *          java.base/java.util:open
 * @build jdk.test.lib.util.OptimalCapacity
 * @run main ConstantDirectoryOptimalCapacity
 */

import jdk.test.lib.util.OptimalCapacity;

public class ConstantDirectoryOptimalCapacity {

    public static void main(String[] args) throws Throwable {
        test(E1.class);
        test(E2.class);
        test(E3.class);
        test(E4.class);
        test(E5.class);
        test(E6.class);
        test(E7.class);
        test(E8.class);
        test(E9.class);
        test(E10.class);
        test(E11.class);
        test(E12.class);
        test(E13.class);
        test(E14.class);
        test(E15.class);
        test(E16.class);
        test(E17.class);
        test(E18.class);
        test(E19.class);
        test(E20.class);
        test(E21.class);
        test(E22.class);
        test(E23.class);
        test(E24.class);
        test(E25.class);
        test(E26.class);
    }

    private static void test(Class<? extends Enum> e) {
        Enum.valueOf(e, "V0"); // trigger init of enumConstantDirectory

        int initialCapacity = (int)(e.getEnumConstants().length / 0.75f) + 1;
        OptimalCapacity.ofHashMap(e.getClass(), e, "enumConstantDirectory",
            initialCapacity);
    }

    enum E1 { V0 }
    enum E2 { V0, V1 }
    enum E3 { V0, V1, V2 }
    enum E4 { V0, V1, V2, V3 }
    enum E5 { V0, V1, V2, V3, V4 }
    enum E6 { V0, V1, V2, V3, V4, V5 }
    enum E7 { V0, V1, V2, V3, V4, V5, V6 }
    enum E8 { V0, V1, V2, V3, V4, V5, V6, V7 }
    enum E9 { V0, V1, V2, V3, V4, V5, V6, V7, V8 }
    enum E10 { V0, V1, V2, V3, V4, V5, V6, V7, V8, V9 }
    enum E11 { V0, V1, V2, V3, V4, V5, V6, V7, V8, V9, V10 }
    enum E12 { V0, V1, V2, V3, V4, V5, V6, V7, V8, V9, V10, V11 }
    enum E13 { V0, V1, V2, V3, V4, V5, V6, V7, V8, V9, V10, V11, V12 }
    enum E14 { V0, V1, V2, V3, V4, V5, V6, V7, V8, V9, V10, V11, V12, V13 }
    enum E15 { V0, V1, V2, V3, V4, V5, V6, V7, V8, V9, V10, V11, V12, V13,
               V14 }
    enum E16 { V0, V1, V2, V3, V4, V5, V6, V7, V8, V9, V10, V11, V12, V13,
               V14, V15 }
    enum E17 { V0, V1, V2, V3, V4, V5, V6, V7, V8, V9, V10, V11, V12, V13,
               V14, V15, V16 }
    enum E18 { V0, V1, V2, V3, V4, V5, V6, V7, V8, V9, V10, V11, V12, V13,
               V14, V15, V16, V17 }
    enum E19 { V0, V1, V2, V3, V4, V5, V6, V7, V8, V9, V10, V11, V12, V13,
               V14, V15, V16, V17, V18 }
    enum E20 { V0, V1, V2, V3, V4, V5, V6, V7, V8, V9, V10, V11, V12, V13,
               V14, V15, V16, V17, V18, V19 }
    enum E21 { V0, V1, V2, V3, V4, V5, V6, V7, V8, V9, V10, V11, V12, V13,
               V14, V15, V16, V17, V18, V19, V20 }
    enum E22 { V0, V1, V2, V3, V4, V5, V6, V7, V8, V9, V10, V11, V12, V13,
               V14, V15, V16, V17, V18, V19, V20, V21 }
    enum E23 { V0, V1, V2, V3, V4, V5, V6, V7, V8, V9, V10, V11, V12, V13,
               V14, V15, V16, V17, V18, V19, V20, V21, V22 }
    enum E24 { V0, V1, V2, V3, V4, V5, V6, V7, V8, V9, V10, V11, V12, V13,
               V14, V15, V16, V17, V18, V19, V20, V21, V22, V23 }
    enum E25 { V0, V1, V2, V3, V4, V5, V6, V7, V8, V9, V10, V11, V12, V13,
               V14, V15, V16, V17, V18, V19, V20, V21, V22, V23, V24 }
    enum E26 { V0, V1, V2, V3, V4, V5, V6, V7, V8, V9, V10, V11, V12, V13,
               V14, V15, V16, V17, V18, V19, V20, V21, V22, V23, V24, V25 }
}
