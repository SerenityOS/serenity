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
 * @summary converted from VM Testbase nsk/jvmti/ClassFileLoadHook/classfloadhk006.
 * VM Testbase keywords: [quick, jpda, jvmti, noras, redefine]
 * VM Testbase readme:
 * DESCRIPTION
 *     This JVMTI test exercises JVMTI event ClassFileLoadHook().
 *     This test checks if CLASS_FILE_LOAD_HOOK event is received
 *     upon redefinition of tested class and correct class reference
 *     as well as original and redefined bytecodes are passed
 *     to the event callback.
 *     The tested class is loaded with default classloader.
 *     This test uses two different bytecodes of the tested class
 *     'classfloadhk006r' compiled from the different sources.
 *     This directory contains original source of the tested class,
 *     which is used to load class with default loader in debuggee class.
 *     The subdirectory 'newclass' contains redefined class
 *     with changed implementation of methods.
 *     The test performs the following checks:
 *         - CLASS_FILE_LOAD_HOOK event callback is invoked and only once
 *           for the tested class upon its redefinition
 *         - reference to redefined class passed to the event callback
 *           is equal to expected one
 *         - redefined bytecode passed to the event callback is equal
 *           to expected one
 *         - the new redefined bytecode of the tested class is actually
 *           used after class redefinition
 *     The debuggee class preliminary loads redefined bytecode of
 *     the tested class into static field and agent gets this
 *     bytecode to use it in CLASS_FILE_LOAD_HOOK event callback.
 *     The debuggee loads tested class using default classloader.
 *     The agent enables CLASS_FILE_LOAD_HOOK event and redefines
 *     tested class with the new bytecode.
 *     Upon receiving CLASS_FILE_LOAD_HOOK event the agent performs
 *     checks for class reference and bytecode passed to the event
 *     callback.
 *     If no CLASS_FILE_LOAD_HOOK events were received, the agent
 *     complains an error.
 *     After tested class has been redefined debuggee uses reflection
 *     API to call static method of this class. If method returns value
 *     according to the redefined implementation, then the new bytecode
 *     was actually used for redefinition of tested class. Otherwise,
 *     if method returns value according to the old implementation,
 *     or any unexpected value, then the test complains an error.
 *     If all checks are successfull, the test passes with exit code 95,
 *     otherwise the test fails with exit code 97.
 * COMMENTS
 *     Modified due to fix of the rfe
 *     5010823 TEST_RFE: some JVMTI tests use the replaced capability
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jvmti.ClassFileLoadHook.classfloadhk006
 *        nsk.jvmti.ClassFileLoadHook.classfloadhk006r
 *
 * @comment compile newclassXX to bin/newclassXX
 * @run driver nsk.share.ExtraClassesBuilder
 *      newclass
 *
 * @run main/othervm/native
 *      -agentlib:classfloadhk006=-waittime=5
 *      nsk.jvmti.ClassFileLoadHook.classfloadhk006
 *      ./bin
 */

