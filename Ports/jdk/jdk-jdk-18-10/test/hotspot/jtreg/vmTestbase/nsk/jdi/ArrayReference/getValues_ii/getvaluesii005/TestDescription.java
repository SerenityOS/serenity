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
 * @summary converted from VM Testbase nsk/jdi/ArrayReference/getValues_ii/getvaluesii005.
 * VM Testbase keywords: [jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION:
 *     The test for the boundary value of the parameters.
 *     The test checks up that the method
 *     com.sun.jdi.ArrayReference.getValues(int, int)
 *     correctly works for the boundary value of parameter and complies with its
 *     spec:
 *     public List getValues(int index,
 *                           int length)
 *          Returns a range of array components.
 *          Parameters:
 *              index - the index of the first component to retrieve
 *              length - the number of components to retrieve, or -1 to retrieve
 *              all components to the end of this array.
 *          Returns:
 *              a list of Value objects, one for each requested array component
 *              ordered by array index. When there are no elements in the specified
 *              range (e.g. length is zero) an empty list is returned
 *          Throws:
 *              IndexOutOfBoundsException - if the range specified with index and
 *              length is not within the range of the array, that is, if either
 *              of the following are true:
 *                  index < 0
 *                  index > length()
 *              or if length != -1 and either of the following are true:
 *                  length < 0
 *                  index + length >  length()
 *     The test cases include static and instance fields of int and Object types,
 *     which are one- and two- dimensional arrays (non-initialized arrays are
 *     considered too). Values of the method parameter are presented as static
 *     two-dimensional array into debuger, which includes the combinations of
 *     Integer.MAX_VALUE + 1, -1, 0, 1, Integer.MAX_VALUE.
 *     Expected results:
 *      - when index is 0 and length is -1, size of returned list has to be array
 *     length;
 *      - when index is 0 and length is 0, size if returned list has to be zero;
 *      - otherwise, IndexOutOfBoundsException is expected.
 *     The test works as follows:
 *     The debugger program - nsk.jdi.ArrayReference.getValues_ii.getvaluesii005;
 *     the debuggee program - nsk.jdi.ArrayReference.getValues_ii.getvaluesii005a.
 *     Using nsk.jdi.share classes, the debugger gets the debuggee running on
 *     another JavaVM, establishes a pipe with the debuggee program, and then
 *     send to the programm commands, to which the debuggee replies via the pipe.
 *     Upon getting reply, the debugger requests fields of checked object
 *     and trys to get a range of arry items correspondence with the test cases
 *     above.
 *     In case of error the test produces the return value 97 and a corresponding
 *     error message(s). Otherwise, the test is passed and produces the return
 *     value 95 and no message.
 * COMMENTS:
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.ArrayReference.getValues_ii.getvaluesii005
 *        nsk.jdi.ArrayReference.getValues_ii.getvaluesii005a
 * @run main/othervm
 *      nsk.jdi.ArrayReference.getValues_ii.getvaluesii005
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

