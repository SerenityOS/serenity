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
 * @summary converted from VM Testbase nsk/jdi/ThreadReference/forceEarlyReturn/forceEarlyReturn010.
 * VM Testbase keywords: [jpda, jdi, feature_jdk6_jpda, vm6]
 * VM Testbase readme:
 * DESCRIPTION
 *         The test checks that a result of the method com.sun.jdi.forceEarlyReturn(Value value)
 *         complies with its specification. The test checks:
 *                 - forcing return on a thread with only one frame on the stack causes the thread to exit when resumed
 *                 - MethodExitEvent is generated as it would be in a normal return
 *         Test scenario:
 *         Test checks case when thread has single frame on the stack because of methods which this thread calls was inlined,
 *         Debugger VM forces debuggee VM start test thread which in it's run() method call in infinite loop
 *         methods which return const values, this methods should be inlined, so thread always has
 *         only one frame on stack. Debugger suspends debuggee VM, calls forceEarlyReturn() for test thread, resumes debuggee
 *         VM and checks that test thread has status ThreadReference.THREAD_STATUS_ZOMBIE(thread has completed execution) and
 *         checks that ThreadDeathEvent and MethodExitEvent was generated for test thread.
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.ThreadReference.forceEarlyReturn.forceEarlyReturn009.forceEarlyReturn009
 *        nsk.jdi.ThreadReference.forceEarlyReturn.forceEarlyReturn009.forceEarlyReturn009a
 * @run main/othervm
 *      nsk.jdi.ThreadReference.forceEarlyReturn.forceEarlyReturn009.forceEarlyReturn009
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="-XX:+Inline"
 *      -inlineType INLINE_METHOD_RETURNING_CONST
 */

