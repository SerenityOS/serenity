/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;

/**
 * A utility to copy the class and all it's inner classes to the specified directory.
 * <p>
 * Usage in jtreg:
 *
 * @build CopyClassFile
 * @run main CopyClassFile package.class dest_directory
 *
 * In case the source file should be removed add -r option
 */
public class CopyClassFile {

    private static final ClassLoader cl = CopyClassFile.class.getClassLoader();

    private static String destinationDir;
    private static String className;
    private static String classFile;

    private static boolean removeSource = false;

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            throw new IllegalArgumentException("Illegal usage: class name and destination directory should be specified");
        }

        int classNameIndex = parseOptions(args);

        className = args[classNameIndex];
        destinationDir = args[classNameIndex + 1];
        classFile = className.replaceAll("\\.", File.separator) + ".class";

        URL url = cl.getResource(classFile);
        if (url == null) {
            throw new RuntimeException("Could not find a class: " + classFile);
        }

        File[] files = new File(url.toURI())
                .getParentFile()
                .listFiles((dir, name) -> name.startsWith(cutPackageName(className)) && name.endsWith(".class"));

        Arrays.stream(files).forEach(CopyClassFile::copyFile);
    }

    private static int parseOptions(String[] args) {
        int optionsEnd = 0;
        while (args[optionsEnd].startsWith("-")) {
            switch (args[optionsEnd].substring(1)) {
                case "r" :
                    removeSource = true;
                    break;
                default:
                    throw new RuntimeException("Unrecognized option passed to CopyClassFile: " + args[optionsEnd]);
            }
            optionsEnd++;
        }
        return optionsEnd;
    }

    private static String cutPackageName(String className) {
        int dotIndex = className.lastIndexOf(".") + 1;
        if (dotIndex <= 0) {
            return className;
        } else {
            return className.substring(dotIndex);
        }
    }

    private static void copyFile(File f) {
        try {
            Path classFilePath = Paths.get(classFile);
            String packagePath = classFilePath.getParent() == null ? "" : classFilePath.getParent().toString();
            Path p = Paths.get(destinationDir + packagePath + File.separator + f.getName());
            Files.createDirectories(p.getParent());
            try (InputStream is = new FileInputStream(f)) {
                Files.copy(is, p, StandardCopyOption.REPLACE_EXISTING);
            }

            if (removeSource && !f.delete()) {
                throw new RuntimeException("Failed to delete a file");
            }

        } catch (IOException ex) {
            throw new RuntimeException("Could not copy file " + f, ex);
        }
    }
}
