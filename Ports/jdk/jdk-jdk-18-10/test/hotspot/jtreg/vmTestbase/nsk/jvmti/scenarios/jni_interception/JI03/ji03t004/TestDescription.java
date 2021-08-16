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
 * @summary converted from VM Testbase nsk/jvmti/scenarios/jni_interception/JI03/ji03t004.
 * VM Testbase keywords: [jpda, jvmti, noras]
 * VM Testbase readme:
 * DESCRIPTION
 *     The test exercises the JVMTI target area "JNI Function Interception".
 *     It implements the following scenario:
 *         Check that functions in JNI function table can be redirected. Then
 *         restore original function and verify that original function works.
 *     The tested JVMTI functions Set/GetJNIFunctionTable are verified with
 *     the Object Operation functions AllocObject, NewObjectV.
 * COMMENTS
 *     Updating the test:
 *     - provide correct package name
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm/native -agentlib:ji03t004 nsk.jvmti.scenarios.jni_interception.JI03.ji03t004
 */

