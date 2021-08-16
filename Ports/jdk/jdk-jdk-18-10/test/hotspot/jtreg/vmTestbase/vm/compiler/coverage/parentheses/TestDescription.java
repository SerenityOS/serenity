/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @modules java.base/jdk.internal.org.objectweb.asm:+open java.base/jdk.internal.misc:+open
 *
 * @summary converted from VM Testbase vm/compiler/coverage/parentheses.
 * VM Testbase keywords: [quick, stressopt, jit]
 * VM Testbase readme:
 * DESCRIPTION
 *     This test works as follows:
 *     1. Generates random proper list of following JVM instructions:
 *     -Integer constants: ICONST_M1, ICONST_0, ICONST_1, ICONST_2, ICONST_3, ICONST_4, ICONST_5
 *     -Integer arithmetics: IADD, ISUB, IMUL, IAND, IOR, IXOR, ISHL, ISHR, INEG
 *     -Stack operations: DUP, SWAP
 *     -NOP
 *     (If loadFrom command line option is setted, test will be use instruction sequence from file.
 *     It is useful for reproducing failures.)
 *     2. Executes this sequence in to ways: with TinyInstructionsExecutor and  HotspotInstructionsExecutor.
 *     3. Checks that results of execution are equals.
 *     For generation instructions test using right parentheses sequence. Open parenthesis "(" is correspondent to
 *     stack increasing instructions, ")" is corresponded stack decreasing instructions.
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm vm.compiler.coverage.parentheses.Parentheses
 */

