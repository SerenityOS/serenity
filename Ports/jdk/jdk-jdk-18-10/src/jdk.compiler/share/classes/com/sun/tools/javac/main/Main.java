/*
 * Copyright (c) 1999, 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.javac.main;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.tools.JavaFileManager;

import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.file.CacheFSInfo;
import com.sun.tools.javac.file.BaseFileManager;
import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.jvm.Target;
import com.sun.tools.javac.main.CommandLine.UnmatchedQuote;
import com.sun.tools.javac.platform.PlatformDescription;
import com.sun.tools.javac.processing.AnnotationProcessingError;
import com.sun.tools.javac.resources.CompilerProperties.Errors;
import com.sun.tools.javac.util.*;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticInfo;
import com.sun.tools.javac.util.Log.PrefixKind;
import com.sun.tools.javac.util.Log.WriterKind;

/** This class provides a command line interface to the javac compiler.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class Main {

    /** The name of the compiler, for use in diagnostics.
     */
    String ownName;

    /** The writer to use for normal output.
     */
    PrintWriter stdOut;

    /** The writer to use for diagnostic output.
     */
    PrintWriter stdErr;

    /** The log to use for diagnostic output.
     */
    public Log log;

    /**
     * If true, certain errors will cause an exception, such as command line
     * arg errors, or exceptions in user provided code.
     */
    boolean apiMode;

    private static final String ENV_OPT_NAME = "JDK_JAVAC_OPTIONS";

    /** Result codes.
     */
    public enum Result {
        OK(0),        // Compilation completed with no errors.
        ERROR(1),     // Completed but reported errors.
        CMDERR(2),    // Bad command-line arguments
        SYSERR(3),    // System error or resource exhaustion.
        ABNORMAL(4);  // Compiler terminated abnormally

        Result(int exitCode) {
            this.exitCode = exitCode;
        }

        public boolean isOK() {
            return (exitCode == 0);
        }

        public final int exitCode;
    }

    /**
     * Construct a compiler instance.
     * @param name the name of this tool
     */
    public Main(String name) {
        this.ownName = name;
    }

    /**
     * Construct a compiler instance.
     * @param name the name of this tool
     * @param out a stream to which to write messages
     */
    public Main(String name, PrintWriter out) {
        this.ownName = name;
        this.stdOut = this.stdErr = out;
    }

    /**
     * Construct a compiler instance.
     * @param name the name of this tool
     * @param out a stream to which to write expected output
     * @param err a stream to which to write diagnostic output
     */
    public Main(String name, PrintWriter out, PrintWriter err) {
        this.ownName = name;
        this.stdOut = out;
        this.stdErr = err;
    }

    /** Report a usage error.
     */
    void reportDiag(DiagnosticInfo diag) {
        if (apiMode) {
            String msg = log.localize(diag);
            throw new PropagatedException(new IllegalStateException(msg));
        }
        reportHelper(diag);
        log.printLines(PrefixKind.JAVAC, "msg.usage", ownName);
    }

    /** Report helper.
     */
    void reportHelper(DiagnosticInfo diag) {
        String msg = log.localize(diag);
        String errorPrefix = log.localize(Errors.Error);
        msg = msg.startsWith(errorPrefix) ? msg : errorPrefix + msg;
        log.printRawLines(msg);
    }


    /**
     * Programmatic interface for main function.
     * @param args  the command line parameters
     * @return the result of the compilation
     */
    public Result compile(String[] args) {
        Context context = new Context();
        JavacFileManager.preRegister(context); // can't create it until Log has been set up
        Result result = compile(args, context);
        try {
            // A fresh context was created above, so the file manager can be safely closed:
            if (fileManager != null)
                fileManager.close();
        } catch (IOException ex) {
            bugMessage(ex);
        }
        return result;
    }

    /**
     * Internal version of compile, allowing context to be provided.
     * Note that the context needs to have a file manager set up.
     * @param argv  the command line parameters
     * @param context the context
     * @return the result of the compilation
     */
    public Result compile(String[] argv, Context context) {
        if (stdOut != null) {
            context.put(Log.outKey, stdOut);
        }

        if (stdErr != null) {
            context.put(Log.errKey, stdErr);
        }

        log = Log.instance(context);

        if (argv.length == 0) {
            OptionHelper h = new OptionHelper.GrumpyHelper(log) {
                @Override
                public String getOwnName() { return ownName; }
                @Override
                public void put(String name, String value) { }
            };
            try {
                Option.HELP.process(h, "-help");
            } catch (Option.InvalidValueException ignore) {
            }
            return Result.CMDERR;
        }

        // prefix argv with contents of environment variable and expand @-files
        Iterable<String> allArgs;
        try {
            allArgs = CommandLine.parse(ENV_OPT_NAME, List.from(argv));
        } catch (UnmatchedQuote ex) {
            reportDiag(Errors.UnmatchedQuote(ex.variableName));
            return Result.CMDERR;
        } catch (FileNotFoundException | NoSuchFileException e) {
            reportHelper(Errors.FileNotFound(e.getMessage()));
            return Result.SYSERR;
        } catch (IOException ex) {
            log.printLines(PrefixKind.JAVAC, "msg.io");
            ex.printStackTrace(log.getWriter(WriterKind.NOTICE));
            return Result.SYSERR;
        }

        Arguments args = Arguments.instance(context);
        args.init(ownName, allArgs);

        if (log.nerrors > 0)
            return Result.CMDERR;

        Options options = Options.instance(context);

        // init Log
        boolean forceStdOut = options.isSet("stdout");
        if (forceStdOut) {
            log.flush();
            log.setWriters(new PrintWriter(System.out, true));
        }

        // init CacheFSInfo
        // allow System property in following line as a Mustang legacy
        boolean batchMode = (options.isUnset("nonBatchMode")
                    && System.getProperty("nonBatchMode") == null);
        if (batchMode)
            CacheFSInfo.preRegister(context);

        boolean ok = true;

        // init file manager
        fileManager = context.get(JavaFileManager.class);
        JavaFileManager undel = fileManager instanceof DelegatingJavaFileManager delegatingJavaFileManager ?
                delegatingJavaFileManager.getBaseFileManager() : fileManager;
        if (undel instanceof BaseFileManager baseFileManager) {
            baseFileManager.setContext(context); // reinit with options
            ok &= baseFileManager.handleOptions(args.getDeferredFileManagerOptions());
        }

        // handle this here so it works even if no other options given
        String showClass = options.get("showClass");
        if (showClass != null) {
            if (showClass.equals("showClass")) // no value given for option
                showClass = "com.sun.tools.javac.Main";
            showClass(showClass);
        }

        ok &= args.validate();
        if (!ok || log.nerrors > 0)
            return Result.CMDERR;

        if (args.isEmpty())
            return Result.OK;

        // init Dependencies
        if (options.isSet("debug.completionDeps")) {
            Dependencies.GraphDependencies.preRegister(context);
        }

        BasicJavacTask t = (BasicJavacTask) BasicJavacTask.instance(context);

        // init plugins
        Set<List<String>> pluginOpts = args.getPluginOpts();
        t.initPlugins(pluginOpts);

        // init multi-release jar handling
        if (fileManager.isSupportedOption(Option.MULTIRELEASE.primaryName) == 1) {
            Target target = Target.instance(context);
            List<String> list = List.of(target.multiReleaseValue());
            fileManager.handleOption(Option.MULTIRELEASE.primaryName, list.iterator());
        }

        // init JavaCompiler
        JavaCompiler comp = JavaCompiler.instance(context);

        // init doclint
        List<String> docLintOpts = args.getDocLintOpts();
        if (!docLintOpts.isEmpty()) {
            t.initDocLint(docLintOpts);
        }

        if (options.get(Option.XSTDOUT) != null) {
            // Stdout reassigned - ask compiler to close it when it is done
            comp.closeables = comp.closeables.prepend(log.getWriter(WriterKind.NOTICE));
        }

        boolean printArgsToFile = options.isSet("printArgsToFile");
        try {
            comp.compile(args.getFileObjects(), args.getClassNames(), null, List.nil());

            if (log.expectDiagKeys != null) {
                if (log.expectDiagKeys.isEmpty()) {
                    log.printRawLines("all expected diagnostics found");
                    return Result.OK;
                } else {
                    log.printRawLines("expected diagnostic keys not found: " + log.expectDiagKeys);
                    return Result.ERROR;
                }
            }

            return (comp.errorCount() == 0) ? Result.OK : Result.ERROR;

        } catch (OutOfMemoryError | StackOverflowError ex) {
            resourceMessage(ex);
            return Result.SYSERR;
        } catch (FatalError ex) {
            feMessage(ex, options);
            return Result.SYSERR;
        } catch (AnnotationProcessingError ex) {
            apMessage(ex);
            return Result.SYSERR;
        } catch (PropagatedException ex) {
            // TODO: what about errors from plugins?   should not simply rethrow the error here
            throw ex.getCause();
        } catch (IllegalAccessError iae) {
            if (twoClassLoadersInUse(iae)) {
                bugMessage(iae);
            }
            printArgsToFile = true;
            return Result.ABNORMAL;
        } catch (Throwable ex) {
            // Nasty.  If we've already reported an error, compensate
            // for buggy compiler error recovery by swallowing thrown
            // exceptions.
            if (comp == null || comp.errorCount() == 0 || options.isSet("dev"))
                bugMessage(ex);
            printArgsToFile = true;
            return Result.ABNORMAL;
        } finally {
            if (printArgsToFile) {
                printArgumentsToFile(argv);
            }
            if (comp != null) {
                try {
                    comp.close();
                } catch (ClientCodeException ex) {
                    throw new RuntimeException(ex.getCause());
                }
            }
        }
    }

    void printArgumentsToFile(String... params) {
        Path out = Paths.get(String.format("javac.%s.args",
                new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime())));
        String strOut = "";
        try {
            try (Writer w = Files.newBufferedWriter(out)) {
                for (String param : params) {
                    param = param.replaceAll("\\\\", "\\\\\\\\");
                    if (param.matches(".*\\s+.*")) {
                        param = "\"" + param + "\"";
                    }
                    strOut += param + '\n';
                }
                w.write(strOut);
            }
            log.printLines(PrefixKind.JAVAC, "msg.parameters.output", out.toAbsolutePath());
        } catch (IOException ioe) {
            log.printLines(PrefixKind.JAVAC, "msg.parameters.output.error", out.toAbsolutePath());
            System.err.println(strOut);
            System.err.println();
        }
    }

    private boolean twoClassLoadersInUse(IllegalAccessError iae) {
        String msg = iae.getMessage();
        Pattern pattern = Pattern.compile("(?i)(?<=tried to access class )([a-z_$][a-z\\d_$]*\\.)*[a-z_$][a-z\\d_$]*");
        Matcher matcher = pattern.matcher(msg);
        if (matcher.find()) {
            try {
                String otherClassName = matcher.group(0);
                Class<?> otherClass = Class.forName(otherClassName);
                ClassLoader otherClassLoader = otherClass.getClassLoader();
                ClassLoader javacClassLoader = this.getClass().getClassLoader();
                if (javacClassLoader != otherClassLoader) {
                    CodeSource otherClassCodeSource = otherClass.getProtectionDomain().getCodeSource();
                    CodeSource javacCodeSource = this.getClass().getProtectionDomain().getCodeSource();
                    if (otherClassCodeSource != null && javacCodeSource != null) {
                        log.printLines(Errors.TwoClassLoaders2(otherClassCodeSource.getLocation(),
                                javacCodeSource.getLocation()));
                    } else {
                        log.printLines(Errors.TwoClassLoaders1);
                    }
                    return true;
                }
            } catch (Throwable t) {
                return false;
            }
        }
        return false;
    }

    /** Print a message reporting an internal error.
     */
    void bugMessage(Throwable ex) {
        log.printLines(PrefixKind.JAVAC, "msg.bug", JavaCompiler.version());
        ex.printStackTrace(log.getWriter(WriterKind.NOTICE));
    }

    /** Print a message reporting a fatal error.
     */
    void feMessage(Throwable ex, Options options) {
        log.printRawLines(ex.getMessage());
        if (ex.getCause() != null && options.isSet("dev")) {
            ex.getCause().printStackTrace(log.getWriter(WriterKind.NOTICE));
        }
    }

    /** Print a message reporting an input/output error.
     */
    void ioMessage(Throwable ex) {
        log.printLines(PrefixKind.JAVAC, "msg.io");
        ex.printStackTrace(log.getWriter(WriterKind.NOTICE));
    }

    /** Print a message reporting an out-of-resources error.
     */
    void resourceMessage(Throwable ex) {
        log.printLines(PrefixKind.JAVAC, "msg.resource");
        ex.printStackTrace(log.getWriter(WriterKind.NOTICE));
    }

    /** Print a message reporting an uncaught exception from an
     * annotation processor.
     */
    void apMessage(AnnotationProcessingError ex) {
        log.printLines(PrefixKind.JAVAC, "msg.proc.annotation.uncaught.exception");
        ex.getCause().printStackTrace(log.getWriter(WriterKind.NOTICE));
    }

    /** Print a message reporting an uncaught exception from an
     * annotation processor.
     */
    void pluginMessage(Throwable ex) {
        log.printLines(PrefixKind.JAVAC, "msg.plugin.uncaught.exception");
        ex.printStackTrace(log.getWriter(WriterKind.NOTICE));
    }

    /** Display the location and checksum of a class. */
    void showClass(String className) {
        PrintWriter pw = log.getWriter(WriterKind.NOTICE);
        pw.println("javac: show class: " + className);

        URL url = getClass().getResource('/' + className.replace('.', '/') + ".class");
        if (url != null) {
            pw.println("  " + url);
        }

        try (InputStream in = getClass().getResourceAsStream('/' + className.replace('.', '/') + ".class")) {
            final String algorithm = "SHA-256";
            byte[] digest;
            MessageDigest md = MessageDigest.getInstance(algorithm);
            try (DigestInputStream din = new DigestInputStream(in, md)) {
                byte[] buf = new byte[8192];
                int n;
                do { n = din.read(buf); } while (n > 0);
                digest = md.digest();
            }
            StringBuilder sb = new StringBuilder();
            for (byte b: digest)
                sb.append(String.format("%02x", b));
            pw.println("  " + algorithm + " checksum: " + sb);
        } catch (NoSuchAlgorithmException | IOException e) {
            pw.println("  cannot compute digest: " + e);
        }
    }

    // TODO: update this to JavacFileManager
    private JavaFileManager fileManager;

    /* ************************************************************************
     * Internationalization
     *************************************************************************/

    public static final String javacBundleName =
        "com.sun.tools.javac.resources.javac";
}
