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
 * @run testng EntryComparators
 */
import java.util.function.Function;
import java.util.Comparator;
import java.util.AbstractMap;
import java.util.Map;
import org.testng.annotations.Test;

import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/**
 * Unit tests for Map.Entry.comparing
 */
@Test(groups = "unit")
public class EntryComparators {

    private <K, V> void assertPairComparison(K k1, V v1, K k2, V v2,
                                        Comparator<Map.Entry<K, V>> ck,
                                        Comparator<Map.Entry<K, V>> cv) {
        final Map.Entry<K, V> p11 = new AbstractMap.SimpleImmutableEntry<>(k1, v1);
        final Map.Entry<K, V> p12 = new AbstractMap.SimpleImmutableEntry<>(k1, v2);
        final Map.Entry<K, V> p21 = new AbstractMap.SimpleImmutableEntry<>(k2, v1);
        final Map.Entry<K, V> p22 = new AbstractMap.SimpleImmutableEntry<>(k2, v2);

        assertTrue(ck.compare(p11, p11) == 0);
        assertTrue(ck.compare(p12, p11) == 0);
        assertTrue(ck.compare(p11, p12) == 0);
        assertTrue(ck.compare(p12, p22) < 0);
        assertTrue(ck.compare(p12, p21) < 0);
        assertTrue(ck.compare(p21, p11) > 0);
        assertTrue(ck.compare(p21, p12) > 0);

        assertTrue(cv.compare(p11, p11) == 0);
        assertTrue(cv.compare(p12, p11) > 0);
        assertTrue(cv.compare(p11, p12) < 0);
        assertTrue(cv.compare(p12, p22) == 0);
        assertTrue(cv.compare(p12, p21) > 0);
        assertTrue(cv.compare(p21, p11) == 0);
        assertTrue(cv.compare(p21, p12) < 0);

        Comparator<Map.Entry<K, V>> cmp = ck.thenComparing(cv);
        assertTrue(cmp.compare(p11, p11) == 0);
        assertTrue(cmp.compare(p12, p11) > 0);
        assertTrue(cmp.compare(p11, p12) < 0);
        assertTrue(cmp.compare(p12, p22) < 0);
        assertTrue(cmp.compare(p12, p21) < 0);
        assertTrue(cmp.compare(p21, p11) > 0);
        assertTrue(cmp.compare(p21, p12) > 0);

        cmp = cv.thenComparing(ck);
        assertTrue(cmp.compare(p11, p11) == 0);
        assertTrue(cmp.compare(p12, p11) > 0);
        assertTrue(cmp.compare(p11, p12) < 0);
        assertTrue(cmp.compare(p12, p22) < 0);
        assertTrue(cmp.compare(p12, p21) > 0);
        assertTrue(cmp.compare(p21, p11) > 0);
        assertTrue(cmp.compare(p21, p12) < 0);
    }

    public void testKVComparables() {
        assertPairComparison(1, "ABC", 2, "XYZ",
                         Map.Entry.<Integer, String>comparingByKey(),
                         Map.Entry.<Integer, String>comparingByValue());
    }

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

    public void testKVComparators() {
        // Comparator<People> cmp = Comparator.naturalOrder(); // Should fail to compiler as People is not comparable
        // We can use simple comparator, but those have been tested above.
        // Thus choose to do compose for some level of interation.
        Comparator<People> cmp1 = Comparator.comparing(People::getFirstName);
        Comparator<People> cmp2 = Comparator.comparing(People::getLastName);
        Comparator<People> cmp = cmp1.thenComparing(cmp2);

        assertPairComparison(people[0], people[0], people[1], people[1],
                         Map.Entry.<People, People>comparingByKey(cmp),
                         Map.Entry.<People, People>comparingByValue(cmp));

    }

    public void testNulls() {
        try {
            Comparator<Map.Entry<String, String>> cmp = Map.Entry.comparingByKey(null);
            fail("comparingByKey(null) should throw NPE");
        } catch (NullPointerException npe) {}

        try {
            Comparator<Map.Entry<String, String>> cmp = Map.Entry.comparingByValue(null);
            fail("comparingByValue(null) should throw NPE");
        } catch (NullPointerException npe) {}
    }
}
