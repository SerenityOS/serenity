/*
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
 * This file is available under and governed by the GNU General Public
 * License version 2 only, as published by the Free Software Foundation.
 * However, the following notice accompanied the original version of this
 * file:
 *
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

/*
 * @test
 * @bug 4486658
 * @summary Checks that a priority queue returns elements in sorted order across various operations
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

public class PriorityQueueSort {

    static class MyComparator implements Comparator<Integer> {
        public int compare(Integer x, Integer y) {
            return Integer.compare(x.intValue(), y.intValue());
        }
    }

    public static void main(String[] args) {
        int n = 10000;
        if (args.length > 0)
            n = Integer.parseInt(args[0]);

        List<Integer> sorted = new ArrayList<>(n);
        for (int i = 0; i < n; i++)
            sorted.add(new Integer(i));
        List<Integer> shuffled = new ArrayList<>(sorted);
        Collections.shuffle(shuffled);

        Queue<Integer> pq = new PriorityQueue<>(n, new MyComparator());
        for (Iterator<Integer> i = shuffled.iterator(); i.hasNext(); )
            pq.add(i.next());

        List<Integer> recons = new ArrayList<>();
        while (!pq.isEmpty())
            recons.add(pq.remove());
        if (!recons.equals(sorted))
            throw new RuntimeException("Sort test failed");

        recons.clear();
        pq = new PriorityQueue<>(shuffled);
        while (!pq.isEmpty())
            recons.add(pq.remove());
        if (!recons.equals(sorted))
            throw new RuntimeException("Sort test failed");

        // Remove all odd elements from queue
        pq = new PriorityQueue<>(shuffled);
        for (Iterator<Integer> i = pq.iterator(); i.hasNext(); )
            if ((i.next().intValue() & 1) == 1)
                i.remove();
        recons.clear();
        while (!pq.isEmpty())
            recons.add(pq.remove());

        for (Iterator<Integer> i = sorted.iterator(); i.hasNext(); )
            if ((i.next().intValue() & 1) == 1)
                i.remove();

        if (!recons.equals(sorted))
            throw new RuntimeException("Iterator remove test failed.");
    }
}
