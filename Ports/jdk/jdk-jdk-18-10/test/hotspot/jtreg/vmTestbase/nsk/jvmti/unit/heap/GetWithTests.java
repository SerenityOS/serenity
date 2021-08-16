/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * Unit tests for JVMTI GetObjectsWithTags function
 *
 */

package nsk.jvmti.unit.heap;

import nsk.share.jvmti.unit.*;
import java.io.PrintStream;

public class GetWithTests {

    final static int JCK_STATUS_BASE = 95;
    final static int PASSED = 0;
    final static int FAILED = 2;

    public static void main(String args[]) {
        args = nsk.share.jvmti.JVMTITest.commonInit(args);

        // produce JCK-like exit status.
        System.exit(run(args, System.out) + JCK_STATUS_BASE);
    }

    public static int run(String args[], PrintStream out) {

        // create 4000 objects
        // tag 1000 with a tag of '1'
        // tag 2000 with a tag of '2'
        // leave 1000 untagged

        int tagged = 0;

        Object[] objs = new Object[4000];
        for (int i=0; i<4000; i++) {
            objs[i] = new Object();
        }
        for (int i=0; i<1000; i++) {
            Heap.setTag(objs[i], 1);
            tagged++;
        }
        for (int i=1000; i<3000; i++) {
            Heap.setTag(objs[i], 2);
            tagged++;
        }

        // call GetObjectsWithTags and check that it returns a count
        // of 3000

        long tags[] = { 1L, 2L };
        int count = Heap.getObjectsWithTags(tags.length, tags);

        if (count != tagged) {
            throw new RuntimeException(count + " objects tagged, expected " +
                tagged);
        }

        // now check that the object_results and tag_results are
        // the correct length

        Object[] object_results = Heap.objectResults();
        long[] tag_results = Heap.tagResults();

        if (object_results.length != count) {
            throw new RuntimeException("object_results.length is incorrect");
        }
        if (tag_results.length != count) {
            throw new RuntimeException("tag_results.length is incorrect");
        }


        // Now verify the results

        for (int i=0; i<count; i++) {
            Object o = object_results[i];
            long tag = tag_results[i];
            if (tag != 1 && tag != 2) {
                throw new RuntimeException("Bogus tag value: " + tag);
            }
            if (Heap.getTag(o) != tag) {
                throw new RuntimeException("tag mismatch!!");
            }

            // check that the object is one of our objects
            boolean found = false;
            int j=0;
            while (j<objs.length) {
                if (objs[j] == o) {
                    found = true;
                    break;
                }
                j++;
            }
            if (!found) {
                throw new RuntimeException("Tagged object not found");
            }

            // to prevent this object from being found again we set it to
            // null in the array.
            objs[j] = null;
        }

        return PASSED;
    }

}
