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
 * @summary converted from VM Testbase nsk/jdi/Accessible/modifiers/modifiers001.
 * VM Testbase keywords: [jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION
 *         nsk/jdi/Accessible/modifiers/modifiers001 test
 *         checks the modifiers() method of Accessible interface
 *         of the com.sun.jdi package for ClassType, InterfaceType.
 * COMMENTS
 *         Bug fixed: 4434819: TEST_BUG: wrongly believe that classes are loaded
 * ---------
 * The test is updated to use new share classes as follows:
 * - two lines, 33 and 34, with argsHandler and logHandler are added befo
 * - statements with definitions, lines 102-115, are added;
 * - all calls to "println" are replaced with calls to
 *   "logHandler.complain" or "logHandler.display"..
 * - all "display" are replaced with "logHandler.display"
 * - one non-used statement, line 148, is commented away.
 * --------
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.Accessible.modifiers.modifiers001
 *        nsk.jdi.Accessible.modifiers.modifiers001a
 * @run main/othervm
 *      nsk.jdi.Accessible.modifiers.modifiers001
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

