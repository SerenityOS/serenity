/*
 * Copyright (c) 2000, 2018, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @key stress
 *
 * @summary converted from VM testbase nsk/stress/stack/stack002.
 * VM testbase keywords: [stress, quick, stack, nonconcurrent]
 * VM testbase readme:
 * DESCRIPTION
 *     Provoke StackOverflowError by infinite recursion in Java method,
 *     intercept the exception and continue to invoke that method until
 *     the test exceeds timeout, or until Java VM crashes.
 * COMMENTS
 *     I believe that the test causes HS crashes due to the following bug:
 *     4330318 (P2/S2) NSK test fails as An irrecoverable stack overflow
 *     See also bugs (lots of bugs!):
 *     Evaluated:
 *     4217960 [native stack overflow bug] reflection test causes crash
 *     Accepted:
 *     4285716 native stack overflow causes crash on Solaris
 *     4281578 Second stack overflow crashes HotSpot VM
 *     Closed (duplicate):
 *     4027933     Native stack overflows not detected or handled correctly
 *     4134353     (hpi) sysThreadCheckStack is a no-op on win32
 *     4185411     Various crashes when using recursive reflection.
 *     4167055     infinite recursion in FindClass
 *     4222359     Infinite recursion crashes jvm
 *     Closed (will not fix):
 *     4231968 StackOverflowError in a native method causes Segmentation Fault
 *     4254634     println() while catching StackOverflowError causes hotspot VM crash
 *     4302288 the second stack overflow causes Classic VM to exit on win32
 *
 * @requires vm.opt.DeoptimizeALot != true
 * @run main/othervm/timeout=900 nsk.stress.stack.stack002
 */

package nsk.stress.stack;


import java.io.PrintStream;

public class stack002 {
    static final long timeout = 10000; // 10 seconds

    public static void main(String[] args) {
        int exitCode = run(args, System.out);
        System.exit(exitCode + 95);
    }

    public static int run(String args[], PrintStream out) {
        Tester tester = new Tester(out);
        Timer timer = new Timer(tester);
        timer.start();
        tester.start();
        while (timer.isAlive())
            try {
                timer.join();
            } catch (InterruptedException e) {
                e.printStackTrace(out);
                return 2;
            }
        //      if (tester.isAlive())
//          return 2;
        out.println("Maximal depth: " + tester.maxdepth);
        return 0;
    }

    private static class Tester extends Thread {
        int maxdepth;
        PrintStream out;

        public Tester(PrintStream out) {
            this.out = out;
            maxdepth = 0;
        }

        public void run() {
            recurse(0);
        }

        void recurse(int depth) {
            maxdepth = depth;
            try {
                recurse(depth + 1);
//          } catch (StackOverflowError e) {
//
// OutOfMemoryError is also eligible to indicate stack overflow:
//
            } catch (Error error) {
                if (!(error instanceof StackOverflowError) &&
                        !(error instanceof OutOfMemoryError))
                    throw error;

/***
 *** Originally, I supposed that VM crashes because of unexpected
 *** native stack overflow (println() invokes native method).
 *** However, I found that HS 1.3 and HS 2.0 crash even on
 *** invocation of Java (not native) method.
 ***
 out.println("StackOverflowError, depth=" + depth);
 ***/
                recurse(depth + 1);
            }
        }
    }

    private static class Timer extends Thread {
        private Tester tester;

        public Timer(Tester tester) {
            this.tester = tester;
        }

        public void run() {
            long started;
            started = System.currentTimeMillis();
            while (System.currentTimeMillis() - started < timeout)
                ; /***
             *** The test hangs on JDK 1.2.2 Classic VM if sleep() is invoked.
             ***
             try {
             this.sleep(1000);
             } catch (InterruptedException e) {
             e.printStackTrace(tester.out);
             return;
             };
             ***/
            tester.stop();
        }
    }
}
