/*
 * Copyright (c) 2003, 2014, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4168833 8034085
 * @summary Tests that Introspector does not create IndexedPropertyDescriptor
 *          from non-indexed PropertyDescriptor
 * @author Mark Davidson
 * @author Sergey Malenkov
 */

import java.awt.Color;
import java.awt.Dimension;

import java.beans.IndexedPropertyDescriptor;
import java.beans.PropertyDescriptor;

/**
 * The base class "color" property
 * creates both an IndexedPropertyDescriptor which has a conflicting
 * type for getColor.
 */
public class Test4168833 {
    public static void main(String[] args) throws Exception {
        // When the Sub class is introspected,
        // the property type should be color.
        // The complete "classic" set of properties
        // has been completed accross the hierarchy.
        test(Sub.class);
        test(Sub2.class);
    }

    private static void test(Class type) {
        PropertyDescriptor pd = BeanUtils.getPropertyDescriptor(type, "prop");
        if (pd instanceof IndexedPropertyDescriptor) {
            error(pd, type.getSimpleName() + ".prop should not be an indexed property");
        }
        if (!pd.getPropertyType().equals(Color.class)) {
            error(pd, type.getSimpleName() + ".prop type should be a Color");
        }
        if (null == pd.getReadMethod()) {
            error(pd, type.getSimpleName() + ".prop should have classic read method");
        }
        if (null == pd.getWriteMethod()) {
            error(pd, type.getSimpleName() + ".prop should have classic write method");
        }
    }

    private static void error(PropertyDescriptor pd, String message) {
        BeanUtils.reportPropertyDescriptor(pd);
        throw new Error(message);
    }

    public static class Base {
        public Color getProp() {
            return null;
        }

        public Dimension getProp(int i) {
            return null;
        }
    }

    public static class Sub extends Base {
        public void setProp(Color c) {
        }
    }

    public static class Base2 {
        public void setProp(Color c) {
        }

        public void setProp(int i, Dimension d) {
        }
    }

    public static class Sub2 extends Base2 {
        public Color getProp() {
            return null;
        }
    }
}
