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
 * @summary converted from VM Testbase nsk/jdi/Event/equals/equals001.
 * VM Testbase keywords: [quick, jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION:
 *  The test for public equals() method of an implementing class of
 *  com.sun.jdi.event.Event interface.
 *  The test checks an assertion cited from spec for equals() method of
 *  java.lang.Object class:
 *   The equals method implements an equivalence relation:
 *     - It is reflexive: for any reference value x, x.equals(x) should return true.
 *     - It is symmetric: for any reference values x and y, x.equals(y) should return
 *       true if and only if y.equals(x) returns true.
 *     - It is consistent: for any reference values x and y, multiple invocations
 *       of x.equals(y) consistently return true or consistently return false,
 *       provided no information used in equals comparisons on the object is modified.
 *     - For any non-null reference value x, x.equals(null) should return false.
 * COMMENTS:
 *  The test is aimed to increase jdi source code coverage and checks
 *  the code which was not yet covered by previous tests for Event
 *  interface. The coverage analysis was done for jdk1.4.0-b92 build.
 *     Test updated to prevent possible VMDisconnectedException on VMDeathEvent:
 *     - resume for these events is skipped in event handling loop
 *     - method Debugee.endDebugee() is used instead of Debugee.waitFor()
 *     Test fixed according to test bug:
 *     4854711 TEST_BUG: race condition in threads synchronization
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.Event.equals.equals001
 *        nsk.jdi.Event.equals.equals001a
 * @run main/othervm
 *      nsk.jdi.Event.equals.equals001
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

