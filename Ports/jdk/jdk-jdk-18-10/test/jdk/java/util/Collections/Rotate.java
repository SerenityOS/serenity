/*
 * Copyright (c) 2000, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4323074
 * @summary Basic test for new rotate algorithm
 * @key randomness
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Vector;

public class Rotate {
    // Should have lots of distinct factors and be > ROTATE_THRESHOLD
    static final int SIZE = 120;

    static Random rnd = new Random();

    public static void main(String[] args) throws Exception {
        List[] a = {new ArrayList(), new LinkedList(), new Vector()};

        for (int i=0; i<a.length; i++) {
            List lst = a[i];
            for (int j=0; j<SIZE; j++)
                lst.add(new Integer(j));
            int totalDist = 0;

            for (int j=0; j<10000; j++) {
                int dist = rnd.nextInt(200) - 100;
                Collections.rotate(lst, dist);

                // Check that things are as they should be
                totalDist = (totalDist + dist) % SIZE;
                if (totalDist < 0)
                    totalDist += SIZE;
                int index =0;
                for (int k=totalDist; k<SIZE; k++, index++)
                    if (((Integer)lst.get(k)).intValue() != index)
                        throw new Exception("j: "+j+", lst["+k+"]="+lst.get(k)+
                                            ", should be "+index);
                for (int k=0; k<totalDist; k++, index++)
                    if (((Integer)lst.get(k)).intValue() != index)
                        throw new Exception("j: "+j+", lst["+k+"]="+lst.get(k)+
                                            ", should be "+index);
            }
        }
    }
}
