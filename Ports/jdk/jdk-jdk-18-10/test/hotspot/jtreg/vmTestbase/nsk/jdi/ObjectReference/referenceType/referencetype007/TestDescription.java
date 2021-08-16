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
 * @summary converted from VM Testbase nsk/jdi/ObjectReference/referenceType/referencetype007.
 * VM Testbase keywords: [jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION:
 *     The test for the implementation of an object of the type
 *     ObjectReference.
 *     The test checks up that a result of the method
 *     com.sun.jdi.ObjectReference.referenceType()
 *     complies with its spec:
 *     public ReferenceType referenceType()
 *     Gets the ReferenceType that mirrors the type of this object.
 *     The type may be a subclass or implementor of the declared type of
 *     any field or variable which currently holds it.
 *     For example, right after the following statement.
 *        Object obj = new String("Hello, world!");
 *     The ReferenceType of obj will mirror java.lang.String and
 *     not java.lang.Object.
 *     The type of an object never changes, so this method will always return
 *     the same ReferenceType over the lifetime of the mirrored object.
 *     The returned ReferenceType will be a ClassType or ArrayType and never
 *     an InterfaceType.
 *     Returns: the ReferenceType for this object.
 *     Throws: ObjectCollectedException -
 *             if this object has been garbage collected.
 *     The case for testing includes primitive type Array object,
 *     and no ObjectCollectedException is expected.
 *     The test works as follows:
 *     The debugger program - nsk.jdi.ObjectReference.referenceType.referencetype007;
 *     the debuggee program - nsk.jdi.ObjectReference.referenceType.referencetype007a.
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
 * @build nsk.jdi.ObjectReference.referenceType.referencetype007
 *        nsk.jdi.ObjectReference.referenceType.referencetype007a
 * @run main/othervm
 *      nsk.jdi.ObjectReference.referenceType.referencetype007
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

