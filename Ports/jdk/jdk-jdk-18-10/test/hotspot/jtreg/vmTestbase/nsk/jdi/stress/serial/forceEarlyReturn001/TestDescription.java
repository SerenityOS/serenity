/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @key stress
 *
 * @summary converted from VM Testbase nsk/jdi/stress/serial/forceEarlyReturn001.
 * VM Testbase keywords: [stress, quick, jpda, jdi, feature_jdk6_jpda, vm6]
 * VM Testbase readme:
 * DESCRIPTION
 *         This test executes one after another several JDI tests in single VM
 *         (for detailed description see nsk.share.jdi.SerialExecutionDebugger and nsk.share.jdi.SerialExecutionDebuggee).
 *         Tests to execute are specified in file 'forceEarlyReturn001.tests' and tests are executed in given order.
 *         Test is treated as FAILED if at least one of executed tests failed.
 *
 * @library /vmTestbase
 *          /test/lib
 * @comment some of the tests from forceEarlyReturn001.tests need WhiteBox
 * @modules java.base/jdk.internal.misc:+open
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 *
 *
 * @comment build classes required for tests from forceEarlyReturn001.tests
 * @build nsk.jdi.ThreadReference.forceEarlyReturn.forceEarlyReturn001.forceEarlyReturn001
 *        nsk.jdi.ThreadReference.forceEarlyReturn.forceEarlyReturn001.forceEarlyReturn001a
 *        nsk.jdi.ThreadReference.forceEarlyReturn.forceEarlyReturn002.forceEarlyReturn002
 *        nsk.jdi.ThreadReference.forceEarlyReturn.forceEarlyReturn002.forceEarlyReturn002a
 *        nsk.jdi.ThreadReference.forceEarlyReturn.forceEarlyReturn003.forceEarlyReturn003
 *        nsk.jdi.ThreadReference.forceEarlyReturn.forceEarlyReturn004.forceEarlyReturn004
 *        nsk.jdi.ThreadReference.forceEarlyReturn.forceEarlyReturn004.forceEarlyReturn004a
 *        nsk.jdi.ThreadReference.forceEarlyReturn.forceEarlyReturn005.forceEarlyReturn005
 *        nsk.jdi.ThreadReference.forceEarlyReturn.forceEarlyReturn005.forceEarlyReturn005a
 *        nsk.jdi.ThreadReference.forceEarlyReturn.forceEarlyReturn008.forceEarlyReturn008
 *        nsk.jdi.ThreadReference.forceEarlyReturn.forceEarlyReturn008.forceEarlyReturn008a
 *        nsk.jdi.ThreadReference.forceEarlyReturn.forceEarlyReturn013.forceEarlyReturn013
 *        nsk.jdi.ThreadReference.forceEarlyReturn.forceEarlyReturn013.forceEarlyReturn013a
 *        nsk.jdi.ThreadReference.forceEarlyReturn.forceEarlyReturn014.forceEarlyReturn014
 *        nsk.jdi.ThreadReference.forceEarlyReturn.forceEarlyReturn014.forceEarlyReturn014a
 *
 * @build nsk.share.jdi.SerialExecutionDebugger
 * @run main/othervm/native
 *      nsk.share.jdi.SerialExecutionDebugger
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="-Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions
 *                       -XX:+WhiteBoxAPI ${test.vm.opts} ${test.java.opts}"
 *      -testClassPath ${test.class.path}
 *      -configFile ${test.src}/forceEarlyReturn001.tests
 */

