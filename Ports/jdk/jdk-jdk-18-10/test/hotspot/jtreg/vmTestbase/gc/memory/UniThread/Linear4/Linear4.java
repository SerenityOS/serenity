/*
 * Copyright (c) 2002, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @key stress randomness
 *
 * @summary converted from VM Testbase gc/memory/UniThread/Linear4.
 * VM Testbase keywords: [gc, stress, stressopt, nonconcurrent]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm gc.memory.UniThread.Linear4.Linear4 -iterations 5
 */

package gc.memory.UniThread.Linear4;

import nsk.share.gc.*;
import gc.memory.UniThread.Linear3.Linear3;

/**
 * Test GC collection of linked lists.
 *
 * This test simply creates a series of singly
 * linked memory objects which should be able to be
 * GC'd.
 *
 * In this test the size of one object is medium, the number
 * of objects in one list is relatively small and the number
 * of lists is large. Also, the order in which references
 * are cleared is randomized.
 */

public class Linear4 {
        public static void main(String args[]) {
                int circularitySize = 100;
                int objectSize = 10000;
                GC.runTest(new Linear3(objectSize, circularitySize), args);
        }
}
