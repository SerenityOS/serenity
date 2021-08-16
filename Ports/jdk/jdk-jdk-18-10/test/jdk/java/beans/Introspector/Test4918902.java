/*
 * Copyright (c) 2003, 2007, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4918902
 * @summary Tests search the most specific methods for PropertyDescriptors
 * @author Mark Davidson
 */

import java.beans.PropertyDescriptor;
import java.beans.IndexedPropertyDescriptor;

public class Test4918902 {
    public static void main(String[] args) {
        testPropertyDescriptor(Child1.class, Child1.class, Parent.class);
        testPropertyDescriptor(Child2.class, Parent.class, Child2.class);
        testPropertyDescriptor(Child3.class, Child3.class, Child3.class);
        testPropertyDescriptor(Child4.class, Parent.class, Parent.class);

        testPropertyDescriptor(Grandchild.class, Child3.class, Child3.class);

        testIndexedPropertyDescriptor(IChild1.class, IChild1.class, IChild1.class);
        testIndexedPropertyDescriptor(IChild2.class, IChild2.class, IParent.class);
        testIndexedPropertyDescriptor(IChild3.class, IParent.class, IChild3.class);
        testIndexedPropertyDescriptor(IChild4.class, IParent.class, IParent.class);
    }

    private static void testPropertyDescriptor(Class type, Class read, Class write) {
        PropertyDescriptor pd = BeanUtils.getPropertyDescriptor(type, "foo");
        if (!read.equals(pd.getReadMethod().getDeclaringClass())) {
            throw new Error("unexpected read method: " + pd.getReadMethod());
        }
        if (!write.equals(pd.getWriteMethod().getDeclaringClass())) {
            throw new Error("unexpected write method: " + pd.getWriteMethod());
        }
    }

    private static void testIndexedPropertyDescriptor(Class type, Class read, Class write) {
        IndexedPropertyDescriptor ipd = BeanUtils.getIndexedPropertyDescriptor(type, "foo");
        if (!read.equals(ipd.getIndexedReadMethod().getDeclaringClass())) {
            throw new Error("unexpected read method: " + ipd.getIndexedReadMethod());
        }
        if (!write.equals(ipd.getIndexedWriteMethod().getDeclaringClass())) {
            throw new Error("unexpected write method: " + ipd.getIndexedWriteMethod());
        }
    }

    // simple properties
    public static class Parent {
        public String getFoo() {
            return null;
        }

        public void setFoo(String str) {
        }
    }

    // getter has been overriden
    public static class Child1 extends Parent {
        public String getFoo() {
            return null;
        }
    }

    // setter has been overriden
    public static class Child2 extends Parent {
        public void setFoo(String str) {
        }
    }

    // both methods have been overriden
    public static class Child3 extends Parent {
        public void setFoo(String str) {
        }

        public String getFoo() {
            return null;
        }
    }

    // methods should be taken from superclass
    public static class Child4 extends Parent {
    }

    // methods should be taken from superclass
    public static class Grandchild extends Child3 {
    }

    // indexed properties
    public static class IParent {
        public String getFoo(int i) {
            return null;
        }

        public void setFoo(int i, String str) {
        }
    }

    // both methods have been overriden
    public static class IChild1 extends IParent {
        public void setFoo(int i, String str) {
        }

        public String getFoo(int i) {
            return null;
        }
    }

    // getter has been overriden
    public static class IChild2 extends IParent {
        public String getFoo(int i) {
            return null;
        }
    }

    // setter has been overriden
    public static class IChild3 extends IParent {
        public void setFoo(int i, String str) {
        }
    }

    // methods should be taken from superclass
    public static class IChild4 extends IParent {
    }
}
