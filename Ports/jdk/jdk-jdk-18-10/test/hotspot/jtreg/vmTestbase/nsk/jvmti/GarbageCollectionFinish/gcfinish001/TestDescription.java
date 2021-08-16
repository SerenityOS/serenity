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
 * @summary converted from VM Testbase nsk/jvmti/GarbageCollectionFinish/gcfinish001.
 * VM Testbase keywords: [jpda, jvmti, noras, nonconcurrent]
 * VM Testbase readme:
 * DESCRIPTION
 *     This test exercises the JVMTI event GarbageCollectionFinish.
 *     It verifies that the raw monitor functions and memory management
 *     functions (Allocate, Deallocate) can be used during processing
 *     this event. Usage of these functions is allowed by the JVMTI spec.
 * COMMENTS
 *     The test has been fixed due to the bug 4968106.
 *     The test has been modified due to the rfe 4990048
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm/native
 *      -XX:-UseGCOverheadLimit
 *      -agentlib:gcfinish001=-waittime=5
 *      nsk.jvmti.GarbageCollectionFinish.gcfinish001
 */

