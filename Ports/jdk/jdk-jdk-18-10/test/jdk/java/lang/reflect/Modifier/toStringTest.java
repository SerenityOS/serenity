/*
 * Copyright (c) 2001, 2014, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4394937 8051382
 * @summary tests the toString method of reflect.Modifier
 */

import java.lang.reflect.Modifier;

public class toStringTest {

    static void testString(int test, String expected) {
        if(!Modifier.toString(test).equals(expected))
            throw new RuntimeException(test +
                                          " yields incorrect toString result");
    }

    public static void main(String [] argv) {
        int allMods = Modifier.PUBLIC | Modifier.PROTECTED | Modifier.PRIVATE |
            Modifier.ABSTRACT | Modifier.STATIC | Modifier.FINAL |
            Modifier.TRANSIENT | Modifier.VOLATILE | Modifier.SYNCHRONIZED |
            Modifier.NATIVE | Modifier.STRICT | Modifier.INTERFACE;

        String allModsString = "public protected private abstract static " +
            "final transient volatile synchronized native strictfp interface";

        /* zero should have an empty string */
        testString(0, "");

        /* test to make sure all modifiers print out in the proper order */
        testString(allMods, allModsString);

        /* verify no extraneous modifiers are printed */
        testString(~0, allModsString);
    }
}
