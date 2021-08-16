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
 * @summary converted from VM Testbase nsk/jvmti/RedefineClasses/redefclass002.
 * VM Testbase keywords: [quick, jpda, jvmti, noras, redefine]
 * VM Testbase readme:
 * DESCRIPTION
 *     This test checks that if any redefined methods have
 *     active stack frames, those active frames continue to run
 *     the bytecodes of the original method.
 *     It creates an instance of tested class redefclass002r and invokes
 *     redefclass002r.activeMethod(). The activeMethod() method is
 *     an active frame mentioned above.
 *     Then the test invokes native function makeRedefinition() that
 *     makes class file redifinition of the loaded class redefclass002r.
 *     Bytes of new version of the class redefclass002r are taken
 *     from the ./newclass directory.
 *     Finally, the test checks that the class redefclass002r has been
 *     really redefined, and at the same time the activeMethod() method
 *     stays original.
 * COMMENTS
 *     The test was changed due to the bug 4474524.
 *     Fixed according to the bug 4509016.
 *     Ported from JVMDI.
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jvmti.RedefineClasses.redefclass002
 *        nsk.jvmti.RedefineClasses.redefclass002r
 *
 * @comment compile newclassXX to bin/newclassXX
 * @run driver nsk.share.ExtraClassesBuilder
 *      newclass
 *
 * @run main/othervm/native -agentlib:redefclass002 nsk.jvmti.RedefineClasses.redefclass002 ./bin
 */

