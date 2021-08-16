/*
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates. All rights reserved.
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

package gc.g1.unloading;

import jdk.test.lib.Utils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Uses {@link gc.g1.unloading.GenClassPoolJar} to build {@code classPool.jar}
 * in current directory.
 */
public class GenClassesBuilder {
    public static void main(String[] args) {
        Path template = Paths.get(Utils.TEST_ROOT)
                             .resolve("vmTestbase")
                             .resolve("gc")
                             .resolve("g1")
                             .resolve("unloading")
                             .resolve("ClassNNN.java.template")
                             .toAbsolutePath();
        Path dir = Paths.get(".").toAbsolutePath();
        String count = "1000";
        if (Files.notExists(template)) {
            throw new Error("can't find template file: " + template);
        }
        try {
            GenClassPoolJar.main(new String[]{template.toString(), dir.toString(), count});
        } catch (Exception e) {
            throw new Error("can't generate classPool.jar", e);
        }
    }
}


