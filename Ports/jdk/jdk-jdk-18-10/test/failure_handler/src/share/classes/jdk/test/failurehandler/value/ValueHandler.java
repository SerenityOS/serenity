/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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

package jdk.test.failurehandler.value;

import jdk.test.failurehandler.Utils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Objects;
import java.util.Properties;

public final class ValueHandler {
    public static <T> void apply(T object, Properties properties,
                                 String prefix) throws InvalidValueException {
        Objects.requireNonNull(object, "object cannot be null");
        Objects.requireNonNull(properties, "properties cannot be null");
        Class<?> aClass = object.getClass();
        while (aClass != null) {
            for (Field field : aClass.getDeclaredFields()) {
                Value p = field.getAnnotation(Value.class);
                if (p != null) {
                    applyToField(p, object, field, properties, prefix);
                } else {
                    SubValues sub
                            = field.getAnnotation(SubValues.class);
                    if (sub != null) {
                        getAccess(field);
                        try {
                            apply(field.get(object), properties,
                                    Utils.prependPrefix(prefix, sub.prefix()));
                        } catch (IllegalAccessException e) {
                            throw new InvalidValueException(String.format(
                                    "can't apply sub properties to %s.",
                                    field.getName()));
                        }
                    }
                }
            }
            aClass = aClass.getSuperclass();
        }
    }

    private static void applyToField(Value property, Object object,
                Field field, Properties properties, String prefix)
            throws InvalidValueException {
        getAccess(field);
        if (Modifier.isFinal(field.getModifiers())) {
            throw new InvalidValueException(
                    String.format("field '%s' is final", field));
        }
        String name = Utils.prependPrefix(prefix, property.name());
        String value = getProperty(properties, prefix, property.name());
        if (value == null) {
            DefaultValue defaultValue
                    = field.getAnnotation(DefaultValue.class);
            value = defaultValue == null ? null : defaultValue.value();
        }
        if (value == null) {
            throw new InvalidValueException(String.format(
                    "can't set '%s', because properties don't have '%s'.",
                    field.getName(), name));
        }
        String delimiter = getProperty(properties,
                Utils.prependPrefix(prefix, property.name()), "delimiter");
        delimiter = delimiter == null ? " " : delimiter;
        Class<? extends ValueParser> parserClass = property.parser();
        try {
            field.set(object, parserClass.getDeclaredConstructor().newInstance().parse(
                    field.getType(), value, delimiter));
        } catch (ReflectiveOperationException | IllegalArgumentException e) {
            throw new InvalidValueException(
                    String.format("can't set field '%s' : %s",
                            field.getName(), e.getMessage()), e);
        }
    }

    private static String getProperty(Properties properties,
                                      String prefix, String name) {
        if (prefix == null || prefix.isEmpty()) {
            return properties.getProperty(name);
        }
        int index = prefix.length();
        do {
            String value = properties.getProperty(
                    Utils.prependPrefix(prefix.substring(0, index), name));
            if (value != null) {
                return value;
            }
            index = prefix.lastIndexOf('.', index - 1);
        } while (index > 0);
        return  properties.getProperty(name);
    }

    private static void getAccess(Field field) {
        int modifiers = field.getModifiers();
        if (!Modifier.isPublic(modifiers)) {
            field.setAccessible(true);
        }
    }

}
