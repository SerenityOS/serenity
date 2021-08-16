/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8196618
 * @summary Check that JDKPlatformProvider.NUMERICAL_COMPARATOR works correctly
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.platform
 * @run main NumericalComparatorTest
 */

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import com.sun.tools.javac.platform.JDKPlatformProvider;

public class NumericalComparatorTest {

    public static void main(String... args) throws IOException {
        new NumericalComparatorTest().run();
    }

    void run() throws IOException {
        doTest(List.of("8", "10", "11", "9", "b1", "a1", "a2"),
               List.of("8", "9", "10", "11", "a1", "a2", "b1"));
    }

    void doTest(List<String> input, List<String> expectedOutput) {
        List<String> actual = input.stream()
                                   .sorted(JDKPlatformProvider.NUMERICAL_COMPARATOR)
                                   .collect(Collectors.toList());
        if (!expectedOutput.equals(actual))
            throw new AssertionError("Unexpected output: " + actual);
    }

}
