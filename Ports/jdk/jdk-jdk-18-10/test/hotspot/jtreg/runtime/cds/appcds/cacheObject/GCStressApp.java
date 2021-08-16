/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

import java.io.*;
import java.util.*;
import jdk.test.lib.Utils;
import sun.hotspot.WhiteBox;

// All strings in archived classes are shared
public class GCStressApp {
    static WhiteBox wb = WhiteBox.getWhiteBox();
    static int[] arr;

    static String get_shared_string() {
        String shared_str = "GCStressApp_shared_string";
        return shared_str;
    }

    static String get_shared_string1() {
        String shared_str1 = "GCStressApp_shared_string1";
        return shared_str1;
    }

    static void allocAlot() {
        try {
            Random random = Utils.getRandomInstance();
            for (int i = 0; i < 1024 * 1024; i++) {
                int len = random.nextInt(10000);
                arr = new int[len];
            }
        } catch (java.lang.OutOfMemoryError e) { }
    }

    static void runGC() {
        wb.fullGC();
    }

    public static void main(String args[]) throws Exception {
        if (!wb.isSharedClass(GCStressApp.class)) {
           System.out.println("GCStressApp is not shared. Possibly there was a mapping failure.");
           return;
        }

        if (wb.areSharedStringsIgnored()) {
          System.out.println("Shared strings are ignored.");
          return;
        }

        Object refs = wb.getResolvedReferences(GCStressApp.class);
        if (wb.isShared(refs)) {
            String shared_str = get_shared_string();
            String shared_str1 = get_shared_string1();

            if (!wb.isShared(shared_str)) {
                throw new RuntimeException("FAILED. GCStressApp_shared_string is not shared");
            }

            if (!wb.isShared(shared_str1)) {
                throw new RuntimeException("FAILED. GCStressApp_shared_string1 is not shared");
            }

            allocAlot();
            runGC();
            runGC();
            runGC();

            System.out.println("Passed");
        } else {
            System.out.println(
                "No cached resolved references. Open archive heap data is not used.");
        }
    }
}
