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
 * @key randomness
 *
 * @summary converted from VM Testbase nsk/jvmti/scenarios/extension/EX03/ex03t001.
 * VM Testbase keywords: [jpda, jvmti, noras, nonconcurrent, quarantine]
 * VM Testbase comments: 8173658
 * VM Testbase readme:
 * DESCRIPTION
 *     The test implements EX03 scenario of test plan for
 *     Extension Mechanism.
 *     The test agent performs the following actions
 *     and checks:
 *        - search for IsClassUnloadingEnabled function in the list
 *          returned by GetExtentionFunctions' and verifies that
 *          it returns JNI_TRUE. Otherwise the test fails and exits;
 *        - search for ClassUnload in the list returned by
 *          GetExtensionEvents and sets callback if the event is
 *          found;
 *        - waits until the debugged 'ex03t001' class loads axiliary
 *         'ext03t001a' class and unloads it;
 *        - checks that ClassUnload was received;
 *        - unsets callback for the event;
 *        - waits until the debugged 'ex03t001' class loads axiliary
 *         'ext03t001b' class and unloads it;
 *        - checks that ClassUnload was not received;
 * COMMENTS
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jvmti.scenarios.extension.EX03.ex03t001
 *
 * @comment compile loadclassXX to bin/loadclassXX
 * @run driver nsk.share.ExtraClassesBuilder
 *      loadclass
 *
 * @run main/othervm/native
 *      -agentlib:ex03t001=-waittime=5
 *      -XX:-UseGCOverheadLimit
 *      nsk.jvmti.scenarios.extension.EX03.ex03t001
 *      ./bin/loadclass
 */

