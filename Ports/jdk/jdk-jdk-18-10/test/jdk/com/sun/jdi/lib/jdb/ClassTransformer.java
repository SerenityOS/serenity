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

package lib.jdb;

import jdk.test.lib.compiler.CompilerUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

// ClassTransformer provides functionality to transform java source and compile it.
// We cannot use InMemoryJavaCompiler as test files usually contain 2 classes (the test itself and debuggee)
// and InMemoryJavaCompiler cannot compile them.
public class ClassTransformer {

    private final List<String> lines;
    private String fileName;
    private String workDir = "ver{0}";
    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

    private ClassTransformer(List<String> lines) {
        this.lines = lines;
    }

    public ClassTransformer setFileName(String fileName) {
        this.fileName = fileName;
        return this;
    }

    // workDir is a MessageFormat pattern, id (int) is an {0} arg of the pattern.
    // can be relative (relatively "scratch" dir) or absolute.
    public ClassTransformer setWorkDir(String dir) {
        workDir = dir;
        return this;
    }

    public static ClassTransformer fromString(String content) {
        return new ClassTransformer(Arrays.asList(content.split("\\R")));
    }

    public static ClassTransformer fromFile(Path filePath) {
        try {
            return new ClassTransformer(Files.readAllLines(filePath))
                    .setFileName(filePath.getFileName().toString());
        } catch (IOException e) {
            throw new RuntimeException("failed to read " + filePath, e);
        }
    }
    public static ClassTransformer fromFile(String filePath) {
        return fromFile(Paths.get(filePath));
    }

    public static ClassTransformer fromTestSource(String fileName) {
        return fromFile(Paths.get(System.getProperty("test.src")).resolve(fileName));
    }

    // returns path to the .class file of the transformed class
    public String transform(int id, String className, String... compilerOptions) {
        Path subdir = Paths.get(".").resolve(MessageFormat.format(workDir, id));
        Path transformedSrc = subdir.resolve(fileName);
        try {
            Files.createDirectories(subdir);
            Files.write(transformedSrc, transform(id).getBytes());
        } catch (IOException e) {
            throw new RuntimeException("failed to write transformed " + transformedSrc, e);
        }
        try {
            // need to add extra classpath args
            List<String> args = new LinkedList<>(Arrays.asList(compilerOptions));
            args.add("-cp");
            args.add(System.getProperty("java.class.path"));
            CompilerUtils.compile(subdir, subdir, false, args.toArray(new String[args.size()]));
        } catch (IOException e) {
            throw new RuntimeException("failed to compile " + transformedSrc, e);
        }
        return subdir.resolve(className + ".class").toString();
    }

    /*
    * To do RedefineClasses operations, embed @1 tags in the .java
    * file to tell this script how to modify it to produce the 2nd
    * version of the .class file to be used in the redefine operation.
    * Here are examples of each editing tag and what change
    * it causes in the new file.  Note that blanks are not preserved
    * in these editing operations.
    *
    * @1 uncomment
    *  orig:   // @1 uncomment   gus = 89;
    *  new:         gus = 89;
    *
    * @1 commentout
    *  orig:   gus = 89      // @1 commentout
    *  new: // gus = 89      // @1 commentout
    *
    * @1 delete
    *  orig:  gus = 89      // @1 delete
    *  new:   entire line deleted
    *
    * @1 newline
    *  orig:  gus = 89;     // @1 newline gus++;
    *  new:   gus = 89;     //
    *         gus++;
    *
    * @1 replace
    *  orig:  gus = 89;     // @1 replace gus = 90;
    *  new:   gus = 90;
    */
    public String transform(int id) {
        Pattern delete = Pattern.compile("@" + id + " *delete");
        Pattern uncomment = Pattern.compile("// *@" + id + " *uncomment (.*)");
        Pattern commentout = Pattern.compile(".* @" + id + " *commentout");
        Pattern newline = Pattern.compile("(.*) @" + id + " *newline (.*)");
        Pattern replace = Pattern.compile("@" + id + " *replace (.*)");
        return lines.stream()
                .filter(s -> !delete.matcher(s).find())     // @1 delete
                .map(s -> {
                    Matcher m = uncomment.matcher(s);       // @1 uncomment
                    return m.find() ? m.group(1) : s;
                })
                .map(s-> {
                    Matcher m = commentout.matcher(s);      // @1 commentout
                    return m.find() ? "//" + s : s;
                })
                .map(s -> {
                    Matcher m = newline.matcher(s);         // @1 newline
                    return m.find() ? m.group(1) + LINE_SEPARATOR + m.group(2) : s;
                })
                .map(s -> {
                    Matcher m = replace.matcher(s);         // @1 replace
                    return m.find() ? m.group(1) : s;
                })
                .collect(Collectors.joining(LINE_SEPARATOR));
    }

}
