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
 * @summary converted from VM Testbase nsk/jdi/ExceptionEvent/_itself_/exevent008.
 * VM Testbase keywords: [jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION
 *     The test checks that both caught and uncaught exception events
 *     are properly reported by the debugger. It calls
 *     com.sun.jdi.request.EventRequestManager.createExceptionRequest()
 *     method with true values of boolean parameters notifyCaught and
 *     notifyUncaught.
 *     The debugee part of the test raises two exceptions:
 *      - caught own exevent008tException;
 *      - uncaught InvokationTargetException with enveloped NumberFormatException
 *        in another class
 * COMMENTS
 *     See also bug 4323439
 *     --------------------------------
 *     Test fixed due to bug:
 *     4455653 VMDisconnectedException on resume
 *     --------------------------------
 *     To fix 4615225,
 *     the test suite was moved here.
 *     --------------------------------
 *     Test updated to prevent possible VMDisconnectedException on VMDeathEvent:
 *     - quit on VMDeathEvent is added to event handling loop
 *     - standard method Debugee.endDebugee() is used instead of cleaning event queue
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.ExceptionEvent._itself_.exevent008
 *        nsk.jdi.ExceptionEvent._itself_.exevent008t
 * @run main/othervm
 *      nsk.jdi.ExceptionEvent._itself_.exevent008
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

