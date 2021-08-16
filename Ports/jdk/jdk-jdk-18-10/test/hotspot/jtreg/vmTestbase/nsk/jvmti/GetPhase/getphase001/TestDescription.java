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
 * @summary converted from VM Testbase nsk/jvmti/GetPhase/getphase001.
 * VM Testbase keywords: [quick, jpda, jvmti, onload_only_logic, noras]
 * VM Testbase readme:
 * DESCRIPTION
 *     The test exercises JVMTI function GetPhase.
 *     The test checks if the function returns the expected values:
 *         JVMTI_PHASE_ONLOAD      while in the Agent_OnLoad function
 *         JVMTI_PHASE_PRIMORDIAL  between return from Agent_OnLoad and
 *                                 the VMStart event
 *         JVMTI_PHASE_START       when the VMStart event is sent and until
 *                                 the VMInit event
 *         JVMTI_PHASE_LIVE        when the VMInit event is sent and until
 *                                 the VMDeath event returns
 *         JVMTI_PHASE_DEAD        after the VMDeath event returns or after
 *                                 start-up failure
 * COMMENTS
 *     Fixed the 4995867 bug.
 *     Fixed the 5005389 bug.
 *     Modified due to fix of the rfe
 *     5010823 TEST_RFE: some JVMTI tests use the replaced capability
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm/native -agentlib:getphase001=-waittime=5 nsk.jvmti.GetPhase.getphase001
 */

