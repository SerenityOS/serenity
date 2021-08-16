/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.SystemFlavorMap;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/* @test
 * @bug 8028230
 * @summary Checks that SystemFlavorMap.getNativesForFlavor returns a list without duplicates
 * @author Petr Pchelko
 * @modules java.datatransfer
 * @run main DuplicatedNativesTest
 */
public class DuplicatedNativesTest {

    public static void main(String[] args) throws Exception {

        // 1. Check that returned natives do not contain duplicates.
        SystemFlavorMap flavorMap = (SystemFlavorMap)SystemFlavorMap.getDefaultFlavorMap();
        for (Map.Entry<DataFlavor, String> entry : flavorMap.getNativesForFlavors(null).entrySet()) {
            List<String> natives = flavorMap.getNativesForFlavor(entry.getKey());
            if (new HashSet<>(natives).size() != natives.size()) {
                throw new RuntimeException("FAILED: returned natives contain duplicates: " + Arrays.toString(natives.toArray()));
            }
        }

        // 2. Check that even if we set a duplicate it would be ignored.
        flavorMap.setNativesForFlavor(DataFlavor.stringFlavor, new String[] {"test", "test", "test"});
        List<String> natives = flavorMap.getNativesForFlavor(DataFlavor.stringFlavor);
        if (new HashSet<>(natives).size() != natives.size()) {
            throw new RuntimeException("FAILED: duplicates were not ignored: " + Arrays.toString(natives.toArray()));
        }
    }
}
