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
 * @summary converted from VM Testbase nsk/jvmti/ForceGarbageCollection/forcegc001.
 * VM Testbase keywords: [quick, jpda, jvmti, noras]
 * VM Testbase readme:
 * DESCRIPTION
 *     This JVMTI test exercises JVMTI thread function ForceGarbageCollection().
 *     This tests checks that ForceGarbageCollection() function is invoked
 *     sucessfully and encourages GC to perform garbage collection and
 *     clean soft references to the tested objects in debugee VM.
 *     After return from ForceGarbageCollection() debugee checks and warns
 *     if the soft references are still not cleared.
 *     In this case debugee also checks and warns if System.gc() also
 *     does not clear soft references.
 *     The test fails only if ForceGarbageCollection() returns error,
 *     otherwise the test passes.
 * COMMENTS
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jvmti.ForceGarbageCollection.forcegc001
 * @run main/othervm/native
 *      -agentlib:forcegc001=-waittime=5,objects=100
 *      nsk.jvmti.ForceGarbageCollection.forcegc001
 */

