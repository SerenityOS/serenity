/*
 * Copyright (c) 1997, 2014, Oracle and/or its affiliates. All rights reserved.
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

package javax.swing;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Set;



/**
 *
 * @author Hans Muller
 */
@SuppressWarnings("serial") // Superclass is not serializable across versions
class MultiUIDefaults extends UIDefaults
{
    private UIDefaults[] tables;

    public MultiUIDefaults(UIDefaults[] defaults) {
        super();
        tables = defaults;
    }

    public MultiUIDefaults() {
        super();
        tables = new UIDefaults[0];
    }

    @Override
    public Object get(Object key)
    {
        Object value = super.get(key);
        if (value != null) {
            return value;
        }

        for (UIDefaults table : tables) {
            value = (table != null) ? table.get(key) : null;
            if (value != null) {
                return value;
            }
        }

        return null;
    }

    @Override
    public Object get(Object key, Locale l)
    {
        Object value = super.get(key,l);
        if (value != null) {
            return value;
        }

        for (UIDefaults table : tables) {
            value = (table != null) ? table.get(key,l) : null;
            if (value != null) {
                return value;
            }
        }

        return null;
    }

    @Override
    public int size() {
        return entrySet().size();
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public Enumeration<Object> keys()
    {
        return new MultiUIDefaultsEnumerator(
                MultiUIDefaultsEnumerator.Type.KEYS, entrySet());
    }

    @Override
    public Set<Object> keySet()
    {
        Set<Object> set = new HashSet<Object>();
        for (int i = tables.length - 1; i >= 0; i--) {
            if (tables[i] != null) {
                set.addAll(tables[i].keySet());
            }
        }
        set.addAll(super.keySet());
        return set;
    }

    @Override
    public Enumeration<Object> elements()
    {
        return new MultiUIDefaultsEnumerator(
                MultiUIDefaultsEnumerator.Type.ELEMENTS, entrySet());
    }

    @Override
    public Set<Entry<Object, Object>> entrySet() {
        Set<Entry<Object, Object>> set = new HashSet<Entry<Object, Object>>();
        for (int i = tables.length - 1; i >= 0; i--) {
            if (tables[i] != null) {
                set.addAll(tables[i].entrySet());
            }
        }
        set.addAll(super.entrySet());
        return set;
    }

    @Override
    protected void getUIError(String msg) {
        if (tables != null && tables.length > 0 && tables[0] != null) {
            tables[0].getUIError(msg);
        } else {
            super.getUIError(msg);
        }
    }

    private static class MultiUIDefaultsEnumerator implements Enumeration<Object>
    {
        public static enum Type { KEYS, ELEMENTS };
        private Iterator<Entry<Object, Object>> iterator;
        private Type type;

        MultiUIDefaultsEnumerator(Type type, Set<Entry<Object, Object>> entries) {
            this.type = type;
            this.iterator = entries.iterator();
        }

        public boolean hasMoreElements() {
            return iterator.hasNext();
        }

        public Object nextElement() {
            switch (type) {
                case KEYS: return iterator.next().getKey();
                case ELEMENTS: return iterator.next().getValue();
                default: return null;
            }
        }
    }

    @Override
    public Object remove(Object key)
    {
        Object value = null;
        for (int i = tables.length - 1; i >= 0; i--) {
            if (tables[i] != null) {
                Object v = tables[i].remove(key);
                if (v != null) {
                    value = v;
                }
            }
        }
        Object v = super.remove(key);
        if (v != null) {
            value = v;
        }

        return value;
    }

    @Override
    public void clear() {
        super.clear();
        for (UIDefaults table : tables) {
            if (table != null) {
                table.clear();
            }
        }
    }

    @Override
    public synchronized String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        Enumeration<?> keys = keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            sb.append(key + "=" + get(key) + ", ");
        }
        int length = sb.length();
        if (length > 1) {
            sb.delete(length-2, length);
        }
        sb.append("}");
        return sb.toString();
    }
}
