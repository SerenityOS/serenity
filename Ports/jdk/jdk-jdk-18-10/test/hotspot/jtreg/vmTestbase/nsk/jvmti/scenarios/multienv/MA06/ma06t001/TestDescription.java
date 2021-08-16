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
 * @summary converted from VM Testbase nsk/jvmti/scenarios/multienv/MA06/ma06t001.
 * VM Testbase keywords: [quick, jpda, jvmti, noras, redefine]
 * VM Testbase readme:
 * DESCRIPTION
 *     This test is for MA06 scenario of "multiple environments support".
 *     VM starts with two different agents, both possessed capability
 *     can_redefine_classes and had callbacks for ClassFileLoadHook
 *     event been set and enabled. Then the test:
 *       - Redefines class C in agent A1.
 *       - Checks that the event for C is received in agent A2.
 *       - Checks that redefinition of C is effective in A2.
 *       - Redefines class C in A2.
 *       - Checks that the event for C is received in A1.
 *       - Checks that last redefinition of C is effective in A1.
 * COMMENTS
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm/native
 *      -agentlib:ma06t001=-waittime=5
 *      -agentlib:ma06t001a=-waittime=5
 *      nsk.jvmti.scenarios.multienv.MA06.ma06t001
 */

