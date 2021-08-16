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
 * @bug 6707231
 * @summary Tests the boolean getter
 * @author Sergey Malenkov
 */

public class Test6707231 {
    public static void main(String[] args) throws Exception {
        test(Bean.class, Bean.class);
        test(Public.class, Public.class);
        test(Private.class, Bean.class);
    }

    public static class Bean {
        private boolean value;

        public boolean isValue() {
            return this.value;
        }

        public void setValue(boolean value) {
            this.value = value;
        }
    }

    public static class Public extends Bean {
        public boolean isValue() {
            return super.isValue();
        }

        public void setValue(boolean value) {
            super.setValue(value);
        }
    }

    private static class Private extends Bean {
        public boolean isValue() {
            return super.isValue();
        }

        public void setValue(boolean value) {
            super.setValue(value);
        }
    }

    private static void test(Class<?> actual, Class<?> expected) {
        PropertyDescriptor pd = BeanUtils.getPropertyDescriptor(actual, "value");
        Class<?> getter = pd.getReadMethod().getDeclaringClass();
        Class<?> setter = pd.getWriteMethod().getDeclaringClass();
        if ((getter != expected) || (setter != expected)) {
            throw new Error(actual.getName());
        }
    }
}
