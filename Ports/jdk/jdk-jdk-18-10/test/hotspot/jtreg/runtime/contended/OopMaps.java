/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.Class;
import java.lang.String;
import java.lang.System;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import jdk.internal.vm.annotation.Contended;

/*
 * @test
 * @bug     8015270
 * @bug     8015493
 * @summary \@Contended: fix multiple issues in the layout code
 *
 * @modules java.base/jdk.internal.vm.annotation
 * @run main/othervm -XX:-RestrictContended -XX:ContendedPaddingWidth=128 -Xmx128m OopMaps
 */
public class OopMaps {

    public static final int COUNT = 10000;

    public static void main(String[] args) throws Exception {
        Object o01 = new Object();
        Object o02 = new Object();
        Object o03 = new Object();
        Object o04 = new Object();
        Object o05 = new Object();
        Object o06 = new Object();
        Object o07 = new Object();
        Object o08 = new Object();
        Object o09 = new Object();
        Object o10 = new Object();
        Object o11 = new Object();
        Object o12 = new Object();
        Object o13 = new Object();
        Object o14 = new Object();

        R1[] rs = new R1[COUNT];

        for (int i = 0; i < COUNT; i++) {
           R1 r1 = new R1();
           r1.o01 = o01;
           r1.o02 = o02;
           r1.o03 = o03;
           r1.o04 = o04;
           r1.o05 = o05;
           r1.o06 = o06;
           r1.o07 = o07;
           r1.o08 = o08;
           r1.o09 = o09;
           r1.o10 = o10;
           r1.o11 = o11;
           r1.o12 = o12;
           r1.o13 = o13;
           r1.o14 = o14;
           r1.i1 = 1;
           r1.i2 = 2;
           r1.i3 = 3;
           r1.i4 = 4;
           rs[i] = r1;
        }

        System.gc();

        for (int i = 0; i < COUNT; i++) {
           R1 r1 = rs[i];
           if (r1.o01 != o01) throw new Error("Test Error: o01");
           if (r1.o02 != o02) throw new Error("Test Error: o02");
           if (r1.o03 != o03) throw new Error("Test Error: o03");
           if (r1.o04 != o04) throw new Error("Test Error: o04");
           if (r1.o05 != o05) throw new Error("Test Error: o05");
           if (r1.o06 != o06) throw new Error("Test Error: o06");
           if (r1.o07 != o07) throw new Error("Test Error: o07");
           if (r1.o08 != o08) throw new Error("Test Error: o08");
           if (r1.o09 != o09) throw new Error("Test Error: o09");
           if (r1.o10 != o10) throw new Error("Test Error: o10");
           if (r1.o11 != o11) throw new Error("Test Error: o11");
           if (r1.o12 != o12) throw new Error("Test Error: o12");
           if (r1.o13 != o13) throw new Error("Test Error: o13");
           if (r1.o14 != o14) throw new Error("Test Error: o14");
           if (r1.i1 != 1)    throw new Error("Test Error: i1");
           if (r1.i2 != 2)    throw new Error("Test Error: i2");
           if (r1.i3 != 3)    throw new Error("Test Error: i3");
           if (r1.i4 != 4)    throw new Error("Test Error: i4");
        }
    }

    public static class R0 {
        int i1;
        int i2;

        Object o01;
        Object o02;

        @Contended
        Object o03;

        @Contended
        Object o04;

        @Contended
        Object o05;

        @Contended
        Object o06;

        @Contended
        Object o07;
   }

   public static class R1 extends R0 {
        int i3;
        int i4;

        Object o08;
        Object o09;

        @Contended
        Object o10;

        @Contended
        Object o11;

        @Contended
        Object o12;

        @Contended
        Object o13;

        @Contended
        Object o14;
   }

}

