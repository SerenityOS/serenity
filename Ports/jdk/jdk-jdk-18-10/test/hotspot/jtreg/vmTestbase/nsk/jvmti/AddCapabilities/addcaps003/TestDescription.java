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
 * @summary converted from VM Testbase nsk/jvmti/AddCapabilities/addcaps003.
 * VM Testbase keywords: [quick, jpda, jvmti, noras, vm6]
 * VM Testbase readme:
 * DESCRIPTION
 *     Bug ID: 6316807
 *     This JVMTI test exercises JVMTI GetCapabilities() function with the following flags:
 *     -XX:+PrintInterpreter -XX:+TaggedStackInterpreter
 *     The bug's evaluation field says:
 *     "The interpreter allocates a fixed size buffer in order to generate code
 *     for itself.  Product code can fail in the interpreter with the
 *     guarantee("not enough space for interpreter generation");
 *     when this code space is not sufficient.  New code has been added to the
 *     interpreter for TaggedStackInterpreter and for JVMTI which can increase the
 *     size needed by the interpreter code buffer.  The interpreter code size is
 *     hardcoded and it's not typical for people to change the size when one
 *     adds new code to the interpreter, so it can run out.  Also the intepreter
 *     code size in NOT_PRODUCT is multiplied by 4 so the code size doesn't typically
 *     run out in debug mode, which is the mode we test."
 * COMMENTS
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm/native
 *      -XX:+UnlockDiagnosticVMOptions
 *      -XX:+PrintInterpreter
 *      -XX:+IgnoreUnrecognizedVMOptions
 *      -XX:+TaggedStackInterpreter
 *      -agentlib:addcaps003=-waittime=5
 *      nsk.jvmti.AddCapabilities.addcaps003
 */

