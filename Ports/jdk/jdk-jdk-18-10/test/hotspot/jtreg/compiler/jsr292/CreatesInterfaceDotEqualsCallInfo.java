/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8026124
 * @summary MethodHandle lookup for an interface method causes assertion failure in linkResolver.cpp
 *
 * @run main/othervm compiler.jsr292.CreatesInterfaceDotEqualsCallInfo
 */

package compiler.jsr292;

import java.lang.invoke.MethodHandles;
import java.nio.file.Path;

public class CreatesInterfaceDotEqualsCallInfo {
    public static void main(String[] args) throws Throwable {
        MethodHandles.publicLookup()
            .unreflect(Path.class.getMethod("toString", new Class[]{}))
            .invoke(Path.of("."));

        System.out.println("PASS, did not crash calling interface method handle");
    }
}
