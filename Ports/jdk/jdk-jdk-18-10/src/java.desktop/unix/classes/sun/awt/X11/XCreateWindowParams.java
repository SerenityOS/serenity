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

package sun.awt.X11;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@SuppressWarnings("serial") // JDK-implementation class
public class XCreateWindowParams extends HashMap<Object, Object> {
    public XCreateWindowParams() {
    }
    public XCreateWindowParams(Object[] map) {
        init(map);
    }
    private void init(Object[] map) {
        if (map.length % 2 != 0) {
            throw new IllegalArgumentException("Map size should be devisible by two");
        }
        for (int i = 0; i < map.length; i += 2) {
            put(map[i], map[i+1]);
        }
    }

    public XCreateWindowParams putIfNull(Object key, Object value) {
        if (!containsKey(key)) {
            put(key, value);
        }
        return this;
    }
    public XCreateWindowParams putIfNull(Object key, int value) {
        if (!containsKey(key)) {
            put(key, Integer.valueOf(value));
        }
        return this;
    }
    public XCreateWindowParams putIfNull(Object key, long value) {
        if (!containsKey(key)) {
            put(key, Long.valueOf(value));
        }
        return this;
    }

    public XCreateWindowParams add(Object key, Object value) {
        put(key, value);
        return this;
    }
    public XCreateWindowParams add(Object key, int value) {
        put(key, Integer.valueOf(value));
        return this;
    }
    public XCreateWindowParams add(Object key, long value) {
        put(key, Long.valueOf(value));
        return this;
    }
    public XCreateWindowParams delete(Object key) {
        remove(key);
        return this;
    }
    public String toString() {
        StringBuilder buf = new StringBuilder();
        Iterator<Map.Entry<Object, Object>> eIter = entrySet().iterator();
        while (eIter.hasNext()) {
            Map.Entry<Object, Object> entry = eIter.next();
            buf.append(entry.getKey())
               .append(": ")
               .append(entry.getValue())
               .append("\n");
        }
        return buf.toString();
    }

}
