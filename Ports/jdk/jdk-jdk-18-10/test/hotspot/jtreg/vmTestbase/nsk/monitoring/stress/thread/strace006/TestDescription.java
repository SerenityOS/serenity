/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @summary converted from VM Testbase nsk/monitoring/stress/thread/strace006.
 * VM Testbase keywords: [stress, monitoring, nonconcurrent, jdk_desktop]
 * VM Testbase readme:
 * DESCRIPTION
 *     The test checks that ThreadInfo.getStackTrace() returns correct results for
 *     a thread in "running" state.
 *     Main thread starts a number of auxiliary threads. This number is specified
 *     in "-threadCount" option. Auxiliary threads begin a recursion until they
 *     reach specified depth ("-depth" option). Each thread may use pure java
 *     and/or native methods based on "-invocationType" option. Then the threads
 *     wait until they get a notification from main thread. So, those auxiliary
 *     threads are definitly in "running" state when main thread performs their
 *     checks.
 *     Main thread makes a snapshot of stack trace for all threads and checks it:
 *         1. If a thread is alive, ThreadMonitor.getThreadInfo(long, -1) must
 *            return not null ThreadInfo.
 *         2. The length of a trace must not be greater than (depth + 3). Number
 *            of recursionJava() or recursionNative() methods must not be greater
 *            than depth, also one Object.wait() or Thread.yield() method, one
 *            run(), and one waitForSign().
 *         3. The latest method of the stack trace must be RunningThread.run().
 *         4. getClassName() and getMethodName() methods must return expected
 *            values for each element of the stack trace.
 *     If one of that testcases fail, the test also fails.
 *     After all threads are checked, main thread notifies them to complete their
 *     job. Then the test repeats the procedure with starting threads and
 *     performing checks ITERATIONS times.
 *     This particular test performs access to the MBeans' methods through custom
 *     MBeanServer and uses native methods in auxiliary threads.
 * COMMENTS
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm/native
 *      nsk.monitoring.stress.thread.strace001
 *      -testMode=server
 *      -MBeanServer=custom
 *      -invocationType=native
 *      -threadCount=50
 *      -depth=200
 */

