/*
 * Copyright (c) 2007, Oracle and/or its affiliates. All rights reserved.
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
 * @test
 * @bug 6422403
 * @summary Tests property types for generics
 * @author Sergey Malenkov
 */

import java.beans.PropertyDescriptor;

public class Test6422403 {
    public static void main(String[] args) {
        test(Grand.class, "array", new Object[0].getClass());

        test(Parent.class, "array", new Number[0].getClass());
        test(Parent.class, "number", Number.class);

        test(Child.class, "array", new Long[0].getClass());
        test(Child.class, "number", Long.class);
        test(Child.class, "value", Long.class);
    }

    private static void test(Class type, String name, Class expected) {
        PropertyDescriptor pd = BeanUtils.getPropertyDescriptor(type, name);
        if (name.equals(pd.getName()))
            if (!expected.equals(pd.getPropertyType()))
                throw new Error("expected " + expected + " but " + pd.getPropertyType() + " is resolved");
    }

    private static class Grand<A> {
        private A[] array;

        public A[] getArray() {
            return this.array;
        }

        public void setArray(A...array) {
            this.array = array;
        }
    }

    private static class Parent<N extends Number> extends Grand<N> {
        private N number;

        public N getNumber() {
            return this.number;
        }

        public void setNumber(N number) {
            this.number = number;
        }
    }

    private static class Child extends Parent<Long> {
        private Long value;

        public Long getValue() {
            return this.value;
        }

        public void setValue(Long value) {
            this.value = value;
        }
    }
}
