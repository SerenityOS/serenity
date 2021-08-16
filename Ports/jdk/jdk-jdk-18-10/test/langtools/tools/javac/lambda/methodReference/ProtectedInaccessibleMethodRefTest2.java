/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8234729 8242214
 * @summary Javac should eagerly change code generation for method references to avert IllegalAccessError in future.
 * @compile ProtectedInaccessibleMethodRefTest2.java
 * @run main ProtectedInaccessibleMethodRefTest2
 */

import pack.I;
import pack.J;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Function;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;

public final class ProtectedInaccessibleMethodRefTest2 extends I {

    public static void main(String... args) {
        ProtectedInaccessibleMethodRefTest2 m = new ProtectedInaccessibleMethodRefTest2();
        m.test(Paths.get("test"));
        // Verify that the method references have been folded into lambdas:
        Set<String> methodNames = new HashSet<>();
        for (Method meth : ProtectedInaccessibleMethodRefTest2.class.getDeclaredMethods()) {
            methodNames.add(meth.getName());
        }
        List<String> expectedMethods =
                Arrays.asList("lambda$test$0", "lambda$test$1", "lambda$test$2");
        if (!methodNames.containsAll(expectedMethods)) {
            throw new AssertionError("Did not find evidence of new code generation");
        }
    }

    void test(Path outputDir) {
        Sub c1 = new Sub(this::readFile);
        c1.check(outputDir);
        Sub c2 = new Sub(ProtectedInaccessibleMethodRefTest2::readFile, this);
        c2.check(outputDir);
        Sub c3 = new Sub(ProtectedInaccessibleMethodRefTest2::readFile2);
        c3.check(outputDir);
    }

    public class Sub extends J {
        Sub(Function<Path,String> fileReader) {
            super(fileReader);
        }
        Sub(BiFunction<ProtectedInaccessibleMethodRefTest2, Path,String> fileReader,
            ProtectedInaccessibleMethodRefTest2 instance) {
            super(p -> fileReader.apply(instance, p));
        }
    }
}
