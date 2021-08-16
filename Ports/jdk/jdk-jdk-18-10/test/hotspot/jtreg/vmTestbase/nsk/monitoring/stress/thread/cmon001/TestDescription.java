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
 * @summary converted from VM Testbase nsk/monitoring/stress/thread/cmon001.
 * VM Testbase keywords: [stress, monitoring, nonconcurrent]
 * VM Testbase readme:
 * DESCRIPTION
 *     The test checks that waited time returned by ThreadInfo.getWaitedTime() is
 *     not greater than overall time of execution of a thread. Also, the test
 *     checks that waited time is not less than minimal time that each thread
 *     waits without notifications on Object.wait(long).
 *     Main thread starts a number of threads that is specified in "-threadCount"
 *     option. Each thread has three Object.wait(long) blocks; two of them do
 *     not receieve notifications, therefore a thread will be waiting for at least
 *     a few milliseconds. All threads access the same Integer variable
 *     ("calculated") and increase its value by one. Main thread waits until
 *     "calculated" becomes equal to number of started threads and then perform
 *     three checks.
 *     The test fails if "calculated" value is not equal to number of started
 *     threads. It also fails if value returned by ThreadInfo.getWaitedTime() in
 *     any thread is greater than overall time of execution of the thread. Also,
 *     the test fails if waited time is less than time waited in first and second
 *     Object.wait(long) methods (those objects do not receive notifications).
 *     Then the test repeats the procedure with starting threads and performing
 *     checks ITERATIONS times.
 *     All comparisions of time are made with precision specified in PRECISION
 *     constant. Values returned by ThreadInfo.getWaitedTime() and
 *     System.nanoTime() and may use different methods to sample time, so
 *     PRECISION is essential to compare those two times and is set to 3
 *     (milliseconds) by default.
 *     This particular test performs directly access to the MBeans' methods.
 * COMMENTS
 *     Fixed the bug
 *     4983682 TEST BUG: cmon001/2/3 should call System.nanoTime for time
 *             comparison
 *     5070997: TEST_BUG: incorrect below-checking of thread waiting time
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm nsk.monitoring.stress.thread.cmon001 -threadCount=400
 */

