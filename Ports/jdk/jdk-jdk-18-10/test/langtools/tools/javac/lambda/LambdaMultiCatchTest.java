/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8036942
 * @summary javac generates incorrect exception table for multi-catch statements inside a lambda
 * @run main LambdaMultiCatchTest
 */

import java.io.IOException;
import java.util.function.Function;

public class LambdaMultiCatchTest {
    public static void main(String[] args) {
        Function<String,String> fi = x -> {
            String result = "nada";
            try {
                switch (x) {
                    case "IO":  throw new IOException();
                    case "Illegal": throw new IllegalArgumentException();
                    case "Run": throw new RuntimeException();
                }
            } catch (IOException|IllegalArgumentException ex) {
               result = "IO/Illegal";
            } catch (Exception ex) {
               result = "Any";
            };
            return result;
        };
        String val = fi.apply("Run");
        if (!val.equals("Any")) {
            throw new AssertionError("Fail: Expected 'Any' but got '" + val + "'");
        }
    }
}
