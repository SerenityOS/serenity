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

import java.beans.IndexedPropertyDescriptor;
import java.beans.PropertyDescriptor;

/*
 * @test
 * @bug 8027648
 * @summary Tests overridden getter and overloaded setter
 * @author Sergey Malenkov
 */

public class Test8027648 {

    public static void main(String[] args) {
        test(false);
        test(true);
    }

    private static void test(boolean indexed) {
        Class<?> parent = getPropertyType(BaseBean.class, indexed);
        Class<?> child = getPropertyType(MyBean.class, indexed);
        if (parent.equals(child) || !parent.isAssignableFrom(child)) {
            throw new Error("the child property type is not override the parent property type");
        }
    }

    private static Class<?> getPropertyType(Class<?> type, boolean indexed) {
        PropertyDescriptor pd = BeanUtils.findPropertyDescriptor(type, indexed ? "index" : "value");
        if (pd instanceof IndexedPropertyDescriptor) {
            IndexedPropertyDescriptor ipd = (IndexedPropertyDescriptor) pd;
            return ipd.getIndexedPropertyType();
        }
        return pd.getPropertyType();
    }

    public static class BaseBean {
        private Object value;

        public Object getValue() {
            return this.value;
        }

        public void setValue(Object value) {
            this.value = value;
        }

        public Object getIndex(int index) {
            return getValue();
        }

        public void setIndex(int index, Object value) {
            setValue(value);
        }
    }

    public static class MyBean extends BaseBean {
        @Override
        public String getValue() {
            return (String) super.getValue();
        }

        public void setValue(String value) {
            setValue((Object) value);
        }

        @Override
        public String getIndex(int index) {
            return getValue();
        }

        public void setIndex(int index, String value) {
            setValue(value);
        }
    }
}
