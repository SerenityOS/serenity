/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     8010310
 * @summary Error processing sources with -private
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 */

import java.io.File;

public class Test {
    public static void main(String... args) throws Exception {
        File testSrc = new File(System.getProperty("test.src"));
        String[] jdoc_args = {
            "-d", "out",
            new File(testSrc, Test.class.getSimpleName() + ".java").getPath()
        };
        int rc = jdk.javadoc.internal.tool.Main.execute(jdoc_args);
        if (rc != 0)
            throw new Exception("unexpected return code from javadoc: " + rc);
    }

    static int array[] = { 1, 2, 3};
    static int method(int p) { return p; }
    static int value = 0;

    public int not_static_not_final = 1;
    public static int static_not_final = 2;
    public final int not_static_final = 3;
    public static final int static_final = 4;

    public static final int array_index = array[0];
    public static final int method_call = method(0);
    public static final int inner_class = new Test() { }.method(0);
    public static final int new_class = new Test().method(0);
    public static final int pre_inc = ++value;
    public static final int pre_dec = --value;
    public static final int post_inc = value++;
    public static final int post_dec = value--;
}

