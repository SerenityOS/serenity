/*
 * Copyright (c) 2005, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.color.ColorSpace;
import java.util.HashMap;
import java.util.Map;

/**
 * @test
 * @bug 4967082
 * @summary ColorSpace.getName(int) should return significant values for some CS
 */
public final class GetNameTest {

    private static final Map<Integer, String[]> colorSpaces = new HashMap<>(5);

    static {
        colorSpaces.put(ColorSpace.CS_CIEXYZ, new String[] {"X", "Y", "Z"});
        colorSpaces.put(ColorSpace.CS_sRGB,
                        new String[] {"Red", "Green", "Blue"});
        colorSpaces.put(ColorSpace.CS_LINEAR_RGB,
                        new String[] {"Red", "Green", "Blue"});
        colorSpaces.put(ColorSpace.CS_GRAY, new String[] {"Gray"});
        colorSpaces.put(ColorSpace.CS_PYCC,
                        new String[] {"Unnamed color component(0)",
                                      "Unnamed color component(1)",
                                      "Unnamed color component(2)"});
    };

    public static void main(String[] args) {
        for (int csType : colorSpaces.keySet()) {
            ColorSpace cs = ColorSpace.getInstance(csType);
            String[] names = colorSpaces.get(csType);
            for (int i = 0; i < cs.getNumComponents(); i++) {
                String name = cs.getName(i);
                if (!name.equals(names[i])) {
                    System.err.println("ColorSpace with type=" + cs.getType() +
                                       " has wrong name of " + i +
                                       " component");
                    throw new RuntimeException("Wrong name of the component");
                }
            }
        }
    }
}
