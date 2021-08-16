/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Wrapper {
    public static void main(String... args) throws Exception {
        if (!isSJavacOnClassPath()) {
            System.out.println("sjavac not available: pass by default");
            return;
        }

        String testClassName = args[0];
        String[] testArgs = Arrays.copyOfRange(args, 1, args.length);

        Path srcDir = Paths.get(System.getProperty("test.src"));
        Path clsDir = Paths.get(System.getProperty("test.classes"));
        String clsPath = System.getProperty("test.class.path");
        String tstMdls = System.getProperty("test.modules");

        Path src = srcDir.resolve(testClassName + ".java");
        Path cls = clsDir.resolve(testClassName + ".class");

        if (isNewer(src, cls)) {
            System.err.println("Recompiling test class...");
            List<String> javacArgs = new ArrayList<>();
            javacArgs.addAll(Arrays.asList("-d", clsDir.toString()));
            javacArgs.addAll(Arrays.asList("-sourcepath", srcDir.toString()));
            javacArgs.addAll(Arrays.asList("-classpath", clsPath));
            Arrays.stream(tstMdls.split("\\s+"))
                .filter(s -> s.contains("/"))
                .map(s -> "--add-exports=" + s + "=ALL-UNNAMED")
                .collect(Collectors.toCollection(() -> javacArgs));
            javacArgs.add(src.toString());
            System.out.println("javac: " + javacArgs);
            int rc = com.sun.tools.javac.Main.compile(
                javacArgs.toArray(new String[javacArgs.size()]));
            if (rc != 0)
                throw new Exception("compilation failed");
        }

        Class<?> sjavac = Class.forName(testClassName);
        Method main = sjavac.getMethod("main", String[].class);
        main.invoke(null, new Object[] { testArgs });
    }

    private static boolean isNewer(Path a, Path b) throws IOException {
        if (Files.notExists(b))
            return true;
        return Files.getLastModifiedTime(a).compareTo(Files.getLastModifiedTime(b)) > 0;
    }

    private static boolean isSJavacOnClassPath() {
        String cls = "com/sun/tools/sjavac/Main.class";
        return Wrapper.class.getClassLoader().getResource(cls) != null;
    }
}
