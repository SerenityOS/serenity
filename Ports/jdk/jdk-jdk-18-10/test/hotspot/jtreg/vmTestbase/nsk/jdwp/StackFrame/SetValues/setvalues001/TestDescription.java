/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @summary converted from VM Testbase nsk/jdwp/StackFrame/SetValues/setvalues001.
 * VM Testbase keywords: [quick, jpda, jdwp]
 * VM Testbase readme:
 * DESCRIPTION
 *     This test performs checking for
 *         command set: StackFrame
 *         command: SetValues
 *     Test checks that debugee accept the command packet and
 *     replies with correct reply packet. Also test checks that
 *     new values of the method local variables is the same
 *     as expected.
 *     Test consists of two compoments:
 *         debugger: setvalues001
 *         debuggee: setvalues001a
 *     To set values to the method local variables for the current
 *     thread frame and check that they are set correctly, test defines
 *     following classes into debuggee VM:
 *         OriginalValuesClass - class with original values of static fields
 *         TargetValuesClass - class with target values of static fields
 *         TestedObjectClass - tested class with tested method with local variables
 *         TestedThreadClass - thread class with tested frame
 *     First, debugger uses nsk.share support classes to launch debuggee
 *     and obtain Transport object, that represents JDWP transport channel.
 *     Also communication channel (IOPipe) is established between
 *     debugger and debuggee to exchange with synchronization signals.
 *     Next, debugger obtains from debuggee classID for tested class
 *     and object ID and threadID as values of the class static fields.
 *     Also debugger obtains values of static fields of the class TargetValuesClass
 *     to use them as new values for local variables from the tested frame.
 *     Debugger suspends the thread and gets frameID for the current thread
 *     frameID and indexes of its local variables. Thread is suspended on the
 *     invokation of the tested method with local variables.
 *     Then, debugger creates command packet for StackFrame.SetValues
 *     command with the found threadID, frameID and new values for local
 *     variables as arguments, writes packet to the transport channel,
 *     and waits for a reply packet.
 *     When reply packet is received, debugger parses the packet structure
 *     and extracts variables values. Debugger checks that no data returmed
 *     with the reply packet. Also debugger sends debuggee signal RUN to
 *     to check new values of the local variables. If all values are set
 *     correctly debuggee replies with the signal DONE.
 *     Finally, debugger sends debuggee signal to quit, waits for it exits
 *     and exits too with the proper exit code.
 *
 * @library /vmTestbase /test/hotspot/jtreg/vmTestbase
 *          /test/lib
 * @build nsk.jdwp.StackFrame.SetValues.setvalues001a
 * @comment debuggee should be compiled w/ debug info
 * @clean nsk.jdwp.StackFrame.SetValues.setvalues001a
 * @compile -g:lines,source,vars ../setvalues001a.java
 * @run main/othervm
 *      nsk.jdwp.StackFrame.SetValues.setvalues001
 *      -arch=${os.family}-${os.simpleArch}
 *      -verbose
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

