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
 * @summary converted from VM Testbase nsk/jdi/ThreadStartEvent/thread/thread001.
 * VM Testbase keywords: [jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION
 *   The test exercises
 *     com.sun.jdi.event.ThreadStartEvent.thread() method.
 *   The test checks the following assertions:
 *     - ThreadStartEvent is always received by debugger
 *       for all started threads in target VM,
 *     - ThreadStartEvent.thread() returns valid ThreadReference
 *       to expected thread in target VM,
 *     - ThreadStartEvent is received only once for each expected thread
 *    A debugger class - nsk.jdi.ThreadStartEvent.thread.thread001  ;
 *    a debuggee class - nsk.jdi.ThreadStartEvent.thread.thread001a .
 *    The test uses supporting nsk/jdi/share classes for launching debuggee
 *    and for creating communication pipe between debugger and debuggee. The
 *    debugger and debugee communicates with special commands.
 *    The debugger creates and enables ThreadStartRequest.
 *    The debuggee creates two auxiliary checked threads: <Thread1>, <Thread2>.
 *    Before starting threads, debuggee's main thread locks special
 *    synchronized object <lock> to prevent auxiliary threads from finishing
 *    before test checks performed.
 *    The debugger starts special thread <EventHandler> for listening events
 *    received from debuggee, and resumes debuggee.The <EventHandler> expects
 *    ThreadStartEvent for each debuggee threads, including main thread, and
 *    marks that thread in <checkedThreads> array.
 *    For each received ThreadStartEvent it is verified to refer to the
 *    right request, VM, thread references.
 *    Debugger waits for <EventHandler> receives all expected events
 *    for WAITTIME timeout. If not all events have been received,
 *    debugger complains about an error.
 *    Finally, debugger disables event request, cleans events queue,
 *    sends debuggee command <QUIT>, waits for debuggee terminated,
 *    and exits.
 *    The test fails if any of the checks failed.
 * COMMENTS
 *        Test fixed due to bugs:
 *        4455653 VMDisconnectedException on resume
 *        4463674 TEST_BUG: some JDI tests are timing dependent
 *    -------------------------------------------------------------
 *    Modified to fix the bug 4501953,
 *    the file thread001a.java is modified as follows.
 *    - A special waitnotify Object is added in the class thread001a.
 *    - In classes InnerThread and OuterThread,
 *       synchronized blocks are added
 *       to make newly created thread certainly to run.
 *    - In the class thread001a,
 *      the pervious mechanizm of waiting for predefined time
 *      is replaced with new one based on wait-notify.
 *    -------------------------------------------------------------
 *    4434432 TEST_BUG: ThreadStartEvent does not received on Linux
 *    -------------------------------------------------------------
 *         Standard method Debugee.endDebugee() is used instead of cleaning
 *         event queue on debuggee VM exit.
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.ThreadStartEvent.thread.thread001
 *        nsk.jdi.ThreadStartEvent.thread.thread001a
 * @run main/othervm
 *      nsk.jdi.ThreadStartEvent.thread.thread001
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

