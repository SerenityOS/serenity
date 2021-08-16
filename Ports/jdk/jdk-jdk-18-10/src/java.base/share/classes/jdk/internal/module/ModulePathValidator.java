/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

package jdk.internal.module;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.module.FindException;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.net.URI;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * A validator to check for errors and conflicts between modules.
 */

class ModulePathValidator {
    private static final String MODULE_INFO = "module-info.class";
    private static final String INDENT = "    ";

    private final Map<String, ModuleReference> nameToModule;
    private final Map<String, ModuleReference> packageToModule;
    private final PrintStream out;

    private int errorCount;

    private ModulePathValidator(PrintStream out) {
        this.nameToModule = new HashMap<>();
        this.packageToModule = new HashMap<>();
        this.out = out;
    }

    /**
     * Scans and the validates all modules on the module path. The module path
     * comprises the upgrade module path, system modules, and the application
     * module path.
     *
     * @param out the print stream for output messages
     * @return the number of errors found
     */
    static int scanAllModules(PrintStream out) {
        ModulePathValidator validator = new ModulePathValidator(out);

        // upgrade module path
        String value = System.getProperty("jdk.module.upgrade.path");
        if (value != null) {
            Stream.of(value.split(File.pathSeparator))
                    .map(Path::of)
                    .forEach(validator::scan);
        }

        // system modules
        ModuleFinder.ofSystem().findAll().stream()
                .sorted(Comparator.comparing(ModuleReference::descriptor))
                .forEach(validator::process);

        // application module path
        value = System.getProperty("jdk.module.path");
        if (value != null) {
            Stream.of(value.split(File.pathSeparator))
                    .map(Path::of)
                    .forEach(validator::scan);
        }

        return validator.errorCount;
    }

    /**
     * Prints the module location and name.
     */
    private void printModule(ModuleReference mref) {
        mref.location()
                .filter(uri -> !isJrt(uri))
                .ifPresent(uri -> out.print(uri + " "));
        ModuleDescriptor descriptor = mref.descriptor();
        out.print(descriptor.name());
        if (descriptor.isAutomatic())
            out.print(" automatic");
        out.println();
    }

    /**
     * Prints the module location and name, checks if the module is
     * shadowed by a previously seen module, and finally checks for
     * package conflicts with previously seen modules.
     */
    private void process(ModuleReference mref) {
        String name = mref.descriptor().name();
        ModuleReference previous = nameToModule.putIfAbsent(name, mref);
        if (previous != null) {
            printModule(mref);
            out.print(INDENT + "shadowed by ");
            printModule(previous);
        } else {
            boolean first = true;

            // check for package conflicts when not shadowed
            for (String pkg :  mref.descriptor().packages()) {
                previous = packageToModule.putIfAbsent(pkg, mref);
                if (previous != null) {
                    if (first) {
                        printModule(mref);
                        first = false;
                        errorCount++;
                    }
                    String mn = previous.descriptor().name();
                    out.println(INDENT + "contains " + pkg
                            + " conflicts with module " + mn);
                }
            }
        }
    }

    /**
     * Scan an element on a module path. The element is a directory
     * of modules, an exploded module, or a JAR file.
     */
    private void scan(Path entry) {
        BasicFileAttributes attrs;
        try {
            attrs = Files.readAttributes(entry, BasicFileAttributes.class);
        } catch (NoSuchFileException ignore) {
            return;
        } catch (IOException ioe) {
            out.println(entry + " " + ioe);
            errorCount++;
            return;
        }

        String fn = entry.getFileName().toString();
        if (attrs.isRegularFile() && fn.endsWith(".jar")) {
            // JAR file, explicit or automatic module
            scanModule(entry).ifPresent(this::process);
        } else if (attrs.isDirectory()) {
            Path mi = entry.resolve(MODULE_INFO);
            if (Files.exists(mi)) {
                // exploded module
                scanModule(entry).ifPresent(this::process);
            } else {
                // directory of modules
                scanDirectory(entry);
            }
        }
    }

    /**
     * Scan the JAR files and exploded modules in a directory.
     */
    private void scanDirectory(Path dir) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            Map<String, Path> moduleToEntry = new HashMap<>();

            for (Path entry : stream) {
                BasicFileAttributes attrs;
                try {
                    attrs = Files.readAttributes(entry, BasicFileAttributes.class);
                } catch (IOException ioe) {
                    out.println(entry + " " + ioe);
                    errorCount++;
                    continue;
                }

                ModuleReference mref = null;

                String fn = entry.getFileName().toString();
                if (attrs.isRegularFile() && fn.endsWith(".jar")) {
                    mref = scanModule(entry).orElse(null);
                } else if (attrs.isDirectory()) {
                    Path mi = entry.resolve(MODULE_INFO);
                    if (Files.exists(mi)) {
                        mref = scanModule(entry).orElse(null);
                    }
                }

                if (mref != null) {
                    String name = mref.descriptor().name();
                    Path previous = moduleToEntry.putIfAbsent(name, entry);
                    if (previous != null) {
                        // same name as other module in the directory
                        printModule(mref);
                        out.println(INDENT + "contains same module as "
                                + previous.getFileName());
                        errorCount++;
                    } else {
                        process(mref);
                    }
                }
            }
        } catch (IOException ioe) {
            out.println(dir + " " + ioe);
            errorCount++;
        }
    }

    /**
     * Scan a JAR file or exploded module.
     */
    private Optional<ModuleReference> scanModule(Path entry) {
        ModuleFinder finder = ModuleFinder.of(entry);
        try {
            return finder.findAll().stream().findFirst();
        } catch (FindException e) {
            out.println(entry);
            out.println(INDENT + e.getMessage());
            Throwable cause = e.getCause();
            if (cause != null) {
                out.println(INDENT + cause);
            }
            errorCount++;
            return Optional.empty();
        }
    }

    /**
     * Returns true if the given URI is a jrt URI
     */
    private static boolean isJrt(URI uri) {
        return (uri != null && uri.getScheme().equalsIgnoreCase("jrt"));
    }
}
