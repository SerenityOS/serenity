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
 * @summary converted from VM Testbase nsk/jdi/ArrayType/newInstance/newinstance001.
 * VM Testbase keywords: [jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION:
 *     The test for the implementation of an object of the type
 *     ArrayType.
 *     The test checks up that a result of the method
 *     com.sun.jdi.ArrayType.newInstance()
 *     complies with its spec:
 *     public ArrayReference newInstance(int length)
 *     Creates a new instance of this array class in the target VM.
 *     The array is created with the given length and
 *     each component is initialized to is standard default value.
 *     Parameters: length - the number of components in the new array
 *     Returns: the newly created ArrayReference mirroring
 *              the new object in the target VM.
 *     Throws: ObjectCollectedException -
 *             if this array type has been unloaded and garbage collected.
 *     when a type of this array class is one of primitive types.
 *     The test works as follows:
 *     The debugger program - nsk.jdi.ArrayType.newInstance.newinstance001;
 *     the debuggee program - nsk.jdi.ArrayType.newInstance.newinstance001a.
 *     Using nsk.jdi.share classes,
 *     the debugger gets the debuggee running on another JavaVM,
 *     creates the object debuggee.VM,
 *     establishes a pipe with the debuggee program, and then
 *     send to the programm commands, to which the debuggee replies
 *     via the pipe. Upon getting reply,
 *     the debugger calls corresponding debuggee.VM methods to get
 *     needed data and compares the data got to the data expected.
 *     In case of error the test produces the return value 97 and
 *     a corresponding error message(s).
 *     Otherwise, the test is passed and produces
 *     the return value 95 and no message.
 * COMMENTS:
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.ArrayType.newInstance.newinstance001
 *        nsk.jdi.ArrayType.newInstance.newinstance001a
 * @run main/othervm
 *      nsk.jdi.ArrayType.newInstance.newinstance001
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

