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
 * @summary converted from VM Testbase nsk/jdi/ClassType/invokeMethod/invokemethod013.
 * VM Testbase keywords: [quick, jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION
 *     This test checks that debuggee method invocation will be
 *     performed properly through the JDI method:
 *         com.sun.jdi.ClassType.invokeMethod().
 *     The following assertions are verified:
 *         - all threads in the target VM are resumed while the method is
 *           being invoked. If the thread's suspend count is greater than 1,
 *           it will remain in a suspended state during the invocation.
 *         - when the invocation completes, all threads in the target VM
 *           are suspended, regardless their state before the invocation.
 *     A debuggee part of the test starts several threads. The debugger
 *     suspends all threads by a breakpoint event and then, some
 *     of them one more time. After that, it calls the JDI method. The
 *     threads are checked to be resumed or remain suspended during the
 *     invocation (depending on the suspend count), and, upon completing
 *     the invocation, to be suspended again.
 * COMMENTS
 *     Fixed TEST_BUG 6358778.
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.ClassType.invokeMethod.invokemethod013
 *        nsk.jdi.ClassType.invokeMethod.invokemethod013t
 * @run main/othervm
 *      nsk.jdi.ClassType.invokeMethod.invokemethod013
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

