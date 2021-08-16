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
 * @summary converted from VM Testbase nsk/jdi/ReferenceType/genericSignature/genericSignature001.
 * VM Testbase keywords: [quick, jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION
 *     nsk/jdi/ReferenceType/genericSignature/genericSignature001 test:
 *     The test for the ReferenceType.genericSignature() method.
 *     The test checks up ReferenceType.genericSignature() method
 *     for ArrayType and ClassType objects.
 *     The first: the test checks that genericSignature() method returns
 *     null for arrays of all primitive types.
 *     The second: the test checks that genericSignature() method returns
 *     null for arrays of reference types which have a ClassType as its
 *     type.
 *     The third: the test checks that genericSignature() method returns
 *     null for ClassType types which are not generic types.
 *     At last: the test checks that genericSignature() method returns
 *     a corresponding signature string for ClassType types which are
 *     generic types.
 * COMMENTS
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.ReferenceType.genericSignature.genericSignature001
 *        nsk.jdi.ReferenceType.genericSignature.genericSignature001a
 * @run main/othervm
 *      nsk.jdi.ReferenceType.genericSignature.genericSignature001
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

