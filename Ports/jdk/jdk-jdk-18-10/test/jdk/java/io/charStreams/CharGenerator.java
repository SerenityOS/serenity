/*
 * Copyright (c) 1997, Oracle and/or its affiliates. All rights reserved.
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

/**/

import java.io.*;


public class CharGenerator {

    private IntGenerator r;
    int min = 0, max = 0xffff;

    public CharGenerator(IntGenerator r) {
        this.r = r;
    }

    public CharGenerator(IntGenerator r, int min, int max) {
        this.r = r;
        this.min = min;
        this.max = max;
    }

    public char next() {
        char c;
        do
            c = (char) (r.next(max - min) + min);
        while ((c == '\r') || (c == '\n')
               || ((c >= 0xd800) && (c <= 0xdfff))
               || (c == 0xfffe));
        return c;
    }
}
