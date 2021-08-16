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
 * @summary converted from VM Testbase nsk/jvmti/scenarios/bcinstr/BI01/bi01t002.
 * VM Testbase keywords: [quick, jpda, jvmti, noras, redefine]
 * VM Testbase readme:
 * DESCRIPTION
 *     This JVMTI test is developed against "bytecode instrumentation" area.
 *     The test performs the following actions:
 *         - load a class by a custom loader L1, source of loaded class
 *           is loadclass/bi01t001a.java;
 *         - instrument methodA() on receiving CLASS_FILE_LOAD_HOOK event;
 *         - load the same class by another custom loader L2;
 *         - instrument methodA() in another way on receiving
 *           CLASS_FILE_LOAD_HOOK event;
 *         - check that instrumentation codes work correctly for every case.
 *         - redefine the class with original version of methodA() for every
 *           pair (C, L1), (C, L2)
 *         - check that that original bytecode is restored.
 *     The test pass in case original bytecode works after all steps.
 * COMMENTS
 *     Modified due to fix of rfe:
 *     #5055417 TEST: warnings and notes caused by generification
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jvmti.scenarios.bcinstr.BI01.bi01t002
 *
 * @comment compile loadclassXX to bin/loadclassXX
 * @run driver nsk.share.ExtraClassesBuilder
 *      loadclass
 *
 * @comment compile newclassXX to bin/newclassXX
 * @run driver nsk.share.ExtraClassesBuilder
 *      newclass02 newclass01
 *
 * @run main/othervm/native
 *      -agentlib:bi01t002=-waittime=5
 *      nsk.jvmti.scenarios.bcinstr.BI01.bi01t002
 *      ./bin
 */

