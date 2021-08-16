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
 * @summary converted from VM Testbase nsk/jdi/Event/hashCode/hashcode001.
 * VM Testbase keywords: [quick, jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION:
 *  The test for public hashCode() method of an implementing class of
 *  com.sun.jdi.event.Event interface.
 *  The test checks an assertion cited from spec for hashCode() method of
 *  java.lang.Object class:
 *  The general contract of hashCode is:
 *    - Whenever it is invoked on the same object more than once during
 *      an execution of a Java application, the hashCode method must
 *      consistently return the same integer, provided no information used
 *      in equals comparisons on the object is modified.
 *      ...
 * COMMENTS:
 *  The test is aimed to increase jdi source code coverage and checks
 *  the code which was not yet covered by previous tests for Event
 *  interface. The coverage analysis was done for jdk1.4.0-b92 build.
 *     Test updated to prevent possible VMDisconnectedException on VMDeathEvent:
 *     - resume for these events is skipped in event handling loop
 *     - method Debugee.endDebugee() is used instead of Debugee.waitFor()
 *     Test fixed according to test bug:
 *     4798088 TEST_BUG: setBreakpoint() method depends of the locations implementation
 *     - using standard Debugee.setBreakpoint() method
 *     - adjusting source line numbers used for setting breakpoint
 *     Test fixed according to test bug:
 *     4783196 TEST_BUG: hashcode001 incorrectly handles VMDisconnectEvent
 *     - eliminating printing VMDeathEvent and VMDisconnectEvent
 *     - ignoring VMDisconnectedException if thrown for these events
 *       while testinging hashcode() method
 *     Test fixed according to test bug:
 *     4854711 TEST_BUG: race condition in threads synchronization
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.Event.hashCode.hashcode001
 *        nsk.jdi.Event.hashCode.hashcode001a
 * @run main/othervm
 *      nsk.jdi.Event.hashCode.hashcode001
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

