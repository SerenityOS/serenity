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
 * @key randomness
 *
 * @summary converted from VM Testbase nsk/jvmti/CompiledMethodUnload/compmethunload001.
 * VM Testbase keywords: [jpda, jvmti, noras, nonconcurrent]
 * VM Testbase readme:
 * DESCRIPTION
 *     This test exercises the JVMTI event CompiledMethodUnload.
 *     It creates an instance of tested class 'HotClass'. Then special
 *     'hot' methods are called in a loop in order to be compiled/inlined.
 *     Then the class is provoked to be unloaded and, thus, CompiledMethodUnload
 *     events generation for the compiled methods mentioned above.
 *     The CompiledMethodUnload events, if they occur, must be sent only
 *     during the live phase of the VM execution.
 * COMMENTS
 *     Fixed according to 4960375 bug.
 *         The test updated to match new JVMTI spec 0.2.94.
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jvmti.CompiledMethodUnload.compmethunload001
 *
 * @comment compile loadclassXX to bin/loadclassXX
 * @run driver nsk.share.ExtraClassesBuilder
 *      loadclass
 *
 * @run main/othervm/native
 *      -agentlib:compmethunload001=-waittime=5
 *      nsk.jvmti.CompiledMethodUnload.compmethunload001
 *      ./bin
 */

