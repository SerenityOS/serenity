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
 * @bug 6318850
 *
 * @summary converted from VM Testbase nsk/jvmti/RedefineClasses/redefclass031.
 * VM Testbase keywords: [quick, jpda, jvmti, regression, noras, redefine]
 * VM Testbase readme:
 * DESCRIPTION
 *     Bug ID: 6318850
 *     This test checks that neither NativeMethodBind event is generated
 *     by JVMTI function RedefineClasses() nor JVM crashes if the
 *     redefined class has a native function.
 *     The test creates an instance of tested class redefclass031r with
 *     a native function.  Then the test calls the native method to force
 *     JVM to bind it and then redefines the class redefclass031r
 *     (the bytes are read from the same class file, so actually
 *     the new and old classes are identical)
 * COMMENTS
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jvmti.RedefineClasses.redefclass031r
 * @run main/othervm/native -agentlib:redefclass031 nsk.jvmti.RedefineClasses.redefclass031
 */

