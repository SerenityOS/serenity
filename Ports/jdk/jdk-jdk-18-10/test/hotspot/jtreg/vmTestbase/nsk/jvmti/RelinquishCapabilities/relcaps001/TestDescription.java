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
 * @summary converted from VM Testbase nsk/jvmti/RelinquishCapabilities/relcaps001.
 * VM Testbase keywords: [quick, jpda, jvmti, noras]
 * VM Testbase readme:
 * DESCRIPTION
 *     This JVMTI test exercises JVMTI thread function RelinquishCapabilities().
 *     This test checks that RelinquishCapabilities() removes all potentially
 *     available capabilities added by AddCapabilities in Agent_OnLoad().
 *     This is checked by GetCapabilities either on OnLoad and live phases.
 *     In verbose mode the test prints obtained capabilities either as raw bits
 *     and as values of known capabilities.
 *     The following checks are performed for RelinquishCapabilities():
 *         - in JVM_OnLoad() on OnLoad phase
 *     The following checks are performed for GetCapabilities():
 *         - in JVM_OnLoad() on OnLoad phase
 *         - in VM_INIT event on live phase
 *         - in agent thread on live phase
 *         - in VM_DEATH callback on live phase
 *     The test passes if RelinquishCapabilities() returns no error code and
 *     GetCapabilities() returns structure with no capabilities set
 *     in all checks.
 *     If the last check (in VM_DEATH callback) fails, then C-language exit()
 *     function is used to force VM exit with fail status 97.
 * COMMENTS
 *     Modified due to fix of the rfe
 *     5010823 TEST_RFE: some JVMTI tests use the replaced capability
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm/native
 *      -agentlib:relcaps001=-waittime=5
 *      nsk.jvmti.RelinquishCapabilities.relcaps001
 */

