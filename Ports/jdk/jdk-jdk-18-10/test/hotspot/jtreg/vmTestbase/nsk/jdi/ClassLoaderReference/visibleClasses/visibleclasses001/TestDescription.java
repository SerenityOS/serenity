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
 * @summary converted from VM Testbase nsk/jdi/ClassLoaderReference/visibleClasses/visibleclasses001.
 * VM Testbase keywords: [jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION:
 *     The test for the implementation of an object of the type
 *     ClassLoader.
 *     The test checks up that a result of the method
 *     com.sun.jdi.ClassLoaderReference.visibleClasses()
 *     complies with its spec:
 *     public List visibleClasses()
 *        Returns a list of all classes for which this class loader has been recorded
 *      as the initiating loader in the target VM. The list contains
 *      ReferenceTypes defined directly by this loader
 *      (as returned by definedClasses()) and any types for which loading was
 *      delegated by this class loader to another class loader.
 *        The visible class list has useful properties with respect to
 *      the type namespace. A particular type name will occur at most once in
 *      the list. Each field or variable declared with that type name in
 *      a class defined by this class loader must be resolved to that single type.
 *        No ordering of the returned list is guaranteed.
 *        See the revised Java Virtual Machine Specification section 5.3
 *      Creation and Loading for more information on the initiating classloader.
 *        Note that unlike definedClasses() and VirtualMachine.allClasses(),
 *      some of the returned reference types may not be prepared.
 *      Attempts to perform some operations on unprepared reference types
 *      (e.g. fields()) will throw a ClassNotPreparedException.
 *      Use ReferenceType.isPrepared() to determine if a reference type is prepared.
 *      Returns:
 *          a List of ReferenceType objects mirroring classes initiated by
 *          this class loader. The list has length 0 if no classes are
 *          visible to this classloader.
 *    The test checks up on the following assertion:
 *       Returns a list of all classes for which this class loader
 *       has been recorded as the initiating loader in the target VM.
 *       The list contains ReferenceTypes defined directly by
 *       this loader (as returned by definedClasses()) and
 *       any types for which loading was delegated by
 *       this class loader to another class loader.
 *    The case to check includes a class loader with no
 *    delegated types, that is, the expected returned value should
 *    be equal to one returned by ClassLoaderReference.definedClasses().
 *     The test works as follows:
 *     The debugger program - nsk.jdi.ClassLoaderReference.visibleClasses.visibleclasses001;
 *     the debuggee program - nsk.jdi.ClassLoaderReference.visibleClasses.visibleclasses001a.
 *     Using nsk.jdi.share classes,
 *     the debugger gets the debuggee running on another JavaVM,
 *     creates the object debuggee.VM, and waits for VMStartEvent.
 *     Upon getting the debuggee VM started,
 *     the debugger calls corresponding debuggee.VM methods to get
 *     needed data and to perform checks.
 *     In case of error the test produces the return value 97 and
 *     a corresponding error message(s).
 *     Otherwise, the test is passed and produces
 *     the return value 95 and no message.
 * COMMENTS:
 *     Test fixed due to test RFE:
 *     4842009 TEST_RFE: Incorrect package name in two JDI tests
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.ClassLoaderReference.visibleClasses.visibleclasses001
 *        nsk.jdi.ClassLoaderReference.visibleClasses.visibleclasses001a
 * @run main/othervm
 *      nsk.jdi.ClassLoaderReference.visibleClasses.visibleclasses001
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

