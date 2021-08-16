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
 * @bug 6723447
 * @summary Tests return type for property setters
 * @author Sergey Malenkov
 */

import java.beans.IndexedPropertyDescriptor;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.math.BigDecimal;

public class Test6723447 {

    public static void main(String[] args) {
        test(Test6723447.class);
        test(BigDecimal.class);
    }

    private static void test(Class<?> type) {
        for (PropertyDescriptor pd : getPropertyDescriptors(type)) {
            test(pd.getWriteMethod());
            if (pd instanceof IndexedPropertyDescriptor) {
                IndexedPropertyDescriptor ipd = (IndexedPropertyDescriptor) pd;
                test(ipd.getIndexedWriteMethod());
            }
        }
    }

    private static void test(Method method) {
        if (method != null) {
            Class<?> type = method.getReturnType();
            if (!type.equals(void.class)) {
                throw new Error("unexpected return type: " + type);
            }
        }
    }

    private static PropertyDescriptor[] getPropertyDescriptors(Class<?> type) {
        try {
            return Introspector.getBeanInfo(type).getPropertyDescriptors();
        }
        catch (IntrospectionException exception) {
            throw new Error("unexpected exception", exception);
        }
    }

    public Object getValue() {
        return null;
    }

    public Object setValue(Object value) {
        return value;
    }

    public Object getValues(int index) {
        return null;
    }

    public Object setValues(int index, Object value) {
        return value;
    }
}
