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
 * @summary converted from VM Testbase nsk/jdi/BScenarios/multithrd/tc02x001.
 * VM Testbase keywords: [quick, jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION:
 *     This test is from the group of so-called Borland's scenarios and
 *     implements the following test case:
 *         Suite 2 - Breakpoints (multiple threads)
 *         Test case:      TC2
 *         Description:    Class breakpoint
 *         Steps:          1.Add class breakpoint: singlethread.Class1
 *                         2.Debug Main
 *                           X. Stops on line 13 in Class1.java
 *     The description was drown up according to steps under JBuilder.
 *     Of course, the test has own line numbers and method/class names and
 *     works as follow:
 *     When the test is starting debugee, debugger creates MethodEntryRequest.
 *     After MethodEntryEvent arrived, debugger checks line number of one's
 *     location. It should be 73th line, that is constructor of tc02x001aClass1
 *     class. Every thread must generate MethodEntryEvent.
 *     The test synchronizes debugger and debugee executing via IOPipe channel.
 *     In case, when at least one event doesn't arrive during waittime
 *     interval or line number of event is wrong, test fails.
 * COMMENTS:
 *     There are a doublness of the "class breakpoint" understanding.
 *     Here is that JBuilder help says:
 *     1. "A class breakpoint causes the debugger to stop when any method
 *         from the specified class is called or when the specified class
 *         is instantiated."
 *         So, a class breakpoint is MethodEntryRequest with filtering
 *         the specified class.
 *     2. "A class breakpoint causes the debugger to stop at the location
 *         when the specified class is loaded or when any method from the
 *         specified class is called."
 *         In this case, a class breakpoint is MethodEntryRequest and
 *         ClassPrepareRequest with filtering the specified class.
 *     Test was fixed according to test bug:
 *     4778296 TEST_BUG: debuggee VM intemittently hangs after resuming
 *     - handling VMStartEvent was removed from the debugger part of the test
 *     - quit on VMDeathEvent was added to the event handling loop
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.BScenarios.multithrd.tc02x001
 *        nsk.jdi.BScenarios.multithrd.tc02x001a
 * @run main/othervm
 *      nsk.jdi.BScenarios.multithrd.tc02x001
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

