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
 * @summary converted from VM Testbase nsk/jdi/ArrayReference/setValue/setvalue002.
 * VM Testbase keywords: [jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION
 *     This test checks the setValue(int, Value) method of ArrayReference
 *     interface of com.sun.jdi package.
 *     The method spec:
 *     public void setValue(int index, Value value) throws InvalidTypeException,
 *                                                         ClassNotLoadedException
 *     Replaces an array component with another value.
 *     Object values must be assignment compatible with the component type (This
 *     implies that the component type must be loaded through the enclosing class's
 *     class loader). Primitive values must be either assignment compatible with
 *     the component type or must be convertible to the component type without loss
 *     of information. See JLS section 5.2 for more information on assignment
 *     compatibility.
 *     Parameters: value - the new value
 *                 index - the index of the component to set
 *     Throws: java.lang.IndexOutOfBoundsException - if index is beyond the end of
 *                 this array.
 *             InvalidTypeException - if the type value is not compatible with the
 *                 declared type of array components.
 *             ClassNotLoadedException - if the array component type has not yet
 *                    been loaded through the appropriate class loader.
 *             ObjectCollectedException - if this object or the new value has been
 *                 garbage collected.
 *             VMMismatchException - if a Mirror argument and this object do not
 *                 belong to the same VirtualMachine.
 *     nsk/jdi/ArrayReference/setValue/setvalue002 checks assertion:
 *     public void setValue(int index, Value value)
 *     1. IndexOutOfBoundsException is thrown if index is out of bounds of this
 *        array. Array has components of primitive types only.
 *     Debuggee defines eight sample fields of primitive types. Also it defines
 *     a number of tested array fields which component type is primitive type only.
 *     Those arrays have different lengths.
 *     Debugger gets each sample field from debuggee by name and gets its
 *     PrimitiveValue. After that the test gets tested array fields by name, gets
 *     their values, casts to ArrayReference types. And then sets sample values
 *     to the correspondent tested arrays invoking the method
 *     setValues(int, Value). Index is from MIN_INDEX to -1 and from
 *     arrayRef.length() to MAX_INDEX, so IndexOutOfBoundsException is expected.
 * COMMENTS
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.ArrayReference.setValue.setvalue002
 *        nsk.jdi.ArrayReference.setValue.setvalue002a
 * @run main/othervm
 *      nsk.jdi.ArrayReference.setValue.setvalue002
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

