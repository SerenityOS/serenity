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
 * @summary converted from VM Testbase nsk/jdi/LocalVariable/genericSignature/gensignature001.
 * VM Testbase keywords: [quick, jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION
 *     The test for genericSignature() method of LocalVariable
 *     interface.
 *     The test checks that names and generic signatures of returned
 *     arguments and local variables are equal to expected ones.
 *     Test consists of two compoments:
 *         debugger application: gensignature001
 *         debugged application: gensignature001a
 *     First, the debugger uses nsk.share support classes to launch
 *     debuggee and obtain Debugee object that represents debugged
 *     application. Also communication channel (IOPipe) is established
 *     between debugger and debuggee to exchange with synchronization
 *     signals.
 *     Next, the debugger obtains JDI mirrors for the tested class
 *     and method from the debuggee.
 *     Then, the debugger gets a mirror of every known local variable
 *     of the tested method, including arguments; gets its generic
 *     signature using genericSignature() call and compares returned
 *     character string with expected one.
 * COMMENTS
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.LocalVariable.genericSignature.gensignature001
 *        nsk.jdi.LocalVariable.genericSignature.gensignature001a
 *
 * @comment make sure gensignature001a is compiled with full debug info
 * @clean nsk.jdi.LocalVariable.genericSignature.gensignature001a
 * @compile -g:lines,source,vars ../gensignature001a.java
 *
 * @run main/othervm
 *      nsk.jdi.LocalVariable.genericSignature.gensignature001
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

