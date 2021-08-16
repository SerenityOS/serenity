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

package jdk.jfr.event.gc.detailed;

import java.util.List;
import java.util.LinkedList;

/**
 * Helper class which triggers and handles out of memory error to generate
 * JFR events
 */
public class OOMApp {

    public static List<DummyObject> dummyList;

    public static void main(String[] args) {
        int bytesToAllocate;

        if (args.length > 0) {
            bytesToAllocate = Integer.parseInt(args[0]);
        } else {
            bytesToAllocate = 1024;
        }
        System.gc();
        dummyList = new LinkedList<DummyObject>();
        System.out.println("## Initiate OOM ##");
        try {
            while (true) {
                dummyList.add(new DummyObject(bytesToAllocate));
            }
        } catch (OutOfMemoryError e) {
            System.out.println("## Got OOM ##");
            dummyList = null;
        }
        System.gc();
    }

    public static class DummyObject {
        public byte[] payload;

        DummyObject(int bytesToAllocate) {
            payload = new byte[bytesToAllocate];
        }
    }
}
