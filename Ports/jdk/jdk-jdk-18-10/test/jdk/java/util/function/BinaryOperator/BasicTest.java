/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8009736 8010279
 * @run testng BasicTest
 */

import java.util.Comparator;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import org.testng.annotations.Test;


import static java.util.function.BinaryOperator.minBy;
import static java.util.function.BinaryOperator.maxBy;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.fail;

/**
 * Unit tests for helper methods in Comparators
 */
@Test(groups = "unit")
public class BasicTest {

    private static class People {
        final String firstName;
        final String lastName;
        final int age;

        People(String first, String last, int age) {
            firstName = first;
            lastName = last;
            this.age = age;
        }

        String getFirstName() { return firstName; }
        String getLastName() { return lastName; }
        int getAge() { return age; }
    }

    private final People people[] = {
        new People("John", "Doe", 34),
        new People("Mary", "Doe", 30),
    };

    public void testMaxBy() {
        Comparator<People> cmp = Comparator.comparing(People::getFirstName);
        // lesser
        assertSame(maxBy(cmp).apply(people[0], people[1]), people[1]);
        // euqal
        cmp = Comparator.comparing(People::getLastName);
        assertSame(maxBy(cmp).apply(people[0], people[1]), people[0]);
        // greater
        cmp = Comparator.comparingInt(People::getAge);
        assertSame(maxBy(cmp).apply(people[0], people[1]), people[0]);
    }

    public void testMinBy() {
        Comparator<People> cmp = Comparator.comparing(People::getFirstName);
        // lesser
        assertSame(minBy(cmp).apply(people[0], people[1]), people[0]);
        // euqal
        cmp = Comparator.comparing(People::getLastName);
        assertSame(minBy(cmp).apply(people[0], people[1]), people[0]);
        // greater
        cmp = Comparator.comparingInt(People::getAge);
        assertSame(minBy(cmp).apply(people[0], people[1]), people[1]);
    }

    public void testNulls() {
        try {
            BinaryOperator<String> op = minBy(null);
            fail("minBy(null) should throw NPE");
        } catch (NullPointerException npe) {}

        try {
            BinaryOperator<String> op = maxBy(null);
            fail("maxBy(null) should throw NPE");
        } catch (NullPointerException npe) {}
    }
}
