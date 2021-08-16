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
 */

/*
 * @test
 * @bug 4656449 4945125 8074355
 * @summary Make MutexLocker smarter about non-Java threads
 * @library /test/lib
 * @run driver/timeout=240 ShutdownTest
 */

// This test is adapted from an old regression test for bug 4945125, where VerifyBeforeExit
// crashes before exit for the regression test for bug 4656449.
// The fix is to acquire the Heap_lock before exit after the JavaThread is removed from
// the threads list.  This fix is still valid.  This code requires Heap_lock be acquired
// without a safepoint check at exit.

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

public class ShutdownTest {
   Object[] obj;

   ShutdownTest() {
       // Allocate to get some GC pressure.
       obj = new Object[100000];
   }

    static class ShutdownTestThread extends Thread {
       public void run() {
         while (true) {
           ShutdownTest st = new ShutdownTest();
         }
       }

       public static void main(String args[]) {
         System.out.println("- ShutdownTest -");

         for (int i = 0; i < 100; i++) {
           ShutdownTestThread st = new ShutdownTestThread();
           st.setDaemon(true);
           st.start();
         }
       }
    }

    private static void startVM(String... options) throws Throwable {
        // Combine VM flags given from command-line and your additional options
        OutputAnalyzer output = ProcessTools.executeTestJvm(options);
        output.shouldContain("- ShutdownTest -");
        output.shouldHaveExitValue(0);

    }

    public static void main(String[] args) throws Throwable {
        // To reproduce original bug you may need this option: "-Xmx2500k",
        for (int iteration = 0; iteration < 5; ++iteration) {
            startVM("-XX:+UnlockDiagnosticVMOptions",
                    "-XX:+VerifyBeforeExit",
                    ShutdownTestThread.class.getName());
        }
    }
}
