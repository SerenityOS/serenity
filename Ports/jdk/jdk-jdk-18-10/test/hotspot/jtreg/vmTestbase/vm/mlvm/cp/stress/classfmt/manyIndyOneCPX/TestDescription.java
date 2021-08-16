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
 * @modules java.base/jdk.internal.org.objectweb.asm
 *
 * @summary converted from VM Testbase vm/mlvm/cp/stress/classfmt/manyIndyOneCPX.
 * VM Testbase keywords: [feature_mlvm, hangup, nonconcurrent, exclude]
 * VM Testbase comments: 8199227
 * VM Testbase readme:
 * DESCRIPTION
 *        The test creates and executes a class containing lot of invokedynamic instructions
 *        pointing to just one constant pool CONSTANT_InvokeDynamic entry.
 *
 * @library /vmTestbase
 *          /test/lib
 *          /vmTestbase/vm/mlvm/patches
 *
 * @comment patch for java.base
 * @build java.base/*
 *
 * @comment build generator
 * @build vm.mlvm.cp.share.GenManyIndyOneCPX
 *
 * @ignore 8199227
 *
 * @comment build test class and indify classes
 * @build vm.mlvm.share.ClassfileGeneratorTest
 * @run driver vm.mlvm.share.IndifiedClassesBuilder
 *
 * @run main/othervm
 *      vm.mlvm.share.ClassfileGeneratorTest
 *      -generator vm.mlvm.cp.share.GenManyIndyOneCPX
 */

