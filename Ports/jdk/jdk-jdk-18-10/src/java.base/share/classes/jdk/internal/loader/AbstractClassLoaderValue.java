/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package jdk.internal.loader;

import jdk.internal.access.JavaLangAccess;
import jdk.internal.access.SharedSecrets;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * AbstractClassLoaderValue is a superclass of root-{@link ClassLoaderValue}
 * and {@link Sub sub}-ClassLoaderValue.
 *
 * @param <CLV> the type of concrete ClassLoaderValue (this type)
 * @param <V>   the type of values associated with ClassLoaderValue
 */
public abstract class AbstractClassLoaderValue<CLV extends AbstractClassLoaderValue<CLV, V>, V> {

    /**
     * Sole constructor.
     */
    AbstractClassLoaderValue() {}

    /**
     * Returns the key component of this ClassLoaderValue. The key component of
     * the root-{@link ClassLoaderValue} is the ClassLoaderValue itself,
     * while the key component of a {@link #sub(Object) sub}-ClassLoaderValue
     * is what was given to construct it.
     *
     * @return the key component of this ClassLoaderValue.
     */
    public abstract Object key();

    /**
     * Constructs new sub-ClassLoaderValue of this ClassLoaderValue with given
     * key component.
     *
     * @param key the key component of the sub-ClassLoaderValue.
     * @param <K> the type of the key component.
     * @return a sub-ClassLoaderValue of this ClassLoaderValue for given key
     */
    public <K> Sub<K> sub(K key) {
        return new Sub<>(key);
    }

    /**
     * Returns {@code true} if this ClassLoaderValue is equal to given {@code clv}
     * or if this ClassLoaderValue was derived from given {@code clv} by a chain
     * of {@link #sub(Object)} invocations.
     *
     * @param clv the ClassLoaderValue to test this against
     * @return if this ClassLoaderValue is equal to given {@code clv} or
     * its descendant
     */
    public abstract boolean isEqualOrDescendantOf(AbstractClassLoaderValue<?, V> clv);

    /**
     * Returns the value associated with this ClassLoaderValue and given ClassLoader
     * or {@code null} if there is none.
     *
     * @param cl the ClassLoader for the associated value
     * @return the value associated with this ClassLoaderValue and given ClassLoader
     * or {@code null} if there is none.
     */
    public V get(ClassLoader cl) {
        Object val = AbstractClassLoaderValue.<CLV>map(cl).get(this);
        try {
            return extractValue(val);
        } catch (Memoizer.RecursiveInvocationException e) {
            // propagate recursive get() for the same key that is just
            // being calculated in computeIfAbsent()
            throw e;
        } catch (Throwable t) {
            // don't propagate exceptions thrown from Memoizer - pretend
            // that there was no entry
            // (computeIfAbsent invocation will try to remove it anyway)
            return null;
        }
    }

    /**
     * Associates given value {@code v} with this ClassLoaderValue and given
     * ClassLoader and returns {@code null} if there was no previously associated
     * value or does nothing and returns previously associated value if there
     * was one.
     *
     * @param cl the ClassLoader for the associated value
     * @param v  the value to associate
     * @return previously associated value or null if there was none
     */
    public V putIfAbsent(ClassLoader cl, V v) {
        ConcurrentHashMap<CLV, Object> map = map(cl);
        @SuppressWarnings("unchecked")
        CLV clv = (CLV) this;
        while (true) {
            try {
                Object val = map.putIfAbsent(clv, v);
                return extractValue(val);
            } catch (Memoizer.RecursiveInvocationException e) {
                // propagate RecursiveInvocationException for the same key that
                // is just being calculated in computeIfAbsent
                throw e;
            } catch (Throwable t) {
                // don't propagate exceptions thrown from foreign Memoizer -
                // pretend that there was no entry and retry
                // (foreign computeIfAbsent invocation will try to remove it anyway)
            }
            // TODO:
            // Thread.onSpinLoop(); // when available
        }
    }

    /**
     * Removes the value associated with this ClassLoaderValue and given
     * ClassLoader if the associated value is equal to given value {@code v} and
     * returns {@code true} or does nothing and returns {@code false} if there is
     * no currently associated value or it is not equal to given value {@code v}.
     *
     * @param cl the ClassLoader for the associated value
     * @param v  the value to compare with currently associated value
     * @return {@code true} if the association was removed or {@code false} if not
     */
    public boolean remove(ClassLoader cl, Object v) {
        return AbstractClassLoaderValue.<CLV>map(cl).remove(this, v);
    }

    /**
     * Returns the value associated with this ClassLoaderValue and given
     * ClassLoader if there is one or computes the value by invoking given
     * {@code mappingFunction}, associates it and returns it.
     * <p>
     * Computation and association of the computed value is performed atomically
     * by the 1st thread that requests a particular association while holding a
     * lock associated with this ClassLoaderValue and given ClassLoader.
     * Nested calls from the {@code mappingFunction} to {@link #get},
     * {@link #putIfAbsent} or {@link #computeIfAbsent} for the same association
     * are not allowed and throw {@link IllegalStateException}. Nested call to
     * {@link #remove} for the same association is allowed but will always return
     * {@code false} regardless of passed-in comparison value. Nested calls for
     * other association(s) are allowed, but care should be taken to avoid
     * deadlocks. When two threads perform nested computations of the overlapping
     * set of associations they should always request them in the same order.
     *
     * @param cl              the ClassLoader for the associated value
     * @param mappingFunction the function to compute the value
     * @return the value associated with this ClassLoaderValue and given
     * ClassLoader.
     * @throws IllegalStateException if a direct or indirect invocation from
     *                               within given {@code mappingFunction} that
     *                               computes the value of a particular association
     *                               to {@link #get}, {@link #putIfAbsent} or
     *                               {@link #computeIfAbsent}
     *                               for the same association is attempted.
     */
    public V computeIfAbsent(ClassLoader cl,
                             BiFunction<
                                 ? super ClassLoader,
                                 ? super CLV,
                                 ? extends V
                                 > mappingFunction) throws IllegalStateException {
        ConcurrentHashMap<CLV, Object> map = map(cl);
        @SuppressWarnings("unchecked")
        CLV clv = (CLV) this;
        Memoizer<CLV, V> mv = null;
        while (true) {
            Object val = (mv == null) ? map.get(clv) : map.putIfAbsent(clv, mv);
            if (val == null) {
                if (mv == null) {
                    // create Memoizer lazily when 1st needed and restart loop
                    mv = new Memoizer<>(cl, clv, mappingFunction);
                    continue;
                }
                // mv != null, therefore sv == null was a result of successful
                // putIfAbsent
                try {
                    // trigger Memoizer to compute the value
                    V v = mv.get();
                    // attempt to replace our Memoizer with the value
                    map.replace(clv, mv, v);
                    // return computed value
                    return v;
                } catch (Throwable t) {
                    // our Memoizer has thrown, attempt to remove it
                    map.remove(clv, mv);
                    // propagate exception because it's from our Memoizer
                    throw t;
                }
            } else {
                try {
                    return extractValue(val);
                } catch (Memoizer.RecursiveInvocationException e) {
                    // propagate recursive attempts to calculate the same
                    // value as being calculated at the moment
                    throw e;
                } catch (Throwable t) {
                    // don't propagate exceptions thrown from foreign Memoizer -
                    // pretend that there was no entry and retry
                    // (foreign computeIfAbsent invocation will try to remove it anyway)
                }
            }
            // TODO:
            // Thread.onSpinLoop(); // when available
        }
    }

    /**
     * Removes all values associated with given ClassLoader {@code cl} and
     * {@link #isEqualOrDescendantOf(AbstractClassLoaderValue) this or descendants}
     * of this ClassLoaderValue.
     * This is not an atomic operation. Other threads may see some associations
     * be already removed and others still present while this method is executing.
     * <p>
     * The sole intention of this method is to cleanup after a unit test that
     * tests ClassLoaderValue directly. It is not intended for use in
     * actual algorithms.
     *
     * @param cl the associated ClassLoader of the values to be removed
     */
    public void removeAll(ClassLoader cl) {
        ConcurrentHashMap<CLV, Object> map = map(cl);
        for (Iterator<CLV> i = map.keySet().iterator(); i.hasNext(); ) {
            if (i.next().isEqualOrDescendantOf(this)) {
                i.remove();
            }
        }
    }

    private static final JavaLangAccess JLA = SharedSecrets.getJavaLangAccess();

    /**
     * @return a ConcurrentHashMap for given ClassLoader
     */
    @SuppressWarnings("unchecked")
    private static <CLV extends AbstractClassLoaderValue<CLV, ?>>
    ConcurrentHashMap<CLV, Object> map(ClassLoader cl) {
        return (ConcurrentHashMap<CLV, Object>)
            (cl == null ? BootLoader.getClassLoaderValueMap()
                        : JLA.createOrGetClassLoaderValueMap(cl));
    }

    /**
     * @return value extracted from the {@link Memoizer} if given
     * {@code memoizerOrValue} parameter is a {@code Memoizer} or
     * just return given parameter.
     */
    @SuppressWarnings("unchecked")
    private V extractValue(Object memoizerOrValue) {
        if (memoizerOrValue instanceof Memoizer) {
            return ((Memoizer<?, V>) memoizerOrValue).get();
        } else {
            return (V) memoizerOrValue;
        }
    }

    /**
     * A memoized supplier that invokes given {@code mappingFunction} just once
     * and remembers the result or thrown exception for subsequent calls.
     * If given mappingFunction returns null, it is converted to NullPointerException,
     * thrown from the Memoizer's {@link #get()} method and remembered.
     * If the Memoizer is invoked recursively from the given {@code mappingFunction},
     * {@link RecursiveInvocationException} is thrown, but it is not remembered.
     * The in-flight call to the {@link #get()} can still complete successfully if
     * such exception is handled by the mappingFunction.
     */
    private static final class Memoizer<CLV extends AbstractClassLoaderValue<CLV, V>, V>
        implements Supplier<V> {

        private final ClassLoader cl;
        private final CLV clv;
        private final BiFunction<? super ClassLoader, ? super CLV, ? extends V>
            mappingFunction;

        private volatile V v;
        private volatile Throwable t;
        private boolean inCall;

        Memoizer(ClassLoader cl,
                 CLV clv,
                 BiFunction<? super ClassLoader, ? super CLV, ? extends V>
                     mappingFunction
        ) {
            this.cl = cl;
            this.clv = clv;
            this.mappingFunction = mappingFunction;
        }

        @Override
        public V get() throws RecursiveInvocationException {
            V v = this.v;
            if (v != null) return v;
            Throwable t = this.t;
            if (t == null) {
                synchronized (this) {
                    if ((v = this.v) == null && (t = this.t) == null) {
                        if (inCall) {
                            throw new RecursiveInvocationException();
                        }
                        inCall = true;
                        try {
                            this.v = v = Objects.requireNonNull(
                                mappingFunction.apply(cl, clv));
                        } catch (Throwable x) {
                            this.t = t = x;
                        } finally {
                            inCall = false;
                        }
                    }
                }
            }
            if (v != null) return v;
            if (t instanceof Error) {
                throw (Error) t;
            } else if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            } else {
                throw new UndeclaredThrowableException(t);
            }
        }

        static class RecursiveInvocationException extends IllegalStateException {
            @java.io.Serial
            private static final long serialVersionUID = 1L;

            RecursiveInvocationException() {
                super("Recursive call");
            }
        }
    }

    /**
     * sub-ClassLoaderValue is an inner class of {@link AbstractClassLoaderValue}
     * and also a subclass of it. It can therefore be instantiated as an inner
     * class of either an instance of root-{@link ClassLoaderValue} or another
     * instance of itself. This enables composing type-safe compound keys of
     * arbitrary length:
     * <pre>{@code
     * ClassLoaderValue<V> clv = new ClassLoaderValue<>();
     * ClassLoaderValue<V>.Sub<K1>.Sub<K2>.Sub<K3> clv_k123 =
     *     clv.sub(k1).sub(k2).sub(k3);
     * }</pre>
     * From which individual components are accessible in a type-safe way:
     * <pre>{@code
     * K1 k1 = clv_k123.parent().parent().key();
     * K2 k2 = clv_k123.parent().key();
     * K3 k3 = clv_k123.key();
     * }</pre>
     * This allows specifying non-capturing lambdas for the mapping function of
     * {@link #computeIfAbsent(ClassLoader, BiFunction)} operation that can
     * access individual key components from passed-in
     * sub-[sub-...]ClassLoaderValue instance in a type-safe way.
     *
     * @param <K> the type of {@link #key()} component contained in the
     *            sub-ClassLoaderValue.
     */
    public final class Sub<K> extends AbstractClassLoaderValue<Sub<K>, V> {

        private final K key;

        Sub(K key) {
            this.key = key;
        }

        /**
         * @return the parent ClassLoaderValue this sub-ClassLoaderValue
         * has been {@link #sub(Object) derived} from.
         */
        public AbstractClassLoaderValue<CLV, V> parent() {
            return AbstractClassLoaderValue.this;
        }

        /**
         * @return the key component of this sub-ClassLoaderValue.
         */
        @Override
        public K key() {
            return key;
        }

        /**
         * sub-ClassLoaderValue is a descendant of given {@code clv} if it is
         * either equal to it or if its {@link #parent() parent} is a
         * descendant of given {@code clv}.
         */
        @Override
        public boolean isEqualOrDescendantOf(AbstractClassLoaderValue<?, V> clv) {
            return equals(Objects.requireNonNull(clv)) ||
                   parent().isEqualOrDescendantOf(clv);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Sub)) return false;
            @SuppressWarnings("unchecked")
            Sub<?> that = (Sub<?>) o;
            return this.parent().equals(that.parent()) &&
                   Objects.equals(this.key, that.key);
        }

        @Override
        public int hashCode() {
            return 31 * parent().hashCode() +
                   Objects.hashCode(key);
        }
    }
}
