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
 * @summary converted from VM Testbase nsk/jvmti/GetOwnedMonitorInfo/ownmoninf001.
 * VM Testbase keywords: [quick, jpda, jvmti, noras]
 * VM Testbase readme:
 * DESCRIPTION
 *     The test exercises JVMTI function GetOwnedMonitorInfo.
 *     The test cases include:
 *       - number of owned monitors: 0, 1, 2;
 *       - thread: current, non-current;
 *       - thread waiting on a monitor
 *     Failing criteria for the test are:
 *       - the objects returned by GetOwnedMonitorInfo are not the same
 *         as expected;
 *       - failure of GetCapabilities, SuspendThread, ResumeThread,
 *         or GetOwnedMonitorInfo functions.
 * COMMENTS
 *     By today, the test is referred from two bugs, 4327280 and 4463667.
 *     To fix bug 4463667, one code fragment with "Thread.sleep(500);" is
 *     replaced with following one:
 *         Object obj = new Object();
 *                 *
 *                 *
 *         synchronized (obj) {
 *             obj.wait(500);
 *         }
 *     Note. Until 4327280 gets fixing, the correction cannot be tested.
 *     Fixed according to the bug 4509016.
 *     Fixed according to 4669812 bug.
 *     The test was fixed due to the following bug:
 *         4762902 nsk/jvmdi/GetOwnedMonitorInfo/ownmoninf001 has a race
 *     Fixed according to 4925857 bug:
 *       - rearranged synchronization of tested thread
 *       - enhanced descripton
 *     Fixed according to 4762902 bug one more time.
 *     Ported from JVMDI.
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm/native -agentlib:ownmoninf001 nsk.jvmti.GetOwnedMonitorInfo.ownmoninf001
 */

