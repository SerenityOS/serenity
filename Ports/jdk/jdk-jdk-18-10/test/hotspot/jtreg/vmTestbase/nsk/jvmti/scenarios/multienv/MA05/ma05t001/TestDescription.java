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
 * @summary converted from VM Testbase nsk/jvmti/scenarios/multienv/MA05/ma05t001.
 * VM Testbase keywords: [quick, jpda, jvmti, onload_only_caps, noras]
 * VM Testbase readme:
 * DESCRIPTION
 *     This test is for MA05 scenario of "multiple environments support".
 *     VM starts with two different agents, both possessed capability
 *     can_generate_frame_pop_events and had callbacks for the FramePop
 *     event been set and enabled. Then the test checks:
 *       - if the event is received in both agents if NotifyFramePop with
 *         the same arguments was invoked in both agents,
 *       - if the event is received only in the first agents if
 *         NotifyFramePop was invoked only in the first agent,
 *       - if the event is received only in the first agent if
 *         NotifyFramePop with the same arguments was invoked in both agents,
 *         but the event was disabled in the other agent.
 * COMMENTS
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm/native
 *      -agentlib:ma05t001=-waittime=5
 *      -agentlib:ma05t001a=-waittime=5
 *      nsk.jvmti.scenarios.multienv.MA05.ma05t001
 */

