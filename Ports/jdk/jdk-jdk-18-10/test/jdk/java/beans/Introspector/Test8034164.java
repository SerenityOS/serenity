/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

import java.beans.IndexedPropertyDescriptor;
import java.beans.PropertyDescriptor;

/*
 * @test
 * @bug 8034164
 * @summary Tests that Introspector does not ignore indexed getter and setter for correct types
 * @author Sergey Malenkov
 */

public class Test8034164 {
    public static final StringBuilder ERROR = new StringBuilder();

    public static void main(String[] args) {
        test(Bean0000.class, false, false, false, false);
        test(Bean0001.class, false, false, false, true);
        test(Bean0010.class, false, false, true, false);
        test(Bean0011.class, false, false, true, true);
        test(Bean0100.class, false, true, false, false);
        test(Bean0101.class, false, true, false, true);
        test(Bean0110.class, false, true, true, false);
        test(Bean0111.class, false, true, true, true);
        test(Bean1000.class, true, false, false, false);
        test(Bean1001.class, true, false, false, true);
        test(Bean1010.class, true, false, true, false);
        test(Bean1011.class, true, false, true, true);
        test(Bean1100.class, true, true, false, false);
        test(Bean1101.class, true, true, false, true);
        test(Bean1110.class, true, true, true, false);
        test(Bean1111.class, true, true, true, true);

        if (0 < ERROR.length()) {
            throw new Error(ERROR.toString());
        }
    }

    private static void test(Class<?> type, boolean read, boolean write, boolean readIndexed, boolean writeIndexed) {
        PropertyDescriptor pd = BeanUtils.findPropertyDescriptor(type, "size");
        if (pd != null) {
            test(type, "read", read, null != pd.getReadMethod());
            test(type, "write", write, null != pd.getWriteMethod());
            if (pd instanceof IndexedPropertyDescriptor) {
                IndexedPropertyDescriptor ipd = (IndexedPropertyDescriptor) pd;
                test(type, "indexed read", readIndexed, null != ipd.getIndexedReadMethod());
                test(type, "indexed write", writeIndexed, null != ipd.getIndexedWriteMethod());
            } else if (readIndexed || writeIndexed) {
                error(type, "indexed property does not exist");
            }
        } else if (read || write || readIndexed || writeIndexed) {
            error(type, "property does not exist");
        }
    }

    private static void test(Class<?> type, String name, boolean expected, boolean actual) {
        if (expected && !actual) {
            error(type, name + " method does not exist");
        } else if (!expected && actual) {
            error(type, name + " method is not expected");
        }
    }

    private static void error(Class<?> type, String message) {
        ERROR.append("\n\t\t").append(type.getSimpleName()).append(".size: ").append(message);
    }

    public static class Bean0000 {
    }

    public static class Bean0001 {
        public void setSize(int index, int value) {
        }
    }

    public static class Bean0010 {
        public int getSize(int index) {
            return 0;
        }
    }

    public static class Bean0011 {
        public int getSize(int index) {
            return 0;
        }

        public void setSize(int index, int value) {
        }
    }

    public static class Bean0100 {
        public void setSize(int[] value) {
        }
    }

    public static class Bean0101 {
        public void setSize(int[] value) {
        }

        public void setSize(int index, int value) {
        }
    }

    public static class Bean0110 {
        public void setSize(int[] value) {
        }

        public int getSize(int index) {
            return 0;
        }
    }

    public static class Bean0111 {
        public void setSize(int[] value) {
        }

        public int getSize(int index) {
            return 0;
        }

        public void setSize(int index, int value) {
        }
    }

    public static class Bean1000 {
        public int[] getSize() {
            return null;
        }
    }

    public static class Bean1001 {
        public int[] getSize() {
            return null;
        }

        public void setSize(int index, int value) {
        }
    }

    public static class Bean1010 {
        public int[] getSize() {
            return null;
        }

        public int getSize(int index) {
            return 0;
        }
    }

    public static class Bean1011 {
        public int[] getSize() {
            return null;
        }

        public int getSize(int index) {
            return 0;
        }

        public void setSize(int index, int value) {
        }
    }

    public static class Bean1100 {
        public int[] getSize() {
            return null;
        }

        public void setSize(int[] value) {
        }
    }

    public static class Bean1101 {
        public int[] getSize() {
            return null;
        }

        public void setSize(int[] value) {
        }

        public void setSize(int index, int value) {
        }
    }

    public static class Bean1110 {
        public int[] getSize() {
            return null;
        }

        public void setSize(int[] value) {
        }

        public int getSize(int index) {
            return 0;
        }
    }

    public static class Bean1111 {
        public int[] getSize() {
            return null;
        }

        public void setSize(int[] value) {
        }

        public int getSize(int index) {
            return 0;
        }

        public void setSize(int index, int value) {
        }
    }
}
