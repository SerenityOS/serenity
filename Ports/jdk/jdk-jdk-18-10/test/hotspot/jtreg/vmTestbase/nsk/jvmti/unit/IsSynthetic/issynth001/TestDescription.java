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
 * @summary converted from VM Testbase nsk/jvmti/unit/IsSynthetic/issynth001.
 * VM Testbase keywords: [quick, jpda, jvmti, noras]
 * VM Testbase readme:
 * DESCRIPTION
 *     The purpose of this test is to ensure that ACC_SYNTHETIC bit is processed
 *     correctly when javac flag "-target 1.5" was used to create class file.
 *     The test exercises JVMTI functions IsMethodSynthetic & IsFieldSynthetic.
 *     The functions are called to get "is synthetic" flag for the methods
 *     and fields.
 * COMMENT
 *     Prepared using sources of jvmti tests:
 *         nsk/jvmti/IsMethodSynthetic/issynth001
 *         nsk/jvmti/IsFieldSynthetic/isfldsin003
 *     Ported from JVMDI.
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm/native -agentlib:IsSyntheticIssynth001 nsk.jvmti.unit.IsSynthetic.issynth001
 */

