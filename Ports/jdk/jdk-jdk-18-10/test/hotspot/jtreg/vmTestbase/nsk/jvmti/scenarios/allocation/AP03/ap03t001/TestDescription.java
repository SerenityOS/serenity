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
 * @summary converted from VM Testbase nsk/jvmti/scenarios/allocation/AP03/ap03t001.
 * VM Testbase keywords: [quick, jpda, jvmti, noras]
 * VM Testbase readme:
 * DESCRIPTION
 *   The test implements AP03 scenario of test plan for Allocation
 *   Profiling.
 *   The test class 'ap03t001' contains 'finalize' method, which
 *   sets strong reference from static field 'catcher' to
 *   finalized instance of the test class.
 *   The test creates a checked instance of 'ap03t001' class,
 *   sets its tag, then nullifies all references to it and
 *   provokes garbage collection and finalization. The checked
 *   object should be restored while finalization.
 *   The test agent enables for ObjectFree event. The agent
 *   runs IterateOverHeap, IterateOverInstancesOfClass,
 *   IterateOverObjectsReachableFromObject functions after
 *   finalization of checked object.
 *   The test fails if heap iteration functions did not found
 *   the checked object or ObjectFree event was reveived
 *   for it.
 * COMMENTS
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jvmti.scenarios.allocation.AP03.ap03t001
 * @run main/othervm/native
 *      -agentlib:ap03t001=-waittime=5,-verbose
 *      nsk.jvmti.scenarios.allocation.AP03.ap03t001
 */

