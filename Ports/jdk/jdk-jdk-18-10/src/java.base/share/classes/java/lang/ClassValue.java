/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

package java.lang;

import java.util.WeakHashMap;
import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicInteger;

import jdk.internal.misc.Unsafe;

import static java.lang.ClassValue.ClassValueMap.probeHomeLocation;
import static java.lang.ClassValue.ClassValueMap.probeBackupLocations;

/**
 * Lazily associate a computed value with (potentially) every type.
 * For example, if a dynamic language needs to construct a message dispatch
 * table for each class encountered at a message send call site,
 * it can use a {@code ClassValue} to cache information needed to
 * perform the message send quickly, for each class encountered.
 * @author John Rose, JSR 292 EG
 * @since 1.7
 */
public abstract class ClassValue<T> {
    /**
     * Sole constructor.  (For invocation by subclass constructors, typically
     * implicit.)
     */
    protected ClassValue() {
    }

    /**
     * Computes the given class's derived value for this {@code ClassValue}.
     * <p>
     * This method will be invoked within the first thread that accesses
     * the value with the {@link #get get} method.
     * <p>
     * Normally, this method is invoked at most once per class,
     * but it may be invoked again if there has been a call to
     * {@link #remove remove}.
     * <p>
     * If this method throws an exception, the corresponding call to {@code get}
     * will terminate abnormally with that exception, and no class value will be recorded.
     *
     * @param type the type whose class value must be computed
     * @return the newly computed value associated with this {@code ClassValue}, for the given class or interface
     * @see #get
     * @see #remove
     */
    protected abstract T computeValue(Class<?> type);

    /**
     * Returns the value for the given class.
     * If no value has yet been computed, it is obtained by
     * an invocation of the {@link #computeValue computeValue} method.
     * <p>
     * The actual installation of the value on the class
     * is performed atomically.
     * At that point, if several racing threads have
     * computed values, one is chosen, and returned to
     * all the racing threads.
     * <p>
     * The {@code type} parameter is typically a class, but it may be any type,
     * such as an interface, a primitive type (like {@code int.class}), or {@code void.class}.
     * <p>
     * In the absence of {@code remove} calls, a class value has a simple
     * state diagram:  uninitialized and initialized.
     * When {@code remove} calls are made,
     * the rules for value observation are more complex.
     * See the documentation for {@link #remove remove} for more information.
     *
     * @param type the type whose class value must be computed or retrieved
     * @return the current value associated with this {@code ClassValue}, for the given class or interface
     * @throws NullPointerException if the argument is null
     * @see #remove
     * @see #computeValue
     */
    public T get(Class<?> type) {
        // non-racing this.hashCodeForCache : final int
        Entry<?>[] cache;
        Entry<T> e = probeHomeLocation(cache = getCacheCarefully(type), this);
        // racing e : current value <=> stale value from current cache or from stale cache
        // invariant:  e is null or an Entry with readable Entry.version and Entry.value
        if (match(e))
            // invariant:  No false positive matches.  False negatives are OK if rare.
            // The key fact that makes this work: if this.version == e.version,
            // then this thread has a right to observe (final) e.value.
            return e.value();
        // The fast path can fail for any of these reasons:
        // 1. no entry has been computed yet
        // 2. hash code collision (before or after reduction mod cache.length)
        // 3. an entry has been removed (either on this type or another)
        // 4. the GC has somehow managed to delete e.version and clear the reference
        return getFromBackup(cache, type);
    }

    /**
     * Removes the associated value for the given class.
     * If this value is subsequently {@linkplain #get read} for the same class,
     * its value will be reinitialized by invoking its {@link #computeValue computeValue} method.
     * This may result in an additional invocation of the
     * {@code computeValue} method for the given class.
     * <p>
     * In order to explain the interaction between {@code get} and {@code remove} calls,
     * we must model the state transitions of a class value to take into account
     * the alternation between uninitialized and initialized states.
     * To do this, number these states sequentially from zero, and note that
     * uninitialized (or removed) states are numbered with even numbers,
     * while initialized (or re-initialized) states have odd numbers.
     * <p>
     * When a thread {@code T} removes a class value in state {@code 2N},
     * nothing happens, since the class value is already uninitialized.
     * Otherwise, the state is advanced atomically to {@code 2N+1}.
     * <p>
     * When a thread {@code T} queries a class value in state {@code 2N},
     * the thread first attempts to initialize the class value to state {@code 2N+1}
     * by invoking {@code computeValue} and installing the resulting value.
     * <p>
     * When {@code T} attempts to install the newly computed value,
     * if the state is still at {@code 2N}, the class value will be initialized
     * with the computed value, advancing it to state {@code 2N+1}.
     * <p>
     * Otherwise, whether the new state is even or odd,
     * {@code T} will discard the newly computed value
     * and retry the {@code get} operation.
     * <p>
     * Discarding and retrying is an important proviso,
     * since otherwise {@code T} could potentially install
     * a disastrously stale value.  For example:
     * <ul>
     * <li>{@code T} calls {@code CV.get(C)} and sees state {@code 2N}
     * <li>{@code T} quickly computes a time-dependent value {@code V0} and gets ready to install it
     * <li>{@code T} is hit by an unlucky paging or scheduling event, and goes to sleep for a long time
     * <li>...meanwhile, {@code T2} also calls {@code CV.get(C)} and sees state {@code 2N}
     * <li>{@code T2} quickly computes a similar time-dependent value {@code V1} and installs it on {@code CV.get(C)}
     * <li>{@code T2} (or a third thread) then calls {@code CV.remove(C)}, undoing {@code T2}'s work
     * <li> the previous actions of {@code T2} are repeated several times
     * <li> also, the relevant computed values change over time: {@code V1}, {@code V2}, ...
     * <li>...meanwhile, {@code T} wakes up and attempts to install {@code V0}; <em>this must fail</em>
     * </ul>
     * We can assume in the above scenario that {@code CV.computeValue} uses locks to properly
     * observe the time-dependent states as it computes {@code V1}, etc.
     * This does not remove the threat of a stale value, since there is a window of time
     * between the return of {@code computeValue} in {@code T} and the installation
     * of the new value.  No user synchronization is possible during this time.
     *
     * @param type the type whose class value must be removed
     * @throws NullPointerException if the argument is null
     */
    public void remove(Class<?> type) {
        ClassValueMap map = getMap(type);
        map.removeEntry(this);
    }

    // Possible functionality for JSR 292 MR 1
    /*public*/ void put(Class<?> type, T value) {
        ClassValueMap map = getMap(type);
        map.changeEntry(this, value);
    }

    /// --------
    /// Implementation...
    /// --------

    /** Return the cache, if it exists, else a dummy empty cache. */
    private static Entry<?>[] getCacheCarefully(Class<?> type) {
        // racing type.classValueMap{.cacheArray} : null => new Entry[X] <=> new Entry[Y]
        ClassValueMap map = type.classValueMap;
        if (map == null)  return EMPTY_CACHE;
        Entry<?>[] cache = map.getCache();
        return cache;
        // invariant:  returned value is safe to dereference and check for an Entry
    }

    /** Initial, one-element, empty cache used by all Class instances.  Must never be filled. */
    private static final Entry<?>[] EMPTY_CACHE = { null };

    /**
     * Slow tail of ClassValue.get to retry at nearby locations in the cache,
     * or take a slow lock and check the hash table.
     * Called only if the first probe was empty or a collision.
     * This is a separate method, so compilers can process it independently.
     */
    private T getFromBackup(Entry<?>[] cache, Class<?> type) {
        Entry<T> e = probeBackupLocations(cache, this);
        if (e != null)
            return e.value();
        return getFromHashMap(type);
    }

    // Hack to suppress warnings on the (T) cast, which is a no-op.
    @SuppressWarnings("unchecked")
    Entry<T> castEntry(Entry<?> e) { return (Entry<T>) e; }

    /** Called when the fast path of get fails, and cache reprobe also fails.
     */
    private T getFromHashMap(Class<?> type) {
        // The fail-safe recovery is to fall back to the underlying classValueMap.
        ClassValueMap map = getMap(type);
        for (;;) {
            Entry<T> e = map.startEntry(this);
            if (!e.isPromise())
                return e.value();
            try {
                // Try to make a real entry for the promised version.
                e = makeEntry(e.version(), computeValue(type));
            } finally {
                // Whether computeValue throws or returns normally,
                // be sure to remove the empty entry.
                e = map.finishEntry(this, e);
            }
            if (e != null)
                return e.value();
            // else try again, in case a racing thread called remove (so e == null)
        }
    }

    /** Check that e is non-null, matches this ClassValue, and is live. */
    boolean match(Entry<?> e) {
        // racing e.version : null (blank) => unique Version token => null (GC-ed version)
        // non-racing this.version : v1 => v2 => ... (updates are read faithfully from volatile)
        return (e != null && e.get() == this.version);
        // invariant:  No false positives on version match.  Null is OK for false negative.
        // invariant:  If version matches, then e.value is readable (final set in Entry.<init>)
    }

    /** Internal hash code for accessing Class.classValueMap.cacheArray. */
    final int hashCodeForCache = nextHashCode.getAndAdd(HASH_INCREMENT) & HASH_MASK;

    /** Value stream for hashCodeForCache.  See similar structure in ThreadLocal. */
    private static final AtomicInteger nextHashCode = new AtomicInteger();

    /** Good for power-of-two tables.  See similar structure in ThreadLocal. */
    private static final int HASH_INCREMENT = 0x61c88647;

    /** Mask a hash code to be positive but not too large, to prevent wraparound. */
    static final int HASH_MASK = (-1 >>> 2);

    /**
     * Private key for retrieval of this object from ClassValueMap.
     */
    static class Identity {
    }
    /**
     * This ClassValue's identity, expressed as an opaque object.
     * The main object {@code ClassValue.this} is incorrect since
     * subclasses may override {@code ClassValue.equals}, which
     * could confuse keys in the ClassValueMap.
     */
    final Identity identity = new Identity();

    /**
     * Current version for retrieving this class value from the cache.
     * Any number of computeValue calls can be cached in association with one version.
     * But the version changes when a remove (on any type) is executed.
     * A version change invalidates all cache entries for the affected ClassValue,
     * by marking them as stale.  Stale cache entries do not force another call
     * to computeValue, but they do require a synchronized visit to a backing map.
     * <p>
     * All user-visible state changes on the ClassValue take place under
     * a lock inside the synchronized methods of ClassValueMap.
     * Readers (of ClassValue.get) are notified of such state changes
     * when this.version is bumped to a new token.
     * This variable must be volatile so that an unsynchronized reader
     * will receive the notification without delay.
     * <p>
     * If version were not volatile, one thread T1 could persistently hold onto
     * a stale value this.value == V1, while another thread T2 advances
     * (under a lock) to this.value == V2.  This will typically be harmless,
     * but if T1 and T2 interact causally via some other channel, such that
     * T1's further actions are constrained (in the JMM) to happen after
     * the V2 event, then T1's observation of V1 will be an error.
     * <p>
     * The practical effect of making this.version be volatile is that it cannot
     * be hoisted out of a loop (by an optimizing JIT) or otherwise cached.
     * Some machines may also require a barrier instruction to execute
     * before this.version.
     */
    private volatile Version<T> version = new Version<>(this);
    Version<T> version() { return version; }
    void bumpVersion() { version = new Version<>(this); }
    static class Version<T> {
        private final ClassValue<T> classValue;
        private final Entry<T> promise = new Entry<>(this);
        Version(ClassValue<T> classValue) { this.classValue = classValue; }
        ClassValue<T> classValue() { return classValue; }
        Entry<T> promise() { return promise; }
        boolean isLive() { return classValue.version() == this; }
    }

    /** One binding of a value to a class via a ClassValue.
     *  States are:<ul>
     *  <li> promise if value == Entry.this
     *  <li> else dead if version == null
     *  <li> else stale if version != classValue.version
     *  <li> else live </ul>
     *  Promises are never put into the cache; they only live in the
     *  backing map while a computeValue call is in flight.
     *  Once an entry goes stale, it can be reset at any time
     *  into the dead state.
     */
    static class Entry<T> extends WeakReference<Version<T>> {
        final Object value;  // usually of type T, but sometimes (Entry)this
        Entry(Version<T> version, T value) {
            super(version);
            this.value = value;  // for a regular entry, value is of type T
        }
        private void assertNotPromise() { assert(!isPromise()); }
        /** For creating a promise. */
        Entry(Version<T> version) {
            super(version);
            this.value = this;  // for a promise, value is not of type T, but Entry!
        }
        /** Fetch the value.  This entry must not be a promise. */
        @SuppressWarnings("unchecked")  // if !isPromise, type is T
        T value() { assertNotPromise(); return (T) value; }
        boolean isPromise() { return value == this; }
        Version<T> version() { return get(); }
        ClassValue<T> classValueOrNull() {
            Version<T> v = version();
            return (v == null) ? null : v.classValue();
        }
        boolean isLive() {
            Version<T> v = version();
            if (v == null)  return false;
            if (v.isLive())  return true;
            clear();
            return false;
        }
        Entry<T> refreshVersion(Version<T> v2) {
            assertNotPromise();
            @SuppressWarnings("unchecked")  // if !isPromise, type is T
            Entry<T> e2 = new Entry<>(v2, (T) value);
            clear();
            // value = null -- caller must drop
            return e2;
        }
        static final Entry<?> DEAD_ENTRY = new Entry<>(null, null);
    }

    /** Return the backing map associated with this type. */
    private static ClassValueMap getMap(Class<?> type) {
        // racing type.classValueMap : null (blank) => unique ClassValueMap
        // if a null is observed, a map is created (lazily, synchronously, uniquely)
        // all further access to that map is synchronized
        ClassValueMap map = type.classValueMap;
        if (map != null)  return map;
        return initializeMap(type);
    }

    private static final Object CRITICAL_SECTION = new Object();
    private static final Unsafe UNSAFE = Unsafe.getUnsafe();
    private static ClassValueMap initializeMap(Class<?> type) {
        ClassValueMap map;
        synchronized (CRITICAL_SECTION) {  // private object to avoid deadlocks
            // happens about once per type
            if ((map = type.classValueMap) == null) {
                map = new ClassValueMap();
                // Place a Store fence after construction and before publishing to emulate
                // ClassValueMap containing final fields. This ensures it can be
                // published safely in the non-volatile field Class.classValueMap,
                // since stores to the fields of ClassValueMap will not be reordered
                // to occur after the store to the field type.classValueMap
                UNSAFE.storeFence();

                type.classValueMap = map;
            }
        }
        return map;
    }

    static <T> Entry<T> makeEntry(Version<T> explicitVersion, T value) {
        // Note that explicitVersion might be different from this.version.
        return new Entry<>(explicitVersion, value);

        // As soon as the Entry is put into the cache, the value will be
        // reachable via a data race (as defined by the Java Memory Model).
        // This race is benign, assuming the value object itself can be
        // read safely by multiple threads.  This is up to the user.
        //
        // The entry and version fields themselves can be safely read via
        // a race because they are either final or have controlled states.
        // If the pointer from the entry to the version is still null,
        // or if the version goes immediately dead and is nulled out,
        // the reader will take the slow path and retry under a lock.
    }

    // The following class could also be top level and non-public:

    /** A backing map for all ClassValues.
     *  Gives a fully serialized "true state" for each pair (ClassValue cv, Class type).
     *  Also manages an unserialized fast-path cache.
     */
    static class ClassValueMap extends WeakHashMap<ClassValue.Identity, Entry<?>> {
        private Entry<?>[] cacheArray;
        private int cacheLoad, cacheLoadLimit;

        /** Number of entries initially allocated to each type when first used with any ClassValue.
         *  It would be pointless to make this much smaller than the Class and ClassValueMap objects themselves.
         *  Must be a power of 2.
         */
        private static final int INITIAL_ENTRIES = 32;

        /** Build a backing map for ClassValues.
         *  Also, create an empty cache array and install it on the class.
         */
        ClassValueMap() {
            sizeCache(INITIAL_ENTRIES);
        }

        Entry<?>[] getCache() { return cacheArray; }

        /** Initiate a query.  Store a promise (placeholder) if there is no value yet. */
        synchronized
        <T> Entry<T> startEntry(ClassValue<T> classValue) {
            @SuppressWarnings("unchecked")  // one map has entries for all value types <T>
            Entry<T> e = (Entry<T>) get(classValue.identity);
            Version<T> v = classValue.version();
            if (e == null) {
                e = v.promise();
                // The presence of a promise means that a value is pending for v.
                // Eventually, finishEntry will overwrite the promise.
                put(classValue.identity, e);
                // Note that the promise is never entered into the cache!
                return e;
            } else if (e.isPromise()) {
                // Somebody else has asked the same question.
                // Let the races begin!
                if (e.version() != v) {
                    e = v.promise();
                    put(classValue.identity, e);
                }
                return e;
            } else {
                // there is already a completed entry here; report it
                if (e.version() != v) {
                    // There is a stale but valid entry here; make it fresh again.
                    // Once an entry is in the hash table, we don't care what its version is.
                    e = e.refreshVersion(v);
                    put(classValue.identity, e);
                }
                // Add to the cache, to enable the fast path, next time.
                checkCacheLoad();
                addToCache(classValue, e);
                return e;
            }
        }

        /** Finish a query.  Overwrite a matching placeholder.  Drop stale incoming values. */
        synchronized
        <T> Entry<T> finishEntry(ClassValue<T> classValue, Entry<T> e) {
            @SuppressWarnings("unchecked")  // one map has entries for all value types <T>
            Entry<T> e0 = (Entry<T>) get(classValue.identity);
            if (e == e0) {
                // We can get here during exception processing, unwinding from computeValue.
                assert(e.isPromise());
                remove(classValue.identity);
                return null;
            } else if (e0 != null && e0.isPromise() && e0.version() == e.version()) {
                // If e0 matches the intended entry, there has not been a remove call
                // between the previous startEntry and now.  So now overwrite e0.
                Version<T> v = classValue.version();
                if (e.version() != v)
                    e = e.refreshVersion(v);
                put(classValue.identity, e);
                // Add to the cache, to enable the fast path, next time.
                checkCacheLoad();
                addToCache(classValue, e);
                return e;
            } else {
                // Some sort of mismatch; caller must try again.
                return null;
            }
        }

        /** Remove an entry. */
        synchronized
        void removeEntry(ClassValue<?> classValue) {
            Entry<?> e = remove(classValue.identity);
            if (e == null) {
                // Uninitialized, and no pending calls to computeValue.  No change.
            } else if (e.isPromise()) {
                // State is uninitialized, with a pending call to finishEntry.
                // Since remove is a no-op in such a state, keep the promise
                // by putting it back into the map.
                put(classValue.identity, e);
            } else {
                // In an initialized state.  Bump forward, and de-initialize.
                classValue.bumpVersion();
                // Make all cache elements for this guy go stale.
                removeStaleEntries(classValue);
            }
        }

        /** Change the value for an entry. */
        synchronized
        <T> void changeEntry(ClassValue<T> classValue, T value) {
            @SuppressWarnings("unchecked")  // one map has entries for all value types <T>
            Entry<T> e0 = (Entry<T>) get(classValue.identity);
            Version<T> version = classValue.version();
            if (e0 != null) {
                if (e0.version() == version && e0.value() == value)
                    // no value change => no version change needed
                    return;
                classValue.bumpVersion();
                removeStaleEntries(classValue);
            }
            Entry<T> e = makeEntry(version, value);
            put(classValue.identity, e);
            // Add to the cache, to enable the fast path, next time.
            checkCacheLoad();
            addToCache(classValue, e);
        }

        /// --------
        /// Cache management.
        /// --------

        // Statics do not need synchronization.

        /** Load the cache entry at the given (hashed) location. */
        static Entry<?> loadFromCache(Entry<?>[] cache, int i) {
            // non-racing cache.length : constant
            // racing cache[i & (mask)] : null <=> Entry
            return cache[i & (cache.length-1)];
            // invariant:  returned value is null or well-constructed (ready to match)
        }

        /** Look in the cache, at the home location for the given ClassValue. */
        static <T> Entry<T> probeHomeLocation(Entry<?>[] cache, ClassValue<T> classValue) {
            return classValue.castEntry(loadFromCache(cache, classValue.hashCodeForCache));
        }

        /** Given that first probe was a collision, retry at nearby locations. */
        static <T> Entry<T> probeBackupLocations(Entry<?>[] cache, ClassValue<T> classValue) {
            if (PROBE_LIMIT <= 0)  return null;
            // Probe the cache carefully, in a range of slots.
            int mask = (cache.length-1);
            int home = (classValue.hashCodeForCache & mask);
            Entry<?> e2 = cache[home];  // victim, if we find the real guy
            if (e2 == null) {
                return null;   // if nobody is at home, no need to search nearby
            }
            // assume !classValue.match(e2), but do not assert, because of races
            int pos2 = -1;
            for (int i = home + 1; i < home + PROBE_LIMIT; i++) {
                Entry<?> e = cache[i & mask];
                if (e == null) {
                    break;   // only search within non-null runs
                }
                if (classValue.match(e)) {
                    // relocate colliding entry e2 (from cache[home]) to first empty slot
                    cache[home] = e;
                    if (pos2 >= 0) {
                        cache[i & mask] = Entry.DEAD_ENTRY;
                    } else {
                        pos2 = i;
                    }
                    cache[pos2 & mask] = ((entryDislocation(cache, pos2, e2) < PROBE_LIMIT)
                                          ? e2                  // put e2 here if it fits
                                          : Entry.DEAD_ENTRY);
                    return classValue.castEntry(e);
                }
                // Remember first empty slot, if any:
                if (!e.isLive() && pos2 < 0)  pos2 = i;
            }
            return null;
        }

        /** How far out of place is e? */
        private static int entryDislocation(Entry<?>[] cache, int pos, Entry<?> e) {
            ClassValue<?> cv = e.classValueOrNull();
            if (cv == null)  return 0;  // entry is not live!
            int mask = (cache.length-1);
            return (pos - cv.hashCodeForCache) & mask;
        }

        /// --------
        /// Below this line all functions are private, and assume synchronized access.
        /// --------

        private void sizeCache(int length) {
            assert((length & (length-1)) == 0);  // must be power of 2
            cacheLoad = 0;
            cacheLoadLimit = (int) ((double) length * CACHE_LOAD_LIMIT / 100);
            cacheArray = new Entry<?>[length];
        }

        /** Make sure the cache load stays below its limit, if possible. */
        private void checkCacheLoad() {
            if (cacheLoad >= cacheLoadLimit) {
                reduceCacheLoad();
            }
        }
        private void reduceCacheLoad() {
            removeStaleEntries();
            if (cacheLoad < cacheLoadLimit)
                return;  // win
            Entry<?>[] oldCache = getCache();
            if (oldCache.length > HASH_MASK)
                return;  // lose
            sizeCache(oldCache.length * 2);
            for (Entry<?> e : oldCache) {
                if (e != null && e.isLive()) {
                    addToCache(e);
                }
            }
        }

        /** Remove stale entries in the given range.
         *  Should be executed under a Map lock.
         */
        private void removeStaleEntries(Entry<?>[] cache, int begin, int count) {
            if (PROBE_LIMIT <= 0)  return;
            int mask = (cache.length-1);
            int removed = 0;
            for (int i = begin; i < begin + count; i++) {
                Entry<?> e = cache[i & mask];
                if (e == null || e.isLive())
                    continue;  // skip null and live entries
                Entry<?> replacement = null;
                if (PROBE_LIMIT > 1) {
                    // avoid breaking up a non-null run
                    replacement = findReplacement(cache, i);
                }
                cache[i & mask] = replacement;
                if (replacement == null)  removed += 1;
            }
            cacheLoad = Math.max(0, cacheLoad - removed);
        }

        /** Clearing a cache slot risks disconnecting following entries
         *  from the head of a non-null run, which would allow them
         *  to be found via reprobes.  Find an entry after cache[begin]
         *  to plug into the hole, or return null if none is needed.
         */
        private Entry<?> findReplacement(Entry<?>[] cache, int home1) {
            Entry<?> replacement = null;
            int haveReplacement = -1, replacementPos = 0;
            int mask = (cache.length-1);
            for (int i2 = home1 + 1; i2 < home1 + PROBE_LIMIT; i2++) {
                Entry<?> e2 = cache[i2 & mask];
                if (e2 == null)  break;  // End of non-null run.
                if (!e2.isLive())  continue;  // Doomed anyway.
                int dis2 = entryDislocation(cache, i2, e2);
                if (dis2 == 0)  continue;  // e2 already optimally placed
                int home2 = i2 - dis2;
                if (home2 <= home1) {
                    // e2 can replace entry at cache[home1]
                    if (home2 == home1) {
                        // Put e2 exactly where he belongs.
                        haveReplacement = 1;
                        replacementPos = i2;
                        replacement = e2;
                    } else if (haveReplacement <= 0) {
                        haveReplacement = 0;
                        replacementPos = i2;
                        replacement = e2;
                    }
                    // And keep going, so we can favor larger dislocations.
                }
            }
            if (haveReplacement >= 0) {
                if (cache[(replacementPos+1) & mask] != null) {
                    // Be conservative, to avoid breaking up a non-null run.
                    cache[replacementPos & mask] = (Entry<?>) Entry.DEAD_ENTRY;
                } else {
                    cache[replacementPos & mask] = null;
                    cacheLoad -= 1;
                }
            }
            return replacement;
        }

        /** Remove stale entries in the range near classValue. */
        private void removeStaleEntries(ClassValue<?> classValue) {
            removeStaleEntries(getCache(), classValue.hashCodeForCache, PROBE_LIMIT);
        }

        /** Remove all stale entries, everywhere. */
        private void removeStaleEntries() {
            Entry<?>[] cache = getCache();
            removeStaleEntries(cache, 0, cache.length + PROBE_LIMIT - 1);
        }

        /** Add the given entry to the cache, in its home location, unless it is out of date. */
        private <T> void addToCache(Entry<T> e) {
            ClassValue<T> classValue = e.classValueOrNull();
            if (classValue != null)
                addToCache(classValue, e);
        }

        /** Add the given entry to the cache, in its home location. */
        private <T> void addToCache(ClassValue<T> classValue, Entry<T> e) {
            if (PROBE_LIMIT <= 0)  return;  // do not fill cache
            // Add e to the cache.
            Entry<?>[] cache = getCache();
            int mask = (cache.length-1);
            int home = classValue.hashCodeForCache & mask;
            Entry<?> e2 = placeInCache(cache, home, e, false);
            if (e2 == null)  return;  // done
            if (PROBE_LIMIT > 1) {
                // try to move e2 somewhere else in his probe range
                int dis2 = entryDislocation(cache, home, e2);
                int home2 = home - dis2;
                for (int i2 = home2; i2 < home2 + PROBE_LIMIT; i2++) {
                    if (placeInCache(cache, i2 & mask, e2, true) == null) {
                        return;
                    }
                }
            }
            // Note:  At this point, e2 is just dropped from the cache.
        }

        /** Store the given entry.  Update cacheLoad, and return any live victim.
         *  'Gently' means return self rather than dislocating a live victim.
         */
        private Entry<?> placeInCache(Entry<?>[] cache, int pos, Entry<?> e, boolean gently) {
            Entry<?> e2 = overwrittenEntry(cache[pos]);
            if (gently && e2 != null) {
                // do not overwrite a live entry
                return e;
            } else {
                cache[pos] = e;
                return e2;
            }
        }

        /** Note an entry that is about to be overwritten.
         *  If it is not live, quietly replace it by null.
         *  If it is an actual null, increment cacheLoad,
         *  because the caller is going to store something
         *  in its place.
         */
        private <T> Entry<T> overwrittenEntry(Entry<T> e2) {
            if (e2 == null)  cacheLoad += 1;
            else if (e2.isLive())  return e2;
            return null;
        }

        /** Percent loading of cache before resize. */
        private static final int CACHE_LOAD_LIMIT = 67;  // 0..100
        /** Maximum number of probes to attempt. */
        private static final int PROBE_LIMIT      =  6;       // 1..
        // N.B.  Set PROBE_LIMIT=0 to disable all fast paths.
    }
}
