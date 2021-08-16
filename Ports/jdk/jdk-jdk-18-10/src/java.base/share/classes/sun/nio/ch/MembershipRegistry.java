/*
 * Copyright (c) 2008, 2019, Oracle and/or its affiliates. All rights reserved.
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

package sun.nio.ch;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.channels.MembershipKey;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Simple registry of membership keys for a MulticastChannel.
 *
 * Instances of this object are not safe by multiple concurrent threads.
 */

class MembershipRegistry {

    // map multicast group to list of keys
    private Map<InetAddress, List<MembershipKeyImpl>> groups;

    MembershipRegistry() {
    }

    /**
     * Checks registry for membership of the group on the given
     * network interface.
     */
    MembershipKey checkMembership(InetAddress group, NetworkInterface interf,
                                  InetAddress source)
    {
        if (groups != null) {
            List<MembershipKeyImpl> keys = groups.get(group);
            if (keys != null) {
                for (MembershipKeyImpl key: keys) {
                    if (key.networkInterface().equals(interf)) {
                        // already a member to receive all packets so return
                        // existing key or detect conflict
                        if (source == null) {
                            if (key.sourceAddress() == null)
                                return key;
                            throw new IllegalStateException("Already a member to receive all packets");
                        }

                        // already have source-specific membership so return key
                        // or detect conflict
                        if (key.sourceAddress() == null)
                            throw new IllegalStateException("Already have source-specific membership");
                        if (source.equals(key.sourceAddress()))
                            return key;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Add membership to the registry, returning a new membership key.
     */
    void add(MembershipKeyImpl key) {
        InetAddress group = key.group();
        List<MembershipKeyImpl> keys;
        if (groups == null) {
            groups = new HashMap<>();
            keys = null;
        } else {
            keys = groups.get(group);
        }
        if (keys == null) {
            keys = new ArrayList<>();
            groups.put(group, keys);
        }
        keys.add(key);
    }

    /**
     * Remove a key from the registry
     */
    void remove(MembershipKeyImpl key) {
        InetAddress group = key.group();
        List<MembershipKeyImpl> keys = groups.get(group);
        if (keys != null) {
            Iterator<MembershipKeyImpl> i = keys.iterator();
            while (i.hasNext()) {
                if (i.next() == key) {
                    i.remove();
                    break;
                }
            }
            if (keys.isEmpty()) {
                groups.remove(group);
            }
        }
    }

    @FunctionalInterface
    interface ThrowingConsumer<T, X extends Throwable> {
        void accept(T action) throws X;
    }

    /**
     * Invoke an action for each key in the registry
     */
     <X extends Throwable>
     void forEach(ThrowingConsumer<MembershipKeyImpl, X> action) throws X {
        if (groups != null) {
            for (List<MembershipKeyImpl> keys : groups.values()) {
                for (MembershipKeyImpl key : keys) {
                    action.accept(key);
                }
            }
        }
    }

    /**
     * Invalidate all keys in the registry
     */
    void invalidateAll() {
        forEach(MembershipKeyImpl::invalidate);
    }
}
