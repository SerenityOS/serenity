/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @summary converted from VM Testbase nsk/jdi/ArrayReference/setValues_l/setvaluesl002.
 * VM Testbase keywords: [jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION
 *     This test checks the setValues(List) method of ArrayReference interface of
 *     com.sun.jdi package.
 *     The method spec:
 *     public void setValues(List values) throws InvalidTypeException,
 *                                               ClassNotLoadedException
 *     Replaces all array components with other values. If the given list is
 *     larger is size than the array, the values at the end of the list are
 *     ignored.
 *     Object values must be assignment compatible with the element type (This
 *     implies that the component type must be loaded through the enclosing class's
 *     class loader). Primitive values must be either assignment compatible with
 *     the component type or must be convertible to the component type without loss
 *     of information. See JLS section 5.2 for more information on assignment
 *     compatibility.
 *     Parameters: values - a list of Value objects to be placed in this array
 *     Throws: InvalidTypeException - if any of the values is not compatible with
 *                 the declared type of array components.
 *             java.lang.IndexOutOfBoundsException - if the size of values is
 *                 larger than the length of this array.
 *             ClassNotLoadedException - if the array component type has not yet
 *                 been loaded through the appropriate class loader.
 *             ObjectCollectedException - if this object or any of the new values
 *                 has been garbage collected.
 *             VMMismatchException - if a Mirror argument and this object do not
 *                   belong to the same VirtualMachine.
 *     nsk/jdi/ArrayReference/setValue_l/setvaluesl002 checks assertion:
 *     public void setValues(List values)
 *     1. Replaces all array components with other values. Array has components of
 *        primitive types only. The list is larger than the array, so the values
 *        at the end of the list is ignored.
 *     Debuggee defines eight sample array fields. One for each primitive type.
 *     Also, it defines tested array fields, that have at least one more element
 *     then correspondent sample field.
 *     Debugger gets each sample field from debuggee by name and gets its
 *     Value, casts it to ArrayReference and gets list of its Values. After that
 *     the test gets tested array fields by name, gets their values, casts to
 *     ArrayReference types and invoke the method setValues(List) to set the
 *     sample list to that ArrayReference. The read list of tested array is larger
 *     than the array, so the values at the end of the list should be ignored.
 *     After that the test gets all Values of the array and checks them. Debugger
 *     determines component's type (by field's name), gets each element of the
 *     list, casts it to correspondent PrimitiveType and then gets its primitive
 *     value. Then the test compares returned and expected primitive values.
 * COMMENTS
 *     4448603: JDI spec: ArrayReference.setValues(List) has discrepancy
 *     Evaluation:
 *     There is indeed an inconsistency.  The safer of the two options should be
 *     chosen: ignore tail elements in Lists that are too long. This is also what
 *     the reference implementation does.
 *     4419982: JDI: two StackFrame methods return incorrect values for double
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.ArrayReference.setValues_l.setvaluesl002
 *        nsk.jdi.ArrayReference.setValues_l.setvaluesl002a
 * @run main/othervm
 *      nsk.jdi.ArrayReference.setValues_l.setvaluesl002
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

