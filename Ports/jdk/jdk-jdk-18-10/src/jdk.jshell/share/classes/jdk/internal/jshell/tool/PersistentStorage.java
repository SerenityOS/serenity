/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

package jdk.internal.jshell.tool;

/**
 * The required functionality jshell uses for persistent storage.  Implementable
 * by both Preferences API and Map.
 */
interface PersistentStorage {

    /**
     * Removes all of the preferences (key-value associations) in
     * preferences.
     *
     * @throws IllegalStateException if this operation cannot be completed
     * because of the state of the system.
     */
    void clear();

    /**
     * Returns all of the keys that have an associated value in
     * preferences.
     *
     * @return an array of the keys that have an associated value in this
     * preference node.
     * @throws IllegalStateException if this operation cannot be completed
     * because of the state of the system.
     */
    String[] keys();

    /**
     * Returns the value associated with the specified key in preferences.
     *
     * @param key key whose associated value is to be returned.
     * @return the value associated with {@code key}, or {@code null} if no
     * value is associated with {@code key}.
     * @throws IllegalStateException if this operation cannot be completed
     * because of the state of the system.
     * @throws NullPointerException if {@code key} is {@code null}.
     */
    String get(String key);

    /**
     * Associates the specified value with the specified key in this
     * preference node.
     *
     * @param key key with which the specified value is to be associated.
     * @param value value to be associated with the specified key.
     * @throws NullPointerException if key or value is {@code null}.
     * @throws IllegalArgumentException if key or value are too long.
     * @throws IllegalStateException if this operation cannot be completed
     * because of the state of the system.
     */
    void put(String key, String value);

    /**
     * Removes the value associated with the specified key in preferences,
     * if any.
     *
     * @param key key whose mapping is to be removed from the preference
     * node.
     * @throws NullPointerException if {@code key} is {@code null}.
     * @throws IllegalStateException if this operation cannot be completed
     * because of the state of the system.
     */
    void remove(String key);

    /**
     * Forces any changes in the contents of this preferences to be stored.
     * Once this method returns successfully, it is safe to assume that all
     * changes have become as permanent as they are going to be.
     * <p>
     * Implementations are free to flush changes into the persistent store
     * at any time. They do not need to wait for this method to be called.
     *
     * @throws IllegalStateException if this operation cannot be completed
     * because of the state of the system.
     */
    void flush();

}
