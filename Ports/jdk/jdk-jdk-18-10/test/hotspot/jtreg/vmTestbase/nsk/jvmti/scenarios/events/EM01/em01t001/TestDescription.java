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
 * @summary converted from VM Testbase nsk/jvmti/scenarios/events/EM01/em01t001.
 * VM Testbase keywords: [quick, jpda, jvmti, noras]
 * VM Testbase readme:
 * DESCRIPTION
 *     This JVMTI test is for EM01 scenario of "events and event management" area.
 *     The test checks phase when events of required functionality VM_START,
 *     VM_INIT, VM_DEATH, CLASS_LOAD, CLASS_PREPARE, THREAD_START, THREAD_END
 *     are sent.
 *     The test starts threads and loads classes through pureJAVA calls.
 *     Checked statements:
 *         - above-mentioned events are sent without holding any capability;
 *         - VM_INIT, VM_DEATH, CLASS_LOAD, CLASS_PREPARE events are sent
 *           during LIVE phase only;
 *         - THREAD_START, THREAD_END, VM_START are sent during START or
 *           LIVE phases;
 * COMMENTS
 *     Fixed bug
 *     5018642 JVMTI ClassPrepare and ClassLoad events now allowed during
 *              start phase
 *     Fixed
 *     #5045048 TEST_BUG: jvmti tests should synchronize access to static vars
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jvmti.scenarios.events.EM01.em01t001
 *
 * @comment compile loadclassXX to bin/loadclassXX
 * @run driver nsk.share.ExtraClassesBuilder
 *      loadclass
 *
 * @run main/othervm/native
 *      -agentlib:em01t001=classLoaderCount=100,-waittime=5
 *      nsk.jvmti.scenarios.events.EM01.em01t001
 *      ./bin/loadclass
 */

