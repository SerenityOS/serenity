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

/**
 * @test
 * @bug 6792161
 * @summary assert("No dead instructions after post-alloc")
 *
 * @run main/othervm/timeout=600 -Xcomp -XX:-TieredCompilation -XX:+IgnoreUnrecognizedVMOptions -XX:MaxInlineSize=120 compiler.c2.Test6792161
 */

package compiler.c2;

import java.lang.reflect.Constructor;

public class Test6792161 {
    static Constructor test(Class cls) throws Exception {
        Class[] args= { String.class };
        try {
            return cls.getConstructor(args);
        } catch (NoSuchMethodException e) {}
        return cls.getConstructor(new Class[0]);
    }
    public static void main(final String[] args) throws Exception {
        try {
            for (int i = 0; i < 100000; i++) {
                Constructor ctor = test(Class.forName("compiler.c2.Test6792161"));
            }
        } catch (NoSuchMethodException e) {}
    }
}
