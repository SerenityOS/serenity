/*
 * Copyright (c) 2018, Google LLC. All rights reserved.
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

package com.sun.tools.javac.comp;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class Deduplication {
    void group(Object... xs) {}

    void test() {

        group(
                (Runnable) () -> { ( (Runnable) () -> {} ).run(); },
                (Runnable) () -> { ( (Runnable) () -> {} ).run(); }
        );

        group(
                (Runnable) () -> { Deduplication.class.toString(); },
                (Runnable) () -> { Deduplication.class.toString(); }
        );

        group(
                (Runnable) () -> { Integer[].class.toString(); },
                (Runnable) () -> { Integer[].class.toString(); }
        );

        group(
                (Runnable) () -> { char.class.toString(); },
                (Runnable) () -> { char.class.toString(); }
        );

        group(
                (Runnable) () -> { Void.class.toString(); },
                (Runnable) () -> { Void.class.toString(); }
        );

        group(
                (Runnable) () -> { void.class.toString(); },
                (Runnable) () -> { void.class.toString(); }
        );

        group((Function<String, Integer>) x -> x.hashCode());
        group((Function<Object, Integer>) x -> x.hashCode());

        {
            int x = 1;
            group((Supplier<Integer>) () -> x + 1);
        }
        {
            int x = 1;
            group((Supplier<Integer>) () -> x + 1);
        }
        group(
                (BiFunction<Integer, Integer, ?>) (x, y) -> x + ((y)),
                (BiFunction<Integer, Integer, ?>) (x, y) -> x + (y),
                (BiFunction<Integer, Integer, ?>) (x, y) -> x + y,
                (BiFunction<Integer, Integer, ?>) (x, y) -> (x) + ((y)),
                (BiFunction<Integer, Integer, ?>) (x, y) -> (x) + (y),
                (BiFunction<Integer, Integer, ?>) (x, y) -> (x) + y,
                (BiFunction<Integer, Integer, ?>) (x, y) -> ((x)) + ((y)),
                (BiFunction<Integer, Integer, ?>) (x, y) -> ((x)) + (y),
                (BiFunction<Integer, Integer, ?>) (x, y) -> ((x)) + y);

        group(
                (Function<Integer, Integer>) x -> x + (1 + 2 + 3),
                (Function<Integer, Integer>) x -> x + 6);

        group((Function<Integer, Integer>) x -> x + 1, (Function<Integer, Integer>) y -> y + 1);

        group((Consumer<Integer>) x -> this.f(), (Consumer<Integer>) x -> this.f());

        group((Consumer<Integer>) y -> this.g());

        group((Consumer<Integer>) x -> f(), (Consumer<Integer>) x -> f());

        group((Consumer<Integer>) y -> g());

        group((Function<Integer, Integer>) x -> this.i, (Function<Integer, Integer>) x -> this.i);

        group((Function<Integer, Integer>) y -> this.j);

        group((Function<Integer, Integer>) x -> i, (Function<Integer, Integer>) x -> i);

        group((Function<Integer, Integer>) y -> j);

        group(
                (Function<Integer, Integer>)
                        y -> {
                            while (true) {
                                break;
                            }
                            return 42;
                        },
                (Function<Integer, Integer>)
                        y -> {
                            while (true) {
                                break;
                            }
                            return 42;
                        });

        group(
                (Function<Integer, Integer>)
                        x -> {
                            int y = x;
                            return y;
                        },
                (Function<Integer, Integer>)
                        x -> {
                            int y = x;
                            return y;
                        });

        group(
                (Function<Integer, Integer>)
                        x -> {
                            int y = 0, z = x;
                            return y;
                        });
        group(
                (Function<Integer, Integer>)
                        x -> {
                            int y = 0, z = x;
                            return z;
                        });

        class Local {
            int i;

            void f() {}

            {
                group((Function<Integer, Integer>) x -> this.i);
                group((Consumer<Integer>) x -> this.f());
                group((Function<Integer, Integer>) x -> Deduplication.this.i);
                group((Consumer<Integer>) x -> Deduplication.this.f());
            }
        }

        group((Function<Integer, Integer>) x -> switch (x) { default: yield x; },
              (Function<Integer, Integer>) x -> switch (x) { default: yield x; });

        group((Function<Object, Integer>) x -> x instanceof Integer i ? i : -1,
              (Function<Object, Integer>) x -> x instanceof Integer i ? i : -1);
    }

    void f() {}

    void g() {}

    int i;
    int j;
}
