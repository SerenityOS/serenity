/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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
package jdk.internal.net.http.websocket;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Stack;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import static java.util.List.of;
import static java.util.Objects.requireNonNull;

/*
 * Auxiliary test infrastructure
 */
final class TestSupport {

    private TestSupport() { }

    static <A, B, R> Iterator<R> cartesianIterator(List<A> a,
                                                   List<B> b,
                                                   F2<A, B, R> f2) {
        @SuppressWarnings("unchecked")
        F<R> t = p -> f2.apply((A) p[0], (B) p[1]);
        return cartesianIterator(of(a, b), t);
    }

    static <A, B, C, R> Iterator<R> cartesianIterator(List<A> a,
                                                      List<B> b,
                                                      List<C> c,
                                                      F3<A, B, C, R> f3) {
        @SuppressWarnings("unchecked")
        F<R> t = p -> f3.apply((A) p[0], (B) p[1], (C) p[2]);
        return cartesianIterator(of(a, b, c), t);
    }

    static <A, B, C, D, R> Iterator<R> cartesianIterator(List<A> a,
                                                         List<B> b,
                                                         List<C> c,
                                                         List<D> d,
                                                         F4<A, B, C, D, R> f4) {
        @SuppressWarnings("unchecked")
        F<R> t = p -> f4.apply((A) p[0], (B) p[1], (C) p[2], (D) p[3]);
        return cartesianIterator(of(a, b, c, d), t);
    }

    static <A, B, C, D, E, R> Iterator<R> cartesianIterator(List<A> a,
                                                            List<B> b,
                                                            List<C> c,
                                                            List<D> d,
                                                            List<E> e,
                                                            F5<A, B, C, D, E, R> f5) {
        @SuppressWarnings("unchecked")
        F<R> t = p -> f5.apply((A) p[0], (B) p[1], (C) p[2], (D) p[3], (E) p[4]);
        return cartesianIterator(of(a, b, c, d, e), t);
    }

    static <R> Iterator<R> cartesianIterator(List<? extends List<?>> params,
                                             F<R> function) {
        if (params.isEmpty()) {
            return Collections.emptyIterator();
        }
        for (List<?> l : params) {
            if (l.isEmpty()) {
                return Collections.emptyIterator();
            }
        }
        // Assertion: if we are still here, there is at least a single element
        // in the product
        return new Iterator<>() {

            private final int arity = params.size();
            private final int[] coordinates = new int[arity];
            private boolean hasNext = true;

            @Override
            public boolean hasNext() {
                return hasNext;
            }

            @Override
            public R next() {
                if (!hasNext) {
                    throw new NoSuchElementException();
                }
                Object[] array = new Object[arity];
                for (int i = 0; i < arity; i++) {
                    array[i] = params.get(i).get(coordinates[i]);
                }
                int p = arity - 1;
                while (p >= 0 && coordinates[p] == params.get(p).size() - 1) {
                    p--;
                }
                if (p < 0) {
                    hasNext = false;
                } else {
                    coordinates[p]++;
                    for (int i = p + 1; i < arity; i++) {
                        coordinates[i] = 0;
                    }
                }
                return function.apply(array);
            }
        };
    }

    @FunctionalInterface
    public interface F1<A, R> {
        R apply(A a);
    }

    @FunctionalInterface
    public interface F2<A, B, R> {
        R apply(A a, B b);
    }

    @FunctionalInterface
    public interface F3<A, B, C, R> {
        R apply(A a, B b, C c);
    }

    @FunctionalInterface
    public interface F4<A, B, C, D, R> {
        R apply(A a, B b, C c, D d);
    }

    @FunctionalInterface
    public interface F5<A, B, C, D, E, R> {
        R apply(A a, B b, C c, D d, E e);
    }

    @FunctionalInterface
    public interface F<R> {
        R apply(Object[] args);
    }

    static <T> Iterator<T> iteratorOf1(T element) {
        return List.of(element).iterator();
    }

    @SafeVarargs
    static <T> Iterator<T> iteratorOf(T... elements) {
        return List.of(elements).iterator();
    }

    static <T> Iterator<T> limit(int maxElements, Iterator<? extends T> elements) {
        return new Iterator<>() {

            int count = maxElements;

            @Override
            public boolean hasNext() {
                return count > 0 && elements.hasNext();
            }

            @Override
            public T next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                count--;
                return elements.next();
            }
        };
    }

    static ByteBuffer fullCopy(ByteBuffer src) {
        ByteBuffer copy = ByteBuffer.allocate(src.capacity());
        int p = src.position();
        int l = src.limit();
        src.clear();
        copy.put(src).position(p).limit(l);
        src.position(p).limit(l);
        return copy;
    }

    static void forEachBufferPartition(ByteBuffer src,
                                       Consumer<? super Iterable<? extends ByteBuffer>> action) {
        forEachPartition(src.remaining(),
                (lengths) -> {
                    int end = src.position();
                    List<ByteBuffer> buffers = new LinkedList<>();
                    for (int len : lengths) {
                        ByteBuffer d = src.duplicate();
                        d.position(end);
                        d.limit(end + len);
                        end += len;
                        buffers.add(d);
                    }
                    action.accept(buffers);
                });
    }

    private static void forEachPartition(int n,
                                         Consumer<? super Iterable<Integer>> action) {
        forEachPartition(n, new Stack<>(), action);
    }

    private static void forEachPartition(int n,
                                         Stack<Integer> path,
                                         Consumer<? super Iterable<Integer>> action) {
        if (n == 0) {
            action.accept(path);
        } else {
            for (int i = 1; i <= n; i++) {
                path.push(i);
                forEachPartition(n - i, path, action);
                path.pop();
            }
        }
    }

    static void forEachPermutation(int n, Consumer<? super int[]> c) {
        int[] a = new int[n];
        for (int i = 0; i < n; i++) {
            a[i] = i;
        }
        permutations(0, a, c);
    }

    private static void permutations(int i, int[] a, Consumer<? super int[]> c) {
        if (i == a.length) {
            c.accept(Arrays.copyOf(a, a.length));
            return;
        }
        for (int j = i; j < a.length; j++) {
            swap(a, i, j);
            permutations(i + 1, a, c);
            swap(a, i, j);
        }
    }

    private static void swap(int[] a, int i, int j) {
        int x = a[i];
        a[i] = a[j];
        a[j] = x;
    }

    public static <T extends Throwable> T assertThrows(Class<? extends T> clazz,
                                                       ThrowingProcedure code) {
        @SuppressWarnings("unchecked")
        T t = (T) assertThrows(clazz::isInstance, code);
        return t;
    }

    /*
     * The rationale behind asking for a regex is to not pollute variable names
     * space in the scope of assertion: if it's something as simple as checking
     * a message, we can do it inside
     */
    @SuppressWarnings("unchecked")
    static <T extends Throwable> T assertThrows(Class<? extends T> clazz,
                                                String messageRegex,
                                                ThrowingProcedure code) {
        requireNonNull(messageRegex, "messagePattern");
        Predicate<Throwable> p = e -> clazz.isInstance(e)
                && Pattern.matches(messageRegex, e.getMessage());
        return (T) assertThrows(p, code);
    }

    static Throwable assertThrows(Predicate<? super Throwable> predicate,
                                  ThrowingProcedure code) {
        requireNonNull(predicate, "predicate");
        requireNonNull(code, "code");
        Throwable caught = null;
        try {
            code.run();
        } catch (Throwable t) {
            caught = t;
        }
        if (caught == null) {
            throw new AssertionFailedException("No exception was thrown");
        }
        if (predicate.test(caught)) {
            System.out.println("Got expected exception: " + caught);
            return caught;
        }
        throw new AssertionFailedException("Caught exception didn't match the predicate", caught);
    }

    interface ThrowingProcedure {
        void run() throws Throwable;
    }

    static final class AssertionFailedException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        AssertionFailedException(String message) {
            super(message);
        }

        AssertionFailedException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
