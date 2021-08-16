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
 * @summary converted from VM Testbase nsk/jdi/LongValue/equals/equals002.
 * VM Testbase keywords: [jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION:
 *     The test for the boundary value of the parameters.
 *     The test checks up that the method
 *     com.sun.jdi.LongValue.equals(Object)
 *     correctly works for the boundary value of parameter, throws described
 *     exceptions and complies with its spec:
 *     public boolean equals(Object obj)
 *          Compares the specified Object with this LongValue for equality.
 *          Overrides:
 *              equals in class Object
 *          Returns:
 *              true if the Object is a LongValue and if applying "==" to the
 *              two mirrored primitives would evaluate to true; false otherwise.
 *     The test check up case when parameter has boundary values of primitive
 *     types. Also, case when parameter is <null> considered.
 *     No exceptions are expected.
 *     In case of error the test produces the return value 97 and a corresponding
 *     error message(s). Otherwise, the test is passed and produces the return
 *     value 95 and no message.
 * COMMENTS:
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.LongValue.equals.equals002
 *        nsk.jdi.LongValue.equals.equals002a
 * @run main/othervm
 *      nsk.jdi.LongValue.equals.equals002
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

