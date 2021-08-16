/*
 * Copyright (c) 2007, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @key stress
 *
 * @summary converted from VM Testbase gc/gctests/CallGC/CallGC01.
 * VM Testbase keywords: [gc, stress, stressopt, nonconcurrent]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm gc.gctests.CallGC.CallGC01.CallGC01 -t 100
 */

package gc.gctests.CallGC.CallGC01;

import nsk.share.gc.*;
import nsk.share.runner.*;

/**
 * This test starts a number of threads that do System.gc() and
 * System.runFinalization() and checks that there are no crashes.
 */
public class CallGC01 extends ThreadedGCTest {
        protected Runnable createRunnable(int i) {
                if (i % 2 == 0)
                        return new GCRunner();
                else
                        return new FinRunner();
        }

        public static void main(String[] args) {
                GC.runTest(new CallGC01(), args);
        }
}
