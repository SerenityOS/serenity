/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * Represents a possible difference between two objects.
 */
package jdk.test.lib.format;

/**
 * An abstraction representing formattable difference between two or more objects
 */
public interface Diff {

    /**
     * Default limits for formatters
     */
    public static class Defaults {
        private Defaults() {  }  // This class should not be instantiated
        public final static int WIDTH = 80;
        public final static int CONTEXT_BEFORE = 2;
    }

    /**
     * Formats the given diff. Different implementations can provide different
     * result and formatting style.
     *
     * @return formatted difference representation.
     */
    String format();

    /**
     * Indicates whether the two source arrays are equal. Different
     * implementations can treat this notion differently.
     *
     * @return {@code true} if the source objects are different, {@code false} otherwise
     */
    boolean areEqual();
}
