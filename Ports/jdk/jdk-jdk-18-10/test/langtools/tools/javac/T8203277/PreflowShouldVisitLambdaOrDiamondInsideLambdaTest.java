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

/*
 * @test
 * @bug 8203277
 * @summary preflow visitor used during lambda attribution shouldn't visit class definitions inside the lambda body
 * @compile PreflowShouldVisitLambdaOrDiamondInsideLambdaTest.java
 */

import java.util.List;
import java.util.function.Function;

class PreflowShouldVisitLambdaOrDiamondInsideLambdaTest {
    void build() {
        List<Function<String, Double>> list1 = transform(null,
                builder -> new Function<>() {
                    public Double apply(String params) { return null; }
                });

        List<Function<String, Double>> list2 = transform(null,
                builder -> arg -> null);
    }

    static <Argument, Result> List<Result> transform(List<Argument> fromList,
            Function<? super Argument,? extends Result> function) { return null; }
}
