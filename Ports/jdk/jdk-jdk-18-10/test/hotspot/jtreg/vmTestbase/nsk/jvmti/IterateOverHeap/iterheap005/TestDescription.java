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
 * @summary converted from VM Testbase nsk/jvmti/IterateOverHeap/iterheap005.
 * VM Testbase keywords: [quick, jpda, jvmti, noras]
 * VM Testbase readme:
 * DESCRIPTION
 *     This test checks if the following functions:
 *         CreateRawMonitor
 *         DestroyRawMonitor
 *         RawMonitorEnter
 *         RawMonitorExit
 *         RawMonitorWait
 *         RawMonitorNotify
 *         RawMonitorNotifyAll
 *         DestroyRawMonitor
 *     may be called in the callback of IterateOverHeap
 *     function as it is specified for raw monitor functions:
 *        This function may be called from the callbacks to the Heap
 *        iteration functions, or from the event handles for the
 *        GarbageCollectionStart, GarbageCollectionFinish, and
 *        ObjectFree events.
 *     The test defines jvmtiHeapObjectCallback callback which
 *     invokes the checked raw monitor functions.
 *     Then IterateOverHeap function is invoked with the defined
 *     callback.
 * COMMENTS
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jvmti.IterateOverHeap.iterheap005
 * @run main/othervm/native
 *      -agentlib:iterheap005=-waittime=5,-verbose
 *      nsk.jvmti.IterateOverHeap.iterheap005
 */

