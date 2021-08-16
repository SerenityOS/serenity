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
 * @key randomness
 *
 * @summary converted from VM Testbase nsk/jdi/ReferenceType/fieldByName/fieldbyname003.
 * VM Testbase keywords: [diehard, jpda, jdi, nonconcurrent]
 * VM Testbase readme:
 * DESCRIPTION
 *         nsk/jdi/ReferenceType/fieldByName/fieldbyname003 test
 *         checks the fieldByName(...) method of ReferenceType interface
 *         of the com.sun.jdi package for UNLOADED class:
 *         the test loads a class, gets a ReferenceType instance for this
 *         class, then enforces the class to be unloaded and calls the
 *         fieldByName(...) method - the com.sun.jdi.ObjectCollectedException
 *         should be thrown in this case.
 * COMMENTS
 *   Fixed test due to bug
 *         4463674: TEST_BUG: some JDI tests are timing dependent
 *   The test was modified to comply with new execution scheme
 *   to have separate directory for precompiled classes:
 *         - fieldbyname003b class was moved in 'loadclass' subdirectory;
 *         - package name was added in fieldbyname003b class;
 *         - ${COMMON_CLASSES_LOCATION} instead of ${TESTDIR} in .cfg file;
 *         - ClassUnloader seekes for fieldbyname003b class in
 *           ${COMMON_CLASSES_LOCATION}/loadclass directory.
 *   4505735 equals002 and other tests fail with merlin
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.ReferenceType.fieldByName.fieldbyname003
 *        nsk.jdi.ReferenceType.fieldByName.fieldbyname003a
 *
 * @comment compile loadclassXX to bin/loadclassXX
 * @run driver nsk.share.ExtraClassesBuilder
 *      loadclass
 *
 * @run main/othervm
 *      nsk.jdi.ReferenceType.fieldByName.fieldbyname003
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}" ./bin
 */

