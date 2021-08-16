/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

package p3;

import p4.Foo;
import java.lang.module.ModuleDescriptor;
import java.lang.reflect.Field;

import static java.lang.module.ModuleDescriptor.Exports.Modifier.*;

/**
 * Test if m4 is an open module and p4 is package that m3 can access
 */
public class Main {
    public static void main(String... args) throws Exception {
        Module m4 = Foo.class.getModule();
        if (!m4.isOpen("p4")) {
            throw new RuntimeException("m3 can't access p4");
        }

        // Test if it can access a private field
        Foo foo = Foo.create("foo");

        Field field = Foo.class.getDeclaredField("name");
        field.setAccessible(true);
        String name = (String) field.get(foo);
        if (!name.equals("foo")) {
            throw new RuntimeException("unexpected Foo::name value = " + name);
        }

        checkOpenModule();
    }

    // check the module descriptor of the open module m4
    static void checkOpenModule() {
        ModuleDescriptor md = Foo.class.getModule().getDescriptor();
        System.out.println(md);

        if (!md.isOpen()) {
            throw new RuntimeException("m4 is a open module");
        }

        if (md.packages().size() != 1 || !md.packages().contains("p4")) {
            throw new RuntimeException("unexpected m4 packages: " + md.packages());
        }

        if (!md.opens().isEmpty()) {
            throw new RuntimeException("unexpected m4 opens: " + md.opens());
        }
    }

}
