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
 * @summary converted from VM Testbase nsk/jvmti/scenarios/sampling/SP02/sp02t001.
 * VM Testbase keywords: [quick, jpda, jvmti, noras]
 * VM Testbase readme:
 * DESCRIPTION
 *     This JVMTI test is for SP02 scenario of "time sampling profiling" area.
 *     This test checks that JVMTI methods GetFrameCount() and GetStackTrace()
 *     return expected number of stack frames for each kind of thread.
 *     Threads are suspended/resumed individually by SuspendThread() and ResumeThread().
 *     Checked statements for suspended threads:
 *         - number of stack frames returned by GetFramesCount() and GetStackTrace()
 *           are not less than expected minimal stack depth
 *         - number of stack frames returned by GetFrameCount() should be equal to
 *           frames number returned by successive call to GetStackTrace()
 *     Tested threads:
 *         Running             - running in Java method
 *         Entering            - entering monitor in synchronized block
 *         Waiting             - waiting on Object.wait()
 *         Sleeping            - sleeping in Thread.sleep()
 *         RunningInterrupted  - running after interruption
 *         RunningNative       - running in native method
 *     Testcases:
 *         - start threads
 *         - suspend each threads
 *         - check stack frames of suspended threads
 *         - resume each threads

 * COMMENTS
 *     Fixed according to test bug:
 *     6405644 TEST_BUG: no proper sync with agent thread in sp02t001/sp02t003
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm/native
 *      -agentlib:sp02t001=-waittime=5
 *      nsk.jvmti.scenarios.sampling.SP02.sp02t001
 */

