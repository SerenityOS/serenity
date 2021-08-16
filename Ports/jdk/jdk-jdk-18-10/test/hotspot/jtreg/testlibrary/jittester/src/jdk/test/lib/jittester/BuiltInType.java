/*
 * Copyright (c) 2005, 2015, Oracle and/or its affiliates. All rights reserved.
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

package jdk.test.lib.jittester;

public abstract class BuiltInType extends Type {

    private static class BuiltInTypeCapacityHelper {

        private static final String builtInTypes[] = {"boolean", "byte", "short", "char", "int", "long", "float", "double"};

        private static int getIndexFor(String typeName) {
            for (int i = 0; i < builtInTypes.length; i++) {
                if (typeName.compareTo(builtInTypes[i]) == 0) {
                    return i;
                }
            }
            return -1;
        }

        public static int compare(String typeName1, String typeName2) {
            int i1 = getIndexFor(typeName1);
            int i2 = getIndexFor(typeName2);

            return i1 - i2;
        }
    }

    protected BuiltInType(String name) {
        super(name);
    }

    @Override
    public boolean canImplicitlyCastTo(Type type) {
        if (equals(type)) {
            return true;
        }
        try {
            BuiltInType t = (BuiltInType) type;
            // You cannot impicitly cast anything to char or boolean
            if (t.getName().compareTo("boolean") == 0 || t.getName().compareTo("char") == 0) {
                return false;
            }
            if (t.isMoreCapaciousThan(this)) {
                return true;
            }
        } catch (Exception e) {
        }
        return false;
    }

    @Override
    public boolean canExplicitlyCastTo(Type t) {
        if (equals(t)) {
            return true;
        }
        try {
            BuiltInType _t = (BuiltInType) t;
            if (_t.getName().compareTo("boolean") != 0) {
                return true;
            }
        } catch (Exception e) {
        }
        return false;
    }

    public boolean isMoreCapaciousThan(BuiltInType t) {
        return BuiltInTypeCapacityHelper.compare(this.getName(), t.getName()) > 0;
    }

    @Override
    public boolean canCompareTo(Type t) {
        return true;
    }

    @Override
    public boolean canEquateTo(Type t) {
        return true;
    }
}
