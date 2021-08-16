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
 * @summary converted from VM Testbase nsk/jvmti/scenarios/events/EM02/em02t008.
 * VM Testbase keywords: [quick, jpda, jvmti, onload_only_caps, noras]
 * VM Testbase readme:
 * DESCRIPTION
 *     This JVMTI test is for EM02 scenario of "events and event management" area.
 *     Test executes the following steps for EXCEPTION, EXCEPTION_CATCH event:
 *     1)
 *     - adds the <can_generate_exception_events> capability during
 *       the OnLoad phase;
 *     - sets callbacks for VM_INIT and chosen events during the OnLoad phase;
 *     - enables all optional events via SetEventNotificationMode during
 *       the OnLoad phase;
 *     - starts debuggee's threads which throw exceptions;
 *     - expects that at least one EXCEPTION and EXCEPTION_CATCH events will
 *       be sent.
 *     2)
 *     - changes callbacks for chosen event;
 *     - starts debuggee's threads which throw exceptions;
 *     - checks that altered callback works and at least one EXCEPTION and
 *       EXCEPTION_CATCH events will be sent.
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
 * @build nsk.jvmti.scenarios.events.EM02.em02t008
 * @run main/othervm/native ExecDriver --java
 *      -agentlib:em02t008=-waittime=5
 *      nsk.jvmti.scenarios.events.EM02.em02t008
 */

