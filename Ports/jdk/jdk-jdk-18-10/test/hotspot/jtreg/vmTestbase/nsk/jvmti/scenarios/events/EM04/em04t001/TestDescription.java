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
 * @summary converted from VM Testbase nsk/jvmti/scenarios/events/EM04/em04t001.
 * VM Testbase keywords: [quick, jpda, jvmti, noras]
 * VM Testbase readme:
 * DESCRIPTION
 *     This JVMTI test is for EM04 scenario of "events and event management" area.
 *     The test performs the following steps:
 *     (1) sets the callback and enables the DynamicCodeGenerated event during
 *         the OnLoad phase
 *     (2) saves the DynamicCodeGenerated received events that is parameters of
 *         these events (name, address, length) are stored into a list.
 *     (3) when VM has been started the test calls GenerateEvents and compares
 *         parameters of newly received events with elements of the list created
 *         on step 2.
 *     Statement:
 *         All events stored on step 2 must be newly received on step 3.
 *         It is supposed that combination of (address, length) is unique.
 * COMMENTS
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm/native
 *      -agentlib:em04t001=-waittime=5
 *      nsk.jvmti.scenarios.events.EM04.em04t001
 */

