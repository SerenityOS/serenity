/*
 * Copyright (c) 2002, 2016, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2011 SAP AG.  All Rights Reserved.
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

class TestMT {

    static boolean loadLib(String libName) {
        try {
            System.loadLibrary(libName);
            System.out.println("Loaded library "+ libName + ".");
            return true;
        } catch (SecurityException e) {
            System.out.println("loadLibrary(\"" + libName + "\") throws: " + e + "\n");
        } catch (UnsatisfiedLinkError e) {
            System.out.println("loadLibrary(\"" + libName + "\") throws: " + e + "\n");
        }
        return false;
    }

    public static int counter = 1;
    static int Runner() {
        counter = counter * -1;
        int i = counter;
        if (counter < 2) counter += Runner();
        return i;
    }

    public static int run(String msg) {
        try {
            Runner();
        } catch (StackOverflowError e) {
            System.out.println(msg + " caught stack overflow error.");
            return 0;
        } catch (OutOfMemoryError e) {
            return 0;
        }
        return 2;
    }

    public static void main(String argv[]) {
        try {
            for (int i = 0; i < 20; i++) {
                Thread t = new DoStackOverflow("SpawnedThread " + i);
                t.start();
            }
            run("Main thread");
            loadLib("test-rwx");
            run("Main thread");
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    static class DoStackOverflow extends Thread {
        public DoStackOverflow(String name) {
            super(name);
        }
        public void run() {
            for (int i = 0; i < 10; ++i) {
                TestMT.run(getName());
                Thread.yield();
            }
        }
    }
}
