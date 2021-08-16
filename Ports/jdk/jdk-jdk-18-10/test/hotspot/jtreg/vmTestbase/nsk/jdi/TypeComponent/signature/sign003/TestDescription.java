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
 * @summary converted from VM Testbase nsk/jdi/TypeComponent/signature/sign003.
 * VM Testbase keywords: [quick, jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION
 *     This test checks the signature() method of TypeComponent interface of
 *     com.sun.jdi package.
 *     The method spec:
 *     public java.lang.String signature()
 *     Gets the JNI-style signature for this type component. The signature is
 *     encoded type information as defined in the JNI documentation. It is a
 *     convenient, compact format for for manipulating type information
 *     internally, not necessarily for display to an end user. See
 *     Field.typeName() and Method.returnTypeName() for ways to help get a more
 *     readable representation of the type.
 *     Returns: a string containing the signature
 *     See Also: Type Signatures
 *     nsk/jdi/TypeComponent/signature/signature003 checks assertions:
 *     public java.lang.String signature()
 *     1. Gets the JNI-style signature for a constructor.
 *     2. Returns not null and not empty string for a static initializer.
 *     Debugger gets all  method from debuggee calling by name and then checks
 *     if signature() returns the expected value for the method.
 *     Debugger gets all methods from debuggee, if a method is constructor test
 *     checks that signature() returns expected value for the constructor. If a
 *     method is static initializer test checks that signature() returns not null
 *     and not empty string.
 * COMMENTS
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.TypeComponent.signature.sign003
 *        nsk.jdi.TypeComponent.signature.sign003a
 * @run main/othervm
 *      nsk.jdi.TypeComponent.signature.sign003
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

