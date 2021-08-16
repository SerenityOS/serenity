/*
 * Copyright (c) 2014, 2020, Oracle and/or its affiliates. All rights reserved.
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

package jdk.tools.jimage;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jdk.internal.jimage.BasicImageReader;
import jdk.internal.jimage.ImageHeader;
import jdk.internal.jimage.ImageLocation;
import jdk.internal.org.objectweb.asm.ClassReader;
import jdk.internal.org.objectweb.asm.tree.ClassNode;
import jdk.tools.jlink.internal.ImageResourcesTree;
import jdk.tools.jlink.internal.TaskHelper;
import jdk.tools.jlink.internal.TaskHelper.BadArgs;
import static jdk.tools.jlink.internal.TaskHelper.JIMAGE_BUNDLE;
import jdk.tools.jlink.internal.TaskHelper.Option;
import jdk.tools.jlink.internal.TaskHelper.OptionsHelper;
import jdk.tools.jlink.internal.Utils;

class JImageTask {
    private static final Option<?>[] RECOGNIZED_OPTIONS = {
        new Option<JImageTask>(true, (task, option, arg) -> {
            task.options.directory = arg;
        }, "--dir"),

        new Option<JImageTask>(true, (task, option, arg) -> {
            task.options.include = arg;
        }, "--include"),

        new Option<JImageTask>(false, (task, option, arg) -> {
            task.options.fullVersion = true;
        }, true, "--full-version"),

        new Option<JImageTask>(false, (task, option, arg) -> {
            task.options.help = true;
        }, "--help", "-h", "-?"),

        new Option<JImageTask>(false, (task, option, arg) -> {
            task.options.verbose = true;
        }, "--verbose"),

        new Option<JImageTask>(false, (task, option, arg) -> {
            task.options.version = true;
        }, "--version")
    };
    private static final TaskHelper TASK_HELPER
            = new TaskHelper(JIMAGE_BUNDLE);
    private static final OptionsHelper<JImageTask> OPTION_HELPER
            = TASK_HELPER.newOptionsHelper(JImageTask.class, RECOGNIZED_OPTIONS);
    private static final String PROGNAME = "jimage";
    private static final FileSystem JRT_FILE_SYSTEM = Utils.jrtFileSystem();

    private final OptionsValues options;
    private final List<Predicate<String>> includePredicates;
    private PrintWriter log;

    JImageTask() {
        this.options = new OptionsValues();
        this.includePredicates = new ArrayList<>();
        log = null;
    }

    void setLog(PrintWriter out) {
        log = out;
        TASK_HELPER.setLog(log);
    }

    static class OptionsValues {
        Task task = null;
        String directory = ".";
        String include = "";
        boolean fullVersion;
        boolean help;
        boolean verbose;
        boolean version;
        List<File> jimages = new LinkedList<>();
    }

    enum Task {
        EXTRACT,
        INFO,
        LIST,
        VERIFY
    };

    private String pad(String string, int width, boolean justifyRight) {
        int length = string.length();

        if (length == width) {
            return string;
        }

        if (length > width) {
            return string.substring(0, width);
        }

        int padding = width - length;

        StringBuilder sb = new StringBuilder(width);
        if (justifyRight) {
            for (int i = 0; i < padding; i++) {
                sb.append(' ');
            }
        }

        sb.append(string);

        if (!justifyRight) {
            for (int i = 0; i < padding; i++) {
                sb.append(' ');
            }
        }

        return sb.toString();
    }

    private String pad(String string, int width) {
        return pad(string, width, false);
    }

    private String pad(long value, int width) {
        return pad(Long.toString(value), width, true);
    }

    private static final int EXIT_OK = 0;        // No errors.
    private static final int EXIT_ERROR = 1;     // Completed but reported errors.
    private static final int EXIT_CMDERR = 2;    // Bad command-line arguments and/or switches.
    private static final int EXIT_ABNORMAL = 4;  // Terminated abnormally.

    int run(String[] args) {
        if (log == null) {
            setLog(new PrintWriter(System.out, true));
        }

        if (args.length == 0) {
            log.println(TASK_HELPER.getMessage("main.usage.summary", PROGNAME));
            return EXIT_ABNORMAL;
        }

        try {
            String command;
            String[] remaining = args;
            try {
                command = args[0];
                options.task = Enum.valueOf(Task.class, args[0].toUpperCase(Locale.ENGLISH));
                remaining = args.length > 1 ? Arrays.copyOfRange(args, 1, args.length)
                                            : new String[0];
            } catch (IllegalArgumentException ex) {
                command = null;
                options.task = null;
            }

            // process arguments
            List<String> unhandled = OPTION_HELPER.handleOptions(this, remaining);
            for (String f : unhandled) {
                options.jimages.add(new File(f));
            }

            if (options.task == null && !options.help && !options.version && !options.fullVersion) {
                throw TASK_HELPER.newBadArgs("err.not.a.task",
                    command != null ? command : "<unspecified>");
            }

            if (options.help) {
                if (options.task == null) {
                    log.println(TASK_HELPER.getMessage("main.usage", PROGNAME));
                    Arrays.asList(RECOGNIZED_OPTIONS).stream()
                        .filter(option -> !option.isHidden())
                        .sorted()
                        .forEach(option -> {
                             log.println(TASK_HELPER.getMessage(option.resourceName()));
                        });
                    log.println(TASK_HELPER.getMessage("main.opt.footer"));
                } else {
                    try {
                        log.println(TASK_HELPER.getMessage("main.usage." +
                                options.task.toString().toLowerCase()));
                    } catch (MissingResourceException ex) {
                        throw TASK_HELPER.newBadArgs("err.not.a.task", command);
                    }
                }
                return EXIT_OK;
            }

            if (options.version || options.fullVersion) {
                if (options.task == null && !unhandled.isEmpty()) {
                    throw TASK_HELPER.newBadArgs("err.not.a.task",
                        Stream.of(args).collect(Collectors.joining(" ")));
                }

                TASK_HELPER.showVersion(options.fullVersion);
                if (unhandled.isEmpty()) {
                    return EXIT_OK;
                }
            }

            processInclude(options.include);

            return run() ? EXIT_OK : EXIT_ERROR;
        } catch (BadArgs e) {
            TASK_HELPER.reportError(e.key, e.args);

            if (e.showUsage) {
                log.println(TASK_HELPER.getMessage("main.usage.summary", PROGNAME));
            }

            return EXIT_CMDERR;
        } catch (Exception x) {
            x.printStackTrace();

            return EXIT_ABNORMAL;
        } finally {
            log.flush();
        }
    }

    private void processInclude(String include) {
        if (include.isEmpty()) {
            return;
        }

        for (String filter : include.split(",")) {
            final PathMatcher matcher = Utils.getPathMatcher(JRT_FILE_SYSTEM, filter);
            Predicate<String> predicate = (path) -> matcher.matches(JRT_FILE_SYSTEM.getPath(path));
            includePredicates.add(predicate);
        }
    }

    private void listTitle(File file, BasicImageReader reader) {
        log.println("jimage: " + file);
    }

    private interface JImageAction {
        public void apply(File file, BasicImageReader reader) throws IOException, BadArgs;
    }

    private interface ModuleAction {
         public void apply(BasicImageReader reader,
                 String oldModule, String newModule) throws IOException, BadArgs;
    }

    private interface ResourceAction {
        public void apply(BasicImageReader reader, String name,
                ImageLocation location) throws IOException, BadArgs;
    }

    private void extract(BasicImageReader reader, String name,
            ImageLocation location) throws IOException, BadArgs {
        File directory = new File(options.directory);
        byte[] bytes = reader.getResource(location);
        File resource =  new File(directory, name);
        File parent = resource.getParentFile();

        if (parent.exists()) {
            if (!parent.isDirectory()) {
                throw TASK_HELPER.newBadArgs("err.cannot.create.dir",
                                            parent.getAbsolutePath());
            }
        } else if (!parent.mkdirs()) {
            throw TASK_HELPER.newBadArgs("err.cannot.create.dir",
                                        parent.getAbsolutePath());
        }

        if (!ImageResourcesTree.isTreeInfoResource(name)) {
            Files.write(resource.toPath(), bytes);
        }
    }

    private static final int OFFSET_WIDTH = 12;
    private static final int SIZE_WIDTH = 10;
    private static final int COMPRESSEDSIZE_WIDTH = 10;

    private String trimModule(String name) {
        int offset = name.indexOf('/', 1);

        if (offset != -1 && offset + 1 < name.length()) {
            return name.substring(offset + 1);
        }

        return name;
    }

    private void print(String name, ImageLocation location) {
        log.print(pad(location.getContentOffset(), OFFSET_WIDTH) + " ");
        log.print(pad(location.getUncompressedSize(), SIZE_WIDTH) + " ");
        log.print(pad(location.getCompressedSize(), COMPRESSEDSIZE_WIDTH) + " ");
        log.println(trimModule(name));
    }

    private void print(BasicImageReader reader, String name) {
        if (options.verbose) {
            print(name, reader.findLocation(name));
        } else {
            log.println("    " + trimModule(name));
        }
    }

    private void info(File file, BasicImageReader reader) throws IOException {
        ImageHeader header = reader.getHeader();

        log.println(" Major Version:  " + header.getMajorVersion());
        log.println(" Minor Version:  " + header.getMinorVersion());
        log.println(" Flags:          " + Integer.toHexString(header.getFlags()));
        log.println(" Resource Count: " + header.getResourceCount());
        log.println(" Table Length:   " + header.getTableLength());
        log.println(" Offsets Size:   " + header.getOffsetsSize());
        log.println(" Redirects Size: " + header.getRedirectSize());
        log.println(" Locations Size: " + header.getLocationsSize());
        log.println(" Strings Size:   " + header.getStringsSize());
        log.println(" Index Size:     " + header.getIndexSize());
    }

    private void listModule(BasicImageReader reader, String oldModule, String newModule) {
        log.println();
        log.println("Module: " + newModule);

        if (options.verbose) {
            log.print(pad("Offset", OFFSET_WIDTH) + " ");
            log.print(pad("Size", SIZE_WIDTH) + " ");
            log.print(pad("Compressed", COMPRESSEDSIZE_WIDTH) + " ");
            log.println("Entry");
        }
    }

    private void list(BasicImageReader reader, String name, ImageLocation location) {
        print(reader, name);
    }

      void verify(BasicImageReader reader, String name, ImageLocation location) {
        if (name.endsWith(".class") && !name.endsWith("module-info.class")) {
            try {
                byte[] bytes = reader.getResource(location);
                ClassReader cr = new ClassReader(bytes);
                ClassNode cn = new ClassNode();
                cr.accept(cn, 0);
            } catch (Exception ex) {
                log.println("Error(s) in Class: " + name);
            }
        }
    }

    private void iterate(JImageAction jimageAction,
            ModuleAction moduleAction,
            ResourceAction resourceAction) throws IOException, BadArgs {
        if (options.jimages.isEmpty()) {
            throw TASK_HELPER.newBadArgs("err.no.jimage");
        }

        for (File file : options.jimages) {
            if (!file.exists() || !file.isFile()) {
                throw TASK_HELPER.newBadArgs("err.not.a.jimage", file);
            }

            try (BasicImageReader reader = BasicImageReader.open(file.toPath())) {
                if (jimageAction != null) {
                    jimageAction.apply(file, reader);
                }

                if (resourceAction != null) {
                    String[] entryNames = reader.getEntryNames();
                    String oldModule = "";

                    for (String name : entryNames) {
                        boolean match = includePredicates.isEmpty();

                        for (Predicate<String> predicate : includePredicates) {
                            if (predicate.test(name)) {
                                match = true;
                                break;
                            }
                        }

                        if (!match) {
                            continue;
                        }

                        if (!ImageResourcesTree.isTreeInfoResource(name)) {
                            if (moduleAction != null) {
                                int offset = name.indexOf('/', 1);

                                String newModule = offset != -1 ?
                                        name.substring(1, offset) :
                                        "<unknown>";

                                if (!oldModule.equals(newModule)) {
                                    moduleAction.apply(reader, oldModule, newModule);
                                    oldModule = newModule;
                                }
                            }

                            ImageLocation location = reader.findLocation(name);
                            resourceAction.apply(reader, name, location);
                        }
                    }
                }
            } catch (IOException ioe) {
                throw TASK_HELPER.newBadArgs("err.invalid.jimage", file, ioe.getMessage());
            }
        }
    }

    private boolean run() throws Exception, BadArgs {
        switch (options.task) {
            case EXTRACT:
                iterate(null, null, this::extract);
                break;
            case INFO:
                iterate(this::info, null, null);
                break;
            case LIST:
                iterate(this::listTitle, this::listModule, this::list);
                break;
            case VERIFY:
                iterate(this::listTitle, null, this::verify);
                break;
            default:
                throw TASK_HELPER.newBadArgs("err.not.a.task",
                        options.task.name()).showUsage(true);
        }
        return true;
    }
}
