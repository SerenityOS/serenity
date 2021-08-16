/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8010117
 * @summary Test CallerSensitiveFinder to find missing annotation
 * @modules java.base/jdk.internal.reflect
 *          jdk.jdeps/com.sun.tools.classfile
 *          jdk.jdeps/com.sun.tools.jdeps
 * @compile -XDignore.symbol.file MissingCallerSensitive.java
 * @build CallerSensitiveFinder
 * @run main MissingCallerSensitive
 */

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;
import jdk.internal.reflect.CallerSensitive;
import jdk.internal.reflect.Reflection;

public class MissingCallerSensitive {
    public static void main(String[] args) throws Exception {
        String testclasses = System.getProperty("test.classes", ".");

        Stream<Path> classes = Stream.of(Paths.get(testclasses, "MissingCallerSensitive.class"));

        CallerSensitiveFinder csfinder = new CallerSensitiveFinder();
        List<String> errors = csfinder.run(classes);
        if (errors.size() != 1) {
            throw new RuntimeException("Unexpected number of methods found: " + errors.size());
        }
        String m = errors.get(0);
        if (!m.startsWith("MissingCallerSensitive#missingCallerSensitiveAnnotation")) {
            throw new RuntimeException("Unexpected method missing annotation: " + m);
        }
    }

    @CallerSensitive
    public ClassLoader getCallerLoader() {
        Class<?> c = Reflection.getCallerClass();
        return c.getClassLoader();
    }

    public ClassLoader missingCallerSensitiveAnnotation() {
        Class<?> c = Reflection.getCallerClass();
        return c.getClassLoader();
    }
}
