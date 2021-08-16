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
 * @bug     6420409
 * @summary JSR 199: StandardFileManager: cannot set CLASS_PATH location
 * @author  Peter von der Ah\u00e9
 * @modules java.compiler
 *          jdk.compiler
 */

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Locale;
import javax.tools.JavaFileManager.Location;
import javax.tools.*;

import static javax.tools.StandardLocation.CLASS_PATH;
import static javax.tools.StandardLocation.SOURCE_PATH;
import static javax.tools.StandardLocation.CLASS_OUTPUT;

public class T6420409 {
    static final File test_src     = new File(System.getProperty("test.src"));
    static final File test_classes = new File(System.getProperty("test.classes"));

    public static void main(String... args) throws IOException {
        final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        try (StandardJavaFileManager fm = tool.getStandardFileManager(null, null, null)) {
            fm.setLocation(SOURCE_PATH,  Arrays.asList(test_classes)); // switcheroo !!!
            fm.setLocation(CLASS_PATH,   Arrays.asList(test_src));
            fm.setLocation(CLASS_OUTPUT, Arrays.asList(test_classes));
            final Iterable<? extends JavaFileObject> compilationUnits =
                fm.getJavaFileObjectsFromFiles(Arrays.asList(new File(test_src, "T6420409.java")));
            tool.getTask(null,
                         fm,
                         null,
                         Arrays.asList("-proc:none"),
                         null,
                         compilationUnits).call();
            test(fm.getLocation(CLASS_PATH),   test_src,     CLASS_PATH);
            test(fm.getLocation(SOURCE_PATH),  test_classes, SOURCE_PATH);
            test(fm.getLocation(CLASS_OUTPUT), test_classes, CLASS_OUTPUT);
        }
    }

    static void test(Iterable<? extends File> path, File file, Location location) {
        Iterator<? extends File> it = path.iterator();
        if (!it.next().equals(file))
            throw new AssertionError(file + " not in " + location);
        if (it.hasNext())
            throw new AssertionError("Unexpected element in "  + location + " : " + it.next());
        System.err.format((Locale)null, "OK: %s: %s%n", location, path);
    }
}
