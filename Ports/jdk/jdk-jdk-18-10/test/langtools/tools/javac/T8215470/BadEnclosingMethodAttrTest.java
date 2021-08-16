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
 * @bug 8215470
 * @summary Bad EnclosingMethod attribute on classes declared in lambdas
 * @modules jdk.compiler/com.sun.tools.javac.util
 */

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import com.sun.tools.javac.util.Assert;

public class BadEnclosingMethodAttrTest<T> {
    protected BadEnclosingMethodAttrTest() {
        Assert.check(getClass().getEnclosingMethod().toString().equals("static void BadEnclosingMethodAttrTest.lambdaScope(java.lang.Object)"));
        Type typeFromEnclosingMethod = getClass().getEnclosingMethod().getGenericParameterTypes()[0];
        ParameterizedType paramType = (ParameterizedType) getClass().getGenericSuperclass();
        Type typeFromGenericClass = paramType.getActualTypeArguments()[0];
        Assert.check(typeFromEnclosingMethod.equals(typeFromGenericClass));
    }

    static <X> void lambdaScope(X x) {
        Runnable r = () -> {
            new BadEnclosingMethodAttrTest<X>() {};
        };
        r.run();
    }

    public static void main(final String[] args) {
        lambdaScope("");
    }
}
