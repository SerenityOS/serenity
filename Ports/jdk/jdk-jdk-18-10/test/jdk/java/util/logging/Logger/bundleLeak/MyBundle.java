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
import java.util.Collections;
import java.util.Enumeration;
import java.util.ResourceBundle;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MyBundle extends ResourceBundle {
    Map<String, String> mapping = new ConcurrentHashMap<>();

    /**
     * Gets an object for the given key from this resource bundle.
     * Returns null if this resource bundle does not contain an
     * object for the given key.
     *
     * @param key the key for the desired object
     * @throws    NullPointerException if {@code key} is {@code null}
     * @return the object for the given key, or null
     */
    protected Object handleGetObject(String key) {
        return mapping.computeIfAbsent(key,
                (k) -> k + "-" + System.identityHashCode(this.getClass().getClassLoader()));
    }

    /**
     * Returns an enumeration of the keys.
     *
     * @return an {@code Enumeration} of the keys contained in
     *         this {@code ResourceBundle} and its parent bundles.
     */
    public Enumeration<String> getKeys() {
        return Collections.enumeration(mapping.keySet());
    }
}
