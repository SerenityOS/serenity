/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8221244
 * @summary Unexpected behavior of PropertyDescription.getReadMethod for boolean properties
 */

import java.beans.PropertyDescriptor;

public class Test8221244 {

    public static void main(String[] args) {
        test("bv", "isBv"); // boolean value
        test("bo", "getBo"); // Boolean object
        test("io", "getIo"); // Integer object
    }

    private static void test(String propertyName, String expectedGetterName) {
        PropertyDescriptor pd = BeanUtils.getPropertyDescriptor(TestBean.class, propertyName);
        String getterName = pd.getReadMethod().getName();
        if (!getterName.equals(expectedGetterName)) {
            throw new Error("unexpected getter: " + getterName);
        }
    }
}

/*
 * Bean class with multiple properties (each property has "is"/"get" getters)
 *
 * For boolean properties, the "is" getter should be used
 */
class TestBean {

    // boolean value
    private boolean bv;

    public boolean isBv() {
        return bv;
    }

    public boolean getBv() {
        return bv;
    }

    // Boolean object
    private Boolean bo;
    public Boolean isBo() {
        return bo;
    }
    public Boolean getBo() {
        return bo;
    }

    // Integer object
    private Integer io;
    public Integer isIo() {
        return io;
    }
    public Integer getIo() {
        return io;
    }
}
