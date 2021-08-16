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
 * @summary converted from VM Testbase nsk/jdi/EventQueue/remove_l/remove_l003.
 * VM Testbase keywords: [jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION
 *     This test checks that a VMDisconnectedException thrown by
 *     the JDI method com.sun.jdi.request.EventQueue.remove(long)
 *     will be preceded by a VMDisconnectEvent after
 *     com.sun.jdi.VirtualMachine.exit() call.
 * COMMENTS
 *     1)
 *     To fix the bug 4504397,
 *     the line 152 in the file remove003.java has been commented away.
 *     2)
 *     To put the name of the package in correspondence with the tesbase_nsk rules,
 *     EventQueue.remove is replaced with EventQueue.remove_l in the following files:
 *     - remove_l003.java,  lines 5, 32;
 *     - remove_l003t.java, line 5;
 *     - remove_l003.cfg,   line 4.
 *     ---------------------------
 *     Test updated to prevent possible VMDisconnectedException on VMDeathEvent:
 *     - resume on VMDeathEvent is skipped in events handling loop
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.EventQueue.remove_l.remove_l003
 *        nsk.jdi.EventQueue.remove_l.remove_l003t
 * @run main/othervm
 *      nsk.jdi.EventQueue.remove_l.remove_l003
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

