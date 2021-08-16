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
 * @summary converted from VM Testbase nsk/jdi/ArrayReference/getValues/getvalues003.
 * VM Testbase keywords: [jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION:
 *     The test for the boundary values.
 *     The test checks up that the method
 *     com.sun.jdi.ArrayReference.getValues()
 *     correctly works for arrays with zero size complies with its spec:
 *     public List getValues()
 *         Returns all of the components in this array.
 *         Returns:
 *             a list of Value objects, one for each array component ordered by
 *             array index. For zero length arrays, an empty list is returned.
 *     The test cases include static and instance fields of int and Object types,
 *     which are one-dimensional arrays of zero-sizes. An empty list is expected.
 *     The test works as follows:
 *     The debugger program - nsk.jdi.ArrayReference.getValues.getvalues003;
 *     the debuggee program - nsk.jdi.ArrayReference.getValues.getvalues003a.
 *     Using nsk.jdi.share classes, the debugger gets the debuggee running
 *     on another JavaVM, establishes a pipe with the debuggee program,
 *     and then send to the programm commands, to which the debuggee replies
 *     via the pipe.
 *     Upon getting reply, the debugger requests fields of checked object
 *     and trys to read array values correspondence with the test cases above.
 *     In case of error the test produces the return value 97 and a corresponding
 *     error message(s). Otherwise, the test is passed and produces the return
 *     value 95 and no message.
 * COMMENTS:
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.ArrayReference.getValues.getvalues003
 *        nsk.jdi.ArrayReference.getValues.getvalues003a
 * @run main/othervm
 *      nsk.jdi.ArrayReference.getValues.getvalues003
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

