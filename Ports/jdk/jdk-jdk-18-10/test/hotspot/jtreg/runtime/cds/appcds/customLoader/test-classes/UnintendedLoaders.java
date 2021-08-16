/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

import sun.hotspot.WhiteBox;

public class UnintendedLoaders {
    public static void main(String[] args) throws Exception {
        ClassLoader loaders[] = new ClassLoader[2];
        loaders[0] = UnintendedLoaders.class.getClassLoader(); // app loader
        loaders[1] = loaders[0].getParent();                   // platform loader

        String[] names = {
            "CustomLoadee",
        };


        for (int i=0; i<3; i++) {
            for (String s : names) {
                try {
                    if (i <= 1) {
                        System.out.println(loaders[i].loadClass(s));
                    } else {
                        System.out.println(Class.forName(s));
                    }
                } catch (ClassNotFoundException e) {
                    System.out.println("Expected exception:" + e);
                    continue;
                }
                throw new RuntimeException("The class \"" + s + "\" should not be resolved by the application or platform class loader");
            }
        }
    }
}
