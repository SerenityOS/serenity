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
 * @summary converted from VM Testbase nsk/jdi/ObjectReference/invokeMethod/invokemethod009.
 * VM Testbase keywords: [quick, jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION
 *     The test checks that virtual and non-virtual method invocation
 *     will be performed properly through the JDI method:
 *         com.sun.jdi.ObjectReference.invokeMethod().
 *     A debugger part of the test invokes several debuggee methods,
 *     overriden in an object reference class, but obtained from
 *     a reference type of its superclass. The debugger calls the
 *     JDI method with and without the flag ObjectReference.INVOKE_NONVIRTUAL
 *     sequentially. It is expected, that methods from the superclass
 *     instead of from the object reference class will be invoked with
 *     the flag and vise versa otherwise.
 * COMMENTS
 *     This test is similar to the test:
 *         nsk/jdi/ObjectReference/invokeMethod/invokemethod008
 *     except the order of virtual and non-virtual invocations and was
 *     written due to the found JDI bug 4705586.
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.ObjectReference.invokeMethod.invokemethod009
 *        nsk.jdi.ObjectReference.invokeMethod.invokemethod009t
 *
 * @comment make sure invokemethod009t is compiled with full debug info
 * @clean nsk.jdi.ObjectReference.invokeMethod.invokemethod009t
 * @compile -g:lines,source,vars ../invokemethod009t.java
 *
 * @run main/othervm
 *      nsk.jdi.ObjectReference.invokeMethod.invokemethod009
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

