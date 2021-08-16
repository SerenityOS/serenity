/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
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

import sun.hotspot.WhiteBox;

public class LockStringTest extends Thread {
    static String lock;
    static boolean done;
    static WhiteBox wb = WhiteBox.getWhiteBox();

    public static void main(String[] args) throws Exception {

        if (wb.areSharedStringsIgnored()) {
            System.out.println("The shared strings are ignored");
            System.out.println("LockStringTest: PASS");
            return;
        }

        if (!wb.isShared(LockStringTest.class)) {
            throw new RuntimeException("Failed: LockStringTest class is not shared.");
        }

        // Note: This class is archived. All string literals (including the ones used in this class)
        // in all archived classes are interned into the CDS shared string table.

        doTest("StringLock", false);
        doTest("", true);

        // The following string has a 0 hashCode. Calling String.hashCode() could cause
        // the String.hash field to be written into, if so make sure we don't functionally
        // break.
        doTest("\u0121\u0151\u00a2\u0001\u0001\udbb2", true);
    }

    private static void doTest(String s, boolean hasZeroHashCode) throws Exception {
        lock = s;
        done = false;

        if (!wb.isShared(lock)) {
            throw new RuntimeException("Failed: String \"" + lock + "\" is not shared.");
        }

        if (hasZeroHashCode && lock.hashCode() != 0) {
            throw new RuntimeException("Shared string \"" + lock + "\" should have 0 hashCode, but is instead " + lock.hashCode());
        }

        String copy = new String(lock);
        if (lock.hashCode() != copy.hashCode()) {
            throw new RuntimeException("Shared string \"" + lock + "\" does not have the same hashCode as its non-shared copy");
        }

        new LockStringTest().start();

        synchronized(lock) {
            while (!done) {
                lock.wait();
            }
        }
        System.gc();
        System.out.println("LockStringTest: PASS");
    }

    public void run() {
        String shared = "LiveOak";
        synchronized (lock) {
            for (int i = 0; i < 100; i++) {
                new String(shared);
                System.gc();
                try {
                    sleep(5);
                } catch (InterruptedException e) {}
            }
            done = true;
            lock.notify();
        }
    }
}
