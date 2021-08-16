/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyright (c) 2011 IBM Corporation
 */

/*
 * @test
 * @bug 6312706
 * @summary Sets from Map.entrySet() return distinct objects for each Entry
 * @author Neil Richards <neil.richards@ngmr.net>, <neil_richards@uk.ibm.com>
 */

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DistinctEntrySetElements {
    static enum TestEnum { e00, e01, e02 }

    public static void main(String[] args) throws Exception {
        final EnumMap<TestEnum, String> enumMap = new EnumMap<>(TestEnum.class);

        for (TestEnum e : TestEnum.values()) {
            enumMap.put(e, e.name());
        }

        Set<Map.Entry<TestEnum, String>> entrySet = enumMap.entrySet();
        HashSet<Map.Entry<TestEnum, String>> hashSet = new HashSet<>(entrySet);

        if (false == hashSet.equals(entrySet)) {
            throw new RuntimeException("Test FAILED: Sets are not equal.");
        }
        if (hashSet.hashCode() != entrySet.hashCode()) {
            throw new RuntimeException("Test FAILED: Set's hashcodes are not equal.");
        }
    }
}
