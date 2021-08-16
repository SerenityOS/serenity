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
 * @summary converted from VM Testbase nsk/jdi/TypeComponent/genericSignature/genericSignature002.
 * VM Testbase keywords: [quick, jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION
 *     nsk/jdi/TypeComponent/genericSignature/genericSignature002 test:
 *     The test for the TypeComponent.genericSignature() method.
 *     The test checks up TypeComponent.genericSignature() method
 *     for Method objects.
 *     The first: the test checks that genericSignature() method returns
 *     null for methods which have not generic signature. The methods with
 *     different sets of arguments and with different returned types are
 *     used.
 *     The second: the test checks that genericSignature() method returns
 *     a corresponding signature string for methods which have generic
 *     signature. The methods with different sets of generic types
 *     arguments and with different returned generic types are used.
 * COMMENTS
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.TypeComponent.genericSignature.genericSignature002
 *        nsk.jdi.TypeComponent.genericSignature.genericSignature002a
 * @run main/othervm
 *      nsk.jdi.TypeComponent.genericSignature.genericSignature002
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

