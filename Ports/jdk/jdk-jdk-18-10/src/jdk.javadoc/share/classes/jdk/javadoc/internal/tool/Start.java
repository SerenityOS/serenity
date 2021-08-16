/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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

package jdk.javadoc.internal.tool;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.BreakIterator;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IllformedLocaleException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;

import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.file.BaseFileManager;
import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.jvm.Target;
import com.sun.tools.javac.main.Arguments;
import com.sun.tools.javac.main.CommandLine;
import com.sun.tools.javac.util.ClientCodeException;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.StringUtils;

import jdk.javadoc.doclet.Doclet;
import jdk.javadoc.doclet.Doclet.Option;
import jdk.javadoc.doclet.DocletEnvironment;
import jdk.javadoc.doclet.StandardDoclet;
import jdk.javadoc.internal.Versions;
import jdk.javadoc.internal.tool.Main.Result;
import jdk.javadoc.internal.tool.ToolOptions.ToolOption;

import static javax.tools.DocumentationTool.Location.*;

import static jdk.javadoc.internal.tool.Main.Result.*;

/**
 * Main program of Javadoc.
 * Previously named "Main".
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class Start {

    /** Context for this invocation. */
    private final Context context;

    private static final String ProgramName = "javadoc";

    private JavadocLog log;

    private final String docletName;

    private final ClassLoader classLoader;

    private Class<?> docletClass;

    private Doclet doclet;

    // used to determine the locale for the log
    private Locale locale;

    /**
     * In API mode, exceptions thrown while calling the doclet are
     * propagated using ClientCodeException.
     */
    private boolean apiMode;

    private JavaFileManager fileManager;

    private final ToolOptions options;

    Start() {
        this(null, null, null, null, null, null);
    }

    Start(PrintWriter outWriter, PrintWriter errWriter) {
        this(null, null, outWriter, errWriter, null, null);
    }

    Start(Context context, String programName,
            PrintWriter outWriter, PrintWriter errWriter,
            String docletName, ClassLoader classLoader) {
        this.context = context == null ? new Context() : context;
        String pname = programName == null ? ProgramName : programName;
        this.log = (outWriter == null && errWriter == null)
                ? new JavadocLog(this.context, pname)
                : new JavadocLog(this.context, pname, outWriter, errWriter);
        this.docletName = docletName;
        this.classLoader = classLoader;
        this.docletClass = null;
        this.locale = Locale.getDefault();

        options = getToolOptions();
    }

    public Start(Context context) {
        this.docletClass = null;
        this.context = Objects.requireNonNull(context);
        this.apiMode = true;
        this.docletName = null;
        this.classLoader = null;
        this.locale = Locale.getDefault();

        Log log = context.get(Log.logKey);
        if (log instanceof JavadocLog l){
            this.log = l;
        } else {
            PrintWriter out = context.get(Log.errKey);
            this.log = (out == null)
                    ? new JavadocLog(context, ProgramName)
                    : new JavadocLog(context, ProgramName, out, out);
        }

        options = getToolOptions();
    }

    private ToolOptions getToolOptions() {
        ToolOptions.ShowHelper helper =  new ToolOptions.ShowHelper() {
            @Override
            public void usage() {
                showUsage("main.usage", ToolOption.Kind.STANDARD, "main.usage.foot");
            }

            @Override
            public void Xusage() {
                showUsage("main.Xusage", ToolOption.Kind.EXTENDED, "main.Xusage.foot");
            }

            @Override
            public void version() {
                showVersion("javadoc.version", orDefault(() -> Versions.shortVersionStringOf(toolVersion())));
            }

            @Override
            public void fullVersion() {
                showVersion("javadoc.fullversion", orDefault(() -> Versions.fullVersionStringOf(toolVersion())));
            }

            private String orDefault(Supplier<String> s) {
                try {
                    return s.get();
                } catch (RuntimeException e) {
                    assert false : e;
                    return Log.getLocalizedString("version.not.available");
                }
            }
        };
        return new ToolOptions(context, log, helper);
    }

    private Runtime.Version toolVersion() {
        return Versions.javadocVersion();
    }

    private void showUsage() {
        showUsage("main.usage", ToolOption.Kind.STANDARD, "main.usage.foot");
    }

    private void showUsage(String headerKey, ToolOption.Kind kind, String footerKey) {
        log.noticeUsingKey(headerKey);
        showToolOptions(kind);

        // let doclet print usage information
        if (docletClass != null) {
            showDocletOptions(kind == ToolOption.Kind.EXTENDED
                    ? Option.Kind.EXTENDED
                    : Option.Kind.STANDARD);
        }
        if (footerKey != null)
            log.noticeUsingKey(footerKey);
    }

    private void showVersion(String labelKey, String value) {
        log.noticeUsingKey(labelKey, log.programName, value);
    }

    private void showToolOptions(ToolOption.Kind kind) {
        Comparator<ToolOption> comp = new Comparator<ToolOption>() {
            final Collator collator = Collator.getInstance(Locale.US);
            { collator.setStrength(Collator.PRIMARY); }

            @Override
            public int compare(ToolOption o1, ToolOption o2) {
                return collator.compare(o1.primaryName, o2.primaryName);
            }
        };

        options.getSupportedOptions().stream()
                    .filter(opt -> opt.kind == kind)
                    .sorted(comp)
                    .forEach(this::showToolOption);
    }

    private void showToolOption(ToolOption option) {
        List<String> names = option.getNames();
        String primaryName = option.primaryName;
        String parameters;
        if (option.hasArg || primaryName.endsWith(":")) {
            String sep = primaryName.endsWith(":")
                    || primaryName.equals(ToolOptions.AT)
                    || primaryName.equals(ToolOptions.J)
                    ? "" : " ";
            parameters = sep + option.getParameters(log);
        } else {
            parameters = "";
        }
        String description = option.getDescription(log);
        showOption(names, parameters, description);
    }

    private void showDocletOptions(Option.Kind kind) {
        String name = doclet.getName();
        Set<? extends Option> options = getSupportedOptionsOf(doclet);
        if (options.isEmpty()) {
            return;
        }
        log.noticeUsingKey("main.doclet.usage.header", name);

        Comparator<Doclet.Option> comp = new Comparator<Doclet.Option>() {
            final Collator collator = Collator.getInstance(Locale.US);
            { collator.setStrength(Collator.PRIMARY); }

            @Override
            public int compare(Doclet.Option o1, Doclet.Option o2) {
                return collator.compare(o1.getNames().get(0), o2.getNames().get(0));
            }
        };

        options.stream()
                .filter(opt -> opt.getKind() == kind)
                .sorted(comp)
                .forEach(this::showDocletOption);
    }

    private void showDocletOption(Doclet.Option option) {
        List<String> names = option.getNames();
        String parameters;
        String primaryName = names.get(0);
        if (option.getArgumentCount() > 0 || primaryName.endsWith(":")) {
            String sep = primaryName.endsWith(":") ? "" : " ";
            parameters = sep + option.getParameters();
        } else {
            parameters = "";
        }
        String description = option.getDescription();
        showOption(names, parameters, description);
    }

    // The following constants are intended to format the output to
    // be similar to that of the java launcher: i.e. "java -help".

    /** The indent for the option synopsis. */
    private static final String SMALL_INDENT = " ".repeat(4);
    /** The automatic indent for the description. */
    private static final String LARGE_INDENT = " ".repeat(18);
    /** The space allowed for the synopsis, if the description is to be shown on the same line. */
    private static final int DEFAULT_SYNOPSIS_WIDTH = 13;
    /** The nominal maximum line length, when seeing if text will fit on a line. */
    private static final int DEFAULT_MAX_LINE_LENGTH = 80;
    /** The format for a single-line help entry. */
    private static final String COMPACT_FORMAT = SMALL_INDENT + "%-" + DEFAULT_SYNOPSIS_WIDTH + "s %s";

    void showOption(List<String> names, String parameters, String description) {
        String synopses = names.stream()
                .map(s -> s + parameters)
                .collect(Collectors.joining(", "));
        // If option synopses and description fit on a single line of reasonable length,
        // display using COMPACT_FORMAT
        if (synopses.length() < DEFAULT_SYNOPSIS_WIDTH
                && !description.contains("\n")
                && (SMALL_INDENT.length() + DEFAULT_SYNOPSIS_WIDTH + 1 + description.length() <= DEFAULT_MAX_LINE_LENGTH)) {
            log.notice(String.format(COMPACT_FORMAT, synopses, description));
            return;
        }

        // If option synopses fit on a single line of reasonable length, show that;
        // otherwise, show 1 per line
        if (synopses.length() <= DEFAULT_MAX_LINE_LENGTH) {
            log.notice(SMALL_INDENT + synopses);
        } else {
            for (String name: names) {
                log.notice(SMALL_INDENT + name + parameters);
            }
        }

        // Finally, show the description
        log.notice(LARGE_INDENT + description.replace("\n", "\n" + LARGE_INDENT));
    }


    /**
     * Main program - external wrapper.
     */
    @SuppressWarnings("deprecation")
    Result begin(String... argv) {
        // Preprocess @file arguments
        List<String> allArgs;
        try {
            allArgs = CommandLine.parse(List.of(argv));
        } catch (IOException e) {
            error("main.cant.read", e.getMessage());
            return ERROR;
        }
        return begin(allArgs, Collections.emptySet());
    }

    // Called by the JSR 199 API
    public boolean begin(Class<?> docletClass,
                         Iterable<String> options,
                         Iterable<? extends JavaFileObject> fileObjects)
    {
        this.docletClass = docletClass;
        List<String> opts = new ArrayList<>();
        for (String opt: options)
            opts.add(opt);

        return begin(opts, fileObjects).isOK();
    }

    private Result begin(List<String> options, Iterable<? extends JavaFileObject> fileObjects) {
        fileManager = context.get(JavaFileManager.class);
        if (fileManager == null) {
            JavacFileManager.preRegister(context);
            fileManager = context.get(JavaFileManager.class);
            if (fileManager instanceof BaseFileManager bfm) {
                bfm.autoClose = true;
            }
        }

        // Perform an initial scan of the options to determine the doclet to be used (if any),
        // so that it may participate in the main round of option processing.
        try {
            doclet = preprocess(options);
        } catch (ToolException te) {
            if (!te.result.isOK()) {
                if (te.message != null) {
                    log.printError(te.message);
                }
                Throwable t = te.getCause();
                dumpStack(t == null ? te : t);
            }
            return te.result;
        } catch (OptionException oe) {
            if (oe.message != null) {
                log.printError(oe.message);
            }
            oe.m.run();
            Throwable t = oe.getCause();
            dumpStack(t == null ? oe : t);
            return oe.result;
        }

        Result result = OK;
        try {
            result = parseAndExecute(options, fileObjects);
        } catch (com.sun.tools.javac.main.Option.InvalidValueException e) {
            // The detail message from javac already includes a localized "error: " prefix,
            // so print the message directly.
            // It would be even better to rethrow this as IllegalArgumentException
            // when invoked via the API.
            // See javac Arguments.error(InvalidValueException) for an example
            log.printRawLines(e.getMessage());
            Throwable t = e.getCause();
            dumpStack(t == null ? e : t);
            return ERROR;
        } catch (OptionException oe) {
            // It would be even better to rethrow this as IllegalArgumentException
            // when invoked via the API.
            // See javac Arguments.error(InvalidValueException) for an example
            if (oe.message != null)
                log.printError(oe.message);

            oe.m.run();
            Throwable t = oe.getCause();
            dumpStack(t == null ? oe : t);
            return oe.result;
        } catch (ToolException exc) {
            if (exc.message != null) {
                log.printError(exc.message);
            }
            Throwable t = exc.getCause();
            if (result == ABNORMAL) {
                reportInternalError(t == null ? exc : t);
            } else {
                dumpStack(t == null ? exc : t);
            }
            return exc.result;
        } catch (OutOfMemoryError ee) {
            error("main.out.of.memory");
            result = SYSERR;
            dumpStack(ee);
        } catch (ClientCodeException e) {
            // simply rethrow these exceptions, to be caught and handled by JavadocTaskImpl
            throw e;
        } catch (Error | Exception ee) {
            error("main.fatal.error", ee);
            reportInternalError(ee);
            result = ABNORMAL;
        } finally {
            if (fileManager instanceof BaseFileManager bfm
                    && bfm.autoClose) {
                try {
                    fileManager.close();
                } catch (IOException ignore) {}
            }
            if (this.options.rejectWarnings() && log.hasWarnings()) {
                error("main.warnings.Werror");
            }
            boolean haveErrors = log.hasErrors();
            if (!result.isOK() && !haveErrors) {
                // the doclet failed, but nothing reported, flag it!.
                error("main.unknown.error");
            }
            if (haveErrors && result.isOK()) {
                result = ERROR;
            }
            log.printErrorWarningCounts();
            log.flush();
        }
        return result;
    }

    private void reportInternalError(Throwable t) {
        log.printErrorUsingKey("doclet.internal.report.bug");
        dumpStack(true, t);
    }

    private void dumpStack(Throwable t) {
        dumpStack(false, t);
    }

    private void dumpStack(boolean enabled, Throwable t) {
        if (t != null && (enabled || options.dumpOnError())) {
            t.printStackTrace(System.err);
        }
    }

    /**
     * Main program - internal
     */
    private Result parseAndExecute(List<String> argList, Iterable<? extends JavaFileObject> fileObjects)
            throws ToolException, OptionException, com.sun.tools.javac.main.Option.InvalidValueException
    {
        final long startNanos = System.nanoTime();

        List<String> javaNames = new ArrayList<>();

        // Make sure no obsolete source/target messages are reported
        try {
            options.processCompilerOption(com.sun.tools.javac.main.Option.XLINT_CUSTOM, "-Xlint:-options");
        } catch (com.sun.tools.javac.main.Option.InvalidValueException ignore) {
        }

        Arguments arguments = Arguments.instance(context);
        arguments.init(ProgramName);
        arguments.allowEmpty();

        doclet.init(locale, log);
        int beforeCount = log.nerrors;
        boolean success = parseArgs(argList, javaNames);
        int afterCount = log.nerrors;
        if (!success && beforeCount == afterCount) { // if there were failures but they have not been reported
            return CMDERR;
        }

        if (!arguments.handleReleaseOptions(extra -> true)) {
            // Arguments does not always increase the error count in the
            // case of errors, so increment the error count only if it has
            // not been updated previously, preventing complaints by callers
            if (!log.hasErrors() && !log.hasWarnings())
                log.nerrors++;
            return CMDERR;
        }

        if (!arguments.validate()) {
            // Arguments does not always increase the error count in the
            // case of errors, so increment the error count only if it has
            // not been updated previously, preventing complaints by callers
            if (!log.hasErrors() && !log.hasWarnings())
                log.nerrors++;
            return CMDERR;
        }

        if (fileManager instanceof BaseFileManager bfm) {
            bfm.handleOptions(options.fileManagerOptions());
        }

        String mr = com.sun.tools.javac.main.Option.MULTIRELEASE.primaryName;
        if (fileManager.isSupportedOption(mr) == 1) {
            Target target = Target.instance(context);
            List<String> list = List.of(target.multiReleaseValue());
            fileManager.handleOption(mr, list.iterator());
        }
        options.compilerOptions().notifyListeners();

        if (options.modules().isEmpty()) {
            if (options.subpackages().isEmpty()) {
                if (javaNames.isEmpty() && isEmpty(fileObjects)) {
                    String text = log.getText("main.No_modules_packages_or_classes_specified");
                    throw new ToolException(CMDERR, text);
                }
            }
        }

        JavadocTool comp = JavadocTool.make0(context);
        if (comp == null) return ABNORMAL;

        DocletEnvironment docEnv = comp.getEnvironment(options, javaNames, fileObjects);

        // release resources
        comp = null;

        if (options.breakIterator() || !locale.getLanguage().equals(Locale.ENGLISH.getLanguage())) {
            JavacTrees trees = JavacTrees.instance(context);
            trees.setBreakIterator(BreakIterator.getSentenceInstance(locale));
        }
        // pass off control to the doclet
        Result returnStatus = docEnv != null && doclet.run(docEnv)
                ? OK
                : ERROR;

        // We're done.
        if (options.verbose()) {
            long elapsedMillis = (System.nanoTime() - startNanos) / 1_000_000;
            JavadocLog.printRawLines(log.getDiagnosticWriter(),
                    log.getText("main.done_in", Long.toString(elapsedMillis)));
        }

        return returnStatus;
    }

    boolean matches(List<String> names, String arg) {
        for (String name : names) {
            if (StringUtils.toLowerCase(name).equals(StringUtils.toLowerCase(arg)))
                return true;
        }
        return false;
    }

    boolean matches(Doclet.Option option, String arg) {
        if (matches(option.getNames(), arg))
             return true;
        int sep = arg.indexOf(':');
        String targ = arg.substring(0, sep + 1);
        return matches(option.getNames(), targ);
    }

    private Set<? extends Doclet.Option> docletOptions = null;

    /*
     * Consumes an option along with its arguments. Returns an advanced index
     * modulo the sign. If the value is negative, it means there was a failure
     * processing one or more options.
     */
    int consumeDocletOption(int idx, List<String> args, boolean isToolOption) throws OptionException {
        if (docletOptions == null) {
            docletOptions = getSupportedOptionsOf(doclet);
        }
        String arg = args.get(idx);
        String argBase, argVal;
        if (arg.startsWith("--") && arg.contains("=")) {
            int sep = arg.indexOf("=");
            argBase = arg.substring(0, sep);
            argVal = arg.substring(sep + 1);
        } else {
            argBase = arg;
            argVal = null;
        }
        int m = 1;
        String text = null;
        for (Doclet.Option opt : docletOptions) {
            if (matches(opt, argBase)) {
                if (argVal != null) {
                    switch (opt.getArgumentCount()) {
                        case 0:
                            text = log.getText("main.unnecessary_arg_provided", argBase);
                            throw new OptionException(ERROR, this::showUsage, text);
                        case 1:
                            if (!opt.process(arg, Collections.singletonList(argVal))) {
                                m = -1;
                            }
                            break;
                        default:
                            text = log.getText("main.only_one_argument_with_equals", argBase);
                            throw new OptionException(ERROR, this::showUsage, text);
                    }
                } else {
                    if (args.size() - idx - 1 < opt.getArgumentCount()) {
                        text = log.getText("main.requires_argument", arg);
                        throw new OptionException(ERROR, this::showUsage, text);
                    }
                    if (!opt.process(arg, args.subList(idx + 1, idx + 1 + opt.getArgumentCount()))) {
                        m = -1;
                    }
                    idx += opt.getArgumentCount();
                }
                return m * idx;
            }
        }
        // check if arg is accepted by the tool before emitting error
        if (!isToolOption) {
            text = log.getText("main.invalid_flag", arg);
            throw new OptionException(ERROR, this::showUsage, text);
        }
        return m * idx;
    }

    private static Set<? extends Option> getSupportedOptionsOf(Doclet doclet) {
        Set<? extends Option> options = doclet.getSupportedOptions();
        return options == null ? Set.of() : options;
    }

    /**
     * Performs an initial pass over the options, primarily to determine
     * the doclet to be used (if any), so that it may participate in the
     * main round of option decoding. This avoids having to specify that
     * the options to specify the doclet should appear before any options
     * that are handled by the doclet.
     *
     * The downside of this initial phase is that we have to skip over
     * unknown options, and assume that we can reliably detect the options
     * we need to handle.
     *
     * @param argv the arguments to be processed
     * @return the doclet
     * @throws ToolException if an error occurs initializing the doclet
     * @throws OptionException if an error occurs while processing an option
     */
    private Doclet preprocess(List<String> argv) throws ToolException, OptionException {
        // doclet specifying arguments
        String userDocletPath = null;
        String userDocletName = null;

        // Step 1: loop through the args, set locale early on, if found.
        for (int i = 0; i < argv.size(); i++) {
            String arg = argv.get(i);
            if (arg.equals(ToolOptions.DUMP_ON_ERROR)) {
                // although this option is not needed in order to initialize the doclet,
                // it is helpful if it is set before trying to initialize the doclet
                options.setDumpOnError(true);
            } else if (arg.equals(ToolOptions.LOCALE)) {
                checkOneArg(argv, i++);
                String lname = argv.get(i);
                locale = getLocale(lname);
            } else if (arg.equals(ToolOptions.DOCLET)) {
                checkOneArg(argv, i++);
                if (userDocletName != null) {
                    if (apiMode) {
                        throw new IllegalArgumentException("More than one doclet specified (" +
                                userDocletName + " and " + argv.get(i) + ").");
                    }
                    String text = log.getText("main.more_than_one_doclet_specified_0_and_1",
                            userDocletName, argv.get(i));
                    throw new ToolException(CMDERR, text);
                }
                if (docletName != null) {
                    if (apiMode) {
                        throw new IllegalArgumentException("More than one doclet specified (" +
                                docletName + " and " + argv.get(i) + ").");
                    }
                    String text = log.getText("main.more_than_one_doclet_specified_0_and_1",
                            docletName, argv.get(i));
                    throw new ToolException(CMDERR, text);
                }
                userDocletName = argv.get(i);
            } else if (arg.equals(ToolOptions.DOCLET_PATH)) {
                checkOneArg(argv, i++);
                if (userDocletPath == null) {
                    userDocletPath = argv.get(i);
                } else {
                    userDocletPath += File.pathSeparator + argv.get(i);
                }
            }
        }

        // Step 3: doclet name specified ? if so find a ClassLoader,
        // and load it.
        if (docletClass == null) {
            if (userDocletName != null) {
                ClassLoader cl = classLoader;
                if (cl == null) {
                    if (!fileManager.hasLocation(DOCLET_PATH)) {
                        List<File> paths = new ArrayList<>();
                        if (userDocletPath != null) {
                            for (String pathname : userDocletPath.split(File.pathSeparator)) {
                                paths.add(new File(pathname));
                            }
                        }
                        try {
                            ((StandardJavaFileManager)fileManager).setLocation(DOCLET_PATH, paths);
                        } catch (IOException ioe) {
                            if (apiMode) {
                                throw new IllegalArgumentException("Could not set location for " +
                                        userDocletPath, ioe);
                            }
                            String text = log.getText("main.doclet_could_not_set_location",
                                    userDocletPath);
                            throw new ToolException(CMDERR, text, ioe);
                        }
                    }
                    cl = fileManager.getClassLoader(DOCLET_PATH);
                    if (cl == null) {
                        // despite doclet specified on cmdline no classloader found!
                        if (apiMode) {
                            throw new IllegalArgumentException("Could not obtain classloader to load "

                                    + userDocletPath);
                        }
                        String text = log.getText("main.doclet_no_classloader_found",
                                userDocletName);
                        throw new ToolException(CMDERR, text);
                    }
                }
                docletClass = loadDocletClass(userDocletName, cl);
            } else if (docletName != null){
                docletClass = loadDocletClass(docletName, getClass().getClassLoader());
            } else {
                docletClass = StandardDoclet.class;
            }
        }

        if (Doclet.class.isAssignableFrom(docletClass)) {
            log.setLocale(Locale.getDefault());  // use default locale for console messages
            try {
                Object o = docletClass.getConstructor().newInstance();
                doclet = (Doclet) o;
            } catch (ReflectiveOperationException exc) {
                if (apiMode) {
                    throw new ClientCodeException(exc);
                }
                String text = log.getText("main.could_not_instantiate_class", docletClass.getName());
                throw new ToolException(ERROR, text);
            }
        } else {
            String text = log.getText("main.not_a_doclet", docletClass.getName());
            throw new ToolException(ERROR, text);
        }
        return doclet;
    }

    private Class<?> loadDocletClass(String docletName, ClassLoader classLoader) throws ToolException {
        try {
            return classLoader == null ? Class.forName(docletName) : classLoader.loadClass(docletName);
        } catch (ClassNotFoundException cnfe) {
            if (apiMode) {
                throw new IllegalArgumentException("Cannot find doclet class " + docletName);
            }
            String text = log.getText("main.doclet_class_not_found", docletName);
            throw new ToolException(CMDERR, text, cnfe);
        }
    }

    private boolean parseArgs(List<String> args, List<String> javaNames)
            throws OptionException, com.sun.tools.javac.main.Option.InvalidValueException
    {
        boolean success = true;
        for (int i = 0; i < args.size(); i++) {
            String arg = args.get(i);
            ToolOption o = options.getOption(arg);
            if (o != null) {
                // handle a doclet argument that may be needed however
                // don't increment the index, and allow the tool to consume args
                if (consumeDocletOption(i, args, true) < 0) {
                    success = false;
                }
                if (o.hasArg) {
                    if (arg.startsWith("--") && arg.contains("=")) {
                        o.process(arg.substring(arg.indexOf('=') + 1));
                    } else {
                        checkOneArg(args, i++);
                        o.process(args.get(i));
                    }
                } else if (o.hasSuffix) {
                    o.process(arg);
                } else {
                    o.process();
                }
            } else if (arg.startsWith("-XD")) {
                // hidden javac options
                String s = arg.substring("-XD".length());
                int eq = s.indexOf('=');
                String key = (eq < 0) ? s : s.substring(0, eq);
                String value = (eq < 0) ? s : s.substring(eq + 1);
                options.compilerOptions().put(key, value);
            } else if (arg.startsWith("-")) {
                i = consumeDocletOption(i, args, false);
                if (i < 0) {
                    i = -i;
                    success = false;
                }
            } else {
                javaNames.add(arg);
            }
        }
        return success;
    }

    private <T> boolean isEmpty(Iterable<T> iter) {
        return !iter.iterator().hasNext();
    }

    /**
     * Check the one arg option.
     * Error and exit if one argument is not provided.
     */
    private void checkOneArg(List<String> args, int index) throws OptionException {
        if ((index + 1) >= args.size() || args.get(index + 1).startsWith("-d")) {
            String text = log.getText("main.requires_argument", args.get(index));
            throw new OptionException(CMDERR, this::showUsage, text);
        }
    }

    void error(String key, Object... args) {
        log.printErrorUsingKey(key, args);
    }

    /**
     * Get the locale if specified on the command line
     * else return null and if locale option is not used
     * then return default locale.
     */
    private Locale getLocale(String localeName) throws ToolException {
        try {
            // Tolerate, at least for a while, the older syntax accepted by javadoc,
            // using _ as the separator
            localeName = localeName.replace("_", "-");
            Locale l =  new Locale.Builder().setLanguageTag(localeName).build();
            // Ensure that a non-empty language is available for the <HTML lang=...> element
            return (l.getLanguage().isEmpty()) ? Locale.ENGLISH : l;
        } catch (IllformedLocaleException e) {
            String text = log.getText("main.malformed_locale_name", localeName);
            throw new ToolException(CMDERR, text);
        }
    }

}
