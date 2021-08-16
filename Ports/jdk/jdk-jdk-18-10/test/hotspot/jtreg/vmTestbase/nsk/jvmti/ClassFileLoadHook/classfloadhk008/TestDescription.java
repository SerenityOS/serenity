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
 * @summary converted from VM Testbase nsk/jvmti/ClassFileLoadHook/classfloadhk008.
 * VM Testbase keywords: [quick, jpda, jvmti, noras, redefine]
 * VM Testbase readme:
 * DESCRIPTION
 *     This JVMTI test exercises JVMTI event ClassFileLoadHook().
 *     This test checks if CLASS_FILE_LOAD_HOOK event is received
 *     for class being redefined and the new instrumented bytecode
 *     returned from this event callback will be used for tested
 *     class after redefinition.
 *     The tested class is loaded with default classloader.
 *     This test uses three different bytecodes of the tested class
 *     'classfloadhk008r' compiled from the different sources.
 *     This directory contains original source of the tested class,
 *     which is used to load class with default loader in debuggee class.
 *     The subdirectory 'newclass' contains redefined class
 *     with changed implementation of methods.
 *     The subdirectory 'newclass01' contains new instrumented class
 *     with one more changed implementation of methods.
 *     The test performs the following checks:
 *         - CLASS_FILE_LOAD_HOOK event callback is invoked and only once
 *           for the tested class upon its redefinition
 *         - pointers to new redefined bytecode and size passed to
 *           the event callback are not NULL
 *         - the new instrumented bytecode is successfully returned
 *           from the event callback
 *         - the new instrumented bytecode of the tested class is actually
 *           used after class redefinition
 *     The debuggee class preliminary loads redefined and instrumented
 *     bytecodes of the tested class into static fields and agent gets
 *     these bytecodes to use them in CLASS_FILE_LOAD_HOOK event callback.
 *     The debuggee loads tested class using default classloader.
 *     The agent enables CLASS_FILE_LOAD_HOOK event and redefines
 *     tested class with the redefined bytecode.
 *     Upon receiving CLASS_FILE_LOAD_HOOK event the agent performs checks
 *     and replaces redefined bytecode with the new instrumented bytecode.
 *     If no CLASS_FILE_LOAD_HOOK events were received, the agent
 *     complains an error.
 *     After tested class has been redefined debuggee uses reflection
 *     API to call static method of this class. If method returns value
 *     according to the new instrumented implementation, then the new
 *     bytecode was actually used for creation of tested class. Otherwise,
 *     if method returns value according to the original or redefined
 *     implementation, or any unexpected value, then the test complains
 *     an error.
 *     The agent does not deallocate memory used for instrumented
 *     bytecode, because this is VM responsibility.
 *     If all checks are successfull, the test passes with exit code 95,
 *     otherwise the test fails with exit code 97.
 * COMMENTS
 *     Modified due to fix of the rfe
 *     5010823 TEST_RFE: some JVMTI tests use the replaced capability
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jvmti.ClassFileLoadHook.classfloadhk008
 *        nsk.jvmti.ClassFileLoadHook.classfloadhk008r
 *
 * @comment compile newclassXX to bin/newclassXX
 * @run driver nsk.share.ExtraClassesBuilder
 *      newclass newclass01
 *
 * @run main/othervm/native
 *      -agentlib:classfloadhk008=-waittime=5
 *      nsk.jvmti.ClassFileLoadHook.classfloadhk008
 *      ./bin
 */

