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
 * @summary converted from VM Testbase nsk/jvmti/unit/GetAllStackTraces/getallstktr001.
 * VM Testbase keywords: [quick, jpda, jvmti, noras, feature_jdk6_jpda, vm6]
 * VM Testbase readme:
 * DESCRIPTION
 *   This is a smoke test for the JVMTI functions:
 *     GetAllStackTraces() and GetThreadListStackTraces().
 *   This is also a unit test for the bug 6221510:
 *     GetAllStackTraces returns a JNI refs created in the wrong handle.
 *   The test checks that the JVMTI functions GetAllStackTraces and
 *   GetThreadListStackTraces return equal stack traces for the same
 *   threads states. It uses the VM flag -Xcheck:jni in order to ensure
 *   that correct handle blocks are used in the JVMTI functions.
 * COMMENTS
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm/native
 *      -Xcheck:jni
 *      -agentlib:getallstktr001=-waittime=5
 *      nsk.jvmti.unit.GetAllStackTraces.getallstktr001
 */

