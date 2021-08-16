/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test MixedHandshakeWalkStackTest
 * @library /testlibrary /test/lib
 * @build MixedHandshakeWalkStackTest
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI MixedHandshakeWalkStackTest
 */

import jdk.test.lib.Asserts;
import sun.hotspot.WhiteBox;

public class MixedHandshakeWalkStackTest {
    public static Thread testThreads[];

    public static void main(String... args) throws Exception {
        testThreads = new Thread[Runtime.getRuntime().availableProcessors()];
        for (int i = 0; i < testThreads.length; i++) {
            testThreads[i] = new Thread(() -> handshake());
        }

        for (Thread t : testThreads) {
            t.start();
        }

        handshake();

        for (Thread t : testThreads) {
            t.join();
        }
    }

    public static void handshake() {
        WhiteBox wb = WhiteBox.getWhiteBox();
        java.util.concurrent.ThreadLocalRandom rand = java.util.concurrent.ThreadLocalRandom.current();
        long end = System.currentTimeMillis() + 20000;
        while (end > System.currentTimeMillis()) {
            wb.asyncHandshakeWalkStack(testThreads[rand.nextInt(testThreads.length)]);
            wb.handshakeWalkStack(testThreads[rand.nextInt(testThreads.length)], false);
            wb.handshakeWalkStack(testThreads[rand.nextInt(testThreads.length)], true);
        }
    }
}
