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
 * @summary converted from VM Testbase nsk/jdi/ArrayReference/setValues_ilii/setvaluesilii004.
 * VM Testbase keywords: [jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION
 *     This test checks the setValues(int, List, int, int) method of ArrayReference
 *     interface of com.sun.jdi package.
 *     The method spec:
 *     public void setValues(int index, List values, int srcIndex, int length)
 *                     throws InvalidTypeException, ClassNotLoadedException
 *     Replaces a range of array components with other values.
 *     Object values must be assignment compatible with the component type (This
 *     implies that the component type must be loaded through the enclosing class's
 *     class loader). Primitive values must be either assignment compatible with
 *     the component type or must be convertible to the component type without loss
 *     of information. See JLS section 5.2 for more information on assignment
 *     compatibility.
 *     Parameters: index - the index of the first component to set.
 *                 values - a list of Value objects to be placed in this array.
 *                 srcIndex - the index of the first source value to to use.
 *                 length - the number of components to set, or -1 to set all
 *                     components to the end of this array.
 *     Throws: InvalidTypeException - if any element of values is not compatible
 *                 with the declared type of array components.
 *             java.lang.IndexOutOfBoundsException - if srcIndex + length is beyond
 *                 the end of this array or if values is smaller inside than the
 *                 given range.
 *             ObjectCollectedException - if this object or any of the new values
 *                 has been garbage collected.
 *             VMMismatchException - if a Mirror argument and this object do not
 *                 belong to the same VirtualMachine.
 *     nsk/jdi/ArrayReference/setValues_ilii/setvaluesilii004 checks assertion:
 *     public void setValues(int index, List values, int srcIndex, int length)
 *     1. IndexOutOfBoundsException is thrown if srcIndex + length is beyond the
 *        end of this array. Array has components of primitive types only.
 *     Debuggee defines eight sample array fields. One for each primitive type.
 *     Also, it defines tested array fields. Sample and correspondent tested
 *     arrays have the same lengths.
 *     Debugger gets each sample field from debuggee by name and gets its
 *     Value, casts it to ArrayReference and gets list of its Values. After that
 *     the test gets tested array fields by name, gets their values, casts to
 *     ArrayReference types. For i field the method
 *     setValues(0, List, 1, List.size()) is invoked to set all elements of sample
 *     array from index 0 to ArrayReference from index 1. Since srcIndex + length
 *     is beyond the end of this array, IndexOutOfBoundsException should be thrown.
 * COMMENTS
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.ArrayReference.setValues_ilii.setvaluesilii004
 *        nsk.jdi.ArrayReference.setValues_ilii.setvaluesilii004a
 * @run main/othervm
 *      nsk.jdi.ArrayReference.setValues_ilii.setvaluesilii004
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

