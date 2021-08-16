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
 * @bug 4818598
 * @summary Tests Method value encoding
 * @run main/othervm -Djava.security.manager=allow java_lang_reflect_Method
 * @author Sergey Malenkov
 */

import java.lang.reflect.Method;

public final class java_lang_reflect_Method extends AbstractTest<Method> {
    public static void main(String[] args) {
        new java_lang_reflect_Method().test(true);
    }

    protected Method getObject() {
        try {
            return java_lang_reflect_Method.class.getMethod("m1");
        } catch (NoSuchMethodException exception) {
            throw new Error("unexpected exception", exception);
        }
    }

    protected Method getAnotherObject() {
        try {
            return java_lang_reflect_Method.class.getMethod("m2");
        } catch (NoSuchMethodException exception) {
            throw new Error("unexpected exception", exception);
        }
    }

    public void m1() {
    }

    public void m2() {
    }
}
