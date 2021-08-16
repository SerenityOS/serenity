/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.pkcs11;

import java.util.*;
import java.lang.ref.*;

import java.security.Key;

import sun.security.util.Cache;

/**
 * Key to P11Key translation cache. The PKCS#11 token can only perform
 * operations on keys stored on the token (permanently or temporarily). That
 * means that in order to allow the PKCS#11 provider to use keys from other
 * providers, we need to transparently convert them to P11Keys. The engines
 * do that using (Secret)KeyFactories, which in turn use this class as a
 * cache.
 *
 * There are two KeyCache instances per provider, one for secret keys and
 * one for public and private keys.
 *
 * @author  Andreas Sterbenz
 * @since   1.5
 */
final class KeyCache {

    private final Cache<IdentityWrapper, P11Key> strongCache;

    private WeakReference<Map<Key,P11Key>> cacheReference;

    KeyCache() {
        strongCache = Cache.newHardMemoryCache(16);
    }

    private static final class IdentityWrapper {
        final Object obj;
        IdentityWrapper(Object obj) {
            this.obj = obj;
        }
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o instanceof IdentityWrapper == false) {
                return false;
            }
            IdentityWrapper other = (IdentityWrapper)o;
            return this.obj == other.obj;
        }
        public int hashCode() {
            return System.identityHashCode(obj);
        }
    }

    synchronized P11Key get(Key key) {
        P11Key p11Key = strongCache.get(new IdentityWrapper(key));
        if (p11Key != null) {
            return p11Key;
        }
        Map<Key,P11Key> map =
                (cacheReference == null) ? null : cacheReference.get();
        if (map == null) {
            return null;
        }
        return map.get(key);
    }

    synchronized void put(Key key, P11Key p11Key) {
        strongCache.put(new IdentityWrapper(key), p11Key);
        Map<Key,P11Key> map =
                (cacheReference == null) ? null : cacheReference.get();
        if (map == null) {
            map = new IdentityHashMap<>();
            cacheReference = new WeakReference<>(map);
        }
        map.put(key, p11Key);
    }

    synchronized void clear() {
        strongCache.clear();
        cacheReference = null;
    }
}
