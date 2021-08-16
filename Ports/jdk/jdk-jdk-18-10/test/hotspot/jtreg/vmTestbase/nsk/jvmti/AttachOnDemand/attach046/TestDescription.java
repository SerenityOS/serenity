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
 * @summary converted from VM Testbase nsk/jvmti/AttachOnDemand/attach046.
 * VM Testbase keywords: [jpda, jvmti, noras, feature_282, vm6, jdk]
 * VM Testbase readme:
 * Description :
 *     Test tries to load 3 jvmti agents to the VM after the VM has started using
 *     Attach API (com.sun.tools.attach).
 *     Test is based on the nsk.share.aod framework. In the terms of this framework
 *     java application running in the VM where agent is loaded to is called 'target application'.
 *     In this test 3 jvmti agents try to redefine the same class from ClassLoad event handler.
 *     Target application checks that redefinition had effect.
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.share.aod.AODTestRunner
 *
 * @comment compile newclassXX to bin/newclassXX
 * @run driver nsk.share.ExtraClassesBuilder
 *      newclass02 newclass01 newclass00
 *
 * @build nsk.jvmti.AttachOnDemand.attach046.attach046Target
 * @run main/othervm/native
 *      nsk.share.aod.AODTestRunner
 *      -jdk ${test.jdk}
 *      -target nsk.jvmti.AttachOnDemand.attach046.attach046Target
 *      -javaOpts="-XX:+UsePerfData ${test.vm.opts} ${test.java.opts}"
 *      -na attach046Agent00=-pathToNewByteCode=./bin/newclass00,attach046Agent00=-pathToNewByteCode=./bin/newclass01,attach046Agent00=-pathToNewByteCode=./bin/newclass02
 */

