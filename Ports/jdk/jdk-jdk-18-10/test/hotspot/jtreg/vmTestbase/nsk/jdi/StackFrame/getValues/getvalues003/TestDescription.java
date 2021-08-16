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
 * @summary converted from VM Testbase nsk/jdi/StackFrame/getValues/getvalues003.
 * VM Testbase keywords: [quick, jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION
 *     This test checks that the JDI method:
 *         com.sun.jdi.StackFrame.getValues()
 *     properly throws IllegalArgumentException - if specified variable
 *     is invalid for this frame's method.
 *     The test works as follows. The target VM executes two debuggee
 *     threads: "getvalues003tMainThr" and "getvalues003tAuxThr".
 *     Debugger part tries to provoke the exception by getting values
 *     of the local variables in stack frame obtained from the
 *     "getvalue0s03tMainThr" thread, and one among them obtained from
 *     the "getvalues003tAuxThr" thread.
 * COMMENTS
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.StackFrame.getValues.getvalues003
 *        nsk.jdi.StackFrame.getValues.getvalues003t
 *
 * @comment make sure getvalues003t is compiled with full debug info
 * @clean nsk.jdi.StackFrame.getValues.getvalues003t
 * @compile -g:lines,source,vars ../getvalues003t.java
 *
 * @run main/othervm
 *      nsk.jdi.StackFrame.getValues.getvalues003
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

