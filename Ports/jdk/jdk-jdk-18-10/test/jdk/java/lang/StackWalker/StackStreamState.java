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
 * @summary Basic test for Stream<StackFrame> state
 * @run main StackStreamState
 */

import java.lang.StackWalker.StackFrame;
import java.util.stream.Stream;

public class StackStreamState {
    public static void main(String... args) {
        StackStreamState test = new StackStreamState();
        test.testStatic();
        test.testInstance();
        test.testLocal();
    }

    private static Stream<StackFrame> staticStream;
    private Stream<StackFrame> instanceStream;
    private final StackWalker walker = StackWalker.getInstance();
    void testStatic() {
        walker.walk(s -> {
            staticStream = s;
            return null;
        });
        checkStreamState(staticStream);
    }
    void testInstance() {
        walker.walk(s -> {
            instanceStream = s;
            return null;
        });
        checkStreamState(instanceStream);
    }
    void testLocal() {
        Stream<StackFrame> stream = walker.walk(s -> {
            return s;
        });
        checkStreamState(stream);
    }
    void checkStreamState(Stream<StackFrame> stream) {
        try {
            stream.count();
            throw new RuntimeException("IllegalStateException not thrown");
        } catch (IllegalStateException e) {
            System.out.println("Got expected IllegalStateException: " + e.getMessage());
            e.printStackTrace(System.out);
        }
    }
}
