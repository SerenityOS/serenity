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
 * @summary converted from VM Testbase nsk/jvmti/AddToSystemClassLoaderSearch/systemclssearch005.
 * VM Testbase keywords: [quick, jpda, jvmti, onload_only_logic, noras, vm6]
 * VM Testbase readme:
 * DESCRIPTION
 *     This JVMTI test exercises JVMTI thread function AddToSystemClassLoaderSearch()
 *     in Live phase.
 *     This test checks that AddToSystemClassLoaderSearch() adds segment
 *     to system class search path in Live phase and system classloader
 *     will find debuggee class using that segment.
 *     Debuggee class does not have direct superclass to be loaded too.
 *     The following checks are performed by the test:
 *         - AddToSystemClassLoaderSearch() returns no errors in Live phase
 *             - in particular it checks that anything other than an
 *               existing JAR file is an invalid path
 *         - debuggee class will be found by system classloader using added segment
 *     Source of debuggee class 'systemclssearch001' is located in 'systemclssearch001.jar'
 *     which is located in special subdirectory 'newclass/nsk/jvmti/AddToSystemClassLoaderSearch/'.
 *     It will be compiled to
 *     '$COMMON_CLASSES_LOCATION/newclass/nsk/jvmti/AddToSystemClassLoaderSearch/systemclssearch001.jar'
 *     jar file, where it usually can not be found by system classloader.
 *     The agent adds this jar to the system class search path
 *     in VM_INIT callback(), and thus the debuggee class should be found by
 *     system classloader and executed normally.
 *     If AddToSystemClassLoaderSearch() does not adds segment to system
 *     class search path, then debuggee class is not found and VM fails on
 *     initialization.
 *     Otherwise, if debuggee class is found, loaded, executed, it returns PASS
 *     and the test passes with exit code 95.
 * COMMENTS
 *     Ported from systemclssearch001.
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.share.jvmti.JVMTITest
 *        nsk.share.Consts
 *
 * @comment compile ../systemclssearch001/newclassXX to bin/newclassXX
 * @run driver nsk.share.ExtraClassesBuilder
 *      ../systemclssearch001/newclass
 *
 * @comment create systemclssearch001.jar in current directory
 * @run driver ExecDriver --cmd
 *      ${compile.jdk}/bin/jar
 *      -cf systemclssearch001.jar
 *      -C ./bin/newclass/
 *      nsk/jvmti/AddToSystemClassLoaderSearch/systemclssearch001.class
 *
 * @comment ExecDriver is used b/c main class isn't on source/class path
 * @run main/othervm/native ExecDriver --java
 *      -agentlib:systemclssearch_agent=-waittime=5,phasetocheck=live,segment1=systemclssearch001.jar
 *      nsk.jvmti.AddToSystemClassLoaderSearch.systemclssearch001
 */

