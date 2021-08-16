/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.beans.introspect;

import java.beans.BeanProperty;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static com.sun.beans.finder.ClassFinder.findClass;

public final class PropertyInfo {

    public enum Name {
        bound, expert, hidden, preferred, required, visualUpdate, description,
        enumerationValues
    }

    private static final String VETO_EXCEPTION_NAME = "java.beans.PropertyVetoException";
    private static final Class<?> VETO_EXCEPTION;

    static {
        Class<?> type;
        try {
            type = Class.forName(VETO_EXCEPTION_NAME);
        } catch (Exception exception) {
            type = null;
        }
        VETO_EXCEPTION = type;
    }

    private Class<?> type;
    private MethodInfo read;
    private MethodInfo write;
    private PropertyInfo indexed;
    private List<MethodInfo> readList;
    private List<MethodInfo> writeList;
    private Map<Name,Object> map;

    private PropertyInfo() {
    }

    private boolean initialize() {
        boolean isInitedToIsGetter = false;
        if (this.read != null) {
            this.type = this.read.type;
            isInitedToIsGetter = isPrefix(this.read.method.getName(), "is");
        }
        if (!isInitedToIsGetter && this.readList != null) {
            for (MethodInfo info : this.readList) {
                if ((this.read == null) || this.read.type.isAssignableFrom(info.type)) {
                    this.read = info;
                    this.type = info.type;
                }
            }
            this.readList = null;
        }
        Class<?> writeType = this.type;
        if (this.writeList != null) {
            for (MethodInfo info : this.writeList) {
                if (writeType == null) {
                    this.write = info;
                    writeType = info.type;
                } else if (writeType.isAssignableFrom(info.type)) {
                    if ((this.write == null) || this.write.type.isAssignableFrom(info.type)) {
                        this.write = info;
                        writeType = info.type;
                    }
                }
            }
            this.writeList = null;
        }
        if (this.type == null) {
            this.type = writeType;
        }
        if (this.indexed != null) {
            if ((this.type != null) && !this.type.isArray()) {
                this.indexed = null; // property type is not an array
            } else if (!this.indexed.initialize()) {
                this.indexed = null; // cannot initialize indexed methods
            } else if ((this.type != null) && (this.indexed.type != this.type.getComponentType())) {
                this.indexed = null; // different property types
            } else {
                this.map = this.indexed.map;
                this.indexed.map = null;
            }
        }
        if ((this.type == null) && (this.indexed == null)) {
            return false;
        }
        boolean done = initialize(this.read);
        if (!done) {
            initialize(this.write);
        }
        return true;
    }

    private boolean initialize(MethodInfo info) {
        if (info != null) {
            BeanProperty annotation = info.method.getAnnotation(BeanProperty.class);
            if (annotation != null) {
                if (!annotation.bound()) {
                    put(Name.bound, Boolean.FALSE);
                }
                put(Name.expert, annotation.expert());
                put(Name.required, annotation.required());
                put(Name.hidden, annotation.hidden());
                put(Name.preferred, annotation.preferred());
                put(Name.visualUpdate, annotation.visualUpdate());
                put(Name.description, annotation.description());
                String[] values = annotation.enumerationValues();
                try {
                    Object[] array = new Object[3 * values.length];
                    int index = 0;
                    for (String value : values) {
                        Class<?> type = info.method.getDeclaringClass();
                        String name = value;
                        int pos = value.lastIndexOf('.');
                        if (pos > 0) {
                            name = value.substring(0, pos);
                            if (name.indexOf('.') < 0) {
                                String pkg = type.getName();
                                name = pkg.substring(0, 1 + Math.max(
                                        pkg.lastIndexOf('.'),
                                        pkg.lastIndexOf('$'))) + name;
                            }
                            type = findClass(name);
                            name = value.substring(pos + 1);
                        }
                        Field field = type.getField(name);
                        if (Modifier.isStatic(field.getModifiers()) && info.type.isAssignableFrom(field.getType())) {
                            array[index++] = name;
                            array[index++] = field.get(null);
                            array[index++] = value;
                        }
                    }
                    if (index == array.length) {
                        put(Name.enumerationValues, array);
                    }
                } catch (Exception ignored) {
                    ignored.printStackTrace();
                }
                return true;
            }
        }
        return false;
    }

    public Class<?> getPropertyType() {
        return this.type;
    }

    public Method getReadMethod() {
        return (this.read == null) ? null : this.read.method;
    }

    public Method getWriteMethod() {
        return (this.write == null) ? null : this.write.method;
    }

    public PropertyInfo getIndexed() {
        return this.indexed;
    }

    public boolean isConstrained() {
        if (this.write != null) {
            if (VETO_EXCEPTION == null) {
                for (Class<?> type : this.write.method.getExceptionTypes()) {
                    if (type.getName().equals(VETO_EXCEPTION_NAME)) {
                        return true;
                    }
                }
            } else if (this.write.isThrow(VETO_EXCEPTION)) {
                return true;
            }
        }
        return (this.indexed != null) && this.indexed.isConstrained();
    }

    public boolean is(Name name) {
        Object value = get(name);
        return (value instanceof Boolean)
                ? (Boolean) value
                : Name.bound.equals(name);
    }

    public Object get(Name name) {
        return this.map == null ? null : this.map.get(name);
    }

    private void put(Name name, boolean value) {
        if (value) {
            put(name, Boolean.TRUE);
        }
    }

    private void put(Name name, String value) {
        if (0 < value.length()) {
            put(name, (Object) value);
        }
    }

    private void put(Name name, Object value) {
        if (this.map == null) {
            this.map = new EnumMap<>(Name.class);
        }
        this.map.put(name, value);
    }

    private static List<MethodInfo> add(List<MethodInfo> list, Method method, Type type) {
        if (list == null) {
            list = new ArrayList<>();
        }
        list.add(new MethodInfo(method, type));
        return list;
    }

    private static boolean isPrefix(String name, String prefix) {
        return name.length() > prefix.length() && name.startsWith(prefix);
    }

    private static PropertyInfo getInfo(Map<String,PropertyInfo> map, String key, boolean indexed) {
        PropertyInfo info = map.get(key);
        if (info == null) {
            info = new PropertyInfo();
            map.put(key, info);
        }
        if (!indexed) {
            return info;
        }
        if (info.indexed == null) {
            info.indexed = new PropertyInfo();
        }
        return info.indexed;
    }

    public static Map<String,PropertyInfo> get(Class<?> type) {
        List<Method> methods = ClassInfo.get(type).getMethods();
        if (methods.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String,PropertyInfo> map = new TreeMap<>();
        for (Method method : methods) {
            if (!Modifier.isStatic(method.getModifiers())) {
                Class<?> returnType = method.getReturnType();
                String name = method.getName();
                switch (method.getParameterCount()) {
                    case 0:
                        if (returnType.equals(boolean.class) && isPrefix(name, "is")) {
                            PropertyInfo info = getInfo(map, name.substring(2), false);
                            info.read = new MethodInfo(method, boolean.class);
                        } else if (!returnType.equals(void.class) && isPrefix(name, "get")) {
                            PropertyInfo info = getInfo(map, name.substring(3), false);
                            info.readList = add(info.readList, method, method.getGenericReturnType());
                        }
                        break;
                    case 1:
                        if (returnType.equals(void.class) && isPrefix(name, "set")) {
                            PropertyInfo info = getInfo(map, name.substring(3), false);
                            info.writeList = add(info.writeList, method, method.getGenericParameterTypes()[0]);
                        } else if (!returnType.equals(void.class) && method.getParameterTypes()[0].equals(int.class) && isPrefix(name, "get")) {
                            PropertyInfo info = getInfo(map, name.substring(3), true);
                            info.readList = add(info.readList, method, method.getGenericReturnType());
                        }
                        break;
                    case 2:
                        if (returnType.equals(void.class) && method.getParameterTypes()[0].equals(int.class) && isPrefix(name, "set")) {
                            PropertyInfo info = getInfo(map, name.substring(3), true);
                            info.writeList = add(info.writeList, method, method.getGenericParameterTypes()[1]);
                        }
                        break;
                }
            }
        }
        Iterator<PropertyInfo> iterator = map.values().iterator();
        while (iterator.hasNext()) {
            if (!iterator.next().initialize()) {
                iterator.remove();
            }
        }
        return !map.isEmpty()
                ? Collections.unmodifiableMap(map)
                : Collections.emptyMap();
    }
}
