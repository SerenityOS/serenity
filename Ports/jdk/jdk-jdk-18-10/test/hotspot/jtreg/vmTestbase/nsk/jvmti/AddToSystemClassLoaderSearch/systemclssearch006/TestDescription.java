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
 * @summary converted from VM Testbase nsk/jvmti/AddToSystemClassLoaderSearch/systemclssearch006.
 * VM Testbase keywords: [quick, jpda, jvmti, onload_only_logic, noras, vm6]
 * VM Testbase readme:
 * DESCRIPTION
 *     This JVMTI test exercises JVMTI thread function AddToSystemClassLoaderSearch()
 *     in Live phase.
 *     This test checks that AddToSystemClassLoaderSearch() adds segment
 *     to system class search path and system classloader will find
 *     debuggee class using that segment while loading debugge supper class
 *     from original classpath.
 *     The following checks are performed by the test:
 *         - AddToSystemClassLoaderSearch() returns no errors in OnLoad phase
 *         - debuggee class will be loaded from system
 *           classpath using added segment
 *         - superclass needed for debuggee class will be found by system
 *           classloader using original classpath
 *     Source of debuggee class 'systemclssearch002' is located in 'systemclssearch002.jar'
 *     which is located in subdirectory 'newclass/nsk/jvmti/AddToSystemClassLoaderSearch/'.
 *     It will be compiled to
 *     '$COMMON_CLASSES_LOCATION/newclass/nsk/jvmti/AddToSystemClassLoaderSearch/systemclssearch002.jar'
 *     jar file where it usually cannot be found by system classloader.
 *     If AddToSystemClassLoaderSearch() does not adds segment to system
 *     classloader search path, then debuggee class is not found and VM
 *     fails on initialization.
 *     If system classloader cannot find superclass located by the original
 *     system classpath, then VM also fails on initialization.
 *     If segment is successfully added and debuggee super class are found, loaded, and executed,
 *     then it returns PASS. and the test passes with exit code 95.
 * COMMENTS
 *     Ported from systemclssearch002.
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jvmti.AddToSystemClassLoaderSearch.systemclssearch002p
 *        nsk.share.jvmti.JVMTITest
 *
 * @comment compile ../systemclssearch002/newclassXX to bin/newclassXX
 * @run driver nsk.share.ExtraClassesBuilder
 *      ../systemclssearch002/newclass
 *
 * @comment create systemclssearch002.jar in current directory
 * @run driver ExecDriver --cmd
 *      ${compile.jdk}/bin/jar
 *      -cf systemclssearch002.jar
 *      -C ./bin/newclass/
 *      nsk/jvmti/AddToSystemClassLoaderSearch/systemclssearch002.class
 *
 * @comment ExecDriver is used b/c main class isn't on source/class path
 * @run main/othervm/native ExecDriver --java
 *      -agentlib:systemclssearch_agent=-waittime=5,phasetocheck=live,segment1=systemclssearch002.jar
 *      nsk.jvmti.AddToSystemClassLoaderSearch.systemclssearch002
 */

