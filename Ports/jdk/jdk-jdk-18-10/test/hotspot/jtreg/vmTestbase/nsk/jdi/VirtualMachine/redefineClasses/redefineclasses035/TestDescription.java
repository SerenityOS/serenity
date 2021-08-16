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
 * @summary converted from VM Testbase nsk/jdi/VirtualMachine/redefineClasses/redefineclasses035.
 * VM Testbase keywords: [quick, jpda, jdi, redefine]
 * VM Testbase readme:
 * DESCRIPTION:
 *     The test checks up com.sun.jdi.VirtualMachine.redefineClasses()
 *     in case when redefined class loaded by a custom loader.
 *     The test perform next steps:
 *         1. Debugee loads a tested class by the custom loader.
 *         2. When getting ClassPrepareEvent, debugger checks class loader
 *            of tested class to be the custom class loader and creates
 *            MethodEntryRequest for tested class and waits a corresponding
 *            event.
 *         3. Debugee calls a method of the tested class.
 *         4. After getting MethodEntryEvent, debugger redefines the tested
 *            class and checks the redefinition to be successful.
 *     The above steps are performed for two cases, when the custom loader
 *     extends java.net.URLClassLoader and when the custom loader directly
 *     extends java.lang.ClassLoader.
 * COMMENTS
 *     The test was developed as a regression test for
 *     #4739099 JDI HotSwap class file replacement with custom classloader broken
 *     4777347 TEST_BUG: redefineclasses035 wrongly redefines method
 *     Test was fixed according to test bug 4778296:
 *         - handling VMStartEvent was removed from the debugger part of the test
 *         - quit on VMDeathEvent was added to the event handling loop
 *     Test updated to wait for debugee VM exit:
 *     - standard method Debugee.endDebugee() is used instead of final Debugee.resume()
 *     #5055417 TEST: warnings and notes caused by generification
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.VirtualMachine.redefineClasses.redefineclasses035
 *        nsk.jdi.VirtualMachine.redefineClasses.redefineclasses035a
 *
 * @comment compile loadclassXX to bin/loadclassXX
 * @run driver nsk.share.ExtraClassesBuilder
 *      loadclass
 *
 * @comment compile newclassXX to bin/newclassXX
 *          with full debug info
 * @run driver nsk.share.ExtraClassesBuilder
 *      -g:lines,source,vars
 *      newclass
 *
 * @run main/othervm
 *      nsk.jdi.VirtualMachine.redefineClasses.redefineclasses035
 *      ./bin
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

