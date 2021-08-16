
/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.beans.ConstructorProperties;
import javax.management.ConstructorParameters;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;

/*
 * @test
 * @bug 7199353
 * @summary Asserts that 'java.beans.ConstructorProperties' annotation is still
 *          recognized and properly handled for custom types mapped to open types.
 *          Also, makes sure that if the same constructor is annotated by both
 *          j.b.ConstructorProperties and j.m.ConstructorProperties annotations
 *          only j.m.ConstructorProperties annotation is considered.
 * @author Jaroslav Bachorik
 *
 * @modules java.desktop
 *          java.management
 *
 * @run main LegacyConstructorPropertiesTest
 */

public class LegacyConstructorPropertiesTest {
    public static class CustomType {
        private String name;
        private int value;
        @ConstructorProperties({"name", "value"})
        public CustomType(String name, int value) {
            this.name = name;
            this.value = value;
        }

        // if @java.beans.ConstructorProperties would be used
        // the introspector would choke on this
        @ConstructorProperties("noname")
        @ConstructorParameters("name")
        public CustomType(String name) {
            this.name = name;
            this.value = -1;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }
    }

    public static interface CustomMXBean {
        public CustomType getProp();
        public void setProp(CustomType prop);
    }

    public static final class Custom implements CustomMXBean {
        private CustomType prop;

        @Override
        public CustomType getProp() {
            return prop;
        }

        @Override
        public void setProp(CustomType prop) {
            this.prop = prop;
        }
    }

    public static void main(String[] args) throws Exception {
        MBeanServer mbs = MBeanServerFactory.createMBeanServer();
        CustomMXBean mbean = new Custom();

        mbs.registerMBean(mbean, ObjectName.getInstance("test:type=Custom"));
    }
}
