/*
 * Copyright (c) 2009, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6868189
 * @summary Tests custom BeanInfo in the same package
 * @author Sergey Malenkov
 */

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.beans.SimpleBeanInfo;

public class Test6868189 {

    private static final String PROPERTY = "$?"; // NON-NLS: the property name
    private static final String GETTER = "name"; // NON-NLS: the method name
    private static final String SETTER = null;

    public static void main(String[] args) throws IntrospectionException {
        PropertyDescriptor[] pds = Introspector.getBeanInfo(Enumeration.class).getPropertyDescriptors();
        if ((pds.length != 1)|| !PROPERTY.equals(pds[0].getName())){
            throw new Error("unexpected property");
        }
    }

    public enum Enumeration {
        FIRST, SECOND
    }

    public static class EnumerationBeanInfo extends SimpleBeanInfo {
        @Override
        public PropertyDescriptor[] getPropertyDescriptors() {
            try {
                return new PropertyDescriptor[] {
                        new PropertyDescriptor(PROPERTY, Enumeration.class, GETTER, SETTER)
                };
            }
            catch (IntrospectionException exception) {
                throw new Error("unexpected exception", exception);
            }
        }
    }
}
