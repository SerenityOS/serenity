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

/*
 * @test
 * @bug 8144580
 * @summary java.lang.AssertionError: Missing type variable in where clause: T
 * @compile -Xlint:unchecked RichFormatterWithAnnotationsTest.java
 */

import java.lang.annotation.*;
import java.util.*;

public class RichFormatterWithAnnotationsTest {

    @Target({ElementType.TYPE_USE})
    @interface NonNull { }

    public static <T> void test() {
        final Collection<@NonNull T> c = new LinkedList<>();
        final List<@NonNull String> l = new LinkedList<@NonNull String>((Collection<@NonNull String>) c) {
            // empty
        };
    }
}
