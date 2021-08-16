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
 * @summary converted from VM Testbase nsk/jvmti/SetEventCallbacks/setevntcallb001.
 * VM Testbase keywords: [quick, jpda, jvmti, onload_only_logic, noras]
 * VM Testbase readme:
 * DESCRIPTION
 *     This JVMTI test exercises JVMTI thread function SetEventCallbacks().
 *     This test checks that SetEventCallbacks() successfully sets callbacks
 *     for the tested avents and these callbacks are invoked when events occur.
 *     The following callbacks are set:
 *         - VM_INIT callback in Agent_OnLoad()
 *         - THREAD_START and THREAD_END callbacks in VM_INIT callback
 *     The debuggee class starts and ends trivial thread to generate
 *     tested events. After thread completed, the test checks if all event
 *     callbacks were invoked.
 *     If all of the set callbacks were invoked at least once, the test
 *     passes with exit status 95.
 *     Otherwise, the test fails with exit status 97.
 * COMMENTS
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm/native
 *      -agentlib:setevntcallb001=-waittime=5
 *      nsk.jvmti.SetEventCallbacks.setevntcallb001
 */

