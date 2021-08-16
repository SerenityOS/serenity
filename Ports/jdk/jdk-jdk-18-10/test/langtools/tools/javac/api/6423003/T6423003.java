/*
 * Copyright (c) 2006, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     6423003
 * @summary JSR 199: confusing help message with compiler API
 * @author  Peter von der Ah\u00e9
 * @library ../lib
 * @modules java.compiler
 *          jdk.compiler
 * @build ToolTester
 * @compile T6423003.java
 * @run main T6423003
 */

import java.io.IOException;
import java.util.Arrays;

public class T6423003 extends ToolTester {
    void test(String... args) {
        task = tool.getTask(null, fm, null, Arrays.asList("-Xlint:all"), null, null);
        try {
            task.call();
        } catch (IllegalStateException ex) {
            return;
        }
        throw new AssertionError("Expected IllegalStateException not thrown");
    }
    public static void main(String... args) throws IOException {
        try (T6423003 t = new T6423003()) {
            t.test(args);
        }
    }
}
