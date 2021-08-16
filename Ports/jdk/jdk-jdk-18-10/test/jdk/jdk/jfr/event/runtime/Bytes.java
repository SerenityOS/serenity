/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr.event.runtime;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper class for working with class files and byte arrays
 */
public final class Bytes {
    public final static byte[] WORLD = Bytes.asBytes("world");
    public final static byte[] EARTH = Bytes.asBytes("earth");

    public static byte[] asBytes(String string) {
        byte[] result = new byte[string.length()];
        for (int i = 0; i < string.length(); i++) {
            result[i] = (byte)string.charAt(i);
        }
        return result;
    }

    public static byte[] classBytes(ClassLoader classLoader, String className) throws IOException {
        String classFileName = className.replace(".", "/") + ".class";
        try (InputStream is = classLoader.getResourceAsStream(classFileName)) {
            if (is == null) {
                throw new IOException("Could not find class file " + classFileName);
            }
            return is.readAllBytes();
        }
    }

    public static byte[] classBytes(Class<?> clazz) throws IOException {
        return classBytes(clazz.getClassLoader(), clazz.getName());
    }

    public static byte[] replaceAll(byte[] input, byte[] target, byte[] replacement) {
        List<Byte> result = new ArrayList<>();
        for (int i = 0; i < input.length; i++) {
            if (hasTarget(input, i, target)) {
                for (int j = 0; j < replacement.length; j++) {
                    result.add(replacement[j]);
                }
                i += target.length - 1;
            } else {
                result.add(input[i]);
            }
        }
        byte[] resultArray = new byte[result.size()];
        for (int i = 0; i < resultArray.length; i++) {
            resultArray[i] = result.get(i);
        }
        return resultArray;
    }

    private static boolean hasTarget(byte[] input, int start, byte[] target) {
        for (int i = 0; i < target.length; i++) {
            if (start + i == input.length) {
                return false;
            }
            if (input[start + i] != target[i]) {
                return false;
            }
        }
        return true;
    }
}
