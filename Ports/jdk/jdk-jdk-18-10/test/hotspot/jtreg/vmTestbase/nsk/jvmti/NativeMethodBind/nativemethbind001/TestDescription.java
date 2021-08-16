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
 * @summary converted from VM Testbase nsk/jvmti/NativeMethodBind/nativemethbind001.
 * VM Testbase keywords: [quick, jpda, jvmti, noras]
 * VM Testbase readme:
 * DESCRIPTION
 *     This test exercises the JVMTI event NativeMethodBind.
 *     It verifies that the event will be properly sent:
 *         - for the native method called for the first time
 *         - when the JNI RegisterNatives() is called.
 *     The test works as follows. The java part invokes the native method
 *     'nativeMethod()' twice. At the first time that method registers
 *     another native method 'anotherNativeMethod()' for the dummy class
 *     'TestedClass'. Registration is made through the JNI RegisterNatives()
 *     call. Being invoked at the second time, the nativeMethod() just returns.
 *     In accordance with the spec, it is expected that the NativeMethodBind
 *     will be generated only one time for the nativeMethod(), and only one
 *     time for the anotherNativeMethod().
 * COMMENTS
 *     The test has been fixed due to the bug 4967116.
 *     Fixed the 4995867 bug.
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm/native
 *      -agentlib:nativemethbind001=-waittime=5
 *      nsk.jvmti.NativeMethodBind.nativemethbind001
 */

