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
 * @summary converted from VM Testbase nsk/jvmti/scenarios/allocation/AP11/ap11t001.
 * VM Testbase keywords: [quick, jpda, jvmti, noras]
 * VM Testbase readme:
 * DESCRIPTION
 *     The test implements AP11 scenario of test plan for Allocation
 *     Profiling area.
 *     The test agent enables VMObjectAllocation events. For every
 *     received event, the agent checks that:
 *       - event is received in the Live phase,
 *       - allocating thread is equal to one returned by GetAllThreads
 *         function,
 *       - class of allocated object is equal to one returned by
 *         GetLoadedClasses function,
 *       - class of allocated object is equal to one returned by JNI
 *         GetObjectClass function,
 *       - size of the object is equal to size returned by GetObjectSize
 *         function.
 * COMMENTS
 *     Modified due to fix of the bug
 *     5010571 TEST_BUG: jvmti tests with VMObjectAlloc callbacks should
 *             be adjusted to new spec
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm/native
 *      -agentlib:ap11t001=-waittime=5
 *      nsk.jvmti.scenarios.allocation.AP11.ap11t001
 */

