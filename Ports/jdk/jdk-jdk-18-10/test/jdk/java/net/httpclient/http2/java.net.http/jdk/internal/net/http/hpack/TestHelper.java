/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
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
package jdk.internal.net.http.hpack;

import org.testng.annotations.Test;

import java.util.Objects;
import java.util.Random;

public final class TestHelper {

    public static Random newRandom() {
        long seed = Long.getLong("jdk.test.lib.random.seed", System.currentTimeMillis());
        System.out.println("new java.util.Random(" + seed + ")");
        return new Random(seed);
    }

    public static <T extends Throwable> T assertVoidThrows(Class<T> clazz, Block<?> code) {
        return assertThrows(clazz, () -> {
            code.run();
            return null;
        });
    }

    public static <T extends Throwable> T assertThrows(Class<T> clazz, ReturningBlock<?> code) {
        Objects.requireNonNull(clazz, "clazz == null");
        Objects.requireNonNull(code, "code == null");
        try {
            code.run();
        } catch (Throwable t) {
            if (clazz.isInstance(t)) {
                return clazz.cast(t);
            }
            throw new AssertionError("Expected to catch exception of type "
                    + clazz.getCanonicalName() + ", instead caught "
                    + t.getClass().getCanonicalName(), t);

        }
        throw new AssertionError(
                "Expected to catch exception of type " + clazz.getCanonicalName()
                        + ", but caught nothing");
    }

    public static <T> T assertDoesNotThrow(ReturningBlock<T> code) {
        Objects.requireNonNull(code, "code == null");
        try {
            return code.run();
        } catch (Throwable t) {
            throw new AssertionError(
                    "Expected code block to exit normally, instead " +
                            "caught " + t.getClass().getCanonicalName(), t);
        }
    }

    public static void assertVoidDoesNotThrow(Block<?> code) {
        Objects.requireNonNull(code, "code == null");
        try {
            code.run();
        } catch (Throwable t) {
            throw new AssertionError(
                    "Expected code block to exit normally, instead " +
                            "caught " + t.getClass().getCanonicalName(), t);
        }
    }


    public static void assertExceptionMessageContains(Throwable t,
                                                      CharSequence firstSubsequence,
                                                      CharSequence... others) {
        assertCharSequenceContains(t.getMessage(), firstSubsequence, others);
    }

    public static void assertCharSequenceContains(CharSequence s,
                                                  CharSequence firstSubsequence,
                                                  CharSequence... others) {
        if (s == null) {
            throw new NullPointerException("Exception message is null");
        }
        String str = s.toString();
        String missing = null;
        if (!str.contains(firstSubsequence.toString())) {
            missing = firstSubsequence.toString();
        } else {
            for (CharSequence o : others) {
                if (!str.contains(o.toString())) {
                    missing = o.toString();
                    break;
                }
            }
        }
        if (missing != null) {
            throw new AssertionError("CharSequence '" + s + "'" + " does not "
                    + "contain subsequence '" + missing + "'");
        }
    }

    public interface ReturningBlock<T> {
        T run() throws Throwable;
    }

    public interface Block<T> {
        void run() throws Throwable;
    }

    // tests

    @Test
    public void assertThrows() {
        assertThrows(NullPointerException.class, () -> ((Object) null).toString());
    }

    @Test
    public void assertThrowsWrongType() {
        try {
            assertThrows(IllegalArgumentException.class, () -> ((Object) null).toString());
        } catch (AssertionError e) {
            Throwable cause = e.getCause();
            String message = e.getMessage();
            if (cause != null
                    && cause instanceof NullPointerException
                    && message != null
                    && message.contains("instead caught")) {
                return;
            }
        }
        throw new AssertionError();
    }

    @Test
    public void assertThrowsNoneCaught() {
        try {
            assertThrows(IllegalArgumentException.class, () -> null);
        } catch (AssertionError e) {
            Throwable cause = e.getCause();
            String message = e.getMessage();
            if (cause == null
                    && message != null
                    && message.contains("but caught nothing")) {
                return;
            }
        }
        throw new AssertionError();
    }
}
