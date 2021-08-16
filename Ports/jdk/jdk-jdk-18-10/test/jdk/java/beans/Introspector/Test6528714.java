/*
 * Copyright (c) 2007, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6528714
 * @summary Tests two getters with different return types
 * @author Sergey Malenkov
 */

import java.beans.PropertyDescriptor;

public class Test6528714 {
    public static void main(String[] args) {
        test(Concrete.class, "id", Integer.class);
    }

    private static void test(Class type, String name, Class expected) {
        PropertyDescriptor pd = BeanUtils.getPropertyDescriptor(type, name);
        if (name.equals(pd.getName()))
            if (!expected.equals(pd.getPropertyType()))
                throw new Error("expected " + expected + " but " + pd.getPropertyType() + " is resolved");
    }

    private static interface Abstract {
        Number getId();
    }

    private static class Concrete implements Abstract {
        private Integer id;

        public Integer getId() {
            return this.id;
        }
    }
}
