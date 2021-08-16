/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8020968
 * @summary Sanity test for Function wildcard signature
 * @run main WalkFunction
 */

import java.lang.StackWalker.StackFrame;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

public class WalkFunction {
    private static final StackWalker walker = StackWalker.getInstance();

    public static void main(String... args) throws Exception {
        testFunctions();
        testWildcards();
        walker.walk(counter());
        walker.walk(wildCounter());
    }

    private static void testFunctions() {
        walker.walk(Stream::count);

        try {
            walker.walk(null);
            throw new RuntimeException("NPE expected");
        } catch (NullPointerException e) {}

        Optional<StackFrame> result = walker.walk(WalkFunction::reduce);
        if (!result.get().getClassName().equals(WalkFunction.class.getName())) {
            throw new RuntimeException(result.get() + " expected: " + WalkFunction.class.getName());
        }
    }

    static Optional<StackFrame> reduce(Stream<StackFrame> stream) {
        return stream.reduce((r,f) -> r.getClassName().compareTo(f.getClassName()) > 0 ? f : r);
    }

    private static void testWildcards() {
        Function<? super Stream<? extends  StackFrame>, Void> f1 = WalkFunction::function;
        Function<? super Stream<? super StackFrame>, Void> f2 = WalkFunction::function;
        Function<? super Stream<StackFrame>, Void> f3 = WalkFunction::function;
        Function<Stream<? extends StackFrame>, Void> f4 = WalkFunction::function;
        Function<Stream<? super StackFrame>, Void> f5 = WalkFunction::function;
        Function<Stream<StackFrame>, Void> f6 = WalkFunction::function;
        walker.walk(f1);
        walker.walk(f2);
        walker.walk(f3);
        walker.walk(f4);
        walker.walk(f5);
        walker.walk(f6);
    }

    private static Void function(Stream<?> s) {
        return null;
    }

    private static Function<Stream<?>, Long> wildCounter() {
        return Stream::count;
    }
    private static <T> Function<Stream<T>, Long> counter() {
        return Stream::count;
    }
}
