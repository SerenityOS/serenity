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
 * @summary converted from VM Testbase nsk/jvmti/scenarios/bcinstr/BI04/bi04t002.
 * VM Testbase keywords: [quick, jpda, jvmti, onload_only_logic, noras, redefine, no_cds]
 * VM Testbase readme:
 * DESCRIPTION
 *     This JVMTI test is developed against "bytecode instrumentation" area.
 *     The test performs the following actions:
 *         - instrument all java methods of java.lang.Object via RedefineClasses
 *           function;
 *         - check that instrumentation code works.
 *     There are some notes regarding new bytecode for java.lang.Object class:
 *      - new bytecode for java.lang.Object class is precompiled and its
 *        classfile is placed in $COMMON_CLASSES_LOCATION/newclass02 directory;
 *      - source of new bytecode for java.lang.Object is the modified original
 *        source file borrowed from JDK 1.5.0-beta2-b43, thus it should be
 *        compiled with the '-source 1.5' option;
 *      - every java-method of instrumeneted java.lang.Object class invokes
 *        a method of test which carries out special action in order to register
 *        invocation, so $COMMON_CLASSES_LOCATION/classes must be added to the
 *        default bootstrap class path;
 *     These points could be considered as a negative side of this tests.
 *     As alternative way - java.lang.Object could be instrumented by bytecode
 *     generated _on_the_fly_. But such implementation would complicate test.
 * COMMENTS
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jvmti.scenarios.bcinstr.BI04.bi04t002
 *        nsk.jvmti.scenarios.bcinstr.BI04.bi04t002a
 *
 * @comment compile newclassXX to bin/newclassXX
 * @run driver ExecDriver --cmd
 *      ${compile.jdk}/bin/javac
 *      --patch-module java.base=${test.src}/newclass02/java.base
 *      -d bin/newclass02
 *      -cp ${test.class.path}
 *      --add-reads=java.base=ALL-UNNAMED
 *      ${test.src}/newclass02/java.base/java/lang/Object.java
 *
 * @run main/othervm/native
 *      --add-reads=java.base=ALL-UNNAMED
 *      -XX:+UnlockDiagnosticVMOptions
 *      -XX:-CheckIntrinsics
 *      -Xbootclasspath/a:${test.class.path}
 *      -agentlib:bi04t002=pathToNewByteCode=./bin,-waittime=5
 *      nsk.jvmti.scenarios.bcinstr.BI04.bi04t002
 */

