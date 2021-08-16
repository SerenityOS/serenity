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
 * @summary converted from VM Testbase nsk/jvmti/scenarios/general_functions/GF08/gf08t003.
 * VM Testbase keywords: [quick, jpda, jvmti, onload_only_logic, noras]
 * VM Testbase readme:
 * DESCRIPTION
 *    This test implements GF08 scenario of test plan for General
 *    Functions:
 *        Do the following:
 *        Run simple java apllication with VM option '-verbose:jni'.
 *        Run the same application with JVMTI agent. The agent should
 *        set JVMTI_VERBOSE_JNI with SetVerboseFlag. Check that outputs
 *        in stderr in both runs are equal.
 *    The test agent has a special input parameter 'setVerboseMode'.
 *    When VM runs the test class 'gf08t003' with
 *      '-agentlib:gf08t003=setVerboseMode=yes'
 *    option, then the agent calls SetVerboseFlag with
 *    JVMTI_VERBOSE_JNI flag in Onload phase.
 *    The test's script wrapper runs the 'gf08t003' class twice.
 *    First time, with "setVerboseMode=yes" agent mode. Second
 *    time, with "setVerboseMode=no" agent mode and with
 *    "-verbose:jni" VM option. In both cases the output is
 *    searched for 'Registering JNI native method' string.
 *    The test fails if this string is not found in the output.
 * COMMENTS
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jvmti.scenarios.general_functions.GF08.gf08t003
 *        nsk.jvmti.scenarios.general_functions.GF08.gf08t
 * @run main/othervm/native
 *      nsk.jvmti.scenarios.general_functions.GF08.gf08t
 *      gf08t003
 *      nsk.jvmti.scenarios.general_functions.GF08.gf08t003
 *      jni
 *      Registering JNI native method
 */

