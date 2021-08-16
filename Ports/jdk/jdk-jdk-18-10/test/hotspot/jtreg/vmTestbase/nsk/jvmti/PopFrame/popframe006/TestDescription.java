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
 * @summary converted from VM Testbase nsk/jvmti/PopFrame/popframe006.
 * VM Testbase keywords: [quick, jpda, jvmti, noras]
 * VM Testbase readme:
 * DESCRIPTION
 *     The test exercises JVMTI function PopFrame(thread).
 *     The test creates a new thread, then does the following:
 *         - set a breakpoint to the thread's method run
 *         - step into method A
 *         - step into method B
 *         - pop frame
 *         - step into method B
 *         - step into method C
 *         - step out of method C
 *         - step out of method B
 *         - step out of method A
 *     watching step into/pop frame via SINGLE_STEP event and
 *     step out via FRAME_POP event, and checking class/method/location
 *     on expected values.
 * COMMENTS
 *     This is a regression test on
 *         4530424 PopFrames causes a Step to run to completion
 *     Ported from JVMDI.
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm/native -agentlib:popframe006 nsk.jvmti.PopFrame.popframe006
 */

