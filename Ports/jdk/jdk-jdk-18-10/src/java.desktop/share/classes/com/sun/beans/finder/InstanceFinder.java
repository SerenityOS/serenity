/*
 * Copyright (c) 2009, 2014, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.beans.finder;

/**
 * This is utility class that provides basic functionality
 * to find an auxiliary class for a JavaBean specified by its type.
 *
 * @since 1.7
 *
 * @author Sergey A. Malenkov
 */
class InstanceFinder<T> {

    private static final String[] EMPTY = { };

    private final Class<? extends T> type;
    private final boolean allow;
    private final String suffix;
    private volatile String[] packages;

    InstanceFinder(Class<? extends T> type, boolean allow, String suffix, String... packages) {
        this.type = type;
        this.allow = allow;
        this.suffix = suffix;
        this.packages = packages.clone();
    }

    public String[] getPackages() {
        return this.packages.clone();
    }

    public void setPackages(String... packages) {
        this.packages = (packages != null) && (packages.length > 0)
                ? packages.clone()
                : EMPTY;
    }

    public T find(Class<?> type) {
        if (type == null) {
            return null;
        }
        String name = type.getName() + this.suffix;
        T object = instantiate(type, name);
        if (object != null) {
            return object;
        }
        if (this.allow) {
            object = instantiate(type, null);
            if (object != null) {
                return object;
            }
        }
        int index = name.lastIndexOf('.') + 1;
        if (index > 0) {
            name = name.substring(index);
        }
        for (String prefix : this.packages) {
            object = instantiate(type, prefix, name);
            if (object != null) {
                return object;
            }
        }
        return null;
    }

    @SuppressWarnings("deprecation")
    protected T instantiate(Class<?> type, String name) {
        if (type != null) {
            try {
                if (name != null) {
                    type = ClassFinder.findClass(name, type.getClassLoader());
                }
                if (this.type.isAssignableFrom(type)) {
                    @SuppressWarnings("unchecked")
                    T tmp = (T) type.newInstance();
                    return tmp;
                }
            }
            catch (Exception exception) {
                // ignore any exceptions
            }
        }
        return null;
    }

    protected T instantiate(Class<?> type, String prefix, String name) {
        return instantiate(type, prefix + '.' + name);
    }
}
