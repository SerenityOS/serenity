/*
 * Copyright (c) 2000, 2011, Oracle and/or its affiliates. All rights reserved.
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
package java.beans;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

import static java.util.Locale.ENGLISH;

/**
 * A utility class which generates unique names for object instances.
 * The name will be a concatenation of the unqualified class name
 * and an instance number.
 * <p>
 * For example, if the first object instance javax.swing.JButton
 * is passed into {@code instanceName} then the returned
 * string identifier will be &quot;JButton0&quot;.
 *
 * @author Philip Milne
 */
class NameGenerator {

    private Map<Object, String> valueToName;
    private Map<String, Integer> nameToCount;

    public NameGenerator() {
        valueToName = new IdentityHashMap<>();
        nameToCount = new HashMap<>();
    }

    /**
     * Clears the name cache. Should be called to near the end of
     * the encoding cycle.
     */
    public void clear() {
        valueToName.clear();
        nameToCount.clear();
    }

    /**
     * Returns the root name of the class.
     */
    @SuppressWarnings("rawtypes")
    public static String unqualifiedClassName(Class type) {
        if (type.isArray()) {
            return unqualifiedClassName(type.getComponentType())+"Array";
        }
        String name = type.getName();
        return name.substring(name.lastIndexOf('.')+1);
    }

    /**
     * Returns a String which capitalizes the first letter of the string.
     */
    public static String capitalize(String name) {
        if (name == null || name.length() == 0) {
            return name;
        }
        return name.substring(0, 1).toUpperCase(ENGLISH) + name.substring(1);
    }

    /**
     * Returns a unique string which identifies the object instance.
     * Invocations are cached so that if an object has been previously
     * passed into this method then the same identifier is returned.
     *
     * @param instance object used to generate string
     * @return a unique string representing the object
     */
    public String instanceName(Object instance) {
        if (instance == null) {
            return "null";
        }
        if (instance instanceof Class) {
            return unqualifiedClassName((Class)instance);
        }
        else {
            String result = valueToName.get(instance);
            if (result != null) {
                return result;
            }
            Class<?> type = instance.getClass();
            String className = unqualifiedClassName(type);

            Integer size = nameToCount.get(className);
            int instanceNumber = (size == null) ? 0 : (size).intValue() + 1;
            nameToCount.put(className, instanceNumber);

            result = className + instanceNumber;
            valueToName.put(instance, result);
            return result;
        }
    }
}
