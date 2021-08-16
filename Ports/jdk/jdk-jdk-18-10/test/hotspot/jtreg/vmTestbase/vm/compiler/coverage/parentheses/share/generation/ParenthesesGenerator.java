/*
 * Copyright (c) 2012, 2020, Oracle and/or its affiliates. All rights reserved.
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
package vm.compiler.coverage.parentheses.share.generation;

import java.util.Random;
import jdk.test.lib.Utils;


/**
 * This class generate rrandom ight parentheses sequence in string representation: "(())", "()(())" etc
 */
public class ParenthesesGenerator {
    private static Random random = Utils.getRandomInstance();

    public static String generate(int size) {
        if (size == 0) {
            return "";
        }
        if (random.nextBoolean()) {
            return "(" + generate(size - 1) + ")";
        } else {
            int splitPoint = random.nextInt(size + 1);
            return generate(splitPoint) + generate(size - splitPoint);
        }
    }
}
