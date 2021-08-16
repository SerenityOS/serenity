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
 * @summary converted from VM Testbase nsk/jdi/ArrayReference/setValues_l/setvaluesl003.
 * VM Testbase keywords: [jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION:
 *     The test for the boundary value of the parameters.
 *     The test checks up that the method
 *     com.sun.jdi.ArrayReference.setValue(int, Value)
 *     correctly works for the boundary value of parameter and complies with its
 *     spec:
 *     public void setValues(List values)
 *                    throws InvalidTypeException,
 *                           ClassNotLoadedException
 *          Replaces all array components with other values. If the given list is
 *          larger in size than the array, the values at the end of the list are
 *          ignored.
 *          Object values must be assignment compatible with the element type
 *          (This implies that the component type must be loaded through the
 *          enclosing class's class loader). Primitive values must be either
 *          assignment compatible with the component type or must be convertible
 *          to the component type without loss of information. See JLS section 5.2
 *          for more information on assignment compatibility.
 *          Parameters:
 *              values - a list of Value objects to be placed in this array.
 *              If values.size() is less that the length of the array, the first
 *              values.size() elements are set.
 *          Throws:
 *              InvalidTypeException - if any of the new values is not compatible
 *              with the declared type of array components.
 *              ClassNotLoadedException - if the array component type has not yet
 *              been loaded through the appropriate class loader.
 *     The test cases include instance fields of the primitive types, which are
 *     one-dimensional arrays. Possible values of <Values> parameter of the method
 *     are generated from debugee's arrays, which contains boundary values of
 *     every primitive type.
 *     Every tested array of primitive type is checked in several steps:
 *         1. List parameter consists of values of the same primitive types.
 *            InvalidTypeException is expected, when primitive values are neither
 *            assignment compatible with the component type nor are convertible
 *            to the component type without loss of information.
 *         2. List is generated from primitive values passed in first step.
 *            In this case no exceptions are expected.
 *         3. List has null value. In this case NullPointerException is
 *            excpected.
 *     The test works as follows:
 *     The debugger program - nsk.jdi.ArrayReference.setValue.setvalue003;
 *     the debuggee program - nsk.jdi.ArrayReference.setValue.setvalue003a.
 *     Using nsk.jdi.share classes, the debugger gets the debuggee running on
 *     another JavaVM, establishes a pipe with the debuggee program, and then
 *     send to the programm commands, to which the debuggee replies via the pipe.
 *     Upon getting reply, the debugger requests fields of checked object
 *     and trys to set a value of arry items correspondence with the test cases
 *     above.
 *     In case of error the test produces the return value 97 and a corresponding
 *     error message(s). Otherwise, the test is passed and produces the return
 *     value 95 and no message.
 * COMMENTS:
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.ArrayReference.setValues_l.setvaluesl003
 *        nsk.jdi.ArrayReference.setValues_l.setvaluesl003a
 * @run main/othervm
 *      nsk.jdi.ArrayReference.setValues_l.setvaluesl003
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

