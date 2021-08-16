/*
 * Copyright (c) 2008, 2021, Oracle and/or its affiliates. All rights reserved.
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
 *
 */
package com.sun.hotspot.igv.data;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 *
 * @author Thomas Wuerthinger
 */
public class Properties implements Serializable, Iterable<Property> {

    public static final long serialVersionUID = 1L;
    protected String[] map = new String[4];

    public Properties() {
    }

    @Override
    public boolean equals(java.lang.Object o) {
        if (!(o instanceof Properties)) {
            return false;
        }

        Properties p = (Properties) o;

        for (Property prop : this) {
            String value = p.get(prop.getName());
            if (value == null || !value.equals(prop.getValue())) {
                return false;
            }
        }

        for (Property prop : p) {
            String value = this.get(prop.getName());
            if (value == null || !value.equals(prop.getValue())) {
                return false;
            }
        }

        return true;
    }

    @Override
    public int hashCode() {
        int hash = 5;

        if (map != null) {
            for (int i = 0; i < this.map.length; i++) {
                if (map[i] == null) {
                    i++;
                } else {
                    hash = hash * 83 + map[i].hashCode();
                }
            }
        }
        return hash;
    }

    public Properties(String name, String value) {
        this();
        this.setProperty(name, value);
    }

    public Properties(String name, String value, String name1, String value1) {
        this(name, value);
        this.setProperty(name1, value1);
    }

    public Properties(String name, String value, String name1, String value1, String name2, String value2) {
        this(name, value, name1, value1);
        this.setProperty(name2, value2);
    }

    public Properties(Properties p) {
        map = new String[p.map.length];
        System.arraycopy(p.map, 0, map, 0, p.map.length);
    }

    protected Properties(String[] map) {
        this.map = map;
    }

    static class SharedProperties extends Properties {
        int hashCode;

        SharedProperties(String[] map) {
            super(map);
            this.hashCode = Arrays.hashCode(map);
        }

        @Override
        protected void setPropertyInternal(String name, String value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof SharedProperties)) {
                return super.equals(other);
            }
            SharedProperties props2 = (SharedProperties) other;
            return Arrays.equals(map, props2.map);
        }

        @Override
        public int hashCode() {
            return hashCode;
        }
    }

    private static class PropertyCache {
        static WeakHashMap<SharedProperties, WeakReference<SharedProperties>> immutableCache = new WeakHashMap<>();

        static synchronized SharedProperties intern(Properties properties) {
            String[] map = properties.map;
            SharedProperties key = new SharedProperties(map);
            WeakReference<SharedProperties> entry = immutableCache.get(key);
            if (entry != null) {
                SharedProperties props = entry.get();
                if (props != null) {
                    return props;
                }
            }
            immutableCache.put(key, new WeakReference<>(key));
            return key;
        }
    }

    public static class Entity implements Provider {

        private Properties properties;

        public Entity() {
            properties = new Properties();
        }

        public Entity(Properties.Entity object) {
            properties = new Properties(object.getProperties());
        }

        @Override
        public Properties getProperties() {
            return properties;
        }

        public void internProperties() {
            properties = PropertyCache.intern(properties);
        }
    }

    public interface PropertyMatcher {

        String getName();

        boolean match(String value);
    }

    public static class InvertPropertyMatcher implements PropertyMatcher {

        private PropertyMatcher matcher;

        public InvertPropertyMatcher(PropertyMatcher matcher) {
            this.matcher = matcher;
        }

        @Override
        public String getName() {
            return matcher.getName();
        }

        @Override
        public boolean match(String p) {
            if (p == null) {
                return false;
            }
            return !matcher.match(p);
        }
    }

    public static class StringPropertyMatcher implements PropertyMatcher {

        private String name;
        private String value;

        public StringPropertyMatcher(String name, String value) {
            if (name == null) {
                throw new IllegalArgumentException("Property name must not be null!");
            }
            if (value == null) {
                throw new IllegalArgumentException("Property value must not be null!");
            }
            this.name = name;
            this.value = value;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public boolean match(String p) {
            if (p == null) {
                throw new IllegalArgumentException("Property value must not be null!");
            }
            return p.equals(value);
        }
    }

    public static class RegexpPropertyMatcher implements PropertyMatcher {

        private String name;
        private Pattern valuePattern;

        public RegexpPropertyMatcher(String name, String value) {
            this(name, value, 0);
        }

        public RegexpPropertyMatcher(String name, String value, int flags) {

            if (name == null) {
                throw new IllegalArgumentException("Property name must not be null!");
            }

            if (value == null) {
                throw new IllegalArgumentException("Property value pattern must not be null!");
            }

            this.name = name;

            try {
                valuePattern = Pattern.compile(value, flags);
            } catch (PatternSyntaxException e) {
                throw new IllegalArgumentException("Bad pattern: " + value);
            }
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public boolean match(String p) {
            if (p == null) {
                throw new IllegalArgumentException("Property value must not be null!");
            }
            Matcher m = valuePattern.matcher(p);
            return m.matches();
        }
    }

    public Property selectSingle(PropertyMatcher matcher) {

        final String name = matcher.getName();
        String value = null;
        for (int i = 0; i < map.length; i += 2) {
            if (map[i] != null && name.equals(map[i])) {
                value = map[i + 1];
                break;
            }
        }
        if (value != null && matcher.match(value)) {
            return new Property(name, value);
        } else {
            return null;
        }
    }

    public interface Provider {

        public Properties getProperties();
    }

    @Override
    public String toString() {
        List<String[]> pairs = new ArrayList<>();
        for (int i = 0; i < map.length; i += 2) {
            if (map[i + 1] != null) {
                pairs.add(new String[]{map[i], map[i + 1]});
            }
        }

        Collections.sort(pairs, new Comparator<String[]>() {
            @Override
            public int compare(String[] o1, String[] o2) {
                assert o1.length == 2;
                assert o2.length == 2;
                return o1[0].compareTo(o2[0]);
            }
        });

        StringBuilder sb = new StringBuilder();
        sb.append("[");
        boolean first = true;
        for (String[] p : pairs) {
            if (first) {
                first = false;
            } else {
                sb.append(", ");
            }
            sb.append(p[0]).append("=").append(p[1]);
        }
        return sb.append("]").toString();
    }

    public static class PropertySelector<T extends Properties.Provider> {

        private Collection<T> objects;

        public PropertySelector(Collection<T> objects) {
            this.objects = objects;
        }

        public T selectSingle(PropertyMatcher matcher) {

            for (T t : objects) {
                Property p = t.getProperties().selectSingle(matcher);
                if (p != null) {
                    return t;
                }
            }

            return null;
        }

        public List<T> selectMultiple(PropertyMatcher matcher) {
            List<T> result = new ArrayList<>();

            for (T t : objects) {
                Property p = t.getProperties().selectSingle(matcher);
                if (p != null) {
                    result.add(t);
                }
            }

            return result;
        }
    }

    public String get(String key) {
        for (int i = 0; i < map.length; i += 2) {
            if (map[i] != null && map[i].equals(key)) {
                return map[i + 1];
            }
        }
        return null;
    }

    public void setProperty(String name, String value) {
        setPropertyInternal(name.intern(), value != null ? value.intern() : null);
    }

    protected void setPropertyInternal(String name, String value) {
        for (int i = 0; i < map.length; i += 2) {
            if (map[i] != null && map[i].equals(name)) {
                String p = map[i + 1];
                if (value == null) {
                    // remove this property
                    map[i] = null;
                    map[i + 1] = null;
                } else {
                    map[i + 1] = value;
                }
                return;
            }
        }
        if (value == null) {
            return;
        }
        for (int i = 0; i < map.length; i += 2) {
            if (map[i] == null) {
                map[i] = name;
                map[i + 1] = value;
                return;
            }
        }
        String[] newMap = new String[map.length + 4];
        System.arraycopy(map, 0, newMap, 0, map.length);
        newMap[map.length] = name;
        newMap[map.length + 1] = value;
        map = newMap;
    }

    public void add(Properties properties) {
        for (Property p : properties) {
            // Already interned
            setPropertyInternal(p.getName(), p.getValue());
        }
    }

    private class PropertiesIterator implements Iterator<Property> {

        int index;

        @Override
        public boolean hasNext() {
            while (index < map.length && map[index + 1] == null) {
                index += 2;
            }
            return index < map.length;
        }

        @Override
        public Property next() {
            if (index < map.length) {
                index += 2;
                return new Property(map[index - 2], map[index - 1]);
            }
            return null;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }

    @Override
    public Iterator<Property> iterator() {
        return new PropertiesIterator();
    }

    public final String resolveString(String string) {

        StringBuilder sb = new StringBuilder();
        boolean inBrackets = false;
        StringBuilder curIdent = new StringBuilder();

        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);
            if (inBrackets) {
                if (c == ']') {
                    String value = get(curIdent.toString());
                    if (value == null) {
                        value = "";
                    }
                    sb.append(value);
                    inBrackets = false;
                } else {
                    curIdent.append(c);
                }
            } else {
                if (c == '[') {
                    inBrackets = true;
                    curIdent = new StringBuilder();
                } else {
                    sb.append(c);
                }
            }
        }

        return sb.toString();
    }
}
