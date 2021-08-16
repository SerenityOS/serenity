/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @summary converted from VM Testbase nsk/jvmti/scenarios/events/EM02/em02t004.
 * VM Testbase keywords: [quick, jpda, jvmti, onload_only_logic, noras]
 * VM Testbase readme:
 * DESCRIPTION
 *     This JVMTI test is for EM02 scenario of "events and event management" area.
 *     Test executes the following several steps for NATIVE_METHOD_BIND event:
 *       (1)
 *         - adds the <can_generate_native_method_bind_events> capability in
 *           the OnLoad phase
 *         - sets callbacks for VM_INIT and all events of optional functionality
 *           during the OnLoad phase
 *         - enables events via SetEventNotificationMode during the OnLoad phase
 *           for all optional events
 *         - provides the state to provoke generation of chosen events
 *         - checks that VM_INIT and chosen events were sent.
 *       (2)
 *         - sets off callbacks for all optional events
 *         - changes callbacks for chosen events
 *         - provides the state to provoke generation of chosen events
 *         - checks that chosen events were sent
 *         - checks that altered callback works.
 *       (3)
 *         - sets off callbacks for chosen events
 *         - sets callback for VM_DEATH event
 *         - checks that no chosen events are sent until VMDeath event.
 *     This test checks that:
 *     (a) except for chosen events no other optional events are generated on
 *         steps (1) and (2).
 *     (b) Numbers of expected event on steps (1) and (2) is equal to 1.
 *     (c) except for VM_DEATH no other events are generated on step (3).
 * COMMENTS
 *     Adjusted according to CCC update
 *     #4989580: JVMTI: Add JVMTI_ERROR_MUST_POSSESS_CAPABILITY to
 *               SetEventNotificationMode
 *     Modified due to fix of the bug
 *     5010571 TEST_BUG: jvmti tests with VMObjectAlloc callbacks should
 *             be adjusted to new spec
 *     Fixed 5028164 bug.
 *     Fixed
 *     #5045048 TEST_BUG: jvmti tests should synchronize access to static vars
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm/native
 *      -agentlib:em02t004=-waittime=5
 *      nsk.jvmti.scenarios.events.EM02.em02t004
 */

