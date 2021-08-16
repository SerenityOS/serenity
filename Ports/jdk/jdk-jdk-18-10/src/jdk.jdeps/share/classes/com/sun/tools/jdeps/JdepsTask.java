/*
 * Copyright (c) 2012, 2020, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.jdeps;

import com.sun.tools.jdeps.Analyzer.Type;
import static com.sun.tools.jdeps.Analyzer.Type.*;
import static com.sun.tools.jdeps.JdepsWriter.*;
import static java.util.stream.Collectors.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.module.ResolutionException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.*;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

/**
 * Implementation for the jdeps tool for static class dependency analysis.
 */
class JdepsTask {
    static interface BadArguments {
        String getKey();
        Object[] getArgs();
        boolean showUsage();
    }
    static class BadArgs extends Exception implements BadArguments {
        static final long serialVersionUID = 8765093759964640721L;
        BadArgs(String key, Object... args) {
            super(JdepsTask.getMessage(key, args));
            this.key = key;
            this.args = args;
        }

        BadArgs showUsage(boolean b) {
            showUsage = b;
            return this;
        }
        final String key;
        final Object[] args;
        boolean showUsage;

        @Override
        public String getKey() {
            return key;
        }

        @Override
        public Object[] getArgs() {
            return args;
        }

        @Override
        public boolean showUsage() {
            return showUsage;
        }
    }

    static class UncheckedBadArgs extends RuntimeException implements BadArguments {
        static final long serialVersionUID = -1L;
        final BadArgs cause;
        UncheckedBadArgs(BadArgs cause) {
            super(cause);
            this.cause = cause;
        }
        @Override
        public String getKey() {
            return cause.key;
        }

        @Override
        public Object[] getArgs() {
            return cause.args;
        }

        @Override
        public boolean showUsage() {
            return cause.showUsage;
        }
    }

    static abstract class Option {
        Option(boolean hasArg, String... aliases) {
            this.hasArg = hasArg;
            this.aliases = aliases;
        }

        Option(boolean hasArg, CommandOption cmd) {
            this(hasArg, cmd.names());
        }

        boolean isHidden() {
            return false;
        }

        boolean matches(String opt) {
            for (String a : aliases) {
                if (a.equals(opt))
                    return true;
                if (hasArg && opt.startsWith(a + "="))
                    return true;
            }
            return false;
        }

        boolean ignoreRest() {
            return false;
        }

        abstract void process(JdepsTask task, String opt, String arg) throws BadArgs;
        final boolean hasArg;
        final String[] aliases;
    }

    static abstract class HiddenOption extends Option {
        HiddenOption(boolean hasArg, String... aliases) {
            super(hasArg, aliases);
        }

        boolean isHidden() {
            return true;
        }
    }

    enum CommandOption {
        ANALYZE_DEPS(""),
        GENERATE_DOT_FILE("-dotoutput", "--dot-output"),
        GENERATE_MODULE_INFO("--generate-module-info"),
        GENERATE_OPEN_MODULE("--generate-open-module"),
        LIST_DEPS("--list-deps"),
        LIST_REDUCED_DEPS("--list-reduced-deps"),
        PRINT_MODULE_DEPS("--print-module-deps"),
        CHECK_MODULES("--check");

        private final String[] names;
        CommandOption(String... names) {
            this.names = names;
        }

        String[] names() {
            return names;
        }

        @Override
        public String toString() {
            return names[0];
        }
    }

    static Option[] recognizedOptions = {
        new Option(false, "-h", "-?", "-help", "--help") {
            void process(JdepsTask task, String opt, String arg) {
                task.options.help = true;
            }
        },
        new Option(true, CommandOption.GENERATE_DOT_FILE) {
            void process(JdepsTask task, String opt, String arg) throws BadArgs {
                if (task.command != null) {
                    throw new BadArgs("err.command.set", task.command, opt);
                }
                task.command = task.genDotFile(Paths.get(arg));
            }
        },
        new Option(false, "-s", "-summary") {
            void process(JdepsTask task, String opt, String arg) {
                task.options.showSummary = true;
            }
        },
        new Option(false, "-v", "-verbose",
                                "-verbose:module",
                                "-verbose:package",
                                "-verbose:class") {
            void process(JdepsTask task, String opt, String arg) throws BadArgs {
                switch (opt) {
                    case "-v":
                    case "-verbose":
                        task.options.verbose = VERBOSE;
                        task.options.filterSameArchive = false;
                        task.options.filterSamePackage = false;
                        break;
                    case "-verbose:module":
                        task.options.verbose = MODULE;
                        break;
                    case "-verbose:package":
                        task.options.verbose = PACKAGE;
                        break;
                    case "-verbose:class":
                        task.options.verbose = CLASS;
                        break;
                    default:
                        throw new BadArgs("err.invalid.arg.for.option", opt);
                }
            }
        },
        new Option(false, "-apionly", "--api-only") {
            void process(JdepsTask task, String opt, String arg) {
                task.options.apiOnly = true;
            }
        },

        new Option(false, "-jdkinternals", "--jdk-internals") {
            void process(JdepsTask task, String opt, String arg) {
                task.options.findJDKInternals = true;
                if (task.options.includePattern == null) {
                    task.options.includePattern = Pattern.compile(".*");
                }
            }
        },

        // ---- paths option ----
        new Option(true, "-cp", "-classpath", "--class-path") {
            void process(JdepsTask task, String opt, String arg) {
                task.options.classpath = arg;
            }
        },
        new Option(true, "--module-path") {
            void process(JdepsTask task, String opt, String arg) throws BadArgs {
                task.options.modulePath = arg;
            }
        },
        new Option(true, "--upgrade-module-path") {
            void process(JdepsTask task, String opt, String arg) throws BadArgs {
                task.options.upgradeModulePath = arg;
            }
        },
        new Option(true, "--system") {
            void process(JdepsTask task, String opt, String arg) throws BadArgs {
                if (arg.equals("none")) {
                    task.options.systemModulePath = null;
                } else {
                    Path path = Paths.get(arg);
                    if (Files.isRegularFile(path.resolve("lib").resolve("modules")))
                        task.options.systemModulePath = arg;
                    else
                        throw new BadArgs("err.invalid.path", arg);
                }
            }
        },
        new Option(true, "--add-modules") {
            void process(JdepsTask task, String opt, String arg) throws BadArgs {
                Set<String> mods = Set.of(arg.split(","));
                task.options.addmods.addAll(mods);
            }
        },
        new Option(true, "--multi-release") {
            void process(JdepsTask task, String opt, String arg) throws BadArgs {
                if (arg.equalsIgnoreCase("base")) {
                    task.options.multiRelease = JarFile.baseVersion();
                } else {
                    try {
                        int v = Integer.parseInt(arg);
                        if (v < 9) {
                            throw new BadArgs("err.invalid.arg.for.option", arg);
                        }
                    } catch (NumberFormatException x) {
                        throw new BadArgs("err.invalid.arg.for.option", arg);
                    }
                    task.options.multiRelease = Runtime.Version.parse(arg);
                }
            }
        },
        new Option(false, "-q", "-quiet") {
            void process(JdepsTask task, String opt, String arg) {
                task.options.nowarning = true;
            }
        },
        new Option(false, "-version", "--version") {
            void process(JdepsTask task, String opt, String arg) {
                task.options.version = true;
            }
        },

        // ---- module-specific options ----

        new Option(true, "-m", "--module") {
            void process(JdepsTask task, String opt, String arg) throws BadArgs {
                if (!task.options.rootModules.isEmpty()) {
                    throw new BadArgs("err.option.already.specified", opt);
                }
                task.options.rootModules.add(arg);
                task.options.addmods.add(arg);
            }
        },
        new Option(true, CommandOption.GENERATE_MODULE_INFO) {
            void process(JdepsTask task, String opt, String arg) throws BadArgs {
                if (task.command != null) {
                    throw new BadArgs("err.command.set", task.command, opt);
                }
                task.command = task.genModuleInfo(Paths.get(arg), false);
            }
        },
        new Option(true, CommandOption.GENERATE_OPEN_MODULE) {
            void process(JdepsTask task, String opt, String arg) throws BadArgs {
                if (task.command != null) {
                    throw new BadArgs("err.command.set", task.command, opt);
                }
                task.command = task.genModuleInfo(Paths.get(arg), true);
            }
        },
        new Option(true, CommandOption.CHECK_MODULES) {
            void process(JdepsTask task, String opt, String arg) throws BadArgs {
                if (task.command != null) {
                    throw new BadArgs("err.command.set", task.command, opt);
                }
                Set<String> mods =  Set.of(arg.split(","));
                task.options.addmods.addAll(mods);
                task.command = task.checkModuleDeps(mods);
            }
        },
        new Option(false, CommandOption.LIST_DEPS) {
            void process(JdepsTask task, String opt, String arg) throws BadArgs {
                if (task.command != null) {
                    throw new BadArgs("err.command.set", task.command, opt);
                }
                task.command = task.listModuleDeps(CommandOption.LIST_DEPS);
            }
        },
        new Option(false, CommandOption.LIST_REDUCED_DEPS) {
            void process(JdepsTask task, String opt, String arg) throws BadArgs {
                if (task.command != null) {
                    throw new BadArgs("err.command.set", task.command, opt);
                }
                task.command = task.listModuleDeps(CommandOption.LIST_REDUCED_DEPS);
            }
        },
        new Option(false, CommandOption.PRINT_MODULE_DEPS) {
            void process(JdepsTask task, String opt, String arg) throws BadArgs {
                if (task.command != null) {
                    throw new BadArgs("err.command.set", task.command, opt);
                }
                task.command = task.listModuleDeps(CommandOption.PRINT_MODULE_DEPS);
            }
        },
        new Option(false, "--ignore-missing-deps") {
            void process(JdepsTask task, String opt, String arg) {
                task.options.ignoreMissingDeps = true;
            }
        },

        // ---- Target filtering options ----
        new Option(true, "-p", "-package", "--package") {
            void process(JdepsTask task, String opt, String arg) {
                task.options.packageNames.add(arg);
            }
        },
        new Option(true, "-e", "-regex", "--regex") {
            void process(JdepsTask task, String opt, String arg) {
                task.options.regex = Pattern.compile(arg);
            }
        },
        new Option(true, "--require") {
            void process(JdepsTask task, String opt, String arg) {
                task.options.requires.add(arg);
                task.options.addmods.add(arg);
            }
        },
        new Option(true, "-f", "-filter") {
            void process(JdepsTask task, String opt, String arg) {
                task.options.filterRegex = Pattern.compile(arg);
            }
        },
        new Option(false, "-filter:package",
                          "-filter:archive", "-filter:module",
                          "-filter:none") {
            void process(JdepsTask task, String opt, String arg) {
                switch (opt) {
                    case "-filter:package":
                        task.options.filterSamePackage = true;
                        task.options.filterSameArchive = false;
                        break;
                    case "-filter:archive":
                    case "-filter:module":
                        task.options.filterSameArchive = true;
                        task.options.filterSamePackage = false;
                        break;
                    case "-filter:none":
                        task.options.filterSameArchive = false;
                        task.options.filterSamePackage = false;
                        break;
                }
            }
        },
        new Option(false, "--missing-deps") {
            void process(JdepsTask task, String opt, String arg) {
                task.options.findMissingDeps = true;
            }
        },

        // ---- Source filtering options ----
        new Option(true, "-include") {
            void process(JdepsTask task, String opt, String arg) throws BadArgs {
                task.options.includePattern = Pattern.compile(arg);
            }
        },

        new Option(false, "-P", "-profile") {
            void process(JdepsTask task, String opt, String arg) throws BadArgs {
                task.options.showProfile = true;
            }
        },

        new Option(false, "-R", "-recursive", "--recursive") {
            void process(JdepsTask task, String opt, String arg) throws BadArgs {
                task.options.recursive = Options.RECURSIVE;
                // turn off filtering
                task.options.filterSameArchive = false;
                task.options.filterSamePackage = false;
            }
        },
        new Option(false, "--no-recursive") {
            void process(JdepsTask task, String opt, String arg) throws BadArgs {
                task.options.recursive = Options.NO_RECURSIVE;
            }
        },
        new Option(false, "-I", "--inverse") {
            void process(JdepsTask task, String opt, String arg) {
                task.options.inverse = true;
                // equivalent to the inverse of compile-time view analysis
                task.options.compileTimeView = true;
                task.options.filterSamePackage = true;
                task.options.filterSameArchive = true;
            }
        },

        new Option(false, "--compile-time") {
            void process(JdepsTask task, String opt, String arg) {
                task.options.compileTimeView = true;
                task.options.recursive = Options.RECURSIVE;
                task.options.filterSamePackage = true;
                task.options.filterSameArchive = true;
            }
        },

        new HiddenOption(false, "-fullversion") {
            void process(JdepsTask task, String opt, String arg) {
                task.options.fullVersion = true;
            }
        },
        new HiddenOption(false, "-showlabel") {
            void process(JdepsTask task, String opt, String arg) {
                task.options.showLabel = true;
            }
        },
        new HiddenOption(false, "--hide-show-module") {
            void process(JdepsTask task, String opt, String arg) {
                task.options.showModule = false;
            }
        },
        new HiddenOption(true, "-depth") {
            void process(JdepsTask task, String opt, String arg) throws BadArgs {
                try {
                    task.options.depth = Integer.parseInt(arg);
                } catch (NumberFormatException e) {
                    throw new BadArgs("err.invalid.arg.for.option", opt);
                }
            }
        },
    };

    private static final String PROGNAME = "jdeps";
    private final Options options = new Options();
    private final List<String> inputArgs = new ArrayList<>();

    private Command command;
    private PrintWriter log;
    void setLog(PrintWriter out) {
        log = out;
    }

    /**
     * Result codes.
     */
    static final int EXIT_OK = 0,       // Completed with no errors.
                     EXIT_ERROR = 1,    // Completed but reported errors.
                     EXIT_CMDERR = 2,   // Bad command-line arguments
                     EXIT_SYSERR = 3,   // System error or resource exhaustion.
                     EXIT_ABNORMAL = 4; // terminated abnormally

    int run(String... args) {
        if (log == null) {
            log = new PrintWriter(System.out);
        }
        try {
            handleOptions(args);
            if (options.help) {
                showHelp();
            }
            if (options.version || options.fullVersion) {
                showVersion(options.fullVersion);
            }
            if (options.help || options.version || options.fullVersion) {
                return EXIT_OK;
            }
            if (options.numFilters() > 1) {
                reportError("err.invalid.filters");
                return EXIT_CMDERR;
            }

            // default command to analyze dependences
            if (command == null) {
                command = analyzeDeps();
            }
            if (!command.checkOptions()) {
                return EXIT_CMDERR;
            }

            boolean ok = run();
            return ok ? EXIT_OK : EXIT_ERROR;

        } catch (BadArgs|UncheckedBadArgs e) {
            reportError(e.getKey(), e.getArgs());
            if (e.showUsage()) {
                log.println(getMessage("main.usage.summary", PROGNAME));
            }
            return EXIT_CMDERR;
        } catch (ResolutionException e) {
            reportError("err.exception.message", e.getMessage());
            return EXIT_CMDERR;
        } catch (IOException e) {
            e.printStackTrace();
            return EXIT_CMDERR;
        } catch (MultiReleaseException e) {
            reportError(e.getKey(), e.getParams());
            return EXIT_CMDERR;  // could be EXIT_ABNORMAL sometimes
        } finally {
            log.flush();
        }
    }

    boolean run() throws IOException {
        try (JdepsConfiguration config = buildConfig()) {
            if (!options.nowarning) {
                // detect split packages
                config.splitPackages().entrySet()
                      .stream()
                      .sorted(Map.Entry.comparingByKey())
                      .forEach(e -> warning("warn.split.package",
                                            e.getKey(),
                                            e.getValue().stream().collect(joining(" "))));
            }

            // check if any module specified in --add-modules, --require, and -m is missing
            options.addmods.stream()
                .filter(mn -> !JdepsConfiguration.isToken(mn))
                .forEach(mn -> config.findModule(mn).orElseThrow(() ->
                    new UncheckedBadArgs(new BadArgs("err.module.not.found", mn))));

            return command.run(config);
        }
    }

    private JdepsConfiguration buildConfig() throws IOException {
        JdepsConfiguration.Builder builder =
            new JdepsConfiguration.Builder(options.systemModulePath);

        builder.upgradeModulePath(options.upgradeModulePath)
               .appModulePath(options.modulePath)
               .addmods(options.addmods)
               .addmods(command.addModules());

        if (options.classpath != null)
            builder.addClassPath(options.classpath);

        if (options.multiRelease != null)
            builder.multiRelease(options.multiRelease);

        // build the root set of archives to be analyzed
        for (String s : inputArgs) {
            Path p = Paths.get(s);
            if (Files.exists(p)) {
                builder.addRoot(p);
            } else {
                warning("warn.invalid.arg", s);
            }
        }

        return builder.build();
    }

    // ---- factory methods to create a Command

    private AnalyzeDeps analyzeDeps() throws BadArgs {
        return options.inverse ? new InverseAnalyzeDeps()
                               : new AnalyzeDeps();
    }

    private GenDotFile genDotFile(Path dir) throws BadArgs {
        if (Files.exists(dir) && (!Files.isDirectory(dir) || !Files.isWritable(dir))) {
            throw new BadArgs("err.invalid.path", dir.toString());
        }
        return new GenDotFile(dir);
    }

    private GenModuleInfo genModuleInfo(Path dir, boolean openModule) throws BadArgs {
        if (Files.exists(dir) && (!Files.isDirectory(dir) || !Files.isWritable(dir))) {
            throw new BadArgs("err.invalid.path", dir.toString());
        }
        return new GenModuleInfo(dir, openModule);
    }

    private ListModuleDeps listModuleDeps(CommandOption option) throws BadArgs {
        // do transitive dependence analysis unless --no-recursive is set
        if (options.recursive != Options.NO_RECURSIVE) {
            options.recursive = Options.RECURSIVE;
        }
        // no need to record the dependences on the same archive or same package
        options.filterSameArchive = true;
        options.filterSamePackage = true;
        switch (option) {
            case LIST_DEPS:
                return new ListModuleDeps(option, true, false);
            case LIST_REDUCED_DEPS:
                return new ListModuleDeps(option, true, true);
            case PRINT_MODULE_DEPS:
                return new ListModuleDeps(option, false, true, ",");
            default:
                throw new IllegalArgumentException(option.toString());
        }
    }

    private CheckModuleDeps checkModuleDeps(Set<String> mods) throws BadArgs {
        return new CheckModuleDeps(mods);
    }

    abstract class Command {
        final CommandOption option;
        protected Command(CommandOption option) {
            this.option = option;
        }

        /**
         * Returns true if the command-line options are all valid;
         * otherwise, returns false.
         */
        abstract boolean checkOptions();

        /**
         * Do analysis
         */
        abstract boolean run(JdepsConfiguration config) throws IOException;

        /**
         * Includes all modules on system module path and application module path
         *
         * When a named module is analyzed, it will analyze the dependences
         * only.  The method should be overridden when this command should
         * analyze all modules instead.
         */
        Set<String> addModules() {
            return Set.of();
        }

        @Override
        public String toString() {
            return option.toString();
        }
    }


    /**
     * Analyze dependences
     */
    class AnalyzeDeps extends Command {
        JdepsWriter writer;
        AnalyzeDeps() {
            this(CommandOption.ANALYZE_DEPS);
        }

        AnalyzeDeps(CommandOption option) {
            super(option);
        }

        @Override
        boolean checkOptions() {
            if (options.findJDKInternals || options.findMissingDeps) {
                // cannot set any filter, -verbose and -summary option
                if (options.showSummary || options.verbose != null) {
                    reportError("err.invalid.options", "-summary or -verbose",
                        options.findJDKInternals ? "-jdkinternals" : "--missing-deps");
                    return false;
                }
                if (options.hasFilter()) {
                    reportError("err.invalid.options", "--package, --regex, --require",
                        options.findJDKInternals ? "-jdkinternals" : "--missing-deps");
                    return false;
                }
            }
            if (options.showSummary) {
                // -summary cannot use with -verbose option
                if (options.verbose != null) {
                    reportError("err.invalid.options", "-v, -verbose", "-s, -summary");
                    return false;
                }
            }

            if (!inputArgs.isEmpty() && !options.rootModules.isEmpty()) {
                reportError("err.invalid.arg.for.option", "-m");
            }
            if (inputArgs.isEmpty() && !options.hasSourcePath()) {
                showHelp();
                return false;
            }
            return true;
        }

        /*
         * Default is to show package-level dependencies
         */
        Type getAnalyzerType() {
            if (options.showSummary)
                return Type.SUMMARY;

            if (options.findJDKInternals || options.findMissingDeps)
                return Type.CLASS;

            // default to package-level verbose
           return options.verbose != null ? options.verbose : PACKAGE;
        }

        @Override
        boolean run(JdepsConfiguration config) throws IOException {
            Type type = getAnalyzerType();
            // default to package-level verbose
            JdepsWriter writer = new SimpleWriter(log,
                                                  type,
                                                  options.showProfile,
                                                  options.showModule);

            return run(config, writer, type);
        }

        boolean run(JdepsConfiguration config, JdepsWriter writer, Type type)
            throws IOException
        {
            // analyze the dependencies
            DepsAnalyzer analyzer = new DepsAnalyzer(config,
                                                     dependencyFilter(config),
                                                     writer,
                                                     type,
                                                     options.apiOnly);

            boolean ok = analyzer.run(options.compileTimeView, options.depth());

            // print skipped entries, if any
            if (!options.nowarning) {
                analyzer.archives()
                    .forEach(archive -> archive.reader()
                        .skippedEntries().stream()
                        .forEach(name -> warning("warn.skipped.entry", name)));
            }

            if (options.findJDKInternals && !options.nowarning) {
                Map<String, String> jdkInternals = new TreeMap<>();
                Set<String> deps = analyzer.dependences();
                // find the ones with replacement
                deps.forEach(cn -> replacementFor(cn).ifPresent(
                    repl -> jdkInternals.put(cn, repl))
                );

                if (!deps.isEmpty()) {
                    log.println();
                    warning("warn.replace.useJDKInternals", getMessage("jdeps.wiki.url"));
                }

                if (!jdkInternals.isEmpty()) {
                    log.println();
                    String internalApiTitle = getMessage("internal.api.column.header");
                    String replacementApiTitle = getMessage("public.api.replacement.column.header");
                    log.format("%-40s %s%n", internalApiTitle, replacementApiTitle);
                    log.format("%-40s %s%n",
                               internalApiTitle.replaceAll(".", "-"),
                               replacementApiTitle.replaceAll(".", "-"));
                    jdkInternals.entrySet().stream()
                        .forEach(e -> {
                            String key = e.getKey();
                            String[] lines = e.getValue().split("\\n");
                            for (String s : lines) {
                                log.format("%-40s %s%n", key, s);
                                key = "";
                            }
                        });
                }
            }
            return ok;
        }
    }


    class InverseAnalyzeDeps extends AnalyzeDeps {
        InverseAnalyzeDeps() {
        }

        @Override
        boolean checkOptions() {
            if (options.recursive != -1 || options.depth != -1) {
                reportError("err.invalid.options", "--recursive and --no-recursive", "--inverse");
                return false;
            }

            if (options.numFilters() == 0) {
                reportError("err.filter.not.specified");
                return false;
            }

            if (!super.checkOptions()) {
                return false;
            }

            return true;
        }

        @Override
        boolean run(JdepsConfiguration config) throws IOException {
            Type type = getAnalyzerType();

            InverseDepsAnalyzer analyzer =
                new InverseDepsAnalyzer(config,
                                        dependencyFilter(config),
                                        writer,
                                        type,
                                        options.apiOnly);
            boolean ok = analyzer.run();

            log.println();
            if (!options.requires.isEmpty())
                log.println(getMessage("inverse.transitive.dependencies.on",
                                       options.requires));
            else
                log.println(getMessage("inverse.transitive.dependencies.matching",
                                       options.regex != null
                                           ? options.regex.toString()
                                           : "packages " + options.packageNames));

            analyzer.inverseDependences()
                    .stream()
                    .sorted(comparator())
                    .map(this::toInversePath)
                    .forEach(log::println);
            return ok;
        }

        private String toInversePath(Deque<Archive> path) {
            return path.stream()
                       .map(Archive::getName)
                       .collect(joining(" <- "));
        }

        /*
         * Returns a comparator for sorting the inversed path, grouped by
         * the first module name, then the shortest path and then sort by
         * the module names of each path
         */
        private Comparator<Deque<Archive>> comparator() {
            return Comparator.<Deque<Archive>, String>
                comparing(deque -> deque.peekFirst().getName())
                    .thenComparingInt(Deque::size)
                    .thenComparing(this::toInversePath);
        }

        /*
         * Returns true if --require is specified so that all modules are
         * analyzed to find all modules that depend on the modules specified in the
         * --require option directly and indirectly
         */
        Set<String> addModules() {
            return options.requires.size() > 0 ? Set.of("ALL-SYSTEM") : Set.of();
        }
    }


    class GenModuleInfo extends Command {
        final Path dir;
        final boolean openModule;
        GenModuleInfo(Path dir, boolean openModule) {
            super(CommandOption.GENERATE_MODULE_INFO);
            this.dir = dir;
            this.openModule = openModule;
        }

        @Override
        boolean checkOptions() {
            if (options.classpath != null) {
                reportError("err.invalid.options", "-classpath",
                            option);
                return false;
            }
            if (options.hasFilter()) {
                reportError("err.invalid.options", "--package, --regex, --require",
                            option);
                return false;
            }
            if (!options.rootModules.isEmpty()) {
                reportError("err.invalid.options", "-m or --module",
                            option);
                return false;
            }
            return true;
        }

        @Override
        boolean run(JdepsConfiguration config) throws IOException {
            // check if any JAR file contains unnamed package
            for (String arg : inputArgs) {
                try (ClassFileReader reader = ClassFileReader.newInstance(Paths.get(arg), config.getVersion())) {
                    Optional<String> classInUnnamedPackage =
                        reader.entries().stream()
                              .filter(n -> n.endsWith(".class"))
                              .filter(cn -> toPackageName(cn).isEmpty())
                              .findFirst();

                    if (classInUnnamedPackage.isPresent()) {
                        if (classInUnnamedPackage.get().equals("module-info.class")) {
                            reportError("err.genmoduleinfo.not.jarfile", arg);
                        } else {
                            reportError("err.genmoduleinfo.unnamed.package", arg);
                        }
                        return false;
                    }
                }
            }

            ModuleInfoBuilder builder
                 = new ModuleInfoBuilder(config, inputArgs, dir, openModule);
            boolean ok = builder.run(options.ignoreMissingDeps, log, options.nowarning);
            if (!ok) {
                reportError("err.missing.dependences");
                log.println();
                builder.visitMissingDeps(new SimpleDepVisitor());
            }
            return ok;
        }

        private String toPackageName(String name) {
            int i = name.lastIndexOf('/');
            return i > 0 ? name.replace('/', '.').substring(0, i) : "";
        }
    }

    class CheckModuleDeps extends Command {
        final Set<String> modules;
        CheckModuleDeps(Set<String> mods) {
            super(CommandOption.CHECK_MODULES);
            this.modules = mods;
        }

        @Override
        boolean checkOptions() {
            if (!inputArgs.isEmpty()) {
                reportError("err.invalid.options", inputArgs, "--check");
                return false;
            }
            return true;
        }

        @Override
        boolean run(JdepsConfiguration config) throws IOException {
            if (!config.initialArchives().isEmpty()) {
                String list = config.initialArchives().stream()
                                    .map(Archive::getPathName).collect(joining(" "));
                throw new UncheckedBadArgs(new BadArgs("err.invalid.options",
                                                       list, "--check"));
            }
            return new ModuleAnalyzer(config, log, modules).run(options.ignoreMissingDeps);
        }

        /*
         * Returns true to analyze all modules
         */
        Set<String> addModules() {
            return Set.of("ALL-SYSTEM", "ALL-MODULE-PATH");
        }
    }

    class ListModuleDeps extends Command {
        final boolean jdkinternals;
        final boolean reduced;
        final String separator;
        ListModuleDeps(CommandOption option, boolean jdkinternals, boolean reduced) {
            this(option, jdkinternals, reduced, System.getProperty("line.separator"));
        }
        ListModuleDeps(CommandOption option, boolean jdkinternals, boolean reduced, String sep) {
            super(option);
            this.jdkinternals = jdkinternals;
            this.reduced = reduced;
            this.separator = sep;
        }

        @Override
        boolean checkOptions() {
            if (options.showSummary || options.verbose != null) {
                reportError("err.invalid.options", "-summary or -verbose", option);
                return false;
            }
            if (options.findJDKInternals) {
                reportError("err.invalid.options", "-jdkinternals", option);
                return false;
            }
            if (options.findMissingDeps) {
                reportError("err.invalid.options", "--missing-deps", option);
                return false;
            }

            if (!inputArgs.isEmpty() && !options.rootModules.isEmpty()) {
                reportError("err.invalid.arg.for.option", "-m");
            }
            if (inputArgs.isEmpty() && !options.hasSourcePath()) {
                showHelp();
                return false;
            }
            return true;
        }

        @Override
        boolean run(JdepsConfiguration config) throws IOException {
            ModuleExportsAnalyzer analyzer = new ModuleExportsAnalyzer(config,
                                                                       dependencyFilter(config),
                                                                       jdkinternals,
                                                                       reduced,
                                                                       log,
                                                                       separator);
            boolean ok = analyzer.run(options.depth(), options.ignoreMissingDeps);
            if (!ok) {
                reportError("err.missing.dependences");
                log.println();
                analyzer.visitMissingDeps(new SimpleDepVisitor());
            }
            return ok;
        }
    }

    class GenDotFile extends AnalyzeDeps {
        final Path dotOutputDir;
        GenDotFile(Path dotOutputDir) {
            super(CommandOption.GENERATE_DOT_FILE);

            this.dotOutputDir = dotOutputDir;
        }

        @Override
        boolean run(JdepsConfiguration config) throws IOException {
            if ((options.showSummary || options.verbose == MODULE) &&
                !options.addmods.isEmpty() && inputArgs.isEmpty()) {
                // generate dot graph from the resolved graph from module
                // resolution.  No class dependency analysis is performed.
                return new ModuleDotGraph(config, options.apiOnly)
                        .genDotFiles(dotOutputDir);
            }

            Type type = getAnalyzerType();
            JdepsWriter writer = new DotFileWriter(dotOutputDir,
                                                   type,
                                                   options.showProfile,
                                                   options.showModule,
                                                   options.showLabel);
            return run(config, writer, type);
        }
    }

    class SimpleDepVisitor implements Analyzer.Visitor {
        private Archive source;
        @Override
        public void visitDependence(String origin, Archive originArchive, String target, Archive targetArchive) {
            if (source != originArchive) {
                source = originArchive;
                log.format("%s%n", originArchive);
            }
            log.format("   %-50s -> %-50s %s%n", origin, target, targetArchive.getName());
        }
    }

    /**
     * Returns a filter used during dependency analysis
     */
    private JdepsFilter dependencyFilter(JdepsConfiguration config) {
        // Filter specified by -filter, -package, -regex, and --require options
        JdepsFilter.Builder builder = new JdepsFilter.Builder();

        // source filters
        builder.includePattern(options.includePattern);

        // target filters
        builder.filter(options.filterSamePackage, options.filterSameArchive);
        builder.findJDKInternals(options.findJDKInternals);
        builder.findMissingDeps(options.findMissingDeps);

        // --require
        if (!options.requires.isEmpty()) {
            options.requires.stream()
                .forEach(mn -> {
                    Module m = config.findModule(mn).get();
                    builder.requires(mn, m.packages());
                });
        }
        // -regex
        if (options.regex != null)
            builder.regex(options.regex);
        // -package
        if (!options.packageNames.isEmpty())
            builder.packages(options.packageNames);
        // -filter
        if (options.filterRegex != null)
            builder.filter(options.filterRegex);

        return builder.build();
    }

    public void handleOptions(String[] args) throws BadArgs {
        // process options
        for (int i=0; i < args.length; i++) {
            if (args[i].charAt(0) == '-') {
                String name = args[i];
                Option option = getOption(name);
                String param = null;
                if (option.hasArg) {
                    if (name.startsWith("-") && name.indexOf('=') > 0) {
                        param = name.substring(name.indexOf('=') + 1, name.length());
                    } else if (i + 1 < args.length) {
                        param = args[++i];
                    }
                    if (param == null || param.isEmpty() || param.charAt(0) == '-') {
                        throw new BadArgs("err.missing.arg", name).showUsage(true);
                    }
                }
                option.process(this, name, param);
                if (option.ignoreRest()) {
                    i = args.length;
                }
            } else {
                // process rest of the input arguments
                for (; i < args.length; i++) {
                    String name = args[i];
                    if (name.charAt(0) == '-') {
                        throw new BadArgs("err.option.after.class", name).showUsage(true);
                    }
                    inputArgs.add(name);
                }
            }
        }
    }

    private Option getOption(String name) throws BadArgs {
        for (Option o : recognizedOptions) {
            if (o.matches(name)) {
                return o;
            }
        }
        throw new BadArgs("err.unknown.option", name).showUsage(true);
    }

    private void reportError(String key, Object... args) {
        log.println(getMessage("error.prefix") + " " + getMessage(key, args));
    }

    void warning(String key, Object... args) {
        log.println(getMessage("warn.prefix") + " " + getMessage(key, args));
    }

    private void showHelp() {
        log.println(getMessage("main.usage", PROGNAME));
        for (Option o : recognizedOptions) {
            String name = o.aliases[0].substring(1); // there must always be at least one name
            name = name.charAt(0) == '-' ? name.substring(1) : name;
            if (o.isHidden() || name.startsWith("filter:")) {
                continue;
            }
            log.println(getMessage("main.opt." + name));
        }
    }

    private void showVersion(boolean full) {
        log.println(version(full ? "full" : "release"));
    }

    private String version(String key) {
        // key=version:  mm.nn.oo[-milestone]
        // key=full:     mm.mm.oo[-milestone]-build
        try {
            return ResourceBundleHelper.getVersion(key);
        } catch (MissingResourceException e) {
            return getMessage("version.unknown", System.getProperty("java.version"));
        }
    }

    static String getMessage(String key, Object... args) {
        try {
            return MessageFormat.format(ResourceBundleHelper.getMessage(key), args);
        } catch (MissingResourceException e) {
            throw new InternalError("Missing message: " + key);
        }
    }

    private static class Options {
        static final int NO_RECURSIVE = 0;
        static final int RECURSIVE = 1;
        boolean help;
        boolean version;
        boolean fullVersion;
        boolean showProfile;
        boolean showModule = true;
        boolean showSummary;
        boolean apiOnly;
        boolean showLabel;
        boolean findJDKInternals;
        boolean findMissingDeps;
        boolean ignoreMissingDeps;
        boolean nowarning = false;
        Analyzer.Type verbose;
        // default filter references from same package
        boolean filterSamePackage = true;
        boolean filterSameArchive = false;
        Pattern filterRegex;
        String classpath;
        int recursive = -1;     // 0: --no-recursive, 1: --recursive
        int depth = -1;
        Set<String> requires = new HashSet<>();
        Set<String> packageNames = new HashSet<>();
        Pattern regex;             // apply to the dependences
        Pattern includePattern;
        boolean inverse = false;
        boolean compileTimeView = false;
        String systemModulePath = System.getProperty("java.home");
        String upgradeModulePath;
        String modulePath;
        Set<String> rootModules = new HashSet<>();
        Set<String> addmods = new HashSet<>();
        Runtime.Version multiRelease;

        boolean hasSourcePath() {
            return !addmods.isEmpty() || includePattern != null;
        }

        boolean hasFilter() {
            return numFilters() > 0;
        }

        int numFilters() {
            int count = 0;
            if (requires.size() > 0) count++;
            if (regex != null) count++;
            if (packageNames.size() > 0) count++;
            return count;
        }

        int depth() {
            // ignore -depth if --no-recursive is set
            if (recursive == NO_RECURSIVE)
                return 1;

            // depth == 0 if recursive
            if (recursive == RECURSIVE && depth == -1)
                return 0;

            // default depth is 1 unless specified via -depth option
            return depth == -1 ? 1 : depth;
        }
    }

    private static class ResourceBundleHelper {
        static final String LS = System.lineSeparator();
        static final ResourceBundle versionRB;
        static final ResourceBundle bundle;
        static final ResourceBundle jdkinternals;

        static {
            Locale locale = Locale.getDefault();
            try {
                bundle = ResourceBundle.getBundle("com.sun.tools.jdeps.resources.jdeps", locale);
            } catch (MissingResourceException e) {
                throw new InternalError("Cannot find jdeps resource bundle for locale " + locale);
            }
            try {
                versionRB = ResourceBundle.getBundle("com.sun.tools.jdeps.resources.version");
            } catch (MissingResourceException e) {
                throw new InternalError("version.resource.missing");
            }
            try {
                jdkinternals = ResourceBundle.getBundle("com.sun.tools.jdeps.resources.jdkinternals");
            } catch (MissingResourceException e) {
                throw new InternalError("Cannot find jdkinternals resource bundle");
            }
        }

        static String getMessage(String key) {
            return bundle.getString(key).replace("\n", LS);
        }

        static String getVersion(String key) {
            if (ResourceBundleHelper.versionRB == null) {
                return System.getProperty("java.version");
            }
            return versionRB.getString(key).replace("\n", LS);
        }

        static String getSuggestedReplacement(String key) {
            return ResourceBundleHelper.jdkinternals.getString(key).replace("\n", LS);
        }
    }

    /**
     * Returns the recommended replacement API for the given classname;
     * or return null if replacement API is not known.
     */
    private Optional<String> replacementFor(String cn) {
        String name = cn;
        String value = null;
        while (value == null && name != null) {
            try {
                value = ResourceBundleHelper.getSuggestedReplacement(name);
            } catch (MissingResourceException e) {
                // go up one subpackage level
                int i = name.lastIndexOf('.');
                name = i > 0 ? name.substring(0, i) : null;
            }
        }
        return Optional.ofNullable(value);
    }
}
