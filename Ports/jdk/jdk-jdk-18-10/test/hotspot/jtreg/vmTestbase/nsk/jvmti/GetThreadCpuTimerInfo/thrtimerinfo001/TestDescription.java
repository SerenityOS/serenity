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
 * @summary converted from VM Testbase nsk/jvmti/GetThreadCpuTimerInfo/thrtimerinfo001.
 * VM Testbase keywords: [quick, jpda, jvmti, onload_only_logic, noras]
 * VM Testbase readme:
 * DESCRIPTION
 *     This JVMTI test exercises JVMTI thread function GetThreadCpuTimerInfo().
 *     This test checks that GetThreadCpuTimerInfo() returns info structure
 *     with correct values and produce no errors. And these values are not
 *     changed between calls.
 *     The following checks are performed for GetThreadCpuTimerInfo():
 *         - in VM_INIT event callback on live phase
 *         - in agent thread on live phase
 *         - in THREAD_START event on live phase
 *         - in THREAD_END event on live phase
 *         - in VM_DEATH callback on live phase
 *     Each check passes if GetThreadCpuTimerInfo() returns not negative
 *     or zero value in max_value field, and values of all fields are equal
 *     to initial values.
 *     If all checks are successful, then test pases with exit status 95.
 *     If any check fails or other error occurs, then agent sets FAIL status
 *     and the test fails with exit status 97.
 *     If the last check (in VM_DEATH callback) fails, then C-language exit()
 *     function is used to force VM exit with fail status 97.
 * COMMENTS
 *     Fixed the 5006885 bug.
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm/native
 *      -agentlib:thrtimerinfo001=-waittime=5
 *      nsk.jvmti.GetThreadCpuTimerInfo.thrtimerinfo001
 */

