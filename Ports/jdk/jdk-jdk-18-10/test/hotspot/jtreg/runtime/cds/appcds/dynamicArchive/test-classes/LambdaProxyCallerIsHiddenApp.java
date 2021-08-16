/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import static java.lang.invoke.MethodHandles.Lookup.ClassOption.*;
import jdk.test.lib.compiler.InMemoryJavaCompiler;
import jdk.internal.misc.Unsafe;

public class LambdaProxyCallerIsHiddenApp {

    static byte klassbuf[] = InMemoryJavaCompiler.compile("LambdaHello",
        "public class LambdaHello { " +
        "    static Runnable run; " +
        "    static { " +
        "        run = LambdaHello::myrun; " +
        "    } " +
        "    static void myrun() { " +
        "        System.out.println(\"Hello\"); " +
        "    } " +
        "} ");

    public static void main(String args[]) throws Exception {
        Lookup lookup = MethodHandles.lookup();
        System.out.println("lookup: " + lookup);

        Class<?> hiddenClass = lookup.defineHiddenClass(klassbuf, true, NESTMATE, STRONG).lookupClass();
        System.out.println("hiddenClass: " + hiddenClass);
        Object o = hiddenClass.newInstance();
        System.out.println("o: " + o);
    }
}
