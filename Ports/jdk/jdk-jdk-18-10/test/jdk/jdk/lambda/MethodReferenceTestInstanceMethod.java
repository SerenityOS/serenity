/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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


import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

@Test(groups = "lib")
public class MethodReferenceTestInstanceMethod {
    public Stream<String> generate() {
        return Arrays.asList("one", "two", "three", "four", "five", "six")
            .stream()
            .filter(s->s.length() > 3)
            .map(s -> s.toUpperCase());
    }

    class Thingy<T,U> {
        U blah(Function<T, U> m, T val) {
            return m.apply(val);
        }
    }

    public void testStringBuffer() {
        String s = generate().collect(Collectors.joining());
        assertEquals(s, "THREEFOURFIVE");
    }

    public void testMRInstance() {
        Thingy<String,String> t = new Thingy<>();
        assertEquals(t.blah(String::toUpperCase, "frogs"), "FROGS");
    }

}
