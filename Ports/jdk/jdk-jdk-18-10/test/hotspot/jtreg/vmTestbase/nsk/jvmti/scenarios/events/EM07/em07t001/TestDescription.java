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
 * @summary converted from VM Testbase nsk/jvmti/scenarios/events/EM07/em07t001.
 * VM Testbase keywords: [quick, jpda, jvmti, noras]
 * VM Testbase readme:
 * DESCRIPTION
 *     This JVMTI test is for EM07 scenario of "events and event management" area.
 *     The test checks that JVMTI no events are generated if no capabilities
 *     are not added.
 *     The test fails if it receives at least one events of optional functionality.
 * COMMENTS
 *     Adjusted according to CCC update:
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
 *      -agentlib:em07t001=-waittime=5
 *      nsk.jvmti.scenarios.events.EM07.em07t001
 */

