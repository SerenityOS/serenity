/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @test HandshakeSuspendExitTest
 * @summary This test tries to stress the handshakes with new and exiting threads while suspending them.
 * @library /testlibrary /test/lib
 * @build HandshakeSuspendExitTest
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions -XX:GuaranteedSafepointInterval=1 -XX:+HandshakeALot HandshakeSuspendExitTest
 */

public class HandshakeSuspendExitTest  implements Runnable {

    static Thread[] _suspend_threads = new Thread[16];
    static volatile boolean _exit_now = false;
    static java.util.concurrent.Semaphore _sem = new java.util.concurrent.Semaphore(0);

    @Override
    public void run() {
        _sem.release();
        while (!_exit_now) {
            // Leave last 2 threads running.
            for (int i = 0; i < _suspend_threads.length - 2; i++) {
                if (Thread.currentThread() != _suspend_threads[i]) {
                    _suspend_threads[i].suspend();
                    _suspend_threads[i].resume();
                }
            }
        }
        _sem.release();
    }

    public static void main(String... args) throws Exception {
        HandshakeSuspendExitTest test = new HandshakeSuspendExitTest();
        // Fire-up suspend threads.
        for (int i = 0; i < _suspend_threads.length; i++) {
            _suspend_threads[i] = new Thread(test);
        }
        for (int i = 0; i < _suspend_threads.length; i++) {
            _suspend_threads[i].start();
        }
        // Wait for all suspend-threads to start looping.
        for (Thread thr : _suspend_threads) {
            _sem.acquire();
        }

        // Fire-up exiting threads.
        Thread[] exit_threads = new Thread[128];
        for (int i = 0; i < exit_threads.length; i++) {
            exit_threads[i] = new Thread();
            exit_threads[i].start();
        }

        // Try to suspend them.
        for (Thread thr : exit_threads) {
            thr.suspend();
        }
        for (Thread thr : exit_threads) {
            thr.resume();
        }

        // Start exit and join.
        _exit_now = true;
        int waiting = _suspend_threads.length;
        do {
            // Resume any worker threads that might have suspended
            // each other at exactly the same time so they can see
            // _exit_now and check in via the semaphore.
            for (Thread thr : _suspend_threads) {
                thr.resume();
            }
            while (_sem.tryAcquire()) {
                --waiting;
            }
        } while (waiting > 0);
        for (Thread thr : _suspend_threads) {
            thr.join();
        }
        for (Thread thr : exit_threads) {
            thr.join();
        }
    }
}
