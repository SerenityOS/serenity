/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
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
package gc.g1.unloading.bytecode;

import java.util.*;


/**
 * This utility class replaces peaces of bytes in bytecode by another peaces according to dictionary. This is useful for class redefenition.
 */
public class BytecodePatcher {

    static private Map<byte[], byte[]> dictionary = new HashMap<>();

    static {
        dictionary.put("bytesToReplace0".getBytes(), "bytesToReplace1".getBytes());
        dictionary.put("bytesToReplace2".getBytes(), "bytesToReplace3".getBytes());
    }

    public static void patch(byte[] bytecode) {
        for (Map.Entry<byte[], byte[]> entry : dictionary.entrySet()) {
            for (int i = 0; i + entry.getKey().length < bytecode.length; i++) {
                boolean match = true;
                for (int j = 0; j < entry.getKey().length; j++) {
                    if (bytecode[i + j] != entry.getKey()[j]) {
                        match = false;
                        break;
                    }
                }
                if (match) {
                    for (int j = 0; j < entry.getKey().length; j++)
                        bytecode[i + j] = entry.getValue()[j];
                }
            }
        }
    }

}
