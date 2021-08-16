/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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
 *
 * @summary converted from VM Testbase nsk/jvmti/GetThreadCpuTime/thrcputime002.
 * VM Testbase keywords: [quick, jpda, jvmti, noras]
 * VM Testbase readme:
 * DESCRIPTION
 *     This JVMTI test exercises JVMTI thread function GetThreadCpuTime().
 *     This test checks that GetThreadCpuTime() returns correct time value
 *     for either Java thread in debuggee class and agent thread itself.
 *     And this value is increased monotonically as each thread run some code.
 *     Function GetThreadCpuTime() is invoked either for current thread and
 *     for other thread, providing thread object in each case.
 *     Debuggee class runs special tested Java thread to generate THREAD_START
 *     and THREAD_END events. Only those events are considered which are
 *     for the thread with the expected name.
 *     The following checks are performed for GetThreadCpuTime():
 *         for initial thread:
 *         - in VM_INIT event callback (initial)
 *         for agent thread:
 *         - at the begin of agent thread (initial)
 *         - in THREAD_START event callback of tested Java thread
 *         - between THREAD_START and THREAD_END events
 *         - in THREAD_END event callback of tested Java thread
 *         - at the end of agent thread
 *         for tested Java thread:
 *         - in THREAD_START event callback (initial)
 *         - between THREAD_START and THREAD_END events from agent thread
 *         - in THREAD_END event callback
 *     In initial check for each thread GetThreadCpuTime() is called
 *     and returned cpu time is saved in corresponding variable.
 *     If this time is negative, then thye chack fails.
 *     If this tuime is zero, then warning message is printed,
 *     but the test passes.
 *     In further checks for each thread GetThreadCpuTime() is called
 *     and returned cpu time is compared with previous value.
 *     If newly returned cpu time is not greated than previously
 *     saved one, the check fails.
 *     Before each futher check each thread calls doIteration() function
 *     to run some code in a loop, to ensure the cpu time for tested
 *     thread is valuable increased. Particular number of iterations
 *     is configured with agent option 'iterations=<number>'.
 *     If all checks are successful, then test pases with exit status 95.
 *     If any check fails or other error occurs, then agent sets FAIL status
 *     and the test fails with exit status 97.
 *     If the last check (in VM_DEATH callback) fails, then C-language exit()
 *     function is used to force VM exit with fail status 97.
 * COMMENTS
 *     Fixed the 4968019, 5006885 bugs.
 *     Fixed the 5010142 bug.
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jvmti.GetThreadCpuTime.thrcputime002
 * @run main/othervm/native
 *      -agentlib:thrcputime002=-waittime=5,iterations=1000
 *      nsk.jvmti.GetThreadCpuTime.thrcputime002
 */

