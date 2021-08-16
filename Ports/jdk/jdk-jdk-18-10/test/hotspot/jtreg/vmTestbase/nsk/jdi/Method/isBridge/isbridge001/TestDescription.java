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
 * @summary converted from VM Testbase nsk/jdi/Method/isBridge/isbridge001.
 * VM Testbase keywords: [quick, jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION
 *     This test exercises the JDI method
 *         com.sun.jdi.Method.isBridge()
 *     It verifies that 'bridge methods' generated during translation of
 *     generic classes are determinated properly by the method.
 *     Here are the rules from the spec "Adding Generics to the Java
 *     Programming Language: Public Draft Specification, Version 2.0"
 *     followed by the test:
 *         6.2 Translation of Methods
 *         ...
 *         - If C.m is directly overridden by a method D.m in D, and the
 *           erasure of the return type or argument types of D.m differs from
 *           the erasure of the corresponding types in C.m, a bridge method
 *           needs to be generated.
 *         - A bridge method also needs to be generated if C.m is not
 *           directly overridden in D, unless C.m is abstract.
 *     The test works as follows. Debuggee contains several dummy superclasses
 *     and subclasses. Some of their methods fall under the rules above.
 *     Debugger verifies that the JDI Method.isBridge() returns true for all
 *     generated bridge methods and false for the non-bridge ones.
 *     The list of the class methods is obtained through the JDI
 *     ReferenceType.methods() which must return each method declared directly
 *     in this type including any synthetic methods created by the compiler.
 * COMMENTS
 *      Fixed bug 5083386: isbridge001c and isbridge001cc classes and
 *      the corresponding tests were removed
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.Method.isBridge.isbridge001
 *        nsk.jdi.Method.isBridge.isbridge001t
 * @run main/othervm
 *      nsk.jdi.Method.isBridge.isbridge001
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

