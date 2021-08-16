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
 * @summary converted from VM Testbase nsk/jdi/ArrayReference/getValue/getvalue001.
 * VM Testbase keywords: [jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION
 *     This test checks the getValue(int) method of ArrayReference interface of
 *     com.sun.jdi package.
 *     The method spec:
 *     public Value getValue(int index)
 *     Returns an array component value.
 *     Parameters: index - the index of the component to retrieve
 *     Returns: the Value at the given index.
 *     Throws: java.lang.IndexOutOfBoundsException - if the index is beyond the
 *                 end of this array.
 *             ObjectCollectedException - if this object has been garbage
 *                 collected.
 *     nsk/jdi/ArrayReference/getValue/getvalue001 checks assertion:
 *     public Value getValue(int index)
 *     1. Returns the Value at the given index. Array has components of primitive
 *        types only, index is in array bounds.
 *     Debuggee defines a number of array fields with components of primitive types
 *     only.
 *     Debugger gets each field from debuggee by name, gets field's value and
 *     casts it to ArrayReference. Then debugger determines field's type (by
 *     field's name), gets each element of the array invoking the method
 *     getValue(int). Returned Value is casted to correspondent PrimitiveType and
 *     the test gets its primitive value. This value is compared with expected.
 * COMMENTS
 *     4419982: JDI: two StackFrame methods return incorrect values for double
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.ArrayReference.getValue.getvalue001
 *        nsk.jdi.ArrayReference.getValue.getvalue001a
 * @run main/othervm
 *      nsk.jdi.ArrayReference.getValue.getvalue001
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

