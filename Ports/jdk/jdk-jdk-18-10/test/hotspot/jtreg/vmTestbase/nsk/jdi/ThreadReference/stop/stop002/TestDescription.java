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
 * @summary converted from VM Testbase nsk/jdi/ThreadReference/stop/stop002.
 * VM Testbase keywords: [quick, jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION
 *     This test checks that the JDI method:
 *         com.sun.jdi.ThreadReference.stop()
 *     properly throws InvalidTypeException - if specified throwable
 *     is not an instance of java.lang.Throwable in the target VM.
 *     Debugger part of the test tries to stop debuggee thread
 *     through the JDI method using as a parameter an object
 *     reference of the main debuggee class <i>stop002t</i> itself
 *     which is not Throwable.
 * COMMENTS
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.ThreadReference.stop.stop002
 *        nsk.jdi.ThreadReference.stop.stop002t
 *
 * @comment make sure stop002t is compiled with full debug info
 * @clean nsk.jdi.ThreadReference.stop.stop002t
 * @compile -g:lines,source,vars ../stop002t.java
 *
 * @run main/othervm
 *      nsk.jdi.ThreadReference.stop.stop002
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

