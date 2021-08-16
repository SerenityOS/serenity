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
 * @summary converted from VM Testbase nsk/jvmti/RunAgentThread/agentthr001.
 * VM Testbase keywords: [quick, jpda, jvmti, onload_only_logic, noras]
 * VM Testbase readme:
 * DESCRIPTION
 *     The test exercises JVMTI function RunAgentThread.
 *     Profiling agent debugthr001.c creates few system threads as
 *     following:
 *       - in callback function when JVMTI_EVENT_THREAD_START event
 *         is detected: three threads of different priorities;
 *       - from agent thread;
 *       - from native method invoked in java-method;
 *     The test checks if all the threads are started and finish.
 *     Failing criteria for the test are:
 *       - the values returned by GetLocalInt are not the same as expected;
 *       - failures of used JVMTI functions.
 * COMMENTS
 *     Ported from JVMDI.
 *     Fixed according to 4925857 bug:
 *       - rearranged synchronization of tested threads
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm/native -agentlib:agentthr001 nsk.jvmti.RunAgentThread.agentthr001 5
 */

