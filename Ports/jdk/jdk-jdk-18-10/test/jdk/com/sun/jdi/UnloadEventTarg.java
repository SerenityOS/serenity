/*
 * Copyright (c) 2001, Oracle and/or its affiliates. All rights reserved.
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
 *
 */
import java.util.*;

class UnloadEventTarg {
    static int loadersFinalized = 0;

    public static void main(String[] args) throws ClassNotFoundException {
        loadup("first");
        loadup("second");
        if (!forceUnload()) {
            System.err.println("Unable to force unload");
        }
        lastStop();
    }

    static void loadup(String id) throws ClassNotFoundException {
        ClassLoaderTarg cl = new ClassLoaderTarg(id);
        cl.findClass("Unload1Targ").getFields();
        cl.findClass("Unload2Targ").getFields();
    }

    static boolean forceUnload() {
        List holdAlot = new ArrayList();
        for (int chunk=10000000; chunk > 10000; chunk = chunk / 2) {
            if (loadersFinalized > 1) {
                return true;
            }
            try {
                while(true) {
                    holdAlot.add(new byte[chunk]);
                    System.err.println("Allocated " + chunk);
                }
            }
            catch ( Throwable thrown ) {  // OutOfMemoryError
                System.gc();
            }
            System.runFinalization();
        }
        return false;
    }

    static void classLoaderFinalized(String id) {
        System.err.println("finalizing ClassLoaderTarg - " + id);
        loadersFinalized++;
    }

    static void unloading1() {
        System.err.println("unloading Unload1Targ");
    }

    static void unloading2() {
        System.err.println("unloading Unload2Targ");
    }

    static void lastStop() {
        System.err.println("UnloadEventTarg exiting");
    }

}
