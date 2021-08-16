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

/*
 * @test
 * @bug 8050818
 * @run testng PredicateNotTest
 */

import java.util.List;
import java.util.function.Predicate;
import org.testng.annotations.Test;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.joining;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

@Test(groups = "unit")
public class PredicateNotTest {
    static class IsEmptyPredicate implements Predicate<String> {
        @Override
        public boolean test(String s) {
            return s.isEmpty();
        }
    }

    public void test() {
        List<String> test = List.of(
           "A non-empty line",
           "",
           "A non-empty line",
           "",
           "A non-empty line",
           "",
           "A non-empty line",
           ""
        );
        String expected = "A non-empty line\nA non-empty line\nA non-empty line\nA non-empty line";

        assertEquals(test.stream().filter(not(String::isEmpty)).collect(joining("\n")), expected);
        assertEquals(test.stream().filter(not(s -> s.isEmpty())).collect(joining("\n")), expected);
        assertEquals(test.stream().filter(not(new IsEmptyPredicate())).collect(joining("\n")), expected);
        assertEquals(test.stream().filter(not(not(not(String::isEmpty)))).collect(joining("\n")), expected);
        assertEquals(test.stream().filter(not(not(not(s -> s.isEmpty())))).collect(joining("\n")), expected);
        assertEquals(test.stream().filter(not(not(not(new IsEmptyPredicate())))).collect(joining("\n")), expected);
    }
}

