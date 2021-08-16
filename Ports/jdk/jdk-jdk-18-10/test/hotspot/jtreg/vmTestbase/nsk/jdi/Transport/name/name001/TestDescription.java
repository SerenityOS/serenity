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
 * @summary converted from VM Testbase nsk/jdi/Transport/name/name001.
 * VM Testbase keywords: [quick, jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION:
 *     The test for the implementation of an object of the type
 *     Connector.Transport.
 *     The test checks up that a result of the method
 *     com.sun.jdi.connect.Connector.Transport.name()
 *     complies with its specification:
 *     public java.lang.String name()
 *     Returns a short identifier for the transport.
 *     Transport implementors should follow similar naming conventions as are
 *     used with packages to avoid name collisions. For example, the Sun transport
 *     implementations have names prefixed with "com.sun.jdi.".
 *     Returns: the name of this transport.
 *     Note. The name of this transport is either "dt_socket" or "dt_shmem"
 *     as it is defined in the document "Connection and Invocation Details".
 *     The test works as follows:
 *     - Virtual Machine Manager is invoked.
 *     - To Transport objects of Connectors
 *     the following check is applied:
 *     String returned by tTransport.name() should be
 *     either "dt_socket" or "dt_shmem"
 *     In case of error the test produces the return value 97 and
 *     a corresponding error message(s).
 *     Otherwise, the test is passed and produces
 *     the return value 95 and no message.
 * COMMENTS:
 *     Fixed 6265221. Transport.name() only checked for emptiness. Former behavior is unspecified and incorrect.
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.Transport.name.name001
 * @run main/othervm
 *      nsk.jdi.Transport.name.name001
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

