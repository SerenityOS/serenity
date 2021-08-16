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
 * @summary converted from VM Testbase nsk/jvmti/scenarios/events/EM02/em02t009.
 * VM Testbase keywords: [quick, jpda, jvmti, onload_only_caps, noras]
 * VM Testbase readme:
 * DESCRIPTION
 *     This JVMTI test is for EM02 scenario of "events and event management" area.
 *     Test executes the following steps for METHOD_ENTRY, METHOD_EXIT events:
 *     1)
 *     - adds the <can_generate_method_entry_events>,
 *       <can_generate_method_exit_events> capabilities during the OnLoad phase;
 *     - sets callbacks for VM_INIT and chosen events during the OnLoad phase;
 *     - enables all optional events via SetEventNotificationMode during
 *       the OnLoad phase;
 *     - starts debuggee's method 1000 times;
 *     - expects that METHOD_ENTRY and METHOD_EXIT events are sent 1000 times.
 *     2)
 *     - changes callbacks for chosen event;
 *     - starts debuggee's method 1000 times;
 *     - checks that altered callback works and METHOD_ENTRY and METHOD_EXIT
 *       events will be sent 1000 times..
 *     3)
 *     - sets off callbacks for chosen event;
 *     - sets callback for VM_DEATH event;
 *     - checks that no chosen events are sent until VMDeath event.
 * COMMENTS
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
 *      -agentlib:em02t009=-waittime=5
 *      nsk.jvmti.scenarios.events.EM02.em02t009
 */

