/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @test HandshakeWalkOneExitTest
 * @summary This test tries to stress the handshakes with new and exiting threads
 * @library /testlibrary /test/lib
 * @build HandshakeWalkOneExitTest
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI HandshakeWalkOneExitTest
 */

import jdk.test.lib.Asserts;
import sun.hotspot.WhiteBox;

public class HandshakeWalkOneExitTest  implements Runnable {

    @Override
    public void run() {
    }

    static volatile boolean exit_now = false;
    static Thread[] threads;

    public static void main(String... args) throws Exception {
        int testRuns = 20;
        int testThreads = 128;

        HandshakeWalkOneExitTest test = new HandshakeWalkOneExitTest();

        Runnable hser = new Runnable(){
            public void run(){
                WhiteBox wb = WhiteBox.getWhiteBox();
                while(!exit_now) {
                    Thread[] t = threads;
                    for (int i = 0; i<t.length ; i++) {
                        wb.handshakeWalkStack(t[i], false);
                    }
                }
            }
        };
        Thread hst = new Thread(hser);
        for (int k = 0; k<testRuns ; k++) {
            threads = new Thread[testThreads];
            for (int i = 0; i<threads.length ; i++) {
                threads[i] = new Thread(test);
                threads[i].start();
            }
            if (k == 0) {
                hst.start();
            }
        }
        exit_now = true;
        hst.join();
    }
}
