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
 * @summary converted from VM Testbase nsk/jdi/TypeComponent/isProtected/isprotected002.
 * VM Testbase keywords: [jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION
 *   This test checks the isProtected() method of Accessible interface
 *   for TypeComponent sub-interface of com.sun.jdi package.
 *   The method spec:
 *   public boolean isProtected()
 *   Determines if this object mirrors a protected item. For ArrayType,
 *   the return value depends on the array component type. For primitive arrays
 *   the return value is always false. For object arrays, the return value is
 *   the same as would be returned for the component type. For primitive
 *   classes, such as Integer.TYPE, the return value is always false.
 *   Returns:
 *         true for items with private access; false otherwise.
 *   nsk/jdi/TypeComponent/isProtected/isprotected002 checks assertions:
 *   public boolean isProtected()
 *   1. Returns true if the method was declared protected.
 *   2. Returns false otherwise.
 *  Debugger gets each method from debuggee calling by name and then checks
 *  if method isProtected() returns an expected value.
 * COMMENTS
 *   The test is aimed to increase jdi source code coverage and checks
 *   the code which was not yet covered by previous tests for isProtected method.
 *   The coverage analysis was done for jdk1.4.0-b92 build.
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.TypeComponent.isProtected.isprotected002
 *        nsk.jdi.TypeComponent.isProtected.isprotected002a
 * @run main/othervm
 *      nsk.jdi.TypeComponent.isProtected.isprotected002
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

