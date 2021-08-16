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
 * @summary converted from VM Testbase nsk/jvmti/scenarios/sampling/SP06/sp06t003.
 * VM Testbase keywords: [quick, jpda, jvmti, noras, quarantine]
 * VM Testbase comments: 8051349
 * VM Testbase readme:
 * DESCRIPTION
 *     This JVMTI test is for SP06 scenario of "time sampling profiling" area.
 *     This test checks that JVMTI methods GetStackTrace() returns the sane
 *     stack frames info as GetFrameCount() with GetFrameLocation() do
 *     even for stack with compiled methods.
 *     The test provokes compilation of tested methods by intensivelly calling
 *     them in a loop and then invokes GenarateEvents() to receive all
 *     COMPILED_METHOD_LOAD events and mark compiled methods.
 *     Threads are supended/resumed individually by SuspendThread() and ResumeThread().
 *     Checked statements:
 *         - stack frame info returned by GetFrameLocation() equals to the
 *           corresponding frame info returmned by GetStackTrace()
 *         - both GetFrameLocation() and GetStackTrace() include frame info
 *           for expected tested method
 *     Tested threads:
 *         Running             - running in Java method
 *         Entering            - entering monitor in synchronized block
 *         Waiting             - waiting on Object.wait()
 *         Sleeping            - sleeping in Thread.sleep()
 *         RunningInterrupted  - running after interruption
 *         RunningNative       - running in native method
 *     Testcases:
 *         - run threads and provoke methods compilation
 *         - enable events
 *         - call GenerateEvents() and mark compiled methods
 *         - check stack frames of not suspended threads
 *         - suspend each threads
 *         - check stack frames of suspended threads
 *         - resume each threads
 *         - check stack frames of resumed threads
 * COMMENTS
 *     Test fixed due to test bug:
 *     4932874 TEST_BUG: wrong cast pointer->int in several JVMTI tests
 *     This test is similar to 'sp02t003' but for compiled methods.
 *     Fixed according to 4960375 bug.
 *         The test updated to match new JVMTI spec 0.2.94.
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm/native
 *      -agentlib:sp06t003=-waittime=5
 *      nsk.jvmti.scenarios.sampling.SP06.sp06t003
 */

