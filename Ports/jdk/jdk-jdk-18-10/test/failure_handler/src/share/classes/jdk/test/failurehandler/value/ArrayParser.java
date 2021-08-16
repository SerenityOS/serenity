/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.reflect.Array;
import java.util.Objects;

public class ArrayParser implements ValueParser {
    private final ValueParser parser;

    public ArrayParser(ValueParser parser) {
        Objects.requireNonNull(parser);
        this.parser = parser;
    }

    @Override
    public Object parse(Class<?> type, String value, String delimiter) {
        Class<?> component = type.getComponentType();
        if (component.isArray()) {
            throw new IllegalArgumentException(
                    "multidimensional array fields aren't supported");
        }
        String[] values = (value == null || value.isEmpty())
                          ? new String[]{}
                          : value.split(delimiter);
        Object result = Array.newInstance(component, values.length);
        for (int i = 0, n = values.length; i < n; ++i) {
            Array.set(result, i, parser.parse(component, values[i], delimiter));
        }
        return result;
    }
}
