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
 * @summary converted from VM Testbase nsk/jvmti/SetJNIFunctionTable/setjniftab001.
 * VM Testbase keywords: [quick, jpda, jvmti, noras]
 * VM Testbase readme:
 * DESCRIPTION
 *     The test exercises the JVMTI function SetJNIFunctionTable().
 *     It checks the following spec assertion:
 *         Set the JNI function table in all current and future JNI
 *         environments. As a result, all future JNI calls are directed
 *         to the specified functions.
 *     The test works as follows. Upon redirection of the JNI function
 *     MonitorEnter(), the interception is verified:
 *         - inside a main thread (checking current JNI environment)
 *         - inside new threads attached after the redirection (checking
 *           future JNI environments)
 *         - inside the main thread after detaching and attaching back
 *           (checking future JNI environments)
 *    Finally, the original JNI function table is restored and verified
 *    inside the main thread and new threads.
 * COMMENTS
 *     The test has been changed due to the rfe 4946196. See also evaluation
 *     of the bug 4921979.
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm/native -agentlib:setjniftab001 nsk.jvmti.SetJNIFunctionTable.setjniftab001
 */

