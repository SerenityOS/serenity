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
 * @summary converted from VM Testbase nsk/jvmti/scenarios/allocation/AP04/ap04t001.
 * VM Testbase keywords: [jpda, jvmti, noras]
 * VM Testbase readme:
 * DESCRIPTION
 *   The test implements AP04 scenario of test plan for Allocation
 *   Profiling.
 *   The test checks the following assertion of spec of heap
 *   iteration function:
 *     During the execution of this function the state of the heap
 *     does not change: no objects are allocated, no objects are
 *     garbage collected, and the state of objects (including held
 *     values) does not change. As a result, threads executing Java
 *     programming language code, threads attempting to resume the
 *     execution of Java programming language code, and threads
 *     attempting to execute JNI functions are typically stalled.
 *   The test class 'ap04t001' contains self-typed array field
 *   'root' which is initialized in preliminary phase of test
 *   cases. Each element of 'root' array is tagged.
 *   Then an auxiliary application thread has started which
 *   runs one of the heap iteration function. After receiving a
 *   notification on start of auxuliary thread, the main thread of
 *   test application nullifies references to elements of 'root'
 *   array and forces garbage collection.
 *   The test agent sets and enables ObjectFree, GarbageCollectionStart,
 *   and GarbageCollectionFinish events. The test fails if any these events
 *   were received during heap iteration.
 * COMMENTS
 *     Test fixed according to test bug:
 *     5050527 nsk/jvmti/scenarios/allocation/AP04/ap04t002 has timing issue
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jvmti.scenarios.allocation.AP04.ap04t001
 * @run main/othervm/native
 *      -agentlib:ap04t001=-waittime=5,-verbose
 *      nsk.jvmti.scenarios.allocation.AP04.ap04t001
 */

