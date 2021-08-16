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
 * @summary converted from VM Testbase nsk/jvmti/scenarios/jni_interception/JI05/ji05t001.
 * VM Testbase keywords: [quick, jpda, jvmti, onload_only_logic, noras]
 * VM Testbase readme:
 * DESCRIPTION
 *     The test exercises the JVMTI target area "JNI Function Interception".
 *     It implements the following scenario:
 *         Let agent A of first JVMTI environment redirects a JNI function,
 *         then agent B of second JVMTI environment redirects the same
 *         function. Check that last redirection takes effect in agent A.
 *     The test works as follows. On phase JVM_OnLoad event VMInit is
 *     enabled. Upon receiving the event, two separate agent threads are
 *     started, each of them with its own JVMTI environment. The agent A
 *     running in one JVMTI environment redirects the JNI function
 *     GetVersion(). Then that redirection is checked in the agent B
 *     running in another JVMTI environment, and vise versa for redirection
 *     made by the agent B.
 * COMMENTS
 *     Updating the test:
 *     - provide correct package name
 *     - add thread argument to VMInit callbacks according to new JVMTI spec 0.2.90
 *     The test has been fixed due to the bug 4981625.
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm/native
 *      -agentlib:ji05t001=-waittime=5
 *      nsk.jvmti.scenarios.jni_interception.JI05.ji05t001
 */

