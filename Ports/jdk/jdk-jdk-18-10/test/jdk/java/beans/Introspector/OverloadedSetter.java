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
import java.lang.reflect.Method;

/**
 * @test
 * @bug 8196373
 * @summary behavior of this class is not specified, but it can be used to check
 *          our implementation for accidental changes
 */
public final class OverloadedSetter {

    class AAA {}
    class CCC extends AAA {}
    class BBB extends CCC {}
    class DDD extends CCC {}

    class ZZZ {}

    // DDD will be selected because it is most specific type.
    class ParentADC<T> {
        public void setValue(AAA value) {}
        public void setValue(DDD value) {}
        public void setValue(CCC value) {}
    }
    // DDD will be selected because it is most specific type.
    class ParentACD<T> {
        public void setValue(AAA value) {}
        public void setValue(CCC value) {}
        public void setValue(DDD value) {}
    }
    // DDD will be selected because it is most specific type.
    class ParentDAC<T> {
        public void setValue(DDD value) {}
        public void setValue(AAA value) {}
        public void setValue(CCC value) {}
    }
    // DDD will be selected because it is most specific type.
    class ParentDCA<T> {
        public void setValue(DDD value) {}
        public void setValue(CCC value) {}
        public void setValue(AAA value) {}
    }
    // DDD will be selected because it is most specific type.
    class ParentCAD<T> {
        public void setValue(CCC value) {}
        public void setValue(AAA value) {}
        public void setValue(DDD value) {}
    }
    // DDD will be selected because it is most specific type.
    class ParentCDA<T> {
        public void setValue(CCC value) {}
        public void setValue(DDD value) {}
        public void setValue(AAA value) {}
    }
    // DDD will be selected because it is most specific type and ZZZ will be
    // skipped because it will be placed at the end of the methods list.
    class ParentCDAZ<T> {
        public void setValue(CCC value) {}
        public void setValue(DDD value) {}
        public void setValue(AAA value) {}
        public void setValue(ZZZ value) {}
    }
    // DDD will be selected because it is most specific type which related to
    // the type of getValue(); BBB will be skipped because it is not a type or
    // subclass of the type returned by getValue();
    class ParentDACB<T> {
        public DDD getValue(){return null;}
        public void setValue(AAA value) {}
        public void setValue(DDD value) {}
        public void setValue(CCC value) {}
        public void setValue(BBB value) {}
    }

    public static void main(String[] args) throws Exception {
        test(ParentADC.class);
        test(ParentACD.class);
        test(ParentDAC.class);
        test(ParentDCA.class);
        test(ParentCAD.class);
        test(ParentCDA.class);
        test(ParentCDAZ.class);
        test(ParentDACB.class);
    }

    private static void test(Class<?> beanClass) throws Exception {
        BeanInfo beanInfo = Introspector.getBeanInfo(beanClass, Object.class);
        PropertyDescriptor[] pds = beanInfo.getPropertyDescriptors();
        if (pds.length != 1) {
            throw new RuntimeException("Wrong number of properties");
        }
        PropertyDescriptor pd = pds[0];
        String name = pd.getName();
        if (!name.equals("value")) {
            throw new RuntimeException("Wrong name: " + name);
        }

        Class<?> propertyType = pd.getPropertyType();
        if (propertyType != DDD.class) {
            throw new RuntimeException("Wrong property type: " + propertyType);
        }
        Method writeMethod = pd.getWriteMethod();
        if (writeMethod == null) {
            throw new RuntimeException("Write method is null");
        }
        Class<?>[] parameterTypes = writeMethod.getParameterTypes();
        if (parameterTypes.length != 1) {
            throw new RuntimeException("Wrong parameters " + parameterTypes);
        }
        if (parameterTypes[0] != DDD.class) {
            throw new RuntimeException("Wrong type: " + parameterTypes[0]);
        }
    }
}
