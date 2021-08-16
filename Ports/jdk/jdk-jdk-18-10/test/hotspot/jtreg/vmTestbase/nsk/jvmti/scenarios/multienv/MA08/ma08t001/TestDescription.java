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
 * @summary converted from VM Testbase nsk/jvmti/scenarios/multienv/MA08/ma08t001.
 * VM Testbase keywords: [quick, jpda, jvmti, onload_only_caps, noras]
 * VM Testbase readme:
 * DESCRIPTION
 *     This test is for MA08 scenario of "multiple environments support".
 *     VM starts with two different agents:
 *       - A1 possesed capability can_signal_thread,
 *       - A2 possesed capability can_generate_exception_events and set
 *         callback for Exception event.
 *     The test kills an application thread in agent A1 by StopThread call and
 *     interrupts another application thread in agent A1 by InterruptThread call,
 *     then checks if the both Exception events are received in agent A2 with
 *     correct passed in parameters.
 * COMMENTS
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm/native
 *      -agentlib:ma08t001=-waittime=5
 *      -agentlib:ma08t001a=-waittime=5
 *      nsk.jvmti.scenarios.multienv.MA08.ma08t001
 */

