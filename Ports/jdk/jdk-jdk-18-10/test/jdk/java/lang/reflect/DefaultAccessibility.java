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
 * @bug 6648344
 * @summary Test that default accessibility is false
 * @author Joseph D. Darcy
 */

import java.lang.reflect.*;

public class DefaultAccessibility {
    private DefaultAccessibility() {
        super();
    }

    private static int f = 42;

    public static void main(String... args) throws Exception {
        Class<?> daClass = (new DefaultAccessibility()).getClass();

        int elementCount = 0;
        for(Constructor<?> ctor : daClass.getDeclaredConstructors()) {
            elementCount++;
            if (ctor.isAccessible())
                throw new RuntimeException("Unexpected accessibility for constructor " +
                                           ctor);
        }

        for(Method method : daClass.getDeclaredMethods()) {
            elementCount++;
            if (method.isAccessible())
                throw new RuntimeException("Unexpected accessibility for method " +
                                           method);
        }

        for(Field field : daClass.getDeclaredFields()) {
            elementCount++;
            if (field.isAccessible())
                throw new RuntimeException("Unexpected accessibility for field " +
                                           field);
        }

        if (elementCount < 3)
            throw new RuntimeException("Expected at least three members; only found " +
                                       elementCount);
    }
}
