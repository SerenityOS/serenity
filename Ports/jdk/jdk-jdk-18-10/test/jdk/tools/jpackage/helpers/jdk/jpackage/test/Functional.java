/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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
package jdk.jpackage.test;

import java.lang.reflect.InvocationTargetException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;


public class Functional {
    @FunctionalInterface
    public interface ThrowingConsumer<T> {
        void accept(T t) throws Throwable;

        public static <T> Consumer<T> toConsumer(ThrowingConsumer<T> v) {
            return o -> {
                try {
                    v.accept(o);
                } catch (Throwable ex) {
                    rethrowUnchecked(ex);
                }
            };
        }
    }

    @FunctionalInterface
    public interface ThrowingBiConsumer<T, U> {
        void accept(T t, U u) throws Throwable;

        public static <T, U> BiConsumer<T, U> toBiConsumer(ThrowingBiConsumer<T, U> v) {
            return (t, u) -> {
                try {
                    v.accept(t, u);
                } catch (Throwable ex) {
                    rethrowUnchecked(ex);
                }
            };
        }
    }

    @FunctionalInterface
    public interface ThrowingSupplier<T> {
        T get() throws Throwable;

        public static <T> Supplier<T> toSupplier(ThrowingSupplier<T> v) {
            return () -> {
                try {
                    return v.get();
                } catch (Throwable ex) {
                    rethrowUnchecked(ex);
                }
                // Unreachable
                return null;
            };
        }
    }

    @FunctionalInterface
    public interface ThrowingFunction<T, R> {
        R apply(T t) throws Throwable;

        public static <T, R> Function<T, R> toFunction(ThrowingFunction<T, R> v) {
            return (t) -> {
                try {
                    return v.apply(t);
                } catch (Throwable ex) {
                    rethrowUnchecked(ex);
                }
                // Unreachable
                return null;
            };
        }
    }

    @FunctionalInterface
    public interface ThrowingRunnable {
        void run() throws Throwable;

        public static Runnable toRunnable(ThrowingRunnable v) {
            return () -> {
                try {
                    v.run();
                } catch (Throwable ex) {
                    rethrowUnchecked(ex);
                }
            };
        }
    }

    public static <T> Supplier<T> identity(Supplier<T> v) {
        return v;
    }

    public static <T> Consumer<T> identity(Consumer<T> v) {
        return v;
    }

    public static <T, U> BiConsumer<T, U> identity(BiConsumer<T, U> v) {
        return v;
    }

    public static Runnable identity(Runnable v) {
        return v;
    }

    public static <T, R> Function<T, R> identity(Function<T, R> v) {
        return v;
    }

    public static <T, R> Function<T, R> identityFunction(Function<T, R> v) {
        return v;
    }

    public static <T> Predicate<T> identity(Predicate<T> v) {
        return v;
    }

    public static <T> Predicate<T> identityPredicate(Predicate<T> v) {
        return v;
    }

    public static class ExceptionBox extends RuntimeException {
        public ExceptionBox(Throwable throwable) {
            super(throwable);
        }
    }

    @SuppressWarnings("unchecked")
    public static void rethrowUnchecked(Throwable throwable) throws ExceptionBox {
        if (throwable instanceof RuntimeException) {
            throw (RuntimeException)throwable;
        }

        if (throwable instanceof InvocationTargetException) {
            throw new ExceptionBox(throwable.getCause());
        }

        throw new ExceptionBox(throwable);
    }
}
