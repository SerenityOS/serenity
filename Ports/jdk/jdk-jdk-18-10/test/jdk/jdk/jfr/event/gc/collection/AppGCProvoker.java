/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr.event.gc.collection;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple application to provoke various kinds of GC.
 */
public class AppGCProvoker {

    private static List<byte[]> garbage = new ArrayList<>();
    public static Object trash;

    public static void main(String args[]) {
        // young gc
        for (int i = 0; i < 100; i++) {
             trash = new byte[100_000];
        }

        // system gc
        System.gc();

        // full gc caused by OOM
        try {
            while(true) {
                garbage.add(new byte[150_000]);
            }
        } catch (OutOfMemoryError e) {
            garbage = null;
        }
    }
}
