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
 * @summary converted from VM Testbase nsk/jvmti/AttachOnDemand/attach024.
 * VM Testbase keywords: [quick, jpda, jvmti, noras, feature_282, vm6, jdk]
 * VM Testbase readme:
 * Description :
 *     Test tries to load java agent to the VM after the VM has started using
 *     Attach API (com.sun.tools.attach).
 *     Test is based on the nsk.share.aod framework. In this framework each java
 *     agent starts new thread from the 'agentmain' method, and all test checks are executed
 *     in this thread.
 *     Test checks that agent's JAR file is appended at the end of the system class path.
 *     Agent's JAR file contains modified class java.util.TooManyListenersException (it is assumed
 *     that this class isn't loaded before agent is loaded), agent instantiates TooManyListenersException
 *     and checks that non-modified version of this class was loaded from rt.jar (not from agent's JAR).
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.share.aod.AODTestRunner
 *        nsk.share.aod.TargetApplicationWaitingAgents
 *        nsk.jvmti.AttachOnDemand.attach024.attach024Agent00
 *
 * @comment compile modified java.util.TooManyListenersException
 * @run driver ExecDriver --cmd
 *      ${compile.jdk}/bin/javac
 *      -cp ${test.class.path}
 *      -d ./bin/classes
 *      --patch-module java.base=${test.src}/java.base
 *      --add-reads java.base=ALL-UNNAMED
 *      ${test.src}/java.base/java/util/TooManyListenersException.java
 *
 * @comment create attach024Agent00.jar in current directory
 * @run driver ExecDriver --cmd
 *      ${compile.jdk}/bin/jar
 *      -cfm attach024Agent00.jar ${test.src}/attach024Agent00.mf
 *      -C ./bin/classes
 *      java/util/TooManyListenersException.class
 * @run driver jdk.test.lib.helpers.ClassFileInstaller
 *      nsk.jvmti.AttachOnDemand.attach024.attach024Agent00
 * @run driver ExecDriver --cmd
 *      ${compile.jdk}/bin/jar
 *      -uf attach024Agent00.jar
 *      nsk/jvmti/AttachOnDemand/attach024/attach024Agent00.class
 *
 * @run main/othervm
 *      nsk.share.aod.AODTestRunner
 *      -jdk ${test.jdk}
 *      -javaOpts="--add-reads java.base=ALL-UNNAMED -XX:+UsePerfData ${test.vm.opts} ${test.java.opts}"
 *      -target nsk.share.aod.TargetApplicationWaitingAgents
 *      -ja attach024Agent00.jar
 */

