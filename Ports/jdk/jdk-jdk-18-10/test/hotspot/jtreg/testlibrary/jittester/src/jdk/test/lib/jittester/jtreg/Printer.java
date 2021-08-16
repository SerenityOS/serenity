/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

package jdk.test.lib.jittester.jtreg;

import java.util.Stack;

public class Printer {

    public static String print(boolean arg) {
        return String.valueOf(arg);
    }

    public static String print(byte arg) {
        return String.valueOf(arg);
    }

    public static String print(short arg) {
        return String.valueOf(arg);
    }

    public static String print(char arg) {
        return String.valueOf((int) arg);
    }

    public static String print(int arg) {
        return String.valueOf(arg);
    }

    public static String print(long arg) {
        return String.valueOf(arg);
    }

    public static String print(float arg) {
        return String.valueOf(arg);
    }

    public static String print(double arg) {
        return String.valueOf(arg);
    }

    public static String print(Object arg) {
        return print_r(new Stack<>(), arg);
    }

    private static String print_r(Stack<Object> visitedObjects, Object arg) {
        String result = "";
        if (arg == null) {
            result += "null";
        } else if (arg.getClass().isArray()) {
            for (int i = 0; i < visitedObjects.size(); i++) {
                if (visitedObjects.elementAt(i) == arg) {
                    return "<recursive>";
                }
            }

            visitedObjects.push(arg);

            final String delimiter = ", ";
            result += "[";

            if (arg instanceof Object[]) {
                Object[] array = (Object[]) arg;
                for (int i = 0; i < array.length; i++) {
                    result += print_r(visitedObjects, array[i]);
                    if (i < array.length - 1) {
                        result += delimiter;
                    }
                }
            } else if (arg instanceof boolean[]) {
                boolean[] array = (boolean[]) arg;
                for (int i = 0; i < array.length; i++) {
                    result += print(array[i]);
                    if (i < array.length - 1) {
                        result += delimiter;
                    }
                }
            } else if (arg instanceof byte[]) {
                byte[] array = (byte[]) arg;
                for (int i = 0; i < array.length; i++) {
                    result += print(array[i]);
                    if (i < array.length - 1) {
                        result += delimiter;
                    }
                }
            } else if (arg instanceof short[]) {
                short[] array = (short[]) arg;
                for (int i = 0; i < array.length; i++) {
                    result += print(array[i]);
                    if (i < array.length - 1) {
                        result += delimiter;
                    }
                }
            } else if (arg instanceof char[]) {
                char[] array = (char[]) arg;
                for (int i = 0; i < array.length; i++) {
                    result += print(array[i]);
                    if (i < array.length - 1) {
                        result += delimiter;
                    }
                }
            } else if (arg instanceof int[]) {
                int[] array = (int[]) arg;
                for (int i = 0; i < array.length; i++) {
                    result += print(array[i]);
                    if (i < array.length - 1) {
                        result += delimiter;
                    }
                }
            } else if (arg instanceof long[]) {
                long[] array = (long[]) arg;
                for (int i = 0; i < array.length; i++) {
                    result += print(array[i]);
                    if (i < array.length - 1) {
                        result += delimiter;
                    }
                }
            } else if (arg instanceof float[]) {
                float[] array = (float[]) arg;
                for (int i = 0; i < array.length; i++) {
                    result += print(array[i]);
                    if (i < array.length - 1) {
                        result += delimiter;
                    }
                }
            } else if (arg instanceof double[]) {
                double[] array = (double[]) arg;
                for (int i = 0; i < array.length; i++) {
                    result += print(array[i]);
                    if (i < array.length - 1) {
                        result += delimiter;
                    }
                }
            }

            result += "]";
            visitedObjects.pop();

        } else {
            result += arg.toString();
        }

        return result;
    }
}
