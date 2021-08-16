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
 * @summary converted from VM Testbase nsk/jvmti/scenarios/general_functions/GF06/gf06t001.
 * VM Testbase keywords: [quick, jpda, jvmti, onload_only_logic, noras]
 * VM Testbase readme:
 * DESCRIPTION
 *     This test implements GF06 scenario of test paln for General
 *     Functions:
 *         Check that a character string saved with
 *         SetEnvironmentLocalStorage in first JVMTI environment,
 *         is not returned by GetEnvironmentLocalStorage in second
 *         environment. The test must check cases of saving and
 *         retrieving in different phases.
 *     At first, the test creates first JVMTI environment and
 *     saves local storage for this environment in Onload phase.
 *     Then, the test creates second environment and checks that
 *     GetEnvironmentLocalStorage() returns NULL pointer for
 *     second environment.
 *     The checks are performed for GetEnvironmentLocalStorage():
 *         - in JVM_OnLoad() on OnLoad phase
 *         - in VM_INIT event callback on live phase
 *         - in agent thread on live phase
 *         - in VM_DEATH callback on live phase
 * COMMENTS
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jvmti.scenarios.general_functions.GF06.gf06t001
 * @run main/othervm/native
 *      -agentlib:gf06t001=-waittime=5,-verbose
 *      nsk.jvmti.scenarios.general_functions.GF06.gf06t001
 */

