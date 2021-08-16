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

import java.util.HashMap;
import java.util.Map;

public class DefaultParser implements ValueParser {
    private static final Map<Class<?>, BasicParser> PARSERS = new HashMap<>();

    static {
        BasicParser.init();
    }

    @Override
    public Object parse(Class<?> type, String value, String s) {
        if (type.isArray()) {
            return new ArrayParser(this).parse(type, value, s);
        }
        ValueParser parser = PARSERS.get(type);
        if (parser == null) {
            throw new IllegalArgumentException("can't find parser for "
                    + type.getName());
        }

        return parser.parse(type, value, s);
    }

    private static enum BasicParser implements ValueParser {
        BOOL(boolean.class, Boolean.class) {
            @Override
            public Object parse(Class<?> type, String value, String s) {
                return Boolean.valueOf(value);
            }
        },
        BYTE(byte.class, Byte.class) {
            @Override
            public Object parse(Class<?> type, String value, String s) {
                return Byte.decode(value);
            }
        },
        CHAR(char.class, Character.class) {
            @Override
            public Object parse(Class<?> type, String value, String s) {
                if (value.length() != 1) {
                    throw new IllegalArgumentException(
                            String.format("can't cast %s to char", value));
                }
                return value.charAt(0);
            }
        },
        SHORT(short.class, Short.class) {
            @Override
            public Object parse(Class<?> type, String value, String s) {
                return Short.decode(value);
            }
        },
        INT(int.class, Integer.class) {
            @Override
            public Object parse(Class<?> type, String value, String s) {
                return Integer.decode(value);
            }
        },
        LONG(long.class, Long.class) {
            @Override
            public Object parse(Class<?> type, String value, String s) {
                return Long.decode(value);
            }
        },
        FLOAT(float.class, Float.class) {
            @Override
            public Object parse(Class<?> type, String value, String s) {
                return Float.parseFloat(value);
            }
        },
        DOUBLE(double.class, Double.class) {
            @Override
            public Object parse(Class<?> type, String value, String s) {
                return Double.parseDouble(value);
            }
        },
        STRING(String.class, Object.class) {
            @Override
            public Object parse(Class<?> type, String value, String s) {
                return value;
            }
        };

        private BasicParser(Class<?>... classes) {
            for (Class<?> aClass : classes) {
                DefaultParser.PARSERS.put(aClass, this);
            }
        }

        private static void init() {
            // no-op used to provoke <cinit>
        }
    }
}
