/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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
package jdk.jpackage.internal;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class DottedVersionTest {

    public DottedVersionTest(boolean greedy) {
        this.greedy = greedy;
        if (greedy) {
            createTestee = DottedVersion::greedy;
        } else {
            createTestee = DottedVersion::lazy;
        }
    }

    @Parameterized.Parameters
    public static List<Object[]> data() {
        return List.of(new Object[] { true }, new Object[] { false });
    }

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @Test
    public void testValid() {
        final List<String> validStrings = List.of(
            "1.0",
            "1",
            "2.234.045",
            "2.234.0",
            "0",
            "0.1",
            "9".repeat(1000)
        );

        final List<String> validLazyStrings;
        if (greedy) {
            validLazyStrings = Collections.emptyList();
        } else {
            validLazyStrings = List.of(
                "1.-1",
                "5.",
                "4.2.",
                "3..2",
                "2.a",
                "0a",
                ".",
                " ",
                " 1",
                "1. 2",
                "+1",
                "-1",
                "-0",
                "+0"
            );
        }

        Stream.concat(validStrings.stream(), validLazyStrings.stream())
        .forEach(value -> {
            DottedVersion version = createTestee.apply(value);
            assertEquals(version.toString(), value);
        });
    }

    @Test
    public void testNull() {
        exceptionRule.expect(NullPointerException.class);
        createTestee.apply(null);
    }

    @Test
    public void testEmpty() {
        if (greedy) {
            exceptionRule.expect(IllegalArgumentException.class);
            exceptionRule.expectMessage("Version may not be empty string");
            createTestee.apply("");
        } else {
            assertTrue(0 == createTestee.apply("").compareTo(""));
            assertTrue(0 == createTestee.apply("").compareTo("0"));
        }
    }

    private final boolean greedy;
    private final Function<String, DottedVersion> createTestee;
}
