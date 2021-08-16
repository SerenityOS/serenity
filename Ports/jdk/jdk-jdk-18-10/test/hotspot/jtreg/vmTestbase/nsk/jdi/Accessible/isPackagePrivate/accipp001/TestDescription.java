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
 * @summary converted from VM Testbase nsk/jdi/Accessible/isPackagePrivate/accipp001.
 * VM Testbase keywords: [jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION
 *         nsk/jdi/Accessible/isPackagePrivate/isPackagePrivate001 test
 *         checks the isPrivatePackage() method of Accessible interface
 *         of the com.sun.jdi package for ArrayType, ClassType, InterfaceType.
 * COMMENTS
 * ---------
 *     The test is updated to use new share classes as follows:
 *     - two lines, 31 and 32, with argsHandler and logHandler are added
 *     - statements with definitions, lines 87-100, are added;
 *       obsolete lines are removed;
 *     - all "complain" are replaced with "logHandler.complain"..
 *     - all "display" are replaced with "logHandler.display"
 * --------
 *     I. To fix the bug 4554254, the following is done:
 *     file accipp001.java:
 *     - additional check : !knownClasses[i][1].equals("private") &&
 *       is added to the statement in lines 131-133
 *     file accipp001a.java:
 *     - initialization values are added in variable declarations in lines
 *       26-36, 47.
 *     II. As the test doesn't contain checks on both short and Short (?),
 *     two lines with variables of both types are added.
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.Accessible.isPackagePrivate.accipp001
 *        nsk.jdi.Accessible.isPackagePrivate.accipp001a
 * @run main/othervm
 *      nsk.jdi.Accessible.isPackagePrivate.accipp001
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

