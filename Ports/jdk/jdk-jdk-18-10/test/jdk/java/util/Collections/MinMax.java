/*
 * Copyright (c) 2001, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4486049
 * @summary min and max methods fail if size changes in between a call to size
 *           and an attempt to iterate.
 * @author Josh Bloch
 */

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public class MinMax {
    public static void main(String[] args) {
        Set s = new LyingSet();
        s.add("x");
        if (!Collections.min(s).equals("x"))
            throw new RuntimeException("1: " + Collections.min(s));
        if (!Collections.max(s).equals("x"))
            throw new RuntimeException("2: " + Collections.max(s));

        s.add("y");
        if (!Collections.min(s).equals("x"))
            throw new RuntimeException("3: " + Collections.min(s));
        if (!Collections.max(s).equals("y"))
            throw new RuntimeException("4: " + Collections.max(s));

        s.add("w");
        if (!Collections.min(s).equals("w"))
            throw new RuntimeException("5: " + Collections.min(s));
        if (!Collections.max(s).equals("y"))
            throw new RuntimeException("6: " + Collections.max(s));

        s.clear();
        s.add("x");
        if (!Collections.min(s, Collections.reverseOrder()).equals("x"))
            throw new RuntimeException("1a: " + Collections.min(s));
        if (!Collections.max(s, Collections.reverseOrder()).equals("x"))
            throw new RuntimeException("2a: " + Collections.max(s));

        s.add("y");
        if (!Collections.min(s, Collections.reverseOrder()).equals("y"))
            throw new RuntimeException("3a: " + Collections.min(s));
        if (!Collections.max(s, Collections.reverseOrder()).equals("x"))
            throw new RuntimeException("4a: " + Collections.max(s));

        s.add("w");
        if (!Collections.min(s, Collections.reverseOrder()).equals("y"))
            throw new RuntimeException("5a: " + Collections.min(s));
        if (!Collections.max(s, Collections.reverseOrder()).equals("w"))
            throw new RuntimeException("6a: " + Collections.max(s));
    }
}

class LyingSet extends LinkedHashSet {
    public int size() {
        return super.size() + 1; // Lies, lies, all lies!
    }
}
