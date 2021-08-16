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
 * @summary converted from VM Testbase nsk/jdi/Connector/toString/tostring001.
 * VM Testbase keywords: [quick, jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION:
 *  The test for public toString() method of an implementing class of
 *  com.sun.jdi.Connector interface.
 *  The test checks an assertion:
 *     In conformity with the contract for java.lang.Object.toString(),
 *     the overrided method returns representing non-empty string for every
 *     connector returned in the VirtualMachineManager.allConnectors() list.
 * COMMENTS:
 *  The test is aimed to increase jdi source code coverage and checks
 *  the code which was not yet covered by previous tests for Connector
 *  interface. The coverage analysis was done for jdk1.4.0-b92 build.
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.Connector.toString.tostring001
 * @run main/othervm
 *      nsk.jdi.Connector.toString.tostring001
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

