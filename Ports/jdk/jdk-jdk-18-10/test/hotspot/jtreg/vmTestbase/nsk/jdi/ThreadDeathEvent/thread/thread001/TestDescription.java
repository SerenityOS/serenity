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
 * @summary converted from VM Testbase nsk/jdi/ThreadDeathEvent/thread/thread001.
 * VM Testbase keywords: [quick, jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION
 *    The test exercises
 *      com.sun.jdi.event.ThreadDeathEvent.thread() method.
 *    The test checks the following assertions:
 *      - ThreadDeathEvent is always received by debugger
 *        for all normally completed threads in target VM,
 *      - ThreadDeathEvent.thread() returns valid ThreadReference
 *        to expected thread in target VM,
 *      - ThreadDeathEvent is received only once for each expected thread
 *    A debugger class - nsk.jdi.ThreadDeathEvent.thread.thread001  ;
 *    a debuggee class - nsk.jdi.ThreadDeathEvent.thread.thread001a .
 *    The test uses NSK supporting classes for launching debuggee
 *    and for creating communication pipe between debugger and debuggee. The
 *    debugger and debugee communicates with special commands.
 *    The debugger creates and enables ThreadDeathRequest.
 *    The debugger starts special thread <EventHandler> for listening events
 *    delivered from debuggee.
 *    The debuggee starts the following checked threads: <innerThread>,
 *    <innerDaemon>, <outerThread>, <outerDaemon>. These threads consequently
 *    lock synchronizing  object and complete. The main thread waits
 *    the completion of <inner> and <outer> threads and complete
 *    upon receiving <QUIT> command from debugger.
 *    The debugger switches <EventHandler> to to listen the event during
 *    the time specified by <waittime> parameter.
 *    The debugger checks if ThreadDeathEvent were receiced for every
 *    checked threads.
 *    The debugger also checks the other assertions of the test.
 *    The test fails if any of this checks failed.
 * COMMENTS
 *        Test fixed due to bugs:
 *        4455653 VMDisconnectedException on resume
 *        4463674 TEST_BUG: some JDI tests are timing dependent
 * -----------------
 *    to fix the bug 4502899,
 *    the statement
 *         eventTimeout = argHandler.getWaitTime() * 60 * 1000; // milliseconds
 *    is added next to line #66:
 *         log = new Log(out, argHandler);
 * -----------------
 *    4757762 nsk/jdi/ThreadDeathEvent/thread/thread001 has a race
 * -----------------
 *       - Standard method Debugee.endDebugee() is used instead of cleaning
 *         event queue on debuggee VM exit.
 *       - Additional synchronization via IOPipe is used after completing
 *         of tested threads
 *       - Event handling loop is completed on receiving all expected events
 *         instead of receiving VMDisconnectEvent
 *       - Testing main thread is removed
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.ThreadDeathEvent.thread.thread001
 *        nsk.jdi.ThreadDeathEvent.thread.thread001a
 * @run main/othervm
 *      nsk.jdi.ThreadDeathEvent.thread.thread001
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

