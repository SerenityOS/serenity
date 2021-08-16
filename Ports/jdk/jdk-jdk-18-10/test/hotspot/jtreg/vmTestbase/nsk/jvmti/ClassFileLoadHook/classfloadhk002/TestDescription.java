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
 * @summary converted from VM Testbase nsk/jvmti/ClassFileLoadHook/classfloadhk002.
 * VM Testbase keywords: [quick, jpda, jvmti, noras]
 * VM Testbase readme:
 * DESCRIPTION
 *     This JVMTI test exercises JVMTI event ClassFileLoadHook().
 *     This test checks if CLASS_FILE_LOAD_HOOK event with expected
 *     bytecode is received upon loading of tested class.
 *     The tested class is loaded with default classloader.
 *     The test performs the following checks:
 *         - CLASS_FILE_LOAD_HOOK event callback is invoked and only once
 *           for the tested class upon its loading
 *         - redefined class reference passed to callback for tested class
 *           is NULL
 *         - original bytecode passed to callback for tested class
 *           is equal to expected one
 *     The debuggee class preliminary loads bytecode of the tested class
 *     as a byte array into static field and agent gets this bytecode
 *     and then compares it with bytecode received with tested event.
 *     If all checks are successfull the test passes with exit code 95,
 *     otherwise the test fails with exit code 97.
 * COMMENTS
 *     Modified due to fix of the rfe
 *     5010823 TEST_RFE: some JVMTI tests use the replaced capability
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jvmti.ClassFileLoadHook.classfloadhk002r
 * @run main/othervm/native
 *      -agentlib:classfloadhk002=-waittime=5
 *      nsk.jvmti.ClassFileLoadHook.classfloadhk002
 *      ./bin
 */

