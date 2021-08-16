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
 * @summary converted from VM Testbase nsk/jdi/ClassType/newInstance/newinstance001.
 * VM Testbase keywords: [jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION:
 *     The test for the implementation of an object of the type
 *     ClassType.
 *     The test checks up that a result of the method
 *     com.sun.jdi.ClassType.newInstance()
 *     complies with its spec:
 *     public ObjectReference newInstance(ThreadReference thread,
 *                                        Method method,
 *                                        List arguments,
 *                                        int options)
 *                             throws InvalidTypeException,
 *                                    ClassNotLoadedException,
 *                                    IncompatibleThreadStateException,
 *                                    InvocationException
 *      Constructs a new instance of this type, using the given constructor Method
 *      in the target VM. The specified constructor must be defined in the this class.
 *      Instance creation will occur in the specified thread.
 *      Instance creation can occur only if the specified thread has
 *      been suspended by an event which occurred in that thread.
 *      Instance creation is not supported when the target
 *      VM has been suspended through VirtualMachine.suspend() or
 *      when the specified thread is suspended through ThreadReference.suspend().
 *      The specified constructor is invoked with the arguments in the specified
 *      argument list. The invocation is synchronous; this method does not return
 *      until the constructor returns in the target VM. If the invoked method
 *      throws an exception, this method will throw an InvocationException which
 *      contains a mirror to the exception object thrown.
 *      Object arguments must be assignment compatible with the argument type
 *      (This implies that the argument type must be loaded through the enclosing
 *      class's class loader). Primitive arguments must be either assignment
 *      compatible with the argument type or must be convertible to the argument type
 *      without loss of information.
 *      See JLS section 5.2 for more information on assignment compatibility.
 *      By default, all threads in the target VM are resumed while the method is being
 *      invoked if they were previously suspended by an event or by
 *      VirtualMachine.suspend() or ThreadReference.suspend().
 *      This is done to prevent the deadlocks that will occur if any of the threads own
 *      monitors that will be needed by the invoked method.
 *      It is possible that breakpoints or other events might occur during
 *      the invocation. Note, however, that this implicit resume acts exactly like
 *      ThreadReference.resume(), so if the thread's suspend count is greater than 1,
 *      it will remain in a suspended state during the invocation.
 *      By default, when the invocation completes,
 *      all threads in the target VM are suspended,
 *      regardless their state before the invocation.
 *      The resumption of other threads during the invocation can be prevented by
 *      specifying the INVOKE_SINGLE_THREADED bit flag in the options argument;
 *      however, there is no protection against or recovery from the deadlocks
 *      described above, so this option should be used with great caution.
 *      Only the specified thread will be resumed (as described for all threads above).
 *      Upon completion of a single threaded invoke, the invoking thread will be
 *      suspended once again. Note that any threads started during the single threaded
 *      invocation will not be suspended when the invocation completes.
 *      If the target VM is disconnected during the invoke
 *      (for example, through VirtualMachine.dispose())
 *      the method invocation continues.
 *      Parameters:
 *          thread    - the thread in which to invoke.
 *          method    - the constructor Method to invoke.
 *          arguments - the list of Value arguments bound to the invoked constructor.
 *                      Values from the list are assigned to arguments in
 *                      the order they appear in the constructor signature.
 *          options   - the integer bit flag options.
 *      Returns:
 *          an ObjectReference mirror of the newly created object.
 *      Throws:
 *          IllegalArgumentException -
 *          if the method is not a member of this class, if the size of the
 *          argument list does not match the number of declared arguments for
 *          the constructor, or if the method is not a constructor.
 *          {<at>link - InvalidTypeException} if any argument in the argument list is not
 *          assignable to the corresponding method argument type.
 *          ClassNotLoadedException -
 *          if any argument type has not yet been loaded through the appropriate class loader.
 *          IncompatibleThreadStateException -
 *          if the specified thread has not been suspended by an event.
 *          InvocationException -
 *          if the method invocation resulted in an exception in the target VM.
 *          ObjectCollectedException -
 *          if the given thread or any object argument has been garbage collected.
 *          Also thrown if this class has been unloaded and garbage collected.
 *          VMMismatchException -
 *          if a Mirror argument and this mirror do not belong to the same VirtualMachine.
 *     The case for testing includes invoking the method on the class TestClass
 *     which is global to a class implementing debuggee's thread in which
 *     instance creation is performed.
 *     No exception is expected to be thrown in response to call to the method.
 *     The test works as follows:
 *     The debugger program - nsk.jdi.ClassType.newInstance.newinstance001;
 *     the debuggee program - nsk.jdi.ClassType.newInstance.newinstance001a.
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
 * @build nsk.jdi.ClassType.newInstance.newinstance001
 *        nsk.jdi.ClassType.newInstance.newinstance001a
 * @run main/othervm
 *      nsk.jdi.ClassType.newInstance.newinstance001
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

