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
 * @summary converted from VM Testbase nsk/jvmti/SetEventCallbacks/setevntcallb003.
 * VM Testbase keywords: [quick, jpda, jvmti, onload_only_logic, noras]
 * VM Testbase readme:
 * DESCRIPTION
 *     This JVMTI test exercises JVMTI thread function SetEventCallbacks().
 *     This test checks that if SetEventCallbacks() is invoked with
 *     zero size of callbacks structure, then no events will be sent
 *     and no callbacks will be invoked.
 *     In Agent_OnLoad() the test sets event callbacks for VM_INIT
 *     and enables it.
 *     In VM_INIT callback the test sets callbacks structure with
 *     THREAD_START and THREAD_END events, but passes zero for
 *     structure size, and enables these events.
 *     The debuggee class starts and ends trivial thread to generate
 *     tested events. After thread completed, the test checks if no event
 *     callbacks were actually invoked.
 *     If callbacks for THREAD_START and THREAD_END events were not invoked,
 *     the test passes with exit status 95.
 *     If callback for VM_INIT event was not invoked or other error occur,
 *     the test fails with exit status 97.
 * COMMENTS
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm/native
 *      -agentlib:setevntcallb003=-waittime=5
 *      nsk.jvmti.SetEventCallbacks.setevntcallb003
 */

