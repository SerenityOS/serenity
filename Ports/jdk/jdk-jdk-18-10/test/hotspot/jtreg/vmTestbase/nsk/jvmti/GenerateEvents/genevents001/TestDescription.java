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
 * @summary converted from VM Testbase nsk/jvmti/GenerateEvents/genevents001.
 * VM Testbase keywords: [quick, jpda, jvmti, noras]
 * VM Testbase readme:
 * DESCRIPTION
 *     This JVMTI test exercises JVMTI thread function GenerateEvents().
 *     This test checks that GenerateEvents() returns no error code
 *     and generates missed COMPILED_METHOD_LOAD and DYNAMIC_CODE_GENERATED
 *     events and does not generate missed COMPILED_METHOD_UNLOAD events.
 *     The test intensively calls tested method in debuggee code
 *     to provoke its compilation. Then agent enables tested events,
 *     calls GenerateEvents() and checks if missed events are received.
 *     It is not guaranteed that tested method will be compiled and
 *     any dynamic code will be generated, so if no COMPILED_METHOD_LOAD
 *     and DYNAMIC_CODE_GENERATED are received then the test just
 *     prints warning message and passes anyway.
 *     If COMPILED_METHOD_UNLOAD events are received which are not listed
 *     as supported events for GenerateEvents(), then the test fails.
 * COMMENTS
 *     Fixed according to 4960375 bug.
 *         The test updated to match new JVMTI spec 0.2.94.
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm/native
 *      -agentlib:genevents001=-waittime=5
 *      nsk.jvmti.GenerateEvents.genevents001
 */

