/*
 * Copyright (c) 2006, 2008, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 4631471 4921212 4994637
 * @summary Tests HashMap encoding
 * @run main/othervm -Djava.security.manager=allow java_util_HashMap
 * @author Sergey Malenkov
 */

import java.util.HashMap;
import java.util.Map;

public final class java_util_HashMap extends AbstractTest<Map<String, String>> {
    public static void main(String[] args) {
        new java_util_HashMap().test(true);
    }

    @Override
    protected Map<String, String> getObject() {
        Map<String, String> map = new HashMap<String, String>();
        map.put(null, null);
        map.put("key", "value");
        map.put("key-null", "null-value");
        map.put("way", "remove");
        return map;
    }

    @Override
    protected Map<String, String> getAnotherObject() {
        Map<String, String> map = new HashMap<String, String>();
        map.put(null, "null-value");
        map.put("key", "value");
        map.put("key-null", null);
        return map;
    }

    @Override
    protected void validate(Map<String, String> before, Map<String, String> after) {
        super.validate(before, after);
        validate(before);
        validate(after);
    }

    private static void validate(Map<String, String> map) {
        switch (map.size()) {
        case 3:
            validate(map, null, "null-value");
            validate(map, "key", "value");
            validate(map, "key-null", null);
            break;
        case 4:
            validate(map, null, null);
            validate(map, "key", "value");
            validate(map, "key-null", "null-value");
            validate(map, "way", "remove");
            break;
        }
    }

    private static void validate(Map<String, String> map, String key, String value) {
        if (!map.containsKey(key))
            throw new Error("There are no key: " + key);

        if (!map.containsValue(value))
            throw new Error("There are no value: " + value);

        if (!isEqual(value, map.get(key)))
            throw new Error("There are no entry: " + key + ", " + value);
    }

    private static boolean isEqual(String str1, String str2) {
        return (str1 == null)
                ? str2 == null
                : str1.equals(str2);
    }
}
