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
 * @summary converted from VM Testbase nsk/jvmti/scenarios/hotswap/HS201/hs201t003.
 * VM Testbase keywords: [quick, jpda, jvmti, onload_only_caps, noras, redefine]
 * VM Testbase readme:
 * DESCRIPTION
 *     The test implements the JVMTI hotswap scenario HS201:
 *         - Enable events Exception, MethodExit, FramePop.
 *         - Call method a() which calls method b().
 *         - Throw an uncaught exception in the method b().
 *         - Redefine a class with the changed methods a() and b() within
 *           triggered callbacks Exception, MethodExit, FramePop caused by
 *           the uncaught exception. New version contains the changed methods
 *           a() and b(). Both methods should stay obsolete during their
 *           execution.
 *     The classfile redefinition and all checks are performed during the
 *     appropriate triggered callbacks. The methods are checked for
 *     obsolescence by analyzing length of the byte code array that
 *     implement the redefined method. The reason is that method ID from
 *     input parameters will be reasigned to the new method during
 *     execution of the very first callback but it will be already referred
 *     to the old method in the others subsequent callbacks in accordance
 *     with the JVMTI spec:
 *         An original method version which is not equivalent to
 *         the new method version is called obsolete and is assigned a new
 *         method ID; the original method ID now refers to the new method version
 * COMMENTS
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jvmti.scenarios.hotswap.HS201.hs201t003
 *
 * @comment compile newclassXX to bin/newclassXX
 * @run driver nsk.share.ExtraClassesBuilder
 *      newclass
 *
 * @run main/othervm/native
 *      -agentlib:hs201t003=-waittime=5
 *      nsk.jvmti.scenarios.hotswap.HS201.hs201t003
 *      ./bin
 */

