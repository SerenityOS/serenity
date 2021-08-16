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
 * @summary Basic test for hidden frames
 * @run main HiddenFrames
 */

import java.lang.StackWalker.Option;
import java.lang.StackWalker.StackFrame;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class HiddenFrames {
    public static void main(String... args) throws Exception {
        new HiddenFrames().test();
        new HiddenFrames(Option.SHOW_REFLECT_FRAMES).test();
        new HiddenFrames(Option.SHOW_HIDDEN_FRAMES).test();
    }

    private final Option option;
    private final StackWalker walker;
    private final List<StackFrame> lambdas = new ArrayList<>();
    private final List<StackFrame> reflects = new ArrayList<>();

    HiddenFrames() {
        this.option = null;
        this.walker = StackWalker.getInstance();
    }
    HiddenFrames(Option option) {
        this.option = option;
        this.walker = StackWalker.getInstance(option);
    }

    void test() throws Exception {
        walk();
        walkFromReflection();
    }

    void walk() {
       Stream.of(0).forEach(i -> walker.walk(s ->
       {
           s.forEach(this::checkFrame);
           return null;
       }));

        // only check hidden frames but not reflection frames
        // walk is not invoked via reflection
        if (option == null && !lambdas.isEmpty()) {
            throw new RuntimeException("Hidden frames are shown");
        }

        if (option == Option.SHOW_HIDDEN_FRAMES && lambdas.isEmpty()) {
            throw new RuntimeException("No hidden Lambda frame");
        }
    }

    void walkFromReflection() throws Exception {
        Method m = HiddenFrames.class.getDeclaredMethod("walk");
        m.invoke(this);

        if (option == null && !lambdas.isEmpty()) {
            throw new RuntimeException("Hidden frames are shown");
        }

        if (option == Option.SHOW_HIDDEN_FRAMES && lambdas.isEmpty()) {
            throw new RuntimeException("No hidden Lambda frame");
        }

        if (option != null && reflects.isEmpty()) {
            throw new RuntimeException("No reflection frame");
        }
    }

    void checkFrame(StackFrame frame) {
        String cn = frame.getClassName();
        if (cn.startsWith("java.lang.reflect.") || cn.startsWith("jdk.internal.reflect.")) {
            reflects.add(frame);
        }
        if (cn.contains("$$Lambda$")) {
            lambdas.add(frame);
        }
    }
}
