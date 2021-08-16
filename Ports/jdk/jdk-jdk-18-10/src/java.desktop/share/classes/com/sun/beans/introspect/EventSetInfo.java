/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.EventListener;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TooManyListenersException;
import java.util.TreeMap;

public final class EventSetInfo {
    private MethodInfo add;
    private MethodInfo remove;
    private MethodInfo get;

    private EventSetInfo() {
    }

    private boolean initialize() {
        if ((this.add == null) || (this.remove == null) || (this.remove.type != this.add.type)) {
            return false;
        }
        if ((this.get != null) && (this.get.type != this.add.type)) {
            this.get = null;
        }
        return true;
    }

    public Class<?> getListenerType() {
        return this.add.type;
    }

    public Method getAddMethod() {
        return this.add.method;
    }

    public Method getRemoveMethod() {
        return this.remove.method;
    }

    public Method getGetMethod() {
        return (this.get == null) ? null : this.get.method;
    }

    public boolean isUnicast() {
        // if the adder method throws the TooManyListenersException
        // then it is an Unicast event source
        return this.add.isThrow(TooManyListenersException.class);
    }

    private static MethodInfo getInfo(MethodInfo info, Method method, int prefix, int postfix) {
        Class<?> type = (postfix > 0)
                ? MethodInfo.resolve(method, method.getGenericReturnType()).getComponentType()
                : MethodInfo.resolve(method, method.getGenericParameterTypes()[0]);

        if ((type != null) && EventListener.class.isAssignableFrom(type)) {
            String name = method.getName();
            if (prefix + postfix < name.length()) {
                if (type.getName().endsWith(name.substring(prefix, name.length() - postfix))) {
                    if ((info == null) || info.type.isAssignableFrom(type)) {
                        return new MethodInfo(method, type);
                    }
                }
            }
        }
        return info;
    }

    private static EventSetInfo getInfo(Map<String,EventSetInfo> map, String key) {
        EventSetInfo info = map.get(key);
        if (info == null) {
            info = new EventSetInfo();
            map.put(key, info);
        }
        return info;
    }

    public static Map<String,EventSetInfo> get(Class<?> type) {
        List<Method> methods = ClassInfo.get(type).getMethods();
        if (methods.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String,EventSetInfo> map = new TreeMap<>();
        for (Method method : ClassInfo.get(type).getMethods()) {
            if (!Modifier.isStatic(method.getModifiers())) {
                Class<?> returnType = method.getReturnType();
                String name = method.getName();
                switch (method.getParameterCount()) {
                    case 1:
                        if ((returnType == void.class) && name.endsWith("Listener")) {
                            if (name.startsWith("add")) {
                                EventSetInfo info = getInfo(map, name.substring(3, name.length() - 8));
                                info.add = getInfo(info.add, method, 3, 0);
                            } else if (name.startsWith("remove")) {
                                EventSetInfo info = getInfo(map, name.substring(6, name.length() - 8));
                                info.remove = getInfo(info.remove, method, 6, 0);
                            }
                        }
                        break;
                    case 0:
                        if (returnType.isArray() && name.startsWith("get") && name.endsWith("Listeners")) {
                            EventSetInfo info = getInfo(map, name.substring(3, name.length() - 9));
                            info.get = getInfo(info.get, method, 3, 1);
                        }
                        break;
                }
            }
        }
        Iterator<EventSetInfo> iterator = map.values().iterator();
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
