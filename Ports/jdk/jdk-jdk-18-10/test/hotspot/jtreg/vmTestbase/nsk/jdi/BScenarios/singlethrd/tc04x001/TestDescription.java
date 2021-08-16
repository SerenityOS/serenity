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
 * @summary converted from VM Testbase nsk/jdi/BScenarios/singlethrd/tc04x001.
 * VM Testbase keywords: [quick, jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION:
 *     This test is from the group of so-called Borland's scenarios and
 *     implements the following test case:
 *         Suite 2 - Breakpoints (multiple threads)
 *         Test case:      TC3
 *         Description:    Exception breakpoint
 *         Steps:          1.Set caught exception breakpoint on class
 *                           javax.sound.midi.MidiUnavailableException
 *                         2.Debug Main
 *                           X. Stops on line 42 in Main.java
 *     The description was drown up according to steps under JBuilder.
 *     Of course, the test has own line numbers and method/class names and
 *     works as follow:
 *     When the test is starting debugee, debugger creates ExceptionRequest.
 *     After ExceptionEvent arrived, debugger checks line number of one's
 *     location. It should be 74th line, that is throwing tc03x001aException.
 *     Every thread must generate ExceptionEvent.
 *     In case, when at least one event doesn't arrive during waittime
 *     interval or line number of event is wrong, test fails.
 * COMMENTS:
 *     Test was fixed according to test bug:
 *     4778296 TEST_BUG: debuggee VM intemittently hangs after resuming
 *     - handling VMStartEvent was removed from the debugger part of the test
 *     - quit on VMDeathEvent was added to the event handling loop
 *     Test fixed according to test bug:
 *     4804095 TEST_BUG: potential race condition with loading classes in JDI tests
 *     - launching debuggee by prepareDebugee() replaced with bindToDebugee()
 *       to exclude first IOPipe communication
 *     - making ClassPrepareRequest moved to begin (right after debuggee started)
 *     - making ExceptionRequest moved to handling ClassPrepareEvent
 *     - handling events moved to a separate thread
 *     - removed extra IOPipe communication points to make algorithm more clear
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.BScenarios.singlethrd.tc04x001
 *        nsk.jdi.BScenarios.singlethrd.tc04x001a
 * @run main/othervm
 *      nsk.jdi.BScenarios.singlethrd.tc04x001
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

