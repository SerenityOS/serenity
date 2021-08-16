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
 * @summary converted from VM Testbase nsk/jdi/AttachingConnector/attach/attach003.
 * VM Testbase keywords: [quick, jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION:
 *     The test for the boundary value of the parameters.
 *     The test checks up that the method
 *     com.sun.jdi.connect.AttachingConnector.attach(Map)
 *     correctly works for the boundary value of parameter, throws described
 *     exceptions and complies with its spec:
 *     public VirtualMachine attach(Map arguments)
 *                           throws IOException,
 *                                  IllegalConnectorArgumentsException
 *          Attaches to a running application and and returns a mirror of its VM.
 *          The connector uses the given argument map in attaching the application.
 *          These arguments will include addressing information that identifies
 *          the VM. The argument map associates argument name strings to instances
 *          of Connector.Argument. The default argument map for a connector can be
 *          obtained through Connector.defaultArguments(). Argument map values can
 *          be changed, but map entries should not be added or deleted.
 *          Parameters:
 *              arguments - the argument map to be used in launching the VM.
 *          Returns:
 *              the VirtualMachine mirror of the target VM.
 *          Throws:
 *              IOException - when unable to attach. Specific exceptions are
 *              dependent on the Connector implementation in use.
 *              IllegalConnectorArgumentsException - when one of the connector
 *              arguments is invalid.
 *     The test check up two cases:
 *         1. Parameter has <null> value. In this case NullPointerException
 *            is expected.
 *         2. Parameter has wrong arguments. In this case
 *            IllegalConnectorArgumentsException is expected.
 *     In case of error the test produces the return value 97 and a corresponding
 *     error message(s). Otherwise, the test is passed and produces the return
 *     value 95 and no message.
 * COMMENTS:
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.AttachingConnector.attach.attach003
 * @run main/othervm
 *      nsk.jdi.AttachingConnector.attach.attach003
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

