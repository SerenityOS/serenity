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
 * @summary converted from VM Testbase nsk/jvmti/RedefineClasses/redefclass019.
 * VM Testbase keywords: [quick, jpda, jvmti, noras, redefine]
 * VM Testbase readme:
 * DESCRIPTION
 *     The test exercises JVMTI function RedefineClasses(classCount, classDefs).
 *     The test creates a child thread, sets a breakpoint into method
 *     checkPoint,  then starts the thread which does some nesting calls
 *     up to the method checkPoint. Catching breakpoint the test redefines
 *     "TestThread" class and requests NotifyFramePop for the current
 *     frame. Catching the frame pop event the test checks their class,
 *     current line number, names and values of local varaibles, and requests
 *     NotifyFramePop for the next frame, and so on till the method "run".
 * COMMENTS
 *     This is a regression test on the following bug:
 *         4628726 RedefineClasses followed by up and locals cmds gets
 *                 JDWP Error 500
 *     Ported from JVMDI.
 *
 * @library /vmTestbase
 *          /test/lib
 *
 * @comment make sure redefclass019 is compiled with full debug info
 * @build nsk.jvmti.RedefineClasses.redefclass019
 * @clean nsk.jvmti.RedefineClasses.redefclass019
 * @compile -g:lines,source,vars ../redefclass019.java
 *
 * @comment compile newclassXX to bin/newclassXX
 *          with full debug info
 * @run driver nsk.share.ExtraClassesBuilder
 *      -g:lines,source,vars
 *      newclass_g
 *
 * @comment make sure redefclass019 is compiled with full debug info
 * @build nsk.jvmti.RedefineClasses.redefclass019
 * @clean nsk.jvmti.RedefineClasses.redefclass019
 * @compile -g:lines,source,vars ../redefclass019.java
 *
 * @run main/othervm/native -agentlib:redefclass019 nsk.jvmti.RedefineClasses.redefclass019 ./bin
 */

