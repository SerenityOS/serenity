/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.beans.PropertyDescriptor;

/*
 * @test
 * @bug 8027905
 * @summary Tests that GC does not affect a property type
 * @author Sergey Malenkov
 * @run main/othervm -mx16m Test8027905
 */

public class Test8027905 {
    public static void main(String[] args) {
        PropertyDescriptor pd = BeanUtils.getPropertyDescriptor(Sub.class, "foo");
        Class<?> type = pd.getPropertyType();

        int[] array = new int[10];
        while (array != null) {
            try {
                array = new int[array.length + array.length];
            }
            catch (OutOfMemoryError error) {
                array = null;
            }
        }
        if (type != pd.getPropertyType()) {
            throw new Error("property type is changed");
        }
    }

    public static class Super<T> {
        public T getFoo() {
            return null;
        }

        public void setFoo(T t) {
        }
    }

    public static class Sub extends Super<String> {
        @Override
        public String getFoo() {
            return null;
        }

        @Override
        public void setFoo(String t) {
        }
    }
}
