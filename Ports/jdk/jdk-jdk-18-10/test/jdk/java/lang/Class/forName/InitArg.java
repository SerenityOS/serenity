/*
 * Copyright (c) 1998, Oracle and/or its affiliates. All rights reserved.
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

/* @test
   @bug 4131701
   @summary This is a basic sanity test for the Class.forName
            variant that the 'whether-initialize' arg.
 */

class x123 {
    static {
        InitArg.x123Initialized = true;
    }
}

public class InitArg {

    public static boolean x123Initialized = false;

    public static void main(String[] args) throws Exception {
        Class c = Class.forName("x123", false,
                                InitArg.class.getClassLoader());
        if (x123Initialized) {
            throw new Exception("forName should not run initializer");
        }
        Class d = Class.forName("x123", true,
                                InitArg.class.getClassLoader());
        if (!x123Initialized) {
            throw new Exception("forName not running initializer");
        }
    }
}
