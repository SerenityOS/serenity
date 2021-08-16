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
 * @summary converted from VM Testbase nsk/jdwp/ObjectReference/SetValues/setvalues001.
 * VM Testbase keywords: [quick, jpda, jdwp]
 * VM Testbase readme:
 * DESCRIPTION
 *     This test performs checking for
 *         command set: ObjectReference
 *         command: SetValues
 *     Test checks that debugee accept the command packet and
 *     replies with correct reply packet. Also test checks that
 *     the values are correctly set to debuggee class fields.
 *     Test consists of two compoments:
 *         debugger: setvalues001
 *         debuggee: setvalues001a
 *     To set values to the static fields of the tested class
 *     and check that they are set correctly, test defines
 *     following classes into debuggee VM:
 *         OriginalValuesClass - class with original values of static fields
 *         TargetValuesClass - class with target values of static fields
 *         TestedClass - tested class with tested fields to set values to
 *         ObjectClass - class with one static field for the tested object
 *     First, debugger uses nsk.share support classes to launch debuggee
 *     and obtain Transport object, that represents JDWP transport channel.
 *     Also communication channel (IOPipe) is established between
 *     debugger and debuggee to exchange with synchronization messages.
 *     Next, debugger obtains classIDs for the tested class and class with
 *     the target values from debugee. It obtains also fieldIDs for thess
 *     classese and target values of the fields. It obtains also objectID
 *     as the value of a static field of the object class.
 *     Then, debugger creates command packet for SetValues command with the
 *     found objectID and list of fieldIDs as arguments, writes packet
 *     to the transport channel, and waits for a reply packet.
 *     When reply packet is received, debugger parses the packet structure
 *     and and checks that there is no data in the reply packet.
 *     Then debugger sends signal RUN to debuggee to ask it to verify
 *     new fields values of tested class. Debuggee compares these values
 *     with the original and target values and sends ERROR signal
 *     to debugger if the values was not set correctly.
 *     Finally, debugger sends debuggee signal to quit, waits for it exits
 *     and exits too with the proper exit code.
 *
 * @library /vmTestbase /test/hotspot/jtreg/vmTestbase
 *          /test/lib
 * @build nsk.jdwp.ObjectReference.SetValues.setvalues001a
 * @run main/othervm
 *      nsk.jdwp.ObjectReference.SetValues.setvalues001
 *      -arch=${os.family}-${os.simpleArch}
 *      -verbose
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

