/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
package jdk.jpackage.internal;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Collection;
import java.util.Objects;
import java.util.Collections;
import java.util.Set;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Builds list of packages providing dynamic libraries for the given set of files.
 */
public final class LibProvidersLookup {
    static boolean supported() {
        return (new ToolValidator(TOOL_LDD).validate() == null);
    }

    public LibProvidersLookup() {
    }

    LibProvidersLookup setPackageLookup(PackageLookup v) {
        packageLookup = v;
        return this;
    }

    List<String> execute(Path root) throws IOException {
        // Get the list of files in the root for which to look up for needed shared libraries
        List<Path> allPackageFiles;
        try (Stream<Path> stream = Files.walk(root)) {
            allPackageFiles = stream.filter(Files::isRegularFile).filter(
                    LibProvidersLookup::canDependOnLibs).collect(
                    Collectors.toList());
        }

        Collection<Path> neededLibs = getNeededLibsForFiles(allPackageFiles);

        // Get the list of unique package names.
        List<String> neededPackages = neededLibs.stream().map(libPath -> {
            try {
                List<String> packageNames = packageLookup.apply(libPath).filter(
                        Objects::nonNull).filter(Predicate.not(String::isBlank)).distinct().collect(
                        Collectors.toList());
                Log.verbose(String.format("%s is provided by %s", libPath, packageNames));
                return packageNames;
            } catch (IOException ex) {
                // Ignore and keep going
                Log.verbose(ex);
                List<String> packageNames = Collections.emptyList();
                return packageNames;
            }
        }).flatMap(List::stream).sorted().distinct().toList();

        return neededPackages;
    }

    private static List<Path> getNeededLibsForFile(Path path) throws IOException {
        List<Path> result = new ArrayList<>();
        int ret = Executor.of(TOOL_LDD, path.toString()).setOutputConsumer(lines -> {
            lines.map(line -> {
                Matcher matcher = LIB_IN_LDD_OUTPUT_REGEX.matcher(line);
                if (matcher.find()) {
                    return matcher.group(1);
                }
                return null;
            }).filter(Objects::nonNull).map(Path::of).forEach(result::add);
        }).execute();

        if (ret != 0) {
            // objdump failed. This is OK if the tool was applied to not a binary file
            return Collections.emptyList();
        }

        return result;
    }

    private static Collection<Path> getNeededLibsForFiles(List<Path> paths) {
        // Depending on tool used, the set can contain full paths (ldd) or
        // only file names (objdump).
        Set<Path> allLibs = paths.stream().map(path -> {
            List<Path> libs;
            try {
                libs = getNeededLibsForFile(path);
            } catch (IOException ex) {
                Log.verbose(ex);
                libs = Collections.emptyList();
            }
            return libs;
        }).flatMap(List::stream).collect(Collectors.toSet());

        // `allLibs` contains names of all .so needed by files from `paths` list.
        // If there are mutual dependencies between binaries from `paths` list,
        // then names or full paths to these binaries are in `allLibs` set.
        // Remove these items from `allLibs`.
        Set<Path> excludedNames = paths.stream().map(Path::getFileName).collect(
                Collectors.toSet());
        Iterator<Path> it = allLibs.iterator();
        while (it.hasNext()) {
            Path libName = it.next().getFileName();
            if (excludedNames.contains(libName)) {
                it.remove();
            }
        }

        return allLibs;
    }

    private static boolean canDependOnLibs(Path path) {
        return path.toFile().canExecute() || path.toString().endsWith(".so");
    }

    @FunctionalInterface
    public interface PackageLookup {
        Stream<String> apply(Path path) throws IOException;
    }

    private PackageLookup packageLookup;

    private static final String TOOL_LDD = "ldd";

    //
    // Typical ldd output:
    //
    // ldd: warning: you do not have execution permission for `/tmp/jdk.jpackage17911687595930080396/images/opt/simplepackagetest/lib/runtime/lib/libawt_headless.so'
    //  linux-vdso.so.1 =>  (0x00007ffce6bfd000)
    //  libawt.so => /tmp/jdk.jpackage17911687595930080396/images/opt/simplepackagetest/lib/runtime/lib/libawt.so (0x00007f4e00c75000)
    //  libjvm.so => not found
    //  libjava.so => /tmp/jdk.jpackage17911687595930080396/images/opt/simplepackagetest/lib/runtime/lib/libjava.so (0x00007f4e00c41000)
    //  libm.so.6 => /lib64/libm.so.6 (0x00007f4e00834000)
    //  libdl.so.2 => /lib64/libdl.so.2 (0x00007f4e00630000)
    //  libc.so.6 => /lib64/libc.so.6 (0x00007f4e00262000)
    //  libjvm.so => not found
    //  libjvm.so => not found
    //  libverify.so => /tmp/jdk.jpackage17911687595930080396/images/opt/simplepackagetest/lib/runtime/lib/libverify.so (0x00007f4e00c2e000)
    //  /lib64/ld-linux-x86-64.so.2 (0x00007f4e00b36000)
    //  libjvm.so => not found
    //
    private static final Pattern LIB_IN_LDD_OUTPUT_REGEX = Pattern.compile(
            "^\\s*\\S+\\s*=>\\s*(\\S+)\\s+\\(0[xX]\\p{XDigit}+\\)");
}
