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
 * @summary converted from VM Testbase nsk/jdwp/EventRequest/Set/set002.
 * VM Testbase keywords: [quick, jpda, jdwp]
 * VM Testbase readme:
 * DESCRIPTION
 *     This test performs checking for
 *         command set: EventRequest
 *         command: Set
 *         event kind: VM_START
 *     Test checks that debuggee accepts tested command and replies
 *     with no error code for VM_START event request. The tests also
 *     checks that requested VM_START event is never received after
 *     initial automatically generated VM_START event.
 *     Test consists of two compoments:
 *         debugger: set002
 *         debuggee: set002a
 *     First, debugger uses nsk.share support classes to launch debuggee
 *     and obtain Transport object, that represents JDWP transport channel.
 *     Then, debugger creates creates command packet for command
 *     EventRequest.Set with VM_START event and no event modifiers;
 *     and sends it to debuggee. After reply packet is received
 *     debuggeer checks if it has no errors and valid requestID.
 *     Then, debugger lets debuggee to run and exit and waits for any event
 *     is received. If VM_START event is received, then debugger complains
 *     error and removes event request. Otherwise, if event is VM_DEATH
 *     the test passes.
 *     Finally, debugger disconnectes debuggee, waits for it exits
 *     and exits too with proper exit code.
 * COMMENTS
 *     Test fixed due to test bug:
 *     4909273 TEST_BUG: Fix nsk/jdwp/EventRequest/Set/set002
 *     Test fixed due to test bug:
 *     4973741 nsk/jdwp/EventRequest/Set/set002 expects request for VMStart event to fail
 *
 * @library /vmTestbase /test/hotspot/jtreg/vmTestbase
 *          /test/lib
 * @build nsk.jdwp.EventRequest.Set.set002a
 * @run main/othervm
 *      nsk.jdwp.EventRequest.Set.set002
 *      -arch=${os.family}-${os.simpleArch}
 *      -verbose
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

