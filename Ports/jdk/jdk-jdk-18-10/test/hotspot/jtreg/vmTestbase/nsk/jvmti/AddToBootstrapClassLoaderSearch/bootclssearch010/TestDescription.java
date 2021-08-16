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
 * @summary converted from VM Testbase nsk/jvmti/AddToBootstrapClassLoaderSearch/bootclssearch010.
 * VM Testbase keywords: [quick, jpda, jvmti, onload_only_logic, noras, vm6, no_cds]
 * VM Testbase readme:
 * DESCRIPTION
 *     This JVMTI test exercises JVMTI thread function AddToBootstrapClassLoaderSearch()
 *     in Live phase.
 *     This test checks that AddToBootstrapClassLoaderSearch() adds segment
 *     to bootstrap class search path and bootstrap classloader will find
 *     debuggee class using that segment.
 *     Debuggee class has direct superclass located in the original bootstrap classpath.
 *     The following checks are performed by the test:
 *         - AddToBootstrapClassLoaderSearch() returns no errors in Live phase
 *             - in particular it checks that anything other than an
 *               existing JAR file is an invalid path
 *         - debuggee class will be found by bootstrap classloader using added segment
 *         - superclass needed for debuggee class will be also found by bootstrap
 *           classloader though located in the original bootstrap classpath
 *         - superclass needed for debuggee class will be loaded from bootstrap
 *           classpath, not from usual classpath
 *     Source of debuggee class 'bootclssearch002' is located in 'bootclssearch002.jar'
 *     which is located in subdirectory 'newclass/nsk/jvmti/AddToBootstrapClassLoaderSearch/'.
 *     It will be compiled to
 *     '$COMMON_CLASSES_LOCATION/newclass/nsk/jvmti/AddToBootstrapClassLoaderSearch/bootclssearch002.jar'
 *     jar file where it usually cannot be found by bootstrap classloader.
 *     The superclass 'bootclssearch002p' has two different implementations.
 *     Source of "positive" implementation is located in subdirectory 'loadclass'.
 *     It will be compiled to '$COMMON_CLASSES_LOCATION/loadclass' that is added
 *     to original bootstrap classpath using option -Xbootclasspath/a.
 *     This implementation returns PASS.
 *     Source of "negative" implementation is located in the currect directory.
 *     It will be compiled to '$COMMON_CLASSES_LOCATION/classes' that is available
 *     from usual classpath.
 *     This implementation prints error and returns FAIL.
 *     The agent adds
 *     '$COMMON_CLASSES_LOCATION/newclass/nsk/jvmti/AddToBootstrapClassLoaderSearch/bootclssearch002.jar'
 *     jar file with debuggee
 *     class implementation to the bootstrap class search path in VM_INIT callback,
 *     and thus the debuggee class should be found by bootstrap classloader.
 *     While loading debuggee class the bootstrap classloader should find and load
 *     "positive" implementation of superclass available from original bootstrap
 *     classpath, but not "negative" implementation available from usual classpath.
 *     If AddToBootstrapClassLoaderSearch() does not adds segment to bootstrap
 *     classloader search path, then debuggee class is not found and VM
 *     fails on initialization.
 *     If bootstrap classloader cannot find superclass located by the original
 *     bootstrap classpath, then VM also fails on initialization.
 *     If bootstrap classloader loads "negative" implementation of superclass
 *     available from usual classpath, then the debuggee class returns FAIL,
 *     and the test fails with exit code 97.
 *     Otherwise, if segment is successfully added and debuggee class with positive
 *     implementation of superclass are found, loaded, and executed, then it
 *     returns PASS. and the test passes with exit code 95.
 * COMMENTS
 *     Ported from bootclssearch002.
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jvmti.AddToBootstrapClassLoaderSearch.bootclssearch002p
 *
 * @comment compile ../bootclssearch002/loadclassXX to bin/loadclassXX
 * @run driver nsk.share.ExtraClassesBuilder
 *      ../bootclssearch002/loadclass
 *
 * @comment compile ../bootclssearch002/newclassXX to bin/newclassXX
 * @run driver nsk.share.ExtraClassesBuilder
 *      ../bootclssearch002/newclass
 *
 * @comment create bootclssearch002.jar in current directory
 * @run driver ExecDriver --cmd
 *      ${compile.jdk}/bin/jar
 *      -cf bootclssearch002.jar
 *      -C ./bin/newclass/
 *      nsk/jvmti/AddToBootstrapClassLoaderSearch/bootclssearch002.class
 *
 * @comment ExecDriver is used b/c main class isn't on source/class path
 * @run main/othervm/native ExecDriver --java
 *      -Xbootclasspath/a:./bin/loadclass
 *      -agentlib:bootclssearch_agent=-waittime=5,phasetocheck=live,segment1=./bootclssearch002.jar
 *      nsk.jvmti.AddToBootstrapClassLoaderSearch.bootclssearch002
 */

