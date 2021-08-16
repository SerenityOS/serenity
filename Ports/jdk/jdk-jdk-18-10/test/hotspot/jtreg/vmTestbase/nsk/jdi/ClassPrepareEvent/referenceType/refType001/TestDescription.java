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
 * @summary converted from VM Testbase nsk/jdi/ClassPrepareEvent/referenceType/refType001.
 * VM Testbase keywords: [jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION
 *   The test exercises
 *      com.sun.jdi.event.lassPrepareEvent.referenceType() method.
 *   The test checks the following assertions:
 *     - ClassPrepareEvent is always received by debugger
 *       for all prepared classes and interfaces in target VM,
 *     - ClassPrepareEvent is received only once for each checked
 *       class or interface
 *     - method ClassPrepareEvent.referenceType returns valid
 *       Reference Type in target VM,
 *     - all static fields and constants are initialized for checked
 *       prepared class or interface.
 *    A debugger class - nsk.jdi.ClassPrepareEvent.referenceType.refType001  ;
 *    a debuggee class - nsk.jdi.ClassPrepareEvent.referenceType.refType001a .
 *    The test uses supporting nsk/share/jdi classes for launching debuggee
 *    and for creating communication pipe between debugger and debuggee. The
 *    debugger and debugee communicates with special commands.
 *    The debugger creates ClassPrepareRequest and starts special thread
 *    <EventHandler> for listening events delivered from debuggee.
 *    The debuger enables ClassPrepareRequest, resumes debuggee and
 *    waits for <READY> command from it. Next, debuugger sends debuggee
 *    command <RUN> to force it to start another thread, and waits
 *    for confirmation <DONE>.
 *    The debuggee starts special thread in which instance of
 *    <ClassForAnotherThread> with a number of static fields,
 *    waits for thread started and sends debugger command <DONE>.
 *    Each event received by debuger is handled by <EventHandler>.
 *    When event is of ClassPrepareEvent and is for checked threads,
 *    all assertions are verifieded for this event.
 *    After debuggee received command <DONE> from debuggee, if notifies
 *    <EventHandler> that all thread have been started in debuggee,
 *    and waits for all expected events received and <EventHandler> finishes.
 *    If not all expected events has been received for WAITTIME period,
 *    debugger complains an error and interrupts <EventHandler>.
 *    Finally, debugger disables event request, sends debuggee command <QUIT),
 *    cleans events queue, waits for deduggee terminates, and exits.
 *    The test fails if any of the checks failed.
 * COMMENTS
 *        Test fixed due to bug:
 *        4455653 VMDisconnectedException on resume
 *    ----------------------------------------------
 *    To fix the bug 4501953, in the file refType001a.java
 *    the line # 29 is commented away and
 *    the line # 51 is modified to remove "threadStartTimeout"
 *    ----------------------------------------------
 *         Standard method Debugee.endDebugee() is used instead of cleaning
 *         event queue on debuggee VM exit.
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.ClassPrepareEvent.referenceType.refType001
 *        nsk.jdi.ClassPrepareEvent.referenceType.refType001a
 * @run main/othervm
 *      nsk.jdi.ClassPrepareEvent.referenceType.refType001
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

