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
 * @summary converted from VM Testbase nsk/jdi/ArrayReference/setValue/setvalue003.
 * VM Testbase keywords: [jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION:
 *     The test for the boundary value of the parameters.
 *     The test checks up that the method
 *     com.sun.jdi.ArrayReference.setValue(int, Value)
 *     correctly works for the boundary value of parameter and complies with its
 *     spec:
 *     public void setValue(int index, Value value)
 *                   throws InvalidTypeException,
 *                          ClassNotLoadedException
 *          Replaces an array component with another value.
 *          Object values must be assignment compatible with the component type
 *          (This implies that the component type must be loaded through the
 *          declaring class's class loader). Primitive values must be either
 *          assignment compatible with the component type or must be
 *          convertible to the component type without loss of information. See
 *          JLS section 5.2 for more information on assignment compatibility.
 *          Parameters:
 *              value - the new value
 *              index - the index of the component to set
 *          Throws:
 *              IndexOutOfBoundsException - if index is outside the range of this
 *              array, that is, if either of the following are true:
 *                  index < 0
 *                  index >= length()
 *              InvalidTypeException - if the type of value is not compatible with
 *              the declared type of array components.
 *              ClassNotLoadedException - if the array component type has not yet
 *              been loaded through the appropriate class loader.
 *     The test cases include instance fields of the primitive types, which are
 *     one-dimensional arrays. Possible values of <Value> parameter of the method
 *     are presented as static array at the debugee part for each primitive types.
 *     These values include boundary values of each primitive types. Possible
 *     values of <Index> parameter are presented as static array at the debuger
 *     part.
 *     Expected results:
 *       for every values of <Value> parameter the method has to work without any
 *       errors, except of cases when either index < 0 or index >= length().
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
 * @build nsk.jdi.ArrayReference.setValue.setvalue003
 *        nsk.jdi.ArrayReference.setValue.setvalue003a
 * @run main/othervm
 *      nsk.jdi.ArrayReference.setValue.setvalue003
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

