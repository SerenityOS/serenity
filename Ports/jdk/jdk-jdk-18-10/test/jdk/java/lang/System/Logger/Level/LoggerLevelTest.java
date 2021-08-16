/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.System.Logger.Level;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
/**
 * @test
 * @bug 8140364
 * @summary Tests System.Logger.Level names and severity.
 * @author danielfuchs
 * @modules java.logging
 */
public class LoggerLevelTest {
    public static void main(String[] args) {
        Set<Level> untested = EnumSet.allOf(Level.class);
        testLevel(untested, Level.ALL, java.util.logging.Level.ALL);
        testLevel(untested, Level.TRACE, java.util.logging.Level.FINER);
        testLevel(untested, Level.DEBUG, java.util.logging.Level.FINE);
        testLevel(untested, Level.INFO, java.util.logging.Level.INFO);
        testLevel(untested, Level.WARNING, java.util.logging.Level.WARNING);
        testLevel(untested, Level.ERROR, java.util.logging.Level.SEVERE);
        testLevel(untested, Level.OFF, java.util.logging.Level.OFF);
        if (!untested.isEmpty()) {
            throw new RuntimeException("Some level values were not tested: " + untested);
        }
    }

    private static void testLevel(Set<Level> untested, Level systemLevel, java.util.logging.Level julLevel) {
        untested.remove(systemLevel);
        assertEquals(systemLevel.getName(), systemLevel.name(),
                "System.Logger.Level." + systemLevel.name() + ".getName()");
        assertEquals(systemLevel.getSeverity(), julLevel.intValue(),
                "System.Logger.Level." + systemLevel.name() + ".getSeverity");
    }

    private static void assertEquals(Object actual, Object expected, String what) {
        if (!Objects.equals(actual, expected)) {
            throw new RuntimeException("Bad value for " + what
                    + "\n\t expected: " + expected
                    + "\n\t   actual: " + actual);
        } else {
            System.out.println("Got expected value for " + what + ": " + actual);
        }
    }

    private static void assertEquals(int actual, int expected, String what) {
        if (!Objects.equals(actual, expected)) {
            throw new RuntimeException("Bad value for " + what
                    + "\n\t expected: " + toString(expected)
                    + "\n\t   actual: " + toString(actual));
        } else {
            System.out.println("Got expected value for " + what + ": " + toString(actual));
        }
    }

    private static String toString(int value) {
        switch (value) {
            case Integer.MAX_VALUE: return "Integer.MAX_VALUE";
            case Integer.MIN_VALUE: return "Integer.MIN_VALUE";
            default:
                return Integer.toString(value);
        }
    }

}
