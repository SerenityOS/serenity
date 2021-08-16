/*
 * Copyright (c) 2007, 2020, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.javap;

import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.FilterInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.NoSuchFileException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.Set;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileManager.Location;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;

import com.sun.tools.classfile.*;

/**
 *  "Main" class for javap, normally accessed from the command line
 *  via Main, or from JSR199 via DisassemblerTool.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class JavapTask implements DisassemblerTool.DisassemblerTask, Messages {
    public class BadArgs extends Exception {
        static final long serialVersionUID = 8765093759964640721L;
        BadArgs(String key, Object... args) {
            super(JavapTask.this.getMessage(key, args));
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
    }

    static abstract class Option {
        Option(boolean hasArg, String... aliases) {
            this.hasArg = hasArg;
            this.aliases = aliases;
        }

        boolean matches(String opt) {
            for (String a: aliases) {
                if (a.equals(opt))
                    return true;
            }
            return false;
        }

        boolean ignoreRest() {
            return false;
        }

        abstract void process(JavapTask task, String opt, String arg) throws BadArgs;

        final boolean hasArg;
        final String[] aliases;
    }

    static final Option[] recognizedOptions = {

        new Option(false, "-help", "--help", "-?", "-h") {
            @Override
            void process(JavapTask task, String opt, String arg) {
                task.options.help = true;
            }
        },

        new Option(false, "-version") {
            @Override
            void process(JavapTask task, String opt, String arg) {
                task.options.version = true;
            }
        },

        new Option(false, "-fullversion") {
            @Override
            void process(JavapTask task, String opt, String arg) {
                task.options.fullVersion = true;
            }
        },

        new Option(false, "-v", "-verbose", "-all") {
            @Override
            void process(JavapTask task, String opt, String arg) {
                task.options.verbose = true;
                task.options.showDescriptors = true;
                task.options.showFlags = true;
                task.options.showAllAttrs = true;
            }
        },

        new Option(false, "-l") {
            @Override
            void process(JavapTask task, String opt, String arg) {
                task.options.showLineAndLocalVariableTables = true;
            }
        },

        new Option(false, "-public") {
            @Override
            void process(JavapTask task, String opt, String arg) {
                task.options.accessOptions.add(opt);
                task.options.showAccess = AccessFlags.ACC_PUBLIC;
            }
        },

        new Option(false, "-protected") {
            @Override
            void process(JavapTask task, String opt, String arg) {
                task.options.accessOptions.add(opt);
                task.options.showAccess = AccessFlags.ACC_PROTECTED;
            }
        },

        new Option(false, "-package") {
            @Override
            void process(JavapTask task, String opt, String arg) {
                task.options.accessOptions.add(opt);
                task.options.showAccess = 0;
            }
        },

        new Option(false, "-p", "-private") {
            @Override
            void process(JavapTask task, String opt, String arg) {
                if (!task.options.accessOptions.contains("-p") &&
                        !task.options.accessOptions.contains("-private")) {
                    task.options.accessOptions.add(opt);
                }
                task.options.showAccess = AccessFlags.ACC_PRIVATE;
            }
        },

        new Option(false, "-c") {
            @Override
            void process(JavapTask task, String opt, String arg) {
                task.options.showDisassembled = true;
            }
        },

        new Option(false, "-s") {
            @Override
            void process(JavapTask task, String opt, String arg) {
                task.options.showDescriptors = true;
            }
        },

        new Option(false, "-sysinfo") {
            @Override
            void process(JavapTask task, String opt, String arg) {
                task.options.sysInfo = true;
            }
        },

        new Option(false, "-XDdetails") {
            @Override
            void process(JavapTask task, String opt, String arg) {
                task.options.details = EnumSet.allOf(InstructionDetailWriter.Kind.class);
            }

        },

        new Option(false, "-XDdetails:") {
            @Override
            boolean matches(String opt) {
                int sep = opt.indexOf(":");
                return sep != -1 && super.matches(opt.substring(0, sep + 1));
            }

            @Override
            void process(JavapTask task, String opt, String arg) throws BadArgs {
                int sep = opt.indexOf(":");
                for (String v: opt.substring(sep + 1).split("[,: ]+")) {
                    if (!handleArg(task, v))
                        throw task.new BadArgs("err.invalid.arg.for.option", v);
                }
            }

            boolean handleArg(JavapTask task, String arg) {
                if (arg.length() == 0)
                    return true;

                if (arg.equals("all")) {
                    task.options.details = EnumSet.allOf(InstructionDetailWriter.Kind.class);
                    return true;
                }

                boolean on = true;
                if (arg.startsWith("-")) {
                    on = false;
                    arg = arg.substring(1);
                }

                for (InstructionDetailWriter.Kind k: InstructionDetailWriter.Kind.values()) {
                    if (arg.equalsIgnoreCase(k.option)) {
                        if (on)
                            task.options.details.add(k);
                        else
                            task.options.details.remove(k);
                        return true;
                    }
                }
                return false;
            }
        },

        new Option(false, "-constants") {
            @Override
            void process(JavapTask task, String opt, String arg) {
                task.options.showConstants = true;
            }
        },

        new Option(false, "-XDinner") {
            @Override
            void process(JavapTask task, String opt, String arg) {
                task.options.showInnerClasses = true;
            }
        },

        new Option(false, "-XDindent:") {
            @Override
            boolean matches(String opt) {
                int sep = opt.indexOf(":");
                return sep != -1 && super.matches(opt.substring(0, sep + 1));
            }

            @Override
            void process(JavapTask task, String opt, String arg) throws BadArgs {
                int sep = opt.indexOf(":");
                try {
                    int i = Integer.valueOf(opt.substring(sep + 1));
                    if (i > 0) // silently ignore invalid values
                        task.options.indentWidth = i;
                } catch (NumberFormatException e) {
                }
            }
        },

        new Option(false, "-XDtab:") {
            @Override
            boolean matches(String opt) {
                int sep = opt.indexOf(":");
                return sep != -1 && super.matches(opt.substring(0, sep + 1));
            }

            @Override
            void process(JavapTask task, String opt, String arg) throws BadArgs {
                int sep = opt.indexOf(":");
                try {
                    int i = Integer.valueOf(opt.substring(sep + 1));
                    if (i > 0) // silently ignore invalid values
                        task.options.tabColumn = i;
                } catch (NumberFormatException e) {
                }
            }
        },

        new Option(true, "--module", "-m") {
            @Override
            void process(JavapTask task, String opt, String arg) throws BadArgs {
                task.options.moduleName = arg;
            }
        },

        // this option is processed by the launcher, and cannot be used when invoked via
        // an API like ToolProvider. It exists here to be documented in the command-line help.
        new Option(false, "-J") {
            @Override
            boolean matches(String opt) {
                return opt.startsWith("-J");
            }

            @Override
            void process(JavapTask task, String opt, String arg) throws BadArgs {
                throw task.new BadArgs("err.only.for.launcher");
            }
        }

    };

    public JavapTask() {
        context = new Context();
        context.put(Messages.class, this);
        options = Options.instance(context);
        attributeFactory = new Attribute.Factory();
    }

    public JavapTask(Writer out,
            JavaFileManager fileManager,
            DiagnosticListener<? super JavaFileObject> diagnosticListener) {
        this();
        this.log = getPrintWriterForWriter(out);
        this.fileManager = fileManager;
        this.diagnosticListener = diagnosticListener;
    }

    public JavapTask(Writer out,
            JavaFileManager fileManager,
            DiagnosticListener<? super JavaFileObject> diagnosticListener,
            Iterable<String> options,
            Iterable<String> classes) {
        this(out, fileManager, diagnosticListener);

        this.classes = new ArrayList<>();
        for (String classname: classes) {
            Objects.requireNonNull(classname);
            this.classes.add(classname);
        }

        try {
            if (options != null)
                handleOptions(options, false);
        } catch (BadArgs e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    public void setLocale(Locale locale) {
        if (locale == null)
            locale = Locale.getDefault();
        task_locale = locale;
    }

    public void setLog(Writer log) {
        this.log = getPrintWriterForWriter(log);
    }

    public void setLog(OutputStream s) {
        setLog(getPrintWriterForStream(s));
    }

    private static PrintWriter getPrintWriterForStream(OutputStream s) {
        return new PrintWriter(s == null ? System.err : s, true);
    }

    private static PrintWriter getPrintWriterForWriter(Writer w) {
        if (w == null)
            return getPrintWriterForStream(null);
        else if (w instanceof PrintWriter)
            return (PrintWriter) w;
        else
            return new PrintWriter(w, true);
    }

    public void setDiagnosticListener(DiagnosticListener<? super JavaFileObject> dl) {
        diagnosticListener = dl;
    }

    public void setDiagnosticListener(OutputStream s) {
        setDiagnosticListener(getDiagnosticListenerForStream(s));
    }

    private DiagnosticListener<JavaFileObject> getDiagnosticListenerForStream(OutputStream s) {
        return getDiagnosticListenerForWriter(getPrintWriterForStream(s));
    }

    private DiagnosticListener<JavaFileObject> getDiagnosticListenerForWriter(Writer w) {
        final PrintWriter pw = getPrintWriterForWriter(w);
        return diagnostic -> {
            switch (diagnostic.getKind()) {
                case ERROR:
                    pw.print(getMessage("err.prefix"));
                    break;
                case WARNING:
                    pw.print(getMessage("warn.prefix"));
                    break;
                case NOTE:
                    pw.print(getMessage("note.prefix"));
                    break;
            }
            pw.print(" ");
            pw.println(diagnostic.getMessage(null));
        };
    }

    /** Result codes.
     */
    static final int
        EXIT_OK = 0,        // Compilation completed with no errors.
        EXIT_ERROR = 1,     // Completed but reported errors.
        EXIT_CMDERR = 2,    // Bad command-line arguments
        EXIT_SYSERR = 3,    // System error or resource exhaustion.
        EXIT_ABNORMAL = 4;  // Compiler terminated abnormally

    int run(String[] args) {
        try {
            try {
                handleOptions(args);

                // the following gives consistent behavior with javac
                if (classes == null || classes.size() == 0) {
                    if (options.help || options.version || options.fullVersion)
                        return EXIT_OK;
                    else
                        return EXIT_CMDERR;
                }

                return run();
            } finally {
                if (defaultFileManager != null) {
                    try {
                        defaultFileManager.close();
                        defaultFileManager = null;
                    } catch (IOException e) {
                        throw new InternalError(e);
                    }
                }
            }
        } catch (BadArgs e) {
            reportError(e.key, e.args);
            if (e.showUsage) {
                printLines(getMessage("main.usage.summary", progname));
            }
            return EXIT_CMDERR;
        } catch (InternalError e) {
            Object[] e_args;
            if (e.getCause() == null)
                e_args = e.args;
            else {
                e_args = new Object[e.args.length + 1];
                e_args[0] = e.getCause();
                System.arraycopy(e.args, 0, e_args, 1, e.args.length);
            }
            reportError("err.internal.error", e_args);
            return EXIT_ABNORMAL;
        } finally {
            log.flush();
        }
    }

    public void handleOptions(String[] args) throws BadArgs {
        handleOptions(Arrays.asList(args), true);
    }

    private void handleOptions(Iterable<String> args, boolean allowClasses) throws BadArgs {
        if (log == null) {
            log = getPrintWriterForStream(System.out);
            if (diagnosticListener == null)
              diagnosticListener = getDiagnosticListenerForStream(System.err);
        } else {
            if (diagnosticListener == null)
              diagnosticListener = getDiagnosticListenerForWriter(log);
        }


        if (fileManager == null)
            fileManager = getDefaultFileManager(diagnosticListener, log);

        Iterator<String> iter = args.iterator();
        boolean noArgs = !iter.hasNext();

        while (iter.hasNext()) {
            String arg = iter.next();
            if (arg.startsWith("-"))
                handleOption(arg, iter);
            else if (allowClasses) {
                if (classes == null)
                    classes = new ArrayList<>();
                classes.add(arg);
                while (iter.hasNext())
                    classes.add(iter.next());
            } else
                throw new BadArgs("err.unknown.option", arg).showUsage(true);
        }

        if (options.accessOptions.size() > 1) {
            StringBuilder sb = new StringBuilder();
            for (String opt: options.accessOptions) {
                if (sb.length() > 0)
                    sb.append(" ");
                sb.append(opt);
            }
            throw new BadArgs("err.incompatible.options", sb);
        }

        if ((classes == null || classes.size() == 0) &&
                !(noArgs || options.help || options.version || options.fullVersion)) {
            throw new BadArgs("err.no.classes.specified");
        }

        if (noArgs || options.help)
            showHelp();

        if (options.version || options.fullVersion)
            showVersion(options.fullVersion);
    }

    private void handleOption(String name, Iterator<String> rest) throws BadArgs {
        for (Option o: recognizedOptions) {
            if (o.matches(name)) {
                if (o.hasArg) {
                    if (rest.hasNext())
                        o.process(this, name, rest.next());
                    else
                        throw new BadArgs("err.missing.arg", name).showUsage(true);
                } else
                    o.process(this, name, null);

                if (o.ignoreRest()) {
                    while (rest.hasNext())
                        rest.next();
                }
                return;
            }
        }

        try {
            if (fileManager.handleOption(name, rest))
                return;
        } catch (IllegalArgumentException e) {
            throw new BadArgs("err.invalid.use.of.option", name).showUsage(true);
        }

        throw new BadArgs("err.unknown.option", name).showUsage(true);
    }

    public Boolean call() {
        return run() == 0;
    }

    public int run() {
        if (classes == null || classes.isEmpty()) {
            return EXIT_ERROR;
        }

        context.put(PrintWriter.class, log);
        ClassWriter classWriter = ClassWriter.instance(context);
        SourceWriter sourceWriter = SourceWriter.instance(context);
        sourceWriter.setFileManager(fileManager);

        if (options.moduleName != null) {
            try {
                moduleLocation = findModule(options.moduleName);
                if (moduleLocation == null) {
                    reportError("err.cant.find.module", options.moduleName);
                    return EXIT_ERROR;
                }
            } catch (IOException e) {
                reportError("err.cant.find.module.ex", options.moduleName, e);
                return EXIT_ERROR;
            }
        }

        int result = EXIT_OK;

        for (String className: classes) {
            try {
                result = writeClass(classWriter, className);
            } catch (ConstantPoolException e) {
                reportError("err.bad.constant.pool", className, e.getLocalizedMessage());
                result = EXIT_ERROR;
            } catch (EOFException e) {
                reportError("err.end.of.file", className);
                result = EXIT_ERROR;
            } catch (FileNotFoundException | NoSuchFileException e) {
                reportError("err.file.not.found", e.getLocalizedMessage());
                result = EXIT_ERROR;
            } catch (IOException e) {
                //e.printStackTrace();
                Object msg = e.getLocalizedMessage();
                if (msg == null) {
                    msg = e;
                }
                reportError("err.ioerror", className, msg);
                result = EXIT_ERROR;
            } catch (OutOfMemoryError e) {
                reportError("err.nomem");
                result = EXIT_ERROR;
            } catch (FatalError e) {
                Object msg = e.getLocalizedMessage();
                if (msg == null) {
                    msg = e;
                }
                reportError("err.fatal.err", msg);
                result = EXIT_ERROR;
            } catch (Throwable t) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                t.printStackTrace(pw);
                pw.close();
                reportError("err.crash", t.toString(), sw.toString());
                result = EXIT_ABNORMAL;
            }
        }

        return result;
    }

    protected int writeClass(ClassWriter classWriter, String className)
            throws IOException, ConstantPoolException {
        JavaFileObject fo = open(className);
        if (fo == null) {
            reportError("err.class.not.found", className);
            return EXIT_ERROR;
        }

        ClassFileInfo cfInfo = read(fo);
        if (!className.endsWith(".class")) {
            if (cfInfo.cf.this_class == 0) {
                if (!className.equals("module-info")) {
                    reportWarning("warn.unexpected.class", fo.getName(), className);
                }
            } else {
                String cfName = cfInfo.cf.getName();
                if (!cfName.replaceAll("[/$]", ".").equals(className.replaceAll("[/$]", "."))) {
                    reportWarning("warn.unexpected.class", fo.getName(), className);
                }
            }
        }
        write(cfInfo);

        if (options.showInnerClasses) {
            ClassFile cf = cfInfo.cf;
            Attribute a = cf.getAttribute(Attribute.InnerClasses);
            if (a instanceof InnerClasses_attribute) {
                InnerClasses_attribute inners = (InnerClasses_attribute) a;
                try {
                    int result = EXIT_OK;
                    for (int i = 0; i < inners.classes.length; i++) {
                        int outerIndex = inners.classes[i].outer_class_info_index;
                        ConstantPool.CONSTANT_Class_info outerClassInfo = cf.constant_pool.getClassInfo(outerIndex);
                        String outerClassName = outerClassInfo.getName();
                        if (outerClassName.equals(cf.getName())) {
                            int innerIndex = inners.classes[i].inner_class_info_index;
                            ConstantPool.CONSTANT_Class_info innerClassInfo = cf.constant_pool.getClassInfo(innerIndex);
                            String innerClassName = innerClassInfo.getName();
                            classWriter.println("// inner class " + innerClassName.replaceAll("[/$]", "."));
                            classWriter.println();
                            result = writeClass(classWriter, innerClassName);
                            if (result != EXIT_OK) return result;
                        }
                    }
                    return result;
                } catch (ConstantPoolException e) {
                    reportError("err.bad.innerclasses.attribute", className);
                    return EXIT_ERROR;
                }
            } else if (a != null) {
                reportError("err.bad.innerclasses.attribute", className);
                return EXIT_ERROR;
            }
        }

        return EXIT_OK;
    }

    protected JavaFileObject open(String className) throws IOException {
        // for compatibility, first see if it is a class name
        JavaFileObject fo = getClassFileObject(className);
        if (fo != null)
            return fo;

        // see if it is an inner class, by replacing dots to $, starting from the right
        String cn = className;
        int lastDot;
        while ((lastDot = cn.lastIndexOf(".")) != -1) {
            cn = cn.substring(0, lastDot) + "$" + cn.substring(lastDot + 1);
            fo = getClassFileObject(cn);
            if (fo != null)
                return fo;
        }

        if (!className.endsWith(".class"))
            return null;

        if (fileManager instanceof StandardJavaFileManager) {
            StandardJavaFileManager sfm = (StandardJavaFileManager) fileManager;
            try {
                fo = sfm.getJavaFileObjects(className).iterator().next();
                if (fo != null && fo.getLastModified() != 0) {
                    return fo;
                }
            } catch (IllegalArgumentException ignore) {
            }
        }

        // see if it is a URL, and if so, wrap it in just enough of a JavaFileObject
        // to suit javap's needs
        if (className.matches("^[A-Za-z]+:.*")) {
            try {
                final URI uri = new URI(className);
                final URL url = uri.toURL();
                final URLConnection conn = url.openConnection();
                conn.setUseCaches(false);
                return new JavaFileObject() {
                    public Kind getKind() {
                        return JavaFileObject.Kind.CLASS;
                    }

                    public boolean isNameCompatible(String simpleName, Kind kind) {
                        throw new UnsupportedOperationException();
                    }

                    public NestingKind getNestingKind() {
                        throw new UnsupportedOperationException();
                    }

                    public Modifier getAccessLevel() {
                        throw new UnsupportedOperationException();
                    }

                    public URI toUri() {
                        return uri;
                    }

                    public String getName() {
                        return uri.toString();
                    }

                    public InputStream openInputStream() throws IOException {
                        return conn.getInputStream();
                    }

                    public OutputStream openOutputStream() throws IOException {
                        throw new UnsupportedOperationException();
                    }

                    public Reader openReader(boolean ignoreEncodingErrors) throws IOException {
                        throw new UnsupportedOperationException();
                    }

                    public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
                        throw new UnsupportedOperationException();
                    }

                    public Writer openWriter() throws IOException {
                        throw new UnsupportedOperationException();
                    }

                    public long getLastModified() {
                        return conn.getLastModified();
                    }

                    public boolean delete() {
                        throw new UnsupportedOperationException();
                    }

                };
            } catch (URISyntaxException | IOException ignore) {
            }
        }

        return null;
    }

    public static class ClassFileInfo {
        ClassFileInfo(JavaFileObject fo, ClassFile cf, byte[] digest, int size) {
            this.fo = fo;
            this.cf = cf;
            this.digest = digest;
            this.size = size;
        }
        public final JavaFileObject fo;
        public final ClassFile cf;
        public final byte[] digest;
        public final int size;
    }

    public ClassFileInfo read(JavaFileObject fo) throws IOException, ConstantPoolException {
        InputStream in = fo.openInputStream();
        try {
            SizeInputStream sizeIn = null;
            MessageDigest md  = null;
            if (options.sysInfo || options.verbose) {
                try {
                    md = MessageDigest.getInstance("SHA-256");
                } catch (NoSuchAlgorithmException ignore) {
                }
                in = new DigestInputStream(in, md);
                in = sizeIn = new SizeInputStream(in);
            }

            ClassFile cf = ClassFile.read(in, attributeFactory);
            byte[] digest = (md == null) ? null : md.digest();
            int size = (sizeIn == null) ? -1 : sizeIn.size();
            return new ClassFileInfo(fo, cf, digest, size);
        } finally {
            in.close();
        }
    }

    public void write(ClassFileInfo info) {
        ClassWriter classWriter = ClassWriter.instance(context);
        if (options.sysInfo || options.verbose) {
            classWriter.setFile(info.fo.toUri());
            classWriter.setLastModified(info.fo.getLastModified());
            classWriter.setDigest("SHA-256", info.digest);
            classWriter.setFileSize(info.size);
        }

        classWriter.write(info.cf);
    }

    protected void setClassFile(ClassFile classFile) {
        ClassWriter classWriter = ClassWriter.instance(context);
        classWriter.setClassFile(classFile);
    }

    protected void setMethod(Method enclosingMethod) {
        ClassWriter classWriter = ClassWriter.instance(context);
        classWriter.setMethod(enclosingMethod);
    }

    protected void write(Attribute value) {
        AttributeWriter attrWriter = AttributeWriter.instance(context);
        ClassWriter classWriter = ClassWriter.instance(context);
        ClassFile cf = classWriter.getClassFile();
        attrWriter.write(cf, value, cf.constant_pool);
    }

    protected void write(Attributes attrs) {
        AttributeWriter attrWriter = AttributeWriter.instance(context);
        ClassWriter classWriter = ClassWriter.instance(context);
        ClassFile cf = classWriter.getClassFile();
        attrWriter.write(cf, attrs, cf.constant_pool);
    }

    protected void write(ConstantPool constant_pool) {
        ConstantWriter constantWriter = ConstantWriter.instance(context);
        constantWriter.writeConstantPool(constant_pool);
    }

    protected void write(ConstantPool constant_pool, int value) {
        ConstantWriter constantWriter = ConstantWriter.instance(context);
        constantWriter.write(value);
    }

    protected void write(ConstantPool.CPInfo value) {
        ConstantWriter constantWriter = ConstantWriter.instance(context);
        constantWriter.println(value);
    }

    protected void write(Field value) {
        ClassWriter classWriter = ClassWriter.instance(context);
        classWriter.writeField(value);
    }

    protected void write(Method value) {
        ClassWriter classWriter = ClassWriter.instance(context);
        classWriter.writeMethod(value);
    }

    private JavaFileManager getDefaultFileManager(final DiagnosticListener<? super JavaFileObject> dl, PrintWriter log) {
        if (defaultFileManager == null)
            defaultFileManager = JavapFileManager.create(dl, log);
        return defaultFileManager;
    }

    private JavaFileObject getClassFileObject(String className) throws IOException {
        try {
            JavaFileObject fo;
            if (moduleLocation != null) {
                fo = fileManager.getJavaFileForInput(moduleLocation, className, JavaFileObject.Kind.CLASS);
            } else {
                fo = fileManager.getJavaFileForInput(StandardLocation.PLATFORM_CLASS_PATH, className, JavaFileObject.Kind.CLASS);
                if (fo == null)
                    fo = fileManager.getJavaFileForInput(StandardLocation.CLASS_PATH, className, JavaFileObject.Kind.CLASS);
            }
            return fo;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private Location findModule(String moduleName) throws IOException {
        Location[] locns = {
            StandardLocation.UPGRADE_MODULE_PATH,
            StandardLocation.SYSTEM_MODULES,
            StandardLocation.MODULE_PATH
        };
        for (Location segment: locns) {
            for (Set<Location> set: fileManager.listLocationsForModules(segment)) {
                Location result = null;
                for (Location l: set) {
                    String name = fileManager.inferModuleName(l);
                    if (name.equals(moduleName)) {
                        if (result == null)
                            result = l;
                        else
                            throw new IOException("multiple definitions found for " + moduleName);
                    }
                }
                if (result != null)
                    return result;
            }
        }
        return null;
    }

    private void showHelp() {
        printLines(getMessage("main.usage", progname));
        for (Option o: recognizedOptions) {
            String name = o.aliases[0].replaceAll("^-+", "").replaceAll("-+", "_"); // there must always be at least one name
            if (name.startsWith("X") || name.equals("fullversion"))
                continue;
            printLines(getMessage("main.opt." + name));
        }

        String[] fmOptions = {
            "--module-path", "--system",
            "--class-path", "-classpath", "-cp",
            "-bootclasspath",
            "--multi-release"
        };

        for (String o: fmOptions) {
            if (fileManager.isSupportedOption(o) == -1)
                continue;
            String name = o.replaceAll("^-+", "").replaceAll("-+", "_");
            printLines(getMessage("main.opt." + name));
        }

        printLines(getMessage("main.usage.foot"));
    }

    private void showVersion(boolean full) {
        printLines(version(full ? "full" : "release"));
    }

    private void printLines(String msg) {
        log.println(msg.replace("\n", nl));
    }

    private static final String nl = System.getProperty("line.separator");

    private static final String versionRBName = "com.sun.tools.javap.resources.version";
    private static ResourceBundle versionRB;

    private String version(String key) {
        // key=version:  mm.nn.oo[-milestone]
        // key=full:     mm.mm.oo[-milestone]-build
        if (versionRB == null) {
            try {
                versionRB = ResourceBundle.getBundle(versionRBName);
            } catch (MissingResourceException e) {
                return getMessage("version.resource.missing", System.getProperty("java.version"));
            }
        }
        try {
            return versionRB.getString(key);
        }
        catch (MissingResourceException e) {
            return getMessage("version.unknown", System.getProperty("java.version"));
        }
    }

    private void reportError(String key, Object... args) {
        diagnosticListener.report(createDiagnostic(Diagnostic.Kind.ERROR, key, args));
    }

    private void reportNote(String key, Object... args) {
        diagnosticListener.report(createDiagnostic(Diagnostic.Kind.NOTE, key, args));
    }

    private void reportWarning(String key, Object... args) {
        diagnosticListener.report(createDiagnostic(Diagnostic.Kind.WARNING, key, args));
    }

    private Diagnostic<JavaFileObject> createDiagnostic(
            final Diagnostic.Kind kind, final String key, final Object... args) {
        return new Diagnostic<>() {
            public Kind getKind() {
                return kind;
            }

            public JavaFileObject getSource() {
                return null;
            }

            public long getPosition() {
                return Diagnostic.NOPOS;
            }

            public long getStartPosition() {
                return Diagnostic.NOPOS;
            }

            public long getEndPosition() {
                return Diagnostic.NOPOS;
            }

            public long getLineNumber() {
                return Diagnostic.NOPOS;
            }

            public long getColumnNumber() {
                return Diagnostic.NOPOS;
            }

            public String getCode() {
                return key;
            }

            public String getMessage(Locale locale) {
                return JavapTask.this.getMessage(locale, key, args);
            }

            @Override
            public String toString() {
                return getClass().getName() + "[key=" + key + ",args=" + Arrays.asList(args) + "]";
            }

        };

    }

    public String getMessage(String key, Object... args) {
        return getMessage(task_locale, key, args);
    }

    public String getMessage(Locale locale, String key, Object... args) {
        if (bundles == null) {
            // could make this a HashMap<Locale,SoftReference<ResourceBundle>>
            // and for efficiency, keep a hard reference to the bundle for the task
            // locale
            bundles = new HashMap<>();
        }

        if (locale == null)
            locale = Locale.getDefault();

        ResourceBundle b = bundles.get(locale);
        if (b == null) {
            try {
                b = ResourceBundle.getBundle("com.sun.tools.javap.resources.javap", locale);
                bundles.put(locale, b);
            } catch (MissingResourceException e) {
                throw new InternalError("Cannot find javap resource bundle for locale " + locale);
            }
        }

        try {
            return MessageFormat.format(b.getString(key), args);
        } catch (MissingResourceException e) {
            throw new InternalError(e, key);
        }
    }

    protected Context context;
    JavaFileManager fileManager;
    JavaFileManager defaultFileManager;
    PrintWriter log;
    DiagnosticListener<? super JavaFileObject> diagnosticListener;
    List<String> classes;
    Location moduleLocation;
    Options options;
    //ResourceBundle bundle;
    Locale task_locale;
    Map<Locale, ResourceBundle> bundles;
    protected Attribute.Factory attributeFactory;

    private static final String progname = "javap";

    private static class SizeInputStream extends FilterInputStream {
        SizeInputStream(InputStream in) {
            super(in);
        }

        int size() {
            return size;
        }

        @Override
        public int read(byte[] buf, int offset, int length) throws IOException {
            int n = super.read(buf, offset, length);
            if (n > 0)
                size += n;
            return n;
        }

        @Override
        public int read() throws IOException {
            int b = super.read();
            size += 1;
            return b;
        }

        private int size;
    }
}
