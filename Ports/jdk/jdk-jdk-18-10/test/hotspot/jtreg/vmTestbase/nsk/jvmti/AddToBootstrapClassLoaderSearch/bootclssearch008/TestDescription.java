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
 * @summary converted from VM Testbase nsk/jvmti/AddToBootstrapClassLoaderSearch/bootclssearch008.
 * VM Testbase keywords: [quick, jpda, jvmti, onload_only_logic, noras, vm6, no_cds]
 * VM Testbase readme:
 * DESCRIPTION
 *     This JVMTI test exercises JVMTI thread function AddToBootstrapClassLoaderSearch()
 *     in Live phase (actually only in VM_INIT callback).
 *     This test checks that AddToBootstrapClassLoaderSearch() can add two segments
 *     to bootstrap class search path in Live phase, and debuggee class will be
 *     loaded from the first segment even if it is found in the second segment too,
 *     The following checks are performed by the test:
 *         - AddToBootstrapClassLoaderSearch() adds two segments and returns
 *           no errors in Live phase
 *             - also it checks that anything other than an
 *               existing JAR file is an invalid path
 *         - debuggee class located by the first added segment will be loaded
 *         - debuggee class located by the second added segment will NOT be loaded
 *     There are two different implementations of debuggee class 'bootclssearch004'.
 *     Source of "positive" implementation is located in jar file 'bootclssearch004.jar'
 *     which is located in subdirectory 'newclass01/nsk/jvmti/AddToBootstrapClassLoaderSearch/'.
 *     It will be compiled to
 *     '$COMMON_CLASSES_LOCATION/newclass01/nsk/jvmti/AddToBootstrapClassLoaderSearch/bootclssearch004.jar',
 *     that will be added first to bootstrap class search path.
 *     This implementation returns PASS.
 *     Source of "negative" implementation is located in jar file 'bootclssearch004.jar'
 *     which is located in subdirectory 'newclass02/nsk/jvmti/AddToBootstrapClassLoaderSearch/'.
 *     It will be compiled to
 *     '$COMMON_CLASSES_LOCATION/newclass02/nsk/jvmti/AddToBootstrapClassLoaderSearch/bootclssearch004.jar',
 *     that will be added second to bootstrap class search path.
 *     This implementation returns FAIL.
 *     The agent adds both jars to the bootstrap class search path
 *     in VM_INIT callback, and expects that positive implementation of debuggee class
 *     from the first added segment will be loaded and executed.
 *     If AddToBootstrapClassLoaderSearch() returns error in VM_INIT callback,
 *     then VM fails on initialization.
 *     If negative implementation of debuggee class is loaded and executed,
 *     then it returns FAIL and test fails with exit code 97.
 *     Otherwise, if both segments are successfully added and positive implementation
 *     of debuggee class is loaded and executed, then it returns PASS and test
 *     passes with exit code 95.
 * COMMENTS
 *     Ported from bootclssearch004.
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.share.Consts
 *
 * @comment compile ../bootclssearch004/newclassXX to bin/newclassXX
 * @run driver nsk.share.ExtraClassesBuilder
 *      ../bootclssearch004/newclass01
 *      ../bootclssearch004/newclass02
 *
 * @comment create bootclssearch004.jar in ./bin/newclass01/
 * @run driver ExecDriver --cmd
 *      ${compile.jdk}/bin/jar
 *      -cf ./bin/newclass01/bootclssearch004.jar
 *      -C ./bin/newclass01/
 *      nsk/jvmti/AddToBootstrapClassLoaderSearch/bootclssearch004.class
 *
 * @comment create bootclssearch004.jar in ./bin/newclass02/
 * @run driver ExecDriver --cmd
 *      ${compile.jdk}/bin/jar
 *      -cf ./bin/newclass02/bootclssearch004.jar
 *      -C ./bin/newclass02/
 *      nsk/jvmti/AddToBootstrapClassLoaderSearch/bootclssearch004.class
 *
 * @comment ExecDriver is used b/c main class isn't on source/class path
 * @run main/othervm/native ExecDriver --java
 *      -agentlib:bootclssearch_agent=-waittime=5,phasetocheck=live,segment1=./bin/newclass01/bootclssearch004.jar,segment2=./bin/newclass02/bootclssearch004.jar
 *      nsk.jvmti.AddToBootstrapClassLoaderSearch.bootclssearch004
 */

