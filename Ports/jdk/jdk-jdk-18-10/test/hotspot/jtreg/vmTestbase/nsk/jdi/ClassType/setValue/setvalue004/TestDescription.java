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
 * @summary converted from VM Testbase nsk/jdi/ClassType/setValue/setvalue004.
 * VM Testbase keywords: [jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION:
 *     The test for the implementation of an object of the type
 *     ClassType.
 *     The test checks up that a result of the method
 *     com.sun.jdi.ClassType.setValue()
 *     complies with its spec:
 *     public void setValue(Field field, Value value)
 *               throws InvalidTypeException,
 *                      ClassNotLoadedException
 *      Assigns a value to a static field.
 *      The Field must be valid for this ObjectReference; that is, there must be
 *      a widening reference conversion from this object to the field's type.
 *      The field must not be final.
 *      Object values must be assignment compatible with the field type
 *      (This implies that the field type must be loaded through
 *      the enclosing class's class loader).
 *      Primitive values must be either assignment compatible with the field type
 *      or must be convertible to the field type without loss of information.
 *      See JLS section 5.2 for more information on assignment compatibility.
 *      Parameters: field - the field to set.
 *                  value - the value to be assigned.
 *      Throws: IllegalArgumentException -
 *              if the field is not static, the field is final, or
 *              the field does not exist in this class.
 *              ClassNotLoadedException -
 *              if the field type has not yet been loaded through
 *              the appropriate class loader.
 *              InvalidTypeException -
 *              if the value's type does not match the field's declared type.
 *              ObjectCollectedException -
 *              if the given Value is an mirrors an object
 *              which has been garbage collected.
 *              Also thrown if this class has been unloaded and garbage collected.
 *              VMMismatchException -
 *              if a Mirror argument and this mirror do not belong to
 *              the same VirtualMachine.
 *     The case for testing includes checking up on assigning null to
 *     both PrimitiveType and ReferenceType obects.
 *     The test works as follows:
 *     The debugger program - nsk.jdi.ClassType.setValue.setvalue004;
 *     the debuggee program - nsk.jdi.ClassType.setValue.setvalue004a.
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
 * @build nsk.jdi.ClassType.setValue.setvalue004
 *        nsk.jdi.ClassType.setValue.setvalue004a
 * @run main/othervm
 *      nsk.jdi.ClassType.setValue.setvalue004
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

