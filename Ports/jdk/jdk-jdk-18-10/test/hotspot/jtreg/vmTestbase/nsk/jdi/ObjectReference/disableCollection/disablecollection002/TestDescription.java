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
 * @summary converted from VM Testbase nsk/jdi/ObjectReference/disableCollection/disablecollection002.
 * VM Testbase keywords: [jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION:
 *     The test for the implementation of an object of the type
 *     ObjectReference.
 *     The test checks up that a result of the method
 *     com.sun.jdi.ObjectReference.disableCollection()
 *     complies with its spec:
 *     public void disableCollection()
 *        Prevents garbage collection for this object.
 *      By default all ObjectReference values returned by JDI may be collected at any
 *      time the target VM is running. A call to this method guarantees that
 *      the object will not be collected.
 *      enableCollection() can be used to allow collection once again.
 *        Calls to this method are counted.
 *      Every call to this method requires a corresponding call to enableCollection()
 *      before garbage collection is re-enabled.
 *        Note that while the target VM is suspended, no garbage collection will occur
 *      because all threads are suspended. The typical examination of variables, fields,
 *      and arrays during the suspension is safe without
 *      explicitly disabling garbage collection.
 *        This method should be used sparingly,
 *      as it alters the pattern of garbage collection in the target VM and, consequently,
 *      may result in application behavior under the debugger that
 *      differs from its non-debugged behavior.
 *      The test checks up on the following assertion:
 *          Prevents garbage collection for this object.
 *          By default all ObjectReference values returned by JDI may be collected at
 *          any time the target VM is running. A call to this method guarantees that
 *          the object will not be collected.
 *     The test works as follows:
 *     The debugger program - nsk.jdi.ObjectReference.disableCollection.disablecollection002;
 *     the debuggee program - nsk.jdi.ObjectReference.disableCollection.disablecollection002a.
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
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.ObjectReference.disableCollection.disablecollection002
 *        nsk.jdi.ObjectReference.disableCollection.disablecollection002a
 * @run main/othervm
 *      nsk.jdi.ObjectReference.disableCollection.disablecollection002
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="-Xmx256M ${test.vm.opts} ${test.java.opts}"
 */

