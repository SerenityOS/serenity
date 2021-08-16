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

/*
 * @test
 * @bug 4124769 8160312
 * @summary Test ignore-case comparison
 *
 */

import java.net.*;
import java.io.InputStream;
import java.io.IOException;

public class CompareIC {

    public static void main(String[] args) throws Exception {
        String test1 = "Tess";
        String test2 = "Test";
        String test3 = "Tesu";
        CompareIC comparer = new CompareIC();

        comparer.testTriplet(test1, test2, test3);
        test2 = test2.toUpperCase();
        comparer.testTriplet(test1, test2, test3);
        test2 = test2.toLowerCase();
        comparer.testTriplet(test1, test2, test3);

        // toLowerCase -> non-latin1
        if ("\u00b5".compareToIgnoreCase("X") < 0)
            throw new RuntimeException("Comparison failure1");
    }

    private void testTriplet(String one, String two, String three)
        throws Exception {
            if (one.compareToIgnoreCase(two) > 0)
                throw new RuntimeException("Comparison failure1");
            if (two.compareToIgnoreCase(three) > 0)
                throw new RuntimeException("Comparison failure2");
            if (three.compareToIgnoreCase(one) < 0)
                throw new RuntimeException("Comparison failure3");
    }

}
