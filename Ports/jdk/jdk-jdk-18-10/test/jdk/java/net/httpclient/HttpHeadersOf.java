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
 * @summary Tests for HttpHeaders.of factory method
 * @run testng HttpHeadersOf
 */

import java.net.http.HttpHeaders;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertThrows;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

public class HttpHeadersOf {

    static final Class<NullPointerException> NPE = NullPointerException.class;
    static final Class<NumberFormatException> NFE = NumberFormatException.class;
    static final Class<UnsupportedOperationException> UOE = UnsupportedOperationException.class;

    static final BiPredicate<String,String> ACCEPT_ALL =
        new BiPredicate<>() {
            @Override public boolean test(String name, String value) { return true; }
            @Override public String toString() { return "ACCEPT_ALL"; }
        };

    static final BiPredicate<String,String> REJECT_ALL =
        new BiPredicate<>() {
            @Override public boolean test(String name, String value) { return false; }
            @Override public String toString() { return "REJECT_ALL"; }
        };

    @DataProvider(name = "predicates")
    public Object[][] predicates() {
        return new Object[][] { { ACCEPT_ALL }, { REJECT_ALL } };
    }

    @Test(dataProvider = "predicates")
    public void testNull(BiPredicate<String,String> filter) {
        assertThrows(NPE, () -> HttpHeaders.of(null, null));
        assertThrows(NPE, () -> HttpHeaders.of(null, filter));
        assertThrows(NPE, () -> HttpHeaders.of(Map.of(), null));
        assertThrows(NPE, () -> HttpHeaders.of(Map.of("name", List.of("value")), null));

        // nulls in the Map
        assertThrows(NPE, () -> HttpHeaders.of(Map.of(null, List.of("value)")), filter));
        assertThrows(NPE, () -> HttpHeaders.of(Map.of("name", null), filter));
        assertThrows(NPE, () -> HttpHeaders.of(Map.of("name", List.of(null)), filter));
        assertThrows(NPE, () -> HttpHeaders.of(Map.of("name", List.of("aValue", null)), filter));
        assertThrows(NPE, () -> HttpHeaders.of(Map.of("name", List.of(null, "aValue")), filter));
    }


    @DataProvider(name = "filterMaps")
    public Object[][] filterMaps() {
        List<Map<String, List<String>>> maps = List.of(
                Map.of("A", List.of("B"),           "X", List.of("Y", "Z")),
                Map.of("A", List.of("B", "C"),      "X", List.of("Y", "Z")),
                Map.of("A", List.of("B", "C", "D"), "X", List.of("Y", "Z"))
        );
        return maps.stream().map(p -> new Object[] { p }).toArray(Object[][]::new);
    }

    @Test(dataProvider = "filterMaps")
    public void testFilter(Map<String,List<String>> map) {
        HttpHeaders headers = HttpHeaders.of(map, REJECT_ALL);
        assertEquals(headers.map().size(), 0);
        assertFalse(headers.firstValue("A").isPresent());
        assertEquals(headers.allValues("A").size(), 0);

        headers = HttpHeaders.of(map, (name, value) -> {
            if (name.equals("A")) return true; else return false; });
        assertEquals(headers.map().size(), 1);
        assertTrue(headers.firstValue("A").isPresent());
        assertEquals(headers.allValues("A"), map.get("A"));
        assertEquals(headers.allValues("A").size(), map.get("A").size());
        assertFalse(headers.firstValue("X").isPresent());

        headers = HttpHeaders.of(map, (name, value) -> {
            if (name.equals("X")) return true; else return false; });
        assertEquals(headers.map().size(), 1);
        assertTrue(headers.firstValue("X").isPresent());
        assertEquals(headers.allValues("X"), map.get("X"));
        assertEquals(headers.allValues("X").size(), map.get("X").size());
        assertFalse(headers.firstValue("A").isPresent());
    }


    @DataProvider(name = "mapValues")
    public Object[][] mapValues() {
        List<Map<String, List<String>>> maps = List.of(
            Map.of("A", List.of("B")),
            Map.of("A", List.of("B", "C")),
            Map.of("A", List.of("B", "C", "D")),

            Map.of("A", List.of("B"),           "X", List.of("Y", "Z")),
            Map.of("A", List.of("B", "C"),      "X", List.of("Y", "Z")),
            Map.of("A", List.of("B", "C", "D"), "X", List.of("Y", "Z")),

            Map.of("A", List.of("B"),           "X", List.of("Y", "Z")),
            Map.of("A", List.of("B", "C"),      "X", List.of("Y", "Z")),
            Map.of("A", List.of("B", "C", "D"), "X", List.of("Y", "Z")),

            Map.of("X", List.of("Y", "Z"), "A", List.of("B")),
            Map.of("X", List.of("Y", "Z"), "A", List.of("B", "C")),
            Map.of("X", List.of("Y", "Z"), "A", List.of("B", "C", "D"))
        );
        return maps.stream().map(p -> new Object[] { p }).toArray(Object[][]::new);
    }

    @Test(dataProvider = "mapValues")
    public void testMapValues(Map<String,List<String>> map) {
        HttpHeaders headers = HttpHeaders.of(map, ACCEPT_ALL);

        assertEquals(headers.map().size(), map.size());
        assertTrue(headers.firstValue("A").isPresent());
        assertTrue(headers.firstValue("a").isPresent());
        assertEquals(headers.firstValue("A").get(), "B");
        assertEquals(headers.firstValue("a").get(), "B");
        assertEquals(headers.allValues("A"), map.get("A"));
        assertEquals(headers.allValues("a"), map.get("A"));
        assertEquals(headers.allValues("F").size(), 0);
        assertTrue(headers.map().get("A").contains("B"));
        assertFalse(headers.map().get("A").contains("F"));
        assertThrows(NFE, () -> headers.firstValueAsLong("A"));

        // a non-exhaustive list of mutators
        assertThrows(UOE, () -> headers.map().put("Z", List.of("Z")));
        assertThrows(UOE, () -> headers.map().remove("A"));
        assertThrows(UOE, () -> headers.map().remove("A", "B"));
        assertThrows(UOE, () -> headers.map().clear());
        assertThrows(UOE, () -> headers.allValues("A").remove("B"));
        assertThrows(UOE, () -> headers.allValues("A").remove(1));
        assertThrows(UOE, () -> headers.allValues("A").clear());
        assertThrows(UOE, () -> headers.allValues("A").add("Z"));
        assertThrows(UOE, () -> headers.allValues("A").addAll(List.of("Z")));
        assertThrows(UOE, () -> headers.allValues("A").add(1, "Z"));
    }


    @DataProvider(name = "caseInsensitivity")
    public Object[][] caseInsensitivity() {
        List<Map<String, List<String>>> maps = List.of(
             Map.of("Accept-Encoding", List.of("gzip, deflate")),
             Map.of("accept-encoding", List.of("gzip, deflate")),
             Map.of("AccePT-ENCoding", List.of("gzip, deflate")),
             Map.of("ACCept-EncodING", List.of("gzip, deflate")),
             Map.of("ACCEPT-ENCODING", List.of("gzip, deflate"))
        );
        return maps.stream().map(p -> new Object[] { p }).toArray(Object[][]::new);
    }

    @Test(dataProvider = "caseInsensitivity")
    public void testCaseInsensitivity(Map<String,List<String>> map) {
        HttpHeaders headers = HttpHeaders.of(map, ACCEPT_ALL);

        for (String name : List.of("Accept-Encoding", "accept-encoding",
                                   "aCCept-EnCODing", "accepT-encodinG")) {
            assertTrue(headers.firstValue(name).isPresent());
            assertTrue(headers.allValues(name).contains("gzip, deflate"));
            assertEquals(headers.firstValue(name).get(), "gzip, deflate");
            assertEquals(headers.allValues(name).size(), 1);
            assertEquals(headers.map().size(), 1);
            assertEquals(headers.map().get(name).size(), 1);
            assertEquals(headers.map().get(name).get(0), "gzip, deflate");
        }
    }

    @Test
    public void testEqualsAndHashCode() {
        List<Map<String, List<String>>> maps = List.of(
                Map.of("Accept-Encoding", List.of("gzip, deflate")),
                Map.of("accept-encoding", List.of("gzip, deflate")),
                Map.of("AccePT-ENCoding", List.of("gzip, deflate")),
                Map.of("ACCept-EncodING", List.of("gzip, deflate")),
                Map.of("ACCEPT-ENCODING", List.of("gzip, deflate"))
        );
        int mapDiffer = 0;
        int mapHashDiffer = 0;
        for (Map<String, List<String>> m1 : maps) {
            HttpHeaders h1 = HttpHeaders.of(m1, ACCEPT_ALL);
            for (Map<String, List<String>> m2 : maps) {
                HttpHeaders h2 = HttpHeaders.of(m2, ACCEPT_ALL);
                if (!m1.equals(m2)) mapDiffer++;
                if (m1.hashCode() != m2.hashCode()) mapHashDiffer++;
                assertEquals(h1, h2, "HttpHeaders differ");
                assertEquals(h1.hashCode(), h2.hashCode(),
                        "hashCode differ for " + List.of(m1,m2));
            }
        }
        assertTrue(mapDiffer > 0, "all maps were equal!");
        assertTrue(mapHashDiffer > 0, "all maps had same hashCode!");
    }

    @DataProvider(name = "valueAsLong")
    public Object[][] valueAsLong() {
        return new Object[][] {
            new Object[] { Map.of("Content-Length", List.of("10")), 10l },
            new Object[] { Map.of("Content-Length", List.of("101")), 101l },
            new Object[] { Map.of("Content-Length", List.of("56789")), 56789l },
            new Object[] { Map.of("Content-Length", List.of(Long.toString(Long.MAX_VALUE))), Long.MAX_VALUE },
            new Object[] { Map.of("Content-Length", List.of(Long.toString(Long.MIN_VALUE))), Long.MIN_VALUE }
        };
    }

    @Test(dataProvider = "valueAsLong")
    public void testValueAsLong(Map<String,List<String>> map, long expected) {
        HttpHeaders headers = HttpHeaders.of(map, ACCEPT_ALL);
        assertEquals(headers.firstValueAsLong("Content-Length").getAsLong(), expected);
    }


    @DataProvider(name = "duplicateNames")
    public Object[][] duplicateNames() {
        List<Map<String, List<String>>> maps = List.of(
                Map.of("X-name", List.of(),
                       "x-name", List.of()),
                Map.of("X-name", List.of(""),
                       "x-name", List.of("")),
                Map.of("X-name", List.of("C"),
                       "x-name", List.of("D")),
                Map.of("X-name", List.of("E"),
                       "Y-name", List.of("F"),
                       "X-Name", List.of("G")),
                Map.of("X-chegar", List.of("H"),
                       "y-dfuchs", List.of("I"),
                       "Y-dfuchs", List.of("J")),
                Map.of("X-name ", List.of("K"),
                       "X-Name", List.of("L")),
                Map.of("X-name", List.of("M"),
                       "\rX-Name", List.of("N"))
        );
        return maps.stream().map(p -> new Object[] { p }).toArray(Object[][]::new);
    }

    @Test(dataProvider = "duplicateNames")
    public void testDuplicates(Map<String,List<String>> map) {
        HttpHeaders headers;
        try {
            headers = HttpHeaders.of(map, ACCEPT_ALL);
            fail("UNEXPECTED: " + headers);
        } catch (IllegalArgumentException iae) {
            System.out.println("caught EXPECTED IAE:" + iae);
            assertTrue(iae.getMessage().contains("duplicate"));
        }
    }


    @DataProvider(name = "noSplittingJoining")
    public Object[][] noSplittingJoining() {
        List<Map<String, List<String>>> maps = List.of(
                Map.of("A", List.of("B")),
                Map.of("A", List.of("B", "C")),
                Map.of("A", List.of("B", "C", "D")),
                Map.of("A", List.of("B", "C", "D", "E")),
                Map.of("A", List.of("B", "C", "D", "E", "F")),
                Map.of("A", List.of("B, C")),
                Map.of("A", List.of("B, C, D")),
                Map.of("A", List.of("B, C, D, E")),
                Map.of("A", List.of("B, C, D, E, F")),
                Map.of("A", List.of("B, C", "D", "E", "F")),
                Map.of("A", List.of("B", "C, D", "E", "F")),
                Map.of("A", List.of("B", "C, D", "E, F")),
                Map.of("A", List.of("B", "C, D, E", "F")),
                Map.of("A", List.of("B", "C, D, E, F"))
        );
        return maps.stream().map(p -> new Object[] { p }).toArray(Object[][]::new);
    }

    @Test(dataProvider = "noSplittingJoining")
    public void testNoSplittingJoining(Map<String,List<String>> map) {
        HttpHeaders headers = HttpHeaders.of(map, ACCEPT_ALL);
        Map<String,List<String>> headersMap = headers.map();

        assertEquals(headers.map().size(), map.size());
        for (Map.Entry<String,List<String>> entry : map.entrySet()) {
            String headerName = entry.getKey();
            List<String> headerValues = entry.getValue();
            assertEquals(headerValues, headersMap.get(headerName));
            assertEquals(headerValues, headers.allValues(headerName));
            assertEquals(headerValues.get(0), headers.firstValue(headerName).get());
        }
    }


    @DataProvider(name = "trimming")
    public Object[][] trimming() {
        List<Map<String, List<String>>> maps = List.of(
                Map.of("A", List.of("B")),
                Map.of(" A", List.of("B")),
                Map.of("A ", List.of("B")),
                Map.of("A", List.of(" B")),
                Map.of("A", List.of("B ")),
                Map.of("\tA", List.of("B")),
                Map.of("A\t", List.of("B")),
                Map.of("A", List.of("\tB")),
                Map.of("A", List.of("B\t")),
                Map.of("A\r", List.of("B")),
                Map.of("A\n", List.of("B")),
                Map.of("A\r\n", List.of("B"))
        );
        return maps.stream().map(p -> new Object[] { p }).toArray(Object[][]::new);
    }

    @Test(dataProvider = "trimming")
    public void testTrimming(Map<String,List<String>> map) {
        HttpHeaders headers = HttpHeaders.of(map, (name, value) -> {
            assertEquals(name, "A");
            assertEquals(value, "B");
            return true;
        });

        assertEquals(headers.map().size(), 1);
        assertEquals(headers.firstValue("A").get(), "B");
        assertEquals(headers.allValues("A"), List.of("B"));
        assertTrue(headers.map().get("A").equals(List.of("B")));
    }


    @DataProvider(name = "emptyKey")
    public Object[][] emptyKey() {
        List<Map<String, List<String>>> maps = List.of(
                Map.of("", List.of("B")),
                Map.of(" ", List.of("B")),
                Map.of("  ", List.of("B")),
                Map.of("\t", List.of("B")),
                Map.of("\t\t", List.of("B"))
        );
        return maps.stream().map(p -> new Object[] { p }).toArray(Object[][]::new);
    }

    @Test(dataProvider = "emptyKey")
    public void testEmptyKey(Map<String,List<String>> map) {
        HttpHeaders headers;
        try {
            headers = HttpHeaders.of(map, ACCEPT_ALL);
            fail("UNEXPECTED: " + headers);
        } catch (IllegalArgumentException iae) {
            System.out.println("caught EXPECTED IAE:" + iae);
            assertTrue(iae.getMessage().contains("empty"));
        }
    }


    @DataProvider(name = "emptyValue")
    public Object[][] emptyValue() {
        List<Map<String, List<String>>> maps = List.of(
                Map.of("A", List.of("")),
                Map.of("A", List.of("", "")),
                Map.of("A", List.of("", "", " ")),
                Map.of("A", List.of("\t")),
                Map.of("A", List.of("\t\t"))
        );
        return maps.stream().map(p -> new Object[] { p }).toArray(Object[][]::new);
    }

    @Test(dataProvider = "emptyValue")
    public void testEmptyValue(Map<String,List<String>> map) {
        HttpHeaders headers = HttpHeaders.of(map, (name, value) -> {
            assertEquals(value, "");
            return true;
        });
        assertEquals(headers.map().size(), map.size());
        assertEquals(headers.map().get("A").get(0), "");
        headers.allValues("A").forEach(v -> assertEquals(v, ""));
        assertEquals(headers.firstValue("A").get(), "");
    }


    @DataProvider(name = "noValues")
    public Object[][] noValues() {
        List<Map<String, List<String>>> maps = List.of(
                Map.of("A", List.of()),
                Map.of("A", List.of(), "B", List.of()),
                Map.of("A", List.of(), "B", List.of(), "C", List.of()),
                Map.of("A", new ArrayList()),
                Map.of("A", new LinkedList())
        );
        return maps.stream().map(p -> new Object[] { p }).toArray(Object[][]::new);
    }

    @Test(dataProvider = "noValues")
    public void testNoValues(Map<String,List<String>> map) {
        HttpHeaders headers = HttpHeaders.of(map, (name, value) -> {
            fail("UNEXPECTED call to filter");
            return true;
        });
        assertEquals(headers.map().size(), 0);
        assertEquals(headers.map().get("A"), null);
        assertEquals(headers.allValues("A").size(), 0);
        assertFalse(headers.firstValue("A").isPresent());
    }
}
