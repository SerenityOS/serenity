/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7192955 8000183
 * @summary Tests that all properties are bound
 * @author Sergey Malenkov
 */

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyChangeListener;
import java.beans.PropertyDescriptor;
import java.util.List;

public class Test7192955 {

    public static void main(String[] args) throws IntrospectionException {
        if (!BeanUtils.findPropertyDescriptor(MyBean.class, "test").isBound()) {
            throw new Error("a simple property is not bound");
        }
        if (!BeanUtils.findPropertyDescriptor(MyBean.class, "list").isBound()) {
            throw new Error("a generic property is not bound");
        }
        if (!BeanUtils.findPropertyDescriptor(MyBean.class, "readOnly").isBound()) {
            throw new Error("a read-only property is not bound");
        }
        PropertyDescriptor[] pds = Introspector.getBeanInfo(MyBean.class, BaseBean.class).getPropertyDescriptors();
        for (PropertyDescriptor pd : pds) {
            if (pd.getName().equals("test") && pd.isBound()) {
                throw new Error("a simple property is bound without superclass");
            }
        }
    }

    public static class BaseBean {

        private List<String> list;

        public List<String> getList() {
            return this.list;
        }

        public void setList(List<String> list) {
            this.list = list;
        }

        public void addPropertyChangeListener(PropertyChangeListener listener) {
        }

        public void removePropertyChangeListener(PropertyChangeListener listener) {
        }

        public List<String> getReadOnly() {
            return this.list;
        }
    }

    public static class MyBean extends BaseBean {

        private String test;

        public String getTest() {
            return this.test;
        }

        public void setTest(String test) {
            this.test = test;
        }
    }
}
