/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;

/**
 * @test
 * @bug 8196373
 * @summary Introspector should work when generics are used
 */
public final class GenericPropertyType {

    /// Nothing is overridden
    static class ParentNo<T> {
        public T getValue() {return null;}
        public void setValue(T value) {}
    }

    static class ChildNoO extends ParentNo<Object> {}
    static class ChildNoA extends ParentNo<ArithmeticException> {}
    static class ChildNoS extends ParentNo<String> {}

    /// no get(), set is overridden
    static class ParentNoGet<T> {
        protected void setValue(T value) {}
    }

    static class ChildNoGetO extends ParentNoGet<Object> {
        @Override
        public void setValue(Object value) {}
    }

    static class ChildNoGetA extends ParentNoGet<ArithmeticException> {
        @Override
        public void setValue(ArithmeticException value) {}
    }

    static class ChildNoGetS extends ParentNoGet<String> {
        @Override
        public void setValue(String value) {}
    }

    /// get() exists, set is overridden
    static class ParentGet<T> {
        public final T getValue() {return null;}
        protected void setValue(T value) {}
    }

    static class ChildGetO extends ParentGet<Object> {
        @Override
        public void setValue(Object value) {}
    }

    static class ChildGetA extends ParentGet<ArithmeticException> {
        @Override
        public void setValue(ArithmeticException value) {}
    }

    static class ChildGetS extends ParentGet<String> {
        @Override
        public void setValue(String value) {}
    }

    /// Both set/get are overridden
    static class ParentAll<T> {
        protected T getValue() {return null;}
        protected void setValue(T value) {}
    }

    static class ChildAllO extends ParentAll<Object> {
        @Override
        public void setValue(Object value) {}
        @Override
        public Object getValue() {return null;}
    }

    static class ChildAllA extends ParentAll<ArithmeticException> {
        @Override
        public void setValue(ArithmeticException value) {}
        @Override
        public ArithmeticException getValue() {return null;}
    }

    static class ChildAllS extends ParentAll<String> {
        @Override
        public void setValue(String value) {}
        @Override
        public String getValue() {return null;}
    }

    public static void main(String[] args) throws Exception {
        testProperty(ChildNoGetA.class, ArithmeticException.class);
        testProperty(ChildNoGetO.class, Object.class);
        testProperty(ChildNoGetS.class, String.class);

        testProperty(ChildGetA.class, ArithmeticException.class);
        testProperty(ChildGetO.class, Object.class);
        testProperty(ChildGetS.class, String.class);

        testProperty(ChildAllA.class, ArithmeticException.class);
        testProperty(ChildAllO.class, Object.class);
        testProperty(ChildAllS.class, String.class);

        testProperty(ChildNoA.class, ArithmeticException.class);
        testProperty(ChildNoO.class, Object.class);
        testProperty(ChildNoS.class, String.class);
    }

    private static void testProperty(Class<?> beanClass, Class<?> type) throws Exception {
        BeanInfo beanInfo = Introspector.getBeanInfo(beanClass, Object.class);
        PropertyDescriptor[] pds = beanInfo.getPropertyDescriptors();
        if (pds.length != 1) {
            throw new RuntimeException("Wrong number of properties");
        }
        PropertyDescriptor pd = pds[0];
        System.out.println("pd = " + pd);
        String name = pd.getName();
        if (!name.equals("value")) {
            throw new RuntimeException("Wrong name: " + name);
        }
        Class<?> propertyType = pd.getPropertyType();
        if (propertyType != type) {
            throw new RuntimeException("Wrong property type: " + propertyType);
        }
    }
}
