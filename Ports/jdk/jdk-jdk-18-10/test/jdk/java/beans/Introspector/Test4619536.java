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
 * @bug 4619536
 * @summary Tests resolving the ambiguities in the resolution of IndexedPropertyDescriptors
 * @author Mark Davidson
 */

import java.beans.IndexedPropertyDescriptor;
import java.beans.PropertyDescriptor;
import java.util.Date;

public class Test4619536 {
    public static void main(String[] args) throws Exception {
        IndexedPropertyDescriptor ipd = BeanUtils.getIndexedPropertyDescriptor(A.class, "foo");
        if (!ipd.getIndexedPropertyType().equals(String.class)) {
            error(ipd, "A.foo should be String type");
        }
        PropertyDescriptor pd = BeanUtils.findPropertyDescriptor(B.class, "foo");
        if (pd instanceof IndexedPropertyDescriptor) {
            error(pd, "B.foo should not be an indexed property");
        }
        if (!pd.getPropertyType().equals(Date.class)) {
            error(pd, "B.foo should be Date type");
        }
        pd = BeanUtils.findPropertyDescriptor(Child.class, "foo");
        if (pd instanceof IndexedPropertyDescriptor) {
            error(pd, "Child.foo should not be an indexed property");
        }
        pd = BeanUtils.findPropertyDescriptor(Classic.class, "foo");
        if (pd instanceof IndexedPropertyDescriptor) {
            error(pd, "Classic.foo should not be an indexed property");
        }
        ipd = BeanUtils.getIndexedPropertyDescriptor(Index.class, "foo");
        if (!hasIPD(ipd)) {
            error(pd, "Index.foo should have ipd values");
        }
        if (hasPD(ipd)) {
            error(ipd, "Index.foo should not have pd values");
        }
        ipd = BeanUtils.getIndexedPropertyDescriptor(All.class, "foo");
        if (!hasPD(ipd) || !hasIPD(ipd)) {
            error(ipd, "All.foo should have all pd/ipd values");
        }
        if (!isValidType(ipd)) {
            error(ipd, "All.foo pdType should equal ipdType");
        }
        ipd = BeanUtils.getIndexedPropertyDescriptor(Getter.class, "foo");
        if (ipd.getReadMethod() == null || ipd.getWriteMethod() != null) {
            error(ipd, "Getter.foo classic methods incorrect");
        }
        if (!isValidType(ipd)) {
            error(ipd, "Getter.foo pdType should equal ipdType");
        }
        ipd = BeanUtils.getIndexedPropertyDescriptor(BadGetter.class, "foo");
        if (hasPD(ipd)) {
            error(ipd, "BadGetter.foo should not have classic methods");
        }
        ipd = BeanUtils.getIndexedPropertyDescriptor(Setter.class, "foo");
        if (ipd.getReadMethod() != null || ipd.getWriteMethod() == null) {
            error(ipd, "Setter.foo classic methods incorrect");
        }
        if (!isValidType(ipd)) {
            error(ipd, "Setter.foo pdType should equal ipdType");
        }
        ipd = BeanUtils.getIndexedPropertyDescriptor(BadSetter.class, "foo");
        if (hasPD(ipd)) {
            error(ipd, "BadSetter.foo should not have classic methods");
        }
    }

    public static boolean hasPD(PropertyDescriptor pd) {
        if (null == pd.getPropertyType()) {
            return false;
        }
        return (null != pd.getReadMethod())
            || (null != pd.getWriteMethod());
    }

    public static boolean hasIPD(IndexedPropertyDescriptor ipd) {
        if (null == ipd.getIndexedPropertyType()) {
            return false;
        }
        return (null != ipd.getIndexedReadMethod())
            || (null != ipd.getIndexedWriteMethod());
    }

    public static boolean isValidType(IndexedPropertyDescriptor ipd) {
        Class type = ipd.getPropertyType();
        return type.isArray() && type.getComponentType().equals(ipd.getIndexedPropertyType());
    }

    public static void error(PropertyDescriptor pd, String message) {
        BeanUtils.reportPropertyDescriptor(pd);
        throw new Error(message);
    }

    // Test case from 4619536
    public static class A {
        // prop foo on A should be indexed of type String
        public String getFoo(int x) {
            return null;
        }
    }

    public static class B extends A {
        // prop foo on should be non-indexed of type Date
        public Date getFoo() {
            return null;
        }
    }

    // Test case from 4812428 (this works in 1.5.0)
    public static class Parent {
        public void setFoo(String foo) {
        }

        public Child getFoo(int index) {
            return null;
        }
    }

    public static class Child extends Parent {
        public Child getFoo() {
            return null;
        }
    }

    // This class has a complete set of pd
    public static class Classic {
        public String[] getFoo() {
            return null;
        }

        public void setFoo(String[] foo) {
        }
    }

    // This class has a complete set of ipd
    public static class Index {
        public String getFoo(int i) {
            return null;
        }

        public void setFoo(int i, String f) {
        }
    }

    // This class adds a proper getter and setter
    public static class All extends Index {
        public String[] getFoo() {
            return null;
        }

        public void setFoo(String[] foo) {
        }
    }

    // This class adds a classic getter
    public static class Getter extends Index {
        public String[] getFoo() {
            return null;
        }
    }

    // This class has an alternate getter and should be merged
    public static class BadGetter extends Index {
        public String getFoo() {
            return null;
        }
    }

    // This class adds a classic setter
    public static class Setter extends Index {
        public void setFoo(String[] f) {
        }
    }

    // This class has an alternate setter and should be merged
    public static class BadSetter extends Index {
        public void setFoo(String f) {
        }
    }
}
