/*
 * Copyright (c) 2011, 2018, Oracle and/or its affiliates. All rights reserved.
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

package vm.mlvm.meth.share;

import nsk.share.test.TestUtils;

public class Argument {

    private final Class<?> type;
    private final Object value;
    private boolean isPreserved;
    private String tag;

    public Argument(Class<?> type, Object value) {
        this(type, value, false, "");
    }

    public Argument(Class<?> type, Object value, boolean isPreserved, String tag) {
        this.type = type;
        this.value = value;
        this.isPreserved = isPreserved;
        this.tag = tag;
    }

    public Class<?> getType() {
        return this.type;
    }

    public Object getValue() {
        return this.value;
    }

    public void setPreserved(boolean newValue) {
        this.isPreserved = newValue;
    }

    public boolean isPreserved() {
        return this.isPreserved;
    }

    public String getTag() {
        return this.tag;
    }

    public void setTag(String newTag) {
        this.tag = newTag;
    }

    public static Argument fromValue(Object value) {
        return new Argument(value.getClass(), value);
    }

    public static Argument fromPrimitiveValue(Object boxedValue) {
        TestUtils.assertInCollection(TestTypes.UNBOX_MAP.keySet(), boxedValue.getClass());
        return new Argument(TestTypes.UNBOX_MAP.get(boxedValue.getClass()), boxedValue);
    }

    public static Argument fromArray(Object[] a) {
        boolean isProtected = false;
        if ( a.length > 2 && a[2].getClass().equals(Boolean.class) )
            isProtected = (Boolean) a[2];

        return new Argument((Class<?>) a[0], a[1], isProtected, "");
    }

    @Override
    public String toString() {
        return getType().getName().replaceFirst("^java.lang.", "") + "="
             + (getType().equals(String.class) ? "{" + getValue() + "}" : getValue() == null ? "null" : getValue() )
             + ((! getTag().isEmpty() || isPreserved()) ? ("[" + (isPreserved() ? "!" : "") + getTag() + "]") : "");
    }

    @Override
    public Argument clone() {
        return new Argument(getType(), getValue(), isPreserved(), getTag());
    }
}
