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
 * @summary converted from VM Testbase nsk/jvmti/GetThreadGroupChildren/getthrdgrpchld001.
 * VM Testbase keywords: [quick, jpda, jvmti, noras]
 * VM Testbase readme:
 * DESCRIPTION
 *     This JVMTI test exercises JVMTI thread function GetThreadGroupChildren().
 *     This tests checks that GetThreadGroupChildren() returns expected number
 *     of groups and threads for each tested thread group:
 *         rootGroup       - thread group with 3 inner groups and no threads
 *         runningGroup    - inner thread group with only running threads
 *         notStartedGroup - inner thread group with only not started threads
 *         finishedGroup   - inner thread group with only finished threads
 *     At the beggining the test searches rootGroup in the groups tree
 *     starting from top level thread groups.
 * COMMENTS
 *     Fixed 5028815 bug.
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jvmti.GetThreadGroupChildren.getthrdgrpchld001
 * @run main/othervm/native
 *      -agentlib:getthrdgrpchld001=-waittime=5,threads=10
 *      nsk.jvmti.GetThreadGroupChildren.getthrdgrpchld001
 */

