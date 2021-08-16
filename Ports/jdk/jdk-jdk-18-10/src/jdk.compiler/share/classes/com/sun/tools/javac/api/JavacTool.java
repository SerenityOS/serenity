/*
 * Copyright (c) 2005, 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.javac.api;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import javax.lang.model.SourceVersion;
import javax.tools.*;

import com.sun.source.util.JavacTask;
import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.main.Arguments;
import com.sun.tools.javac.main.Option;
import com.sun.tools.javac.file.BaseFileManager;
import com.sun.tools.javac.file.CacheFSInfo;
import com.sun.tools.javac.jvm.Target;
import com.sun.tools.javac.util.ClientCodeException;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.DefinedBy;
import com.sun.tools.javac.util.DefinedBy.Api;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.PropagatedException;

/**
 * TODO: describe com.sun.tools.javac.api.Tool
 *
 * <p><b>This is NOT part of any supported API.
 * If you write code that depends on this, you do so at your own
 * risk.  This code and its internal interfaces are subject to change
 * or deletion without notice.</b></p>
 *
 * @author Peter von der Ah\u00e9
 */
public final class JavacTool implements JavaCompiler {
    /**
     * Constructor used by service provider mechanism.  The recommended way to
     * obtain an instance of this class is by using {@link #create} or the
     * service provider mechanism.
     * @see javax.tools.JavaCompiler
     * @see javax.tools.ToolProvider
     * @see #create
     */
    @Deprecated
    public JavacTool() {}

    // @Override // can't add @Override until bootstrap JDK provides Tool.name()
    @DefinedBy(Api.COMPILER)
    public String name() {
        return "javac";
    }

    /**
     * Static factory method for creating new instances of this tool.
     * @return new instance of this tool
     */
    public static JavacTool create() {
        return new JavacTool();
    }

    @Override @DefinedBy(Api.COMPILER)
    public JavacFileManager getStandardFileManager(
        DiagnosticListener<? super JavaFileObject> diagnosticListener,
        Locale locale,
        Charset charset) {
        Context context = new Context();
        context.put(Locale.class, locale);
        if (diagnosticListener != null)
            context.put(DiagnosticListener.class, diagnosticListener);
        PrintWriter pw = (charset == null)
                ? new PrintWriter(System.err, true)
                : new PrintWriter(new OutputStreamWriter(System.err, charset), true);
        context.put(Log.errKey, pw);
        CacheFSInfo.preRegister(context);
        return new JavacFileManager(context, true, charset);
    }

    @Override @DefinedBy(Api.COMPILER)
    public JavacTask getTask(Writer out,
                             JavaFileManager fileManager,
                             DiagnosticListener<? super JavaFileObject> diagnosticListener,
                             Iterable<String> options,
                             Iterable<String> classes,
                             Iterable<? extends JavaFileObject> compilationUnits) {
        Context context = new Context();
        return getTask(out, fileManager, diagnosticListener,
                options, classes, compilationUnits,
                context);
    }

    /* Internal version of getTask, allowing context to be provided. */
    public JavacTask getTask(Writer out,
                             JavaFileManager fileManager,
                             DiagnosticListener<? super JavaFileObject> diagnosticListener,
                             Iterable<String> options,
                             Iterable<String> classes,
                             Iterable<? extends JavaFileObject> compilationUnits,
                             Context context)
    {
        try {
            ClientCodeWrapper ccw = ClientCodeWrapper.instance(context);

            if (options != null) {
                for (String option : options)
                    Objects.requireNonNull(option);
            }

            if (classes != null) {
                for (String cls : classes) {
                    int sep = cls.indexOf('/'); // implicit null check
                    if (sep > 0) {
                        String mod = cls.substring(0, sep);
                        if (!SourceVersion.isName(mod))
                            throw new IllegalArgumentException("Not a valid module name: " + mod);
                        cls = cls.substring(sep + 1);
                    }
                    if (!SourceVersion.isName(cls))
                        throw new IllegalArgumentException("Not a valid class name: " + cls);
                }
            }

            if (compilationUnits != null) {
                compilationUnits = ccw.wrapJavaFileObjects(compilationUnits); // implicit null check
                for (JavaFileObject cu : compilationUnits) {
                    if (cu.getKind() != JavaFileObject.Kind.SOURCE) {
                        String kindMsg = "Compilation unit is not of SOURCE kind: "
                                + "\"" + cu.getName() + "\"";
                        throw new IllegalArgumentException(kindMsg);
                    }
                }
            }

            if (diagnosticListener != null)
                context.put(DiagnosticListener.class, ccw.wrap(diagnosticListener));

            // If out is null and the value is set in the context, we need to do nothing.
            if (out == null && context.get(Log.errKey) == null)
                // Situation: out is null and the value is not set in the context.
                context.put(Log.errKey, new PrintWriter(System.err, true));
            else if (out instanceof PrintWriter pw)
                // Situation: out is not null and out is a PrintWriter.
                context.put(Log.errKey, pw);
            else if (out != null)
                // Situation: out is not null and out is not a PrintWriter.
                context.put(Log.errKey, new PrintWriter(out, true));

            if (fileManager == null) {
                fileManager = getStandardFileManager(diagnosticListener, null, null);
                if (fileManager instanceof BaseFileManager baseFileManager) {
                    baseFileManager.autoClose = true;
                }
            }
            fileManager = ccw.wrap(fileManager);

            context.put(JavaFileManager.class, fileManager);

            Arguments args = Arguments.instance(context);
            args.init("javac", options, classes, compilationUnits);

            // init multi-release jar handling
            if (fileManager.isSupportedOption(Option.MULTIRELEASE.primaryName) == 1) {
                Target target = Target.instance(context);
                List<String> list = List.of(target.multiReleaseValue());
                fileManager.handleOption(Option.MULTIRELEASE.primaryName, list.iterator());
            }

            return new JavacTaskImpl(context);
        } catch (PropagatedException ex) {
            throw ex.getCause();
        } catch (ClientCodeException ex) {
            throw new RuntimeException(ex.getCause());
        }
    }

    @Override @DefinedBy(Api.COMPILER)
    public int run(InputStream in, OutputStream out, OutputStream err, String... arguments) {
        if (err == null)
            err = System.err;
        for (String argument : arguments)
            Objects.requireNonNull(argument);
        return com.sun.tools.javac.Main.compile(arguments, new PrintWriter(err, true));
    }

    @Override @DefinedBy(Api.COMPILER)
    public Set<SourceVersion> getSourceVersions() {
        return Collections.unmodifiableSet(EnumSet.range(SourceVersion.RELEASE_3,
                                                         SourceVersion.latest()));
    }

    @Override @DefinedBy(Api.COMPILER)
    public int isSupportedOption(String option) {
        Set<Option> recognizedOptions = Option.getJavacToolOptions();
        for (Option o : recognizedOptions) {
            if (o.matches(option)) {
                return o.hasSeparateArg() ? 1 : 0;
            }
        }
        return -1;
    }

}
