/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7193977
 * @summary Tests that generified property descriptors do not loose additional info
 * @author Sergey Malenkov
 */

import java.awt.Image;
import java.beans.BeanDescriptor;
import java.beans.BeanInfo;
import java.beans.EventSetDescriptor;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.MethodDescriptor;
import java.beans.PropertyDescriptor;
import java.util.Arrays;
import java.util.List;

public class Test7193977 {

    private static final List<String> names = Arrays.asList("listType", "list", "value");

    public static void main(String args[]) {
        for (String name : names) {
            test(Abstract.class, name);
            test(Concrete.class, name);
        }
    }

    private static void test(Class<?> type, String name) {
        if (!Boolean.TRUE.equals(BeanUtils.getPropertyDescriptor(type, name).getValue("transient"))) {
            throw new Error("property '" + name + "' is not transient");
        }
    }

    public static final class Concrete extends Abstract<String> {
    }

    public static abstract class Abstract<T> {
        private List<T> list;

        public List<T> getList() {
            return this.list;
        }

        public void setList(List<T> list) {
            this.list = list;
        }

        public T getValue(int index) {
            return (0 <= index) && (this.list != null) && (index < this.list.size())
                    ? this.list.get(index)
                    : null;
        }

        public void setValue(int index, T value) {
            if ((0 <= index) && (this.list != null)) {
                if (index == this.list.size()) {
                    this.list.add(value);
                }
                else if (index < this.list.size()) {
                    this.list.set(index, value);
                }
            }
        }

        public String getListType() {
            return (this.list != null)
                    ? this.list.getClass().getName()
                    : null;
        }

        public void setListType(String type) throws Exception {
            this.list = (type != null)
                    ? (List<T>) Class.forName(type).newInstance()
                    : null;
        }
    }

    public static final class ConcreteBeanInfo extends Wrapper {
        public ConcreteBeanInfo() throws IntrospectionException {
            super(Concrete.class);
        }
    }

    public static final class AbstractBeanInfo extends Wrapper {
        public AbstractBeanInfo() throws IntrospectionException {
            super(Abstract.class);
            for (PropertyDescriptor pd : getPropertyDescriptors()) {
                if (names.contains(pd.getName())) {
                    pd.setValue("transient", Boolean.TRUE);
                }
            }
        }
    }

    private static class Wrapper implements BeanInfo {
        private final BeanInfo info;

        Wrapper(Class<?> type) throws IntrospectionException {
            this.info = Introspector.getBeanInfo(type, Introspector.IGNORE_IMMEDIATE_BEANINFO);
        }

        public BeanDescriptor getBeanDescriptor() {
            return this.info.getBeanDescriptor();
        }

        public EventSetDescriptor[] getEventSetDescriptors() {
            return this.info.getEventSetDescriptors();
        }

        public int getDefaultEventIndex() {
            return this.info.getDefaultEventIndex();
        }

        public PropertyDescriptor[] getPropertyDescriptors() {
            return this.info.getPropertyDescriptors();
        }

        public int getDefaultPropertyIndex() {
            return this.info.getDefaultPropertyIndex();
        }

        public MethodDescriptor[] getMethodDescriptors() {
            return this.info.getMethodDescriptors();
        }

        public BeanInfo[] getAdditionalBeanInfo() {
            return this.info.getAdditionalBeanInfo();
        }

        public Image getIcon(int kind) {
            return this.info.getIcon(kind);
        }
    }
}
