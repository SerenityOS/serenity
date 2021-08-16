/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

package java.net;

import java.net.CookieManager;
import java.net.HttpCookie;
import java.util.*;
import java.util.stream.Collectors;

// Whitebox Test of CookieManager.sortByPathAndAge

public class CookieManagerTest
{
    Random r1; // random is reseeded so it is reproducible
    long seed;

    public static void main(String[] args) {
        CookieManagerTest test = new CookieManagerTest();
        test.run();
    }

    CookieManagerTest() {
        r1 = new Random();
        seed = r1.nextLong();
        System.out.println("Random seed is: " + seed);
        r1.setSeed(seed);
    }

    static class TestCookie {
        String path;
        long creationTime;

        static TestCookie of(String path, long creationTime) {
            TestCookie t = new TestCookie();
            t.path = path;
            t.creationTime = creationTime;
            return t;
        }
    }

    // The order shown below is what we expect the sort to produce
    // longest paths first then for same length path by age (oldest first)
    // This array is copied to a list and shuffled before being passed to
    // the sort function several times.

    TestCookie[] cookies = new TestCookie[] {
        TestCookie.of("alpha/bravo/charlie/delta", 50),
        TestCookie.of("alphA/Bravo/charlie/delta", 100),
        TestCookie.of("bravo/charlie/delta", 1),
        TestCookie.of("bravo/chArlie/delta", 2),
        TestCookie.of("bravo/charlie/delta", 5),
        TestCookie.of("bravo/charliE/dElta", 10),
        TestCookie.of("charlie/delta", 1),
        TestCookie.of("charlie/delta", 1),
        TestCookie.of("charlie/delta", 1),
        TestCookie.of("charlie/delta", 2),
        TestCookie.of("charlie/delta", 2),
        TestCookie.of("charlie/delta", 2),
        TestCookie.of("ChaRlie/delta", 3),
        TestCookie.of("charliE/deLta", 4),
        TestCookie.of("cHarlie/delta", 7),
        TestCookie.of("chaRRie/delta", 9),
        TestCookie.of("delta", 999),
        TestCookie.of("Delta", 1000),
        TestCookie.of("Delta", 1000),
        TestCookie.of("DeLta", 1001),
        TestCookie.of("DeLta", 1002),
        TestCookie.of("/", 2),
        TestCookie.of("/", 3),
        TestCookie.of("/", 300)
    };

    public void run()
    {
        for (int i=0; i<100; i++) {
            List<TestCookie> l1 = new LinkedList(Arrays.asList(cookies));
            Collections.shuffle(l1, r1);
            List<HttpCookie> l2 = l1
                    .stream()
                    .map(this::createCookie)
                    .collect(Collectors.toList());


            // call PP method of CookieManager
            List<String> result = CookieManager.sortByPathAndAge(l2);
            int index = 0;
            for (String r : result) {
                if (!r.contains("name=\"value\""))
                    continue;
                // extract Path value
                r = r.substring(r.indexOf("Path=")+6);
                // remove trailing "

                // should go from name="value";$Path="bravo/charlie/delta" ->
                //                                    bravo/charlie/delta
                r = r.substring(0, r.indexOf('"'));
                if (!r.equals(cookies[index].path)) {
                    System.err.printf("ERROR: got %s index: %d", r, index);
                    System.err.printf(" expected: %s\n", cookies[index].path);
                    throw new RuntimeException("Test failed");
                }
                index++;
            }
        }
    }

    private HttpCookie createCookie(TestCookie tc) {
        HttpCookie ck = new HttpCookie("name", "value", null, tc.creationTime);
        ck.setPath(tc.path);
        return ck;
    }
}
