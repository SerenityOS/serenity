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
 * @bug     8015272
 * @summary \@Contended within the same group to use the same oop map
 *
 * @modules java.base/jdk.internal.vm.annotation
 * @run main/othervm -XX:-RestrictContended -XX:ContendedPaddingWidth=128 -Xmx128m OopMapsSameGroup
 */
public class OopMapsSameGroup {

    public static final int COUNT = 10000;

    public static void main(String[] args) throws Exception {
        Object o01 = new Object();
        Object o02 = new Object();
        Object o03 = new Object();
        Object o04 = new Object();

        R[] rs = new R[COUNT];

        for (int i = 0; i < COUNT; i++) {
           R r = new R();
           r.o01 = o01;
           r.o02 = o02;
           r.o03 = o03;
           r.o04 = o04;
           rs[i] = r;
        }

        System.gc();

        for (int i = 0; i < COUNT; i++) {
           R r = rs[i];
           if (r.o01 != o01) throw new Error("Test Error: o01");
           if (r.o02 != o02) throw new Error("Test Error: o02");
           if (r.o03 != o03) throw new Error("Test Error: o03");
           if (r.o04 != o04) throw new Error("Test Error: o04");
        }
    }

    public static class R {
        @Contended("group1")
        Object o01;

        @Contended("group1")
        Object o02;

        @Contended("group2")
        Object o03;

        @Contended("group2")
        Object o04;
    }

}

