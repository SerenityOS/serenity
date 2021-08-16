/*
 * Copyright 2014 Google, Inc.  All Rights Reserved.
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

/* @test
 * @bug 8062194
 * @summary Ensure Attribute iteration order is the insertion order.
 */

import java.util.Arrays;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;

public class IterationOrder {
    static void checkOrder(Attributes.Name k0, String v0,
                           Attributes.Name k1, String v1,
                           Attributes.Name k2, String v2) {
        Attributes x = new Attributes();
        x.put(k0, v0);
        x.put(k1, v1);
        x.put(k2, v2);
        Map.Entry<?,?>[] entries
            = x.entrySet().toArray(new Map.Entry<?,?>[3]);
        if (!(entries.length == 3
              && entries[0].getKey() == k0
              && entries[0].getValue() == v0
              && entries[1].getKey() == k1
              && entries[1].getValue() == v1
              && entries[2].getKey() == k2
              && entries[2].getValue() == v2)) {
            throw new AssertionError(Arrays.toString(entries));
        }

        Object[] keys = x.keySet().toArray();
        if (!(keys.length == 3
              && keys[0] == k0
              && keys[1] == k1
              && keys[2] == k2)) {
             throw new AssertionError(Arrays.toString(keys));
        }
    }

    public static void main(String[] args) throws Exception {
        Attributes.Name k0 = Name.MANIFEST_VERSION;
        Attributes.Name k1 = Name.MAIN_CLASS;
        Attributes.Name k2 = Name.SEALED;
        String v0 = "42.0";
        String v1 = "com.google.Hello";
        String v2 = "yes";
        checkOrder(k0, v0, k1, v1, k2, v2);
        checkOrder(k1, v1, k0, v0, k2, v2);
        checkOrder(k2, v2, k1, v1, k0, v0);
    }
}
