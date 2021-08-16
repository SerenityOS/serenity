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
 * @summary converted from VM Testbase nsk/jdi/ArrayReference/getValues_ii/getvaluesii003.
 * VM Testbase keywords: [jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION
 *     This test checks the getValues(int, int) method of ArrayReference
 *     interface of com.sun.jdi package.
 *     The method spec:
 *     public List getValues(int index, int length)
 *     Returns a range of array components.
 *     Parameters: index - the index of the first component to retrieve
 *                 length - the number of components to retrieve, or -1 to
 *                 retrieve all components to the end of this array.
 *     Returns: a list of Value objects, one for each requested array component
 *              ordered by array index.
 *     Throws: java.lang.IndexOutOfBoundsException - if index + length is an
 *             index beyond the end of this array.
 *             ObjectCollectedException - if this object has been garbage
 *             collected.
 *     nsk/jdi/ArrayReference/getValues_ii/getvaluesii003 checks assertion:
 *     public Value getValues(int index, int length)
 *     1. IndexOutOfBoundsException is thrown, if the index is out of range of
 *        ArrayReference. Array has components of primitive types only.
 *     Debuggee defines a number of array fields which component type is primitive
 *     type only. Each array has at least one element.
 *     Debugger gets each field from debuggee by name, gets its value and casts it
 *     to ArrayReference. Then debugger tries to invoke the method getValues(j, 1)
 *     that gets one element forn index j. Index j is form MIN_INDEX to -1 and
 *     from last index to MAX_INDEX. For each index IndexOutOfBoundsException is
 *     expected.
 * COMMENTS
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.ArrayReference.getValues_ii.getvaluesii003
 *        nsk.jdi.ArrayReference.getValues_ii.getvaluesii003a
 * @run main/othervm
 *      nsk.jdi.ArrayReference.getValues_ii.getvaluesii003
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

