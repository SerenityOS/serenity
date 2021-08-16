/*
 * Copyright (c) 1997, 1998, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 4076283
 * @summary java.lang.ref.SoftReference should reliably prevent
 *          OutOfMemoryErrors
 * @author Peter Jones
 * @author Mark Reinhold
 */

/* If this test fails, an OutOfMemoryError will be thrown */

import java.lang.ref.SoftReference;


public class Bash {

    static class TestReference extends SoftReference {

        public static TestReference head;
        public TestReference next;

        public TestReference(Object referent) {
            super(referent);
            next = head;
            head = this;
        }
    }


    static final int NUM_BLOCKS = 2048;
    static final int BLOCK_SIZE = 32768;


    public static void main(String[] args) throws Exception {

        for (int i = 0; i < NUM_BLOCKS; ++ i) {
            TestReference ref = new TestReference(new byte[BLOCK_SIZE]);
        }

        int emptyCount = 0;
        int fullCount = 0;
        for (TestReference r = TestReference.head; r != null; r = r.next) {
            if (r.get() == null) emptyCount++;
            else fullCount++;
        }

        System.err.println(fullCount + " full, " + emptyCount + " empty ");

    }

}
