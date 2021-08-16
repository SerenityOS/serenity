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
 * @summary converted from VM Testbase nsk/jdi/ListeningConnector/stopListening/stoplis001.
 * VM Testbase keywords: [quick, jpda, jdi, nonconcurrent]
 * VM Testbase readme:
 * DESCRIPTION
 *     The test exercises JDI function ListeningConnector.stopListening.
 *     The Socket Connector is using as listening connector.
 *     The test cases include:
 *     - checking that ListeningConnector.stopListening throws
 *       an Exception if it has been invoked with argument map different
 *       from the one given for a previous ListeningConnector.startListening()
 *       invocation;
 *     - checking that listening can be successfully stopped if given
 *       argument map is the same with the one given for the previous
 *       ListeningConnector.startListening() invocation.
 *
 *     NOTE: this test is tagged "nonconcurrent" because it uses the default
 *     "javadebug" shmem file, as do some other tests.
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.ListeningConnector.stopListening.stoplis001
 * @run main/othervm
 *      nsk.jdi.ListeningConnector.stopListening.stoplis001
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

