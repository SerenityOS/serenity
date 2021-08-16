/*
 * Copyright (c) 2010, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6976577
 * @summary Tests public methods in non-public beans
 * @author Sergey Malenkov
 */

import test.Accessor;

import java.beans.EventSetDescriptor;
import java.beans.IndexedPropertyDescriptor;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;

public class Test6976577 {

    public static void main(String[] args) throws Exception {
        Class<?> bt = Accessor.getBeanType();
        Class<?> lt = Accessor.getListenerType();

        // test PropertyDescriptor
        PropertyDescriptor pd = new PropertyDescriptor("boolean", bt);
        test(pd.getReadMethod());
        test(pd.getWriteMethod());

        // test IndexedPropertyDescriptor
        IndexedPropertyDescriptor ipd = new IndexedPropertyDescriptor("indexed", bt);
        test(ipd.getReadMethod());
        test(ipd.getWriteMethod());
        test(ipd.getIndexedReadMethod());
        test(ipd.getIndexedWriteMethod());

        // test EventSetDescriptor
        EventSetDescriptor esd = new EventSetDescriptor(bt, "test", lt, "process");
        test(esd.getAddListenerMethod());
        test(esd.getRemoveListenerMethod());
        test(esd.getGetListenerMethod());
        test(esd.getListenerMethods());
    }

    private static void test(Method... methods) {
        for (Method method : methods) {
            if (method == null) {
                throw new Error("public method is not found");
            }
        }
    }
}
