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
 * @summary converted from VM Testbase nsk/jdi/ThreadReference/forceEarlyReturn/forceEarlyReturn007.
 * VM Testbase keywords: [quick, jpda, jdi, feature_jdk6_jpda, vm6]
 * VM Testbase readme:
 * DESCRIPTION
 *         This is stress test for com.sun.jdi.forceEarlyReturn(Value value).
 *         The test do the same as forceEarlyReturn001, but do all checks 100 times and
 *         with multiple number of threads in debuggee VM.
 *         Test scenario:
 *         Debuggee VM start multiple number of threads(class nsk.share.jpda.ForceEarlyReturnTestThread is used) which
 *         sequentially call test methods with different return value type:
 *                 - void
 *                 - all primitive types
 *                 - String
 *                 - Object
 *                 - array of java.lang.Object
 *                 - Thread
 *                 - ThreadGroup
 *                 - Class object
 *                 - ClassLoader
 *         One of this threads is intended for force early return and call test method in loop 100 times, another threads
 *         execute until not stoped.
 *         Debugger VM create breakpoint requests for all this methods and add breakpoint request's filter only for
 *         thread which intended for force early return. When debuggee's test thread stop at breakpoint debugger call
 *         forceEarlyReturn() with follows parameters:
 *                 - try pass incompatible value to forceEarlyReturn (expect InvalidTypeException)
 *                 - force thread return with value defined at test thread class
 *         Test thread in debuggee VM intended for force return check that value returned from test methods equals predefined
 *         value and no instructions was executed in called method after force return (finally blocks are not executed too).
 *         Another debuggee's threads check that values returned from test methods aren't affected by force return.
 *         Debugger check that MethodExitEvent is generated after forceEarlyReturn.
 *         Debugger perform all this check for each tested method 100 times.
 *         This test execute test 'nsk.jdi.ThreadReference.forceEarlyReturn.forceEarlyReturn001' with follows parameters: '-iterationsNumber 100 -totalThreadsCount 20'
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.ThreadReference.forceEarlyReturn.forceEarlyReturn001.forceEarlyReturn001
 *        nsk.jdi.ThreadReference.forceEarlyReturn.forceEarlyReturn001.forceEarlyReturn001a
 * @run main/othervm
 *      nsk.jdi.ThreadReference.forceEarlyReturn.forceEarlyReturn001.forceEarlyReturn001
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 *      -iterationsNumber 50
 *      -totalThreadsCount 3
 */

