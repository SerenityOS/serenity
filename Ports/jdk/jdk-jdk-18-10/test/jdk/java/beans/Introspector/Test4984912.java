/*
 * Copyright (c) 2006, 2007, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4984912
 * @summary Tests property descriptors without getter and setter
 * @author Sergey Malenkov
 */

import java.beans.IndexedPropertyDescriptor;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.beans.SimpleBeanInfo;

public class Test4984912 {
    public static void main(String[] args) {
        PropertyDescriptor[] array = BeanUtils.getPropertyDescriptors(SimpleBean.class);
        for (PropertyDescriptor pd : array) {
            BeanUtils.reportPropertyDescriptor(pd);
        }
        if (array.length != 3)
            throw new Error("unexpected count of properties: " + array.length);
    }

    public static class SimpleBean {
        private int property;

        public int getProperty() {
            return this.property;
        }

        public void setProperty(int property) {
            this.property = property;
        }
    }

    public static class SimpleBeanBeanInfo extends SimpleBeanInfo {
        public PropertyDescriptor[] getPropertyDescriptors() {
            try {
                PropertyDescriptor pdProperty = new PropertyDescriptor("property", SimpleBean.class, "getProperty", "setProperty");
                PropertyDescriptor pdNullable = new PropertyDescriptor("nullable", SimpleBean.class, null, null);
                PropertyDescriptor pdIndexed = new IndexedPropertyDescriptor("indexed", SimpleBean.class, null, null, null, null);
                pdNullable.setValue("name", "value");
                return new PropertyDescriptor[] {pdProperty, pdNullable, pdIndexed};
            }
            catch (IntrospectionException exception) {
                throw new Error(exception);
            }
        }
    }
}
