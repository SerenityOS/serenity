/*
 * Copyright (c) 2002, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4736959
 * @summary Make sure to parse "PM" (only) and produce the correct value.
 */

import java.text.*;
import java.util.*;

@SuppressWarnings("deprecation")
public class Bug4736959 {
    /**
     * 4736959: JSpinner won't work for AM/PM field
     */
    public static void main(String[] args) {
        SimpleDateFormat f = new SimpleDateFormat("a", Locale.US);

        Date d1 = f.parse("AM", new ParsePosition(0));
        System.out.println("d1: " + d1);
        if (d1.getHours() != 0) {
            throw new RuntimeException("Parsing \"AM\": expected 0 (midnight), got " +
                                       d1.getHours());
        }
        Date d2 = f.parse("PM", new ParsePosition(0));
        System.out.println("d2: " + d2);
        if (d2.getHours() != 12) {
            throw new RuntimeException("Parsing \"PM\": expected 12 (noon), got " +
                                       d2.getHours());
        }
    }
}
