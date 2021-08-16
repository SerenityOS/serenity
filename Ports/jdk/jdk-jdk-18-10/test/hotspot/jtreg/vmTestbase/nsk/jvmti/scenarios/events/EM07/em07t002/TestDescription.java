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
 * @key randomness
 *
 * @summary converted from VM Testbase nsk/jvmti/scenarios/events/EM07/em07t002.
 * VM Testbase keywords: [jpda, jvmti, noras, nonconcurrent]
 * VM Testbase readme:
 * DESCRIPTION
 *     This JVMTI test is developed against "events and event management" area.
 *     Test executes the following scenario to check events COMPILED_METHOD_LOAD,
 *     COMPILED_METHOD_UNLOAD:
 *     (1) adds the <can_generate_compiled_method_load_events> capability in the OnLoad phase;
 *     (2) sets callbacks for COMPILED_METHOD_LOAD, COMPILED_METHOD_UNLOAD events
 *         during the OnLoad phase;
 *     (3) enables these events during the OnLoad phase;
 *     (4) provides the state to provoke generation of chosen events (see details
 *         in comments of em07t002.java)
 *     (5) checks number of COMPILED_METHOD_UNLOAD events is less than
 *         COMPILED_METHOD_LOAD or equal.
 * COMMENTS
 *     Adjusted according to J2SE CCC update:
 *     #5003914: JVMTI Spec: CompiledMethodUnload should specify that method is
 *               for identific
 *     fixed:
 *     #5010807 TEST_BUG: COMPILED_METHOD_UNLOAD events are not filtered
 *     fixed:
 *     #5045048 TEST_BUG: jvmti tests should synchronize access to static vars
 *     Modified due to fix of rfe:
 *     #5055417 TEST: warnings and notes caused by generification
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jvmti.scenarios.events.EM07.em07t002
 *
 * @comment compile loadclassXX to bin/loadclassXX
 * @run driver nsk.share.ExtraClassesBuilder
 *      loadclass
 *
 * @run main/othervm/native
 *      -agentlib:em07t002=attempts=2,-waittime=5
 *      -XX:-UseGCOverheadLimit
 *      nsk.jvmti.scenarios.events.EM07.em07t002
 *      ./bin/loadclass
 */

