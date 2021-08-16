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
 * @summary converted from VM Testbase nsk/jdi/ArrayReference/getValue/getvalue003.
 * VM Testbase keywords: [jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION:
 *     The test for the boundary value of the parameters.
 *     The test checks up that the method
 *     com.sun.jdi.ArrayReference.getValue(int)
 *     correctly works for the boundary value of parameter complies with its spec:
 *     public Value getValue(int index)
 *          Returns an array component value.
 *          Parameters:
 *              index - the index of the component to retrieve
 *          Returns:
 *              the Value at the given index.
 *          Throws:
 *              IndexOutOfBoundsException - if index is outside the range of
 *              this array, that is, if either of the following are true:
 *                  index < 0
 *                  index >= length()
 *     The test cases include static and instance fields of int and Object types,
 *     which are one- and two- dimensional arrays. Values of the method parameter
 *     are <length of array> + 1, Integer.MAX_VALUE and Integer.MAX_VALUE + 1.
 *     IndexOutOfBoundsException is expected or, when array have no initalization,
 *     <null> value is expected.
 *     The test works as follows:
 *     The debugger program - nsk.jdi.ArrayReference.getValue.getvalue003;
 *     the debuggee program - nsk.jdi.ArrayReference.getValue.getvalue003a.
 *     Using nsk.jdi.share classes, the debugger gets the debuggee running
 *     on another JavaVM, establishes a pipe with the debuggee program,
 *     and then send to the programm commands, to which the debuggee replies
 *     via the pipe.
 *     Upon getting reply, the debugger requests fields of checked object
 *     and trys to read array values or an item of arrays correspondence with
 *     the test cases above.
 *     In case of error the test produces the return value 97 and a corresponding
 *     error message(s). Otherwise, the test is passed and produces the return
 *     value 95 and no message.
 * COMMENTS:
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.ArrayReference.getValue.getvalue003
 *        nsk.jdi.ArrayReference.getValue.getvalue003a
 * @run main/othervm
 *      nsk.jdi.ArrayReference.getValue.getvalue003
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

