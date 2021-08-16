/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.beans.util.Cache;

/*
 * @test
 * @bug 8039137
 * @summary Tests Cache recursion
 * @author Sergey Malenkov
 * @modules java.desktop/com.sun.beans.util
 * @compile -XDignore.symbol.file TestCacheRecursion.java
 * @run main TestCacheRecursion
 */

public class TestCacheRecursion {
    private static boolean ERROR;
    private static final Cache<Class<?>,Boolean> CACHE
            = new Cache<Class<?>,Boolean>(Cache.Kind.WEAK, Cache.Kind.STRONG) {
        @Override
        public Boolean create(Class<?> type) {
            if (ERROR) {
                throw new Error("not initialized");
            }
            type = type.getSuperclass();
            return (type != null) && get(type);
        }
    };

    public static void main(String[] args) {
        CACHE.get(Z.class);
        ERROR = true;
        for (Class<?> type = Z.class; type != null; type = type.getSuperclass()) {
            CACHE.get(type);
        }
    }

    private class A {}
    private class B extends A {}
    private class C extends B {}
    private class D extends C {}
    private class E extends D {}
    private class F extends E {}
    private class G extends F {}
    private class H extends G {}
    private class I extends H {}
    private class J extends I {}
    private class K extends J {}
    private class L extends K {}
    private class M extends L {}
    private class N extends M {}
    private class O extends N {}
    private class P extends O {}
    private class Q extends P {}
    private class R extends Q {}
    private class S extends R {}
    private class T extends S {}
    private class U extends T {}
    private class V extends U {}
    private class W extends V {}
    private class X extends W {}
    private class Y extends X {}
    private class Z extends Y {}
}
