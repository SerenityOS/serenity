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
 * @summary converted from VM Testbase nsk/jdi/ObjectReference/invokeMethod/invokemethod010.
 * VM Testbase keywords: [quick, jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION
 *     This test checks that invocation with all threads resumption
 *     and the single threaded one will be performed properly through
 *     the JDI method:
 *         com.sun.jdi.ObjectReference.invokeMethod().
 *     The following assertions are verified:
 *         - by default, all threads in the target VM are resumed while
 *           the method is being invoked;
 *         - when the invocation completes, all threads in the target VM
 *           are suspended, regardless their state before the invocation;
 *         - only the specified thread will be resumed with the
 *           INVOKE_SINGLE_THREADED;
 *         - upon completion of a single threaded invoke, the invoking
 *           thread will be suspended once again.
 *     A debuggee part of the test starts several threads. Then debugger
 *     calls the JDI method without and with the flag
 *     ObjectReference.INVOKE_SINGLE_THREADED sequentially. During
 *     the invocations and after them the threads state is expected to be as
 *     described above.
 * COMMENTS
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.ObjectReference.invokeMethod.invokemethod010
 *        nsk.jdi.ObjectReference.invokeMethod.invokemethod010t
 *
 * @comment make sure invokemethod010t is compiled with full debug info
 * @clean nsk.jdi.ObjectReference.invokeMethod.invokemethod010t
 * @compile -g:lines,source,vars ../invokemethod010t.java
 *
 * @run main/othervm
 *      nsk.jdi.ObjectReference.invokeMethod.invokemethod010
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

