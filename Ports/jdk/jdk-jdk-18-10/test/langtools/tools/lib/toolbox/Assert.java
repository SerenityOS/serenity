/*
 * Copyright (c) 2011, 2016, Oracle and/or its affiliates. All rights reserved.
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

package toolbox;

import java.util.function.Supplier;

/**
 * Simple facility for unconditional assertions.
 * The methods in this class are described in terms of equivalent assert
 * statements, assuming that assertions have been enabled.
 */
public class Assert {
    /** Equivalent to
     *   assert cond;
     */
    public static void check(boolean cond) {
        if (!cond)
            error();
    }

    /** Equivalent to
     *   assert (o == null);
     */
    public static void checkNull(Object o) {
        if (o != null)
            error();
    }

    /** Equivalent to
     *   assert (t != null); return t;
     */
    public static <T> T checkNonNull(T t) {
        if (t == null)
            error();
        return t;
    }

    /** Equivalent to
     *   assert cond : value;
     */
    public static void check(boolean cond, int value) {
        if (!cond)
            error(String.valueOf(value));
    }

    /** Equivalent to
     *   assert cond : value;
     */
    public static void check(boolean cond, long value) {
        if (!cond)
            error(String.valueOf(value));
    }

    /** Equivalent to
     *   assert cond : value;
     */
    public static void check(boolean cond, Object value) {
        if (!cond)
            error(String.valueOf(value));
    }

    /** Equivalent to
     *   assert cond : msg;
     */
    public static void check(boolean cond, String msg) {
        if (!cond)
            error(msg);
    }

    /** Equivalent to
     *   assert cond : msg.get();
     *  Note: message string is computed lazily.
     */
    public static void check(boolean cond, Supplier<String> msg) {
        if (!cond)
            error(msg.get());
    }

    /** Equivalent to
     *   assert (o == null) : value;
     */
    public static void checkNull(Object o, Object value) {
        if (o != null)
            error(String.valueOf(value));
    }

    /** Equivalent to
     *   assert (o == null) : msg;
     */
    public static void checkNull(Object o, String msg) {
        if (o != null)
            error(msg);
    }

    /** Equivalent to
     *   assert (o == null) : msg.get();
     *  Note: message string is computed lazily.
     */
    public static void checkNull(Object o, Supplier<String> msg) {
        if (o != null)
            error(msg.get());
    }

    /** Equivalent to
     *   assert (o != null) : msg;
     */
    public static <T> T checkNonNull(T t, String msg) {
        if (t == null)
            error(msg);
        return t;
    }

    /** Equivalent to
     *   assert (o != null) : msg.get();
     *  Note: message string is computed lazily.
     */
    public static <T> T checkNonNull(T t, Supplier<String> msg) {
        if (t == null)
            error(msg.get());
        return t;
    }

    /** Equivalent to
     *   assert false;
     */
    public static void error() {
        throw new AssertionError();
    }

    /** Equivalent to
     *   assert false : msg;
     */
    public static void error(String msg) {
        throw new AssertionError(msg);
    }

    /** Prevent instantiation. */
    private Assert() { }
}
