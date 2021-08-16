/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
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


/**
 * @test
 * @summary Exercise initial transformation (ClassFileLoadHook)
 *  with CDS with SubClass and SuperClass, each lives in own separate package
 * @library /test/lib /runtime/cds /testlibrary/jvmti
 * @requires vm.cds
 * @requires vm.jvmti
 * @requires !vm.graal.enabled
 * @build TransformUtil TransformerAgent SubClass SuperClazz
 * @compile myPkg2/SubClass.java myPkg1/SuperClazz.java
 * @run main/othervm TransformRelatedClasses myPkg1.SuperClazz myPkg2.SubClass
*/

// Clarification on @requires declarations:
// CDS is not supported w/o the use of Compressed OOPs
// JVMTI's ClassFileLoadHook is not supported under minimal VM

// This test class uses TransformRelatedClasses to do its work.
// The goal of this test is to exercise transformation of related superclass
// and subclass in combination with CDS; each class lives in its own package.
// The transformation is done via ClassFileLoadHook mechanism.
// Both superclass and subclass reside in the shared archive.
// The test consists of 4 test cases where transformation is applied
// to a parent and child in combinatorial manner.
// Please see TransformRelatedClasses.java for details.
