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
 * @summary converted from VM Testbase nsk/jdwp/ClassType/NewInstance/newinst001.
 * VM Testbase keywords: [quick, jpda, jdwp]
 * VM Testbase readme:
 * DESCRIPTION
 *     This test performs checking for
 *         command set: ClassType
 *         command: NewInstance
 *     Test checks that debugee accept the command packet and
 *     replies with correct reply packet. Also test checks that
 *     the tested constructor is really invoked into debuggee.
 *     Test consists of two compoments:
 *         debugger: newinst001
 *         debuggee: newinst001a
 *     First, debugger uses nsk.share.* support classes to launch debuggee
 *     and obtain Transport object, that represents JDWP transport channel.
 *     Next, debugger waits for tested class loaded, requests methodID
 *     for tested constructtor, sets breakpoint and wait for breakpoint reached.
 *     When breakpoint event is received the tested thread into debuggee
 *     is suspended by this event.
 *     Then, debugger creates command packet for classType.NewInstance
 *     command with the found classID, methodID, threadID, and also adds one
 *     integer argument for the constructor invocation. Then debugger writes
 *     packet to the transport channel and waits for a reply packet.
 *     When reply packet is received, debugger parses the packet structure
 *     and extracts tegged objectID's of new created object and exception.
 *     Test checks if objectID is not null and exception objectID is null.
 *     Also test gets value of static field, wich should be modified by
 *     the invoked constructor, to verify if this constructor was really
 *     invoked into debuggee.
 *     Finally, debugger disconnects debuggee, waits for it exits
 *     and exits too with the proper exit code.
 *
 * @library /vmTestbase /test/hotspot/jtreg/vmTestbase
 *          /test/lib
 * @build nsk.jdwp.ClassType.NewInstance.newinst001a
 * @run main/othervm
 *      nsk.jdwp.ClassType.NewInstance.newinst001
 *      -arch=${os.family}-${os.simpleArch}
 *      -verbose
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

