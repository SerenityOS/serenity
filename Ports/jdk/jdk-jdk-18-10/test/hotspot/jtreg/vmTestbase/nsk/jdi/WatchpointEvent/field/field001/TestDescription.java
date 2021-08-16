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
 * @modules java.base/jdk.internal.misc:+open
 *
 * @summary converted from VM Testbase nsk/jdi/WatchpointEvent/field/field001.
 * VM Testbase keywords: [quick, jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION
 *    This test exercises
 *      com.sun.jdi.event.WatchpointEvent.field() method.
 *    The test checks the following assertion:
 *      Field reference returned by WatchpointEvent.field()
 *      exists in the list of fields returned by com.sun.jdi.ReferenceType.fields()
 *      method for .
 *    A debugger class - nsk.jdi.WatchpointEvent.field.field001  ;
 *    a debuggee class - nsk.jdi.WatchpointEvent.field.field001a .
 *    The test uses supporting nsk/jdi/share classes for launching debuggee
 *    and for creating communication pipe between debugger and debuggee. The
 *    debugger and debugee communicates with special commands.
 *    The debugger creates WatchpointRequests to the fields defined
 *    in checked class.
 *    The debugger starts <Eventhandler> thread for listening events delivered
 *    from debuggee.
 *    Upon receiving <GO> command from debugger, the debuggee initializes its
 *    fields.
 *    The debugger switches to <EventHandler> to listen the event during the
 *    time  specified by <waittime> parameter. Then debugger shuts down debuggee
 *    by <QUIT> command.
 *    If debugger receives WatchpointEvent, it checks if reference returned by
 *    WatchpointEvent.field() exists in the list of fields of the class loaded
 *    in the debuggee.
 *    The test also fails if no WatchpointEvent was received.
 * COMMENTS
 *    Test fixed due to bug:
 *        4455653 VMDisconnectedException on resume
 *    Standard method Debugee.endDebugee() is used instead of cleaning
 *         event queue on debuggee VM exit.
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.WatchpointEvent.field.field001
 *        nsk.jdi.WatchpointEvent.field.field001a
 * @run main/othervm
 *      nsk.jdi.WatchpointEvent.field.field001
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

