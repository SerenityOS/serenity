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
 * @summary converted from VM Testbase nsk/jvmti/scenarios/events/EM05/em05t002.
 * VM Testbase keywords: [jpda, jvmti, noras]
 * VM Testbase readme:
 * DESCRIPTION
 *     This JVMTI test is for EM05 scenario of "events and event management" area.
 *     This test checks that JVMTI events COMPILED_METHOD_LOAD and COMPILED_METHOD_UNLOAD
 *     are generated if methods are intensively used and if JVMTI function
 *     GenerateEvents() is called.
 *     Checked statements:
 *         - final call to GenerateEvents() should not generate COMPILED_METHOD_LOAD
 *           events for never compiled or compiled but unloaded methods
 *         - final call to GenerateEvents() should not ganerate number of
 *           COMPILED_METHOD_LOAD events greater than received before
 *         - total number of received COMPILED_METHOD_UNLOAD events should not be
 *           greater than total number of received COMPILED_METHOD_LOAD events
 *     Tested methods:
 *         javaMethod      - thread method written in Java language
 *         nativeMethod    - thread native method
 *     Testcases:
 *         - enable events
 *         - initial call GenerateEvants()
 *         - check received events
 *         - run thread to provoke methods compilation
 *         - check received events
 *         - final call GenerateEvants()
 *         - check received events
 * COMMENTS
 *     Test fixed due to test bug:
 *     4932874 TEST_BUG: wrong cast pointer->int in several JVMTI tests
 *     Fixed according to 4960375 bug.
 *         The test updated to match new JVMTI spec 0.2.94.
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm/native
 *      -agentlib:em05t002=-waittime=5
 *      nsk.jvmti.scenarios.events.EM05.em05t002
 */

