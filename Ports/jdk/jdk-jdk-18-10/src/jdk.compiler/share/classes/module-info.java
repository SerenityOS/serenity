/*
 * Copyright (c) 2014, 2019, Oracle and/or its affiliates. All rights reserved.
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

/**
 * Defines the implementation of the
 * {@linkplain javax.tools.ToolProvider#getSystemJavaCompiler system Java compiler}
 * and its command line equivalent, <em>{@index javac javac tool}</em>.
 *
 * <h2 style="font-family:'DejaVu Sans Mono', monospace; font-style:italic">javac</h2>
 *
 * <p>
 * This module provides the equivalent of command-line access to <em>javac</em>
 * via the {@link java.util.spi.ToolProvider ToolProvider} and
 * {@link javax.tools.Tool} service provider interfaces (SPIs),
 * and more flexible access via the {@link javax.tools.JavaCompiler JavaCompiler}
 * SPI.</p>
 *
 * <p> Instances of the tools can be obtained by calling
 * {@link java.util.spi.ToolProvider#findFirst ToolProvider.findFirst}
 * or the {@linkplain java.util.ServiceLoader service loader} with the name
 * {@code "javac"}.
 *
 * <p>
 * In addition, instances of {@link javax.tools.JavaCompiler.CompilationTask}
 * obtained from {@linkplain javax.tools.JavaCompiler JavaCompiler} can be
 * downcast to {@link com.sun.source.util.JavacTask JavacTask} for access to
 * lower level aspects of <em>javac</em>, such as the
 * {@link com.sun.source.tree Abstract Syntax Tree} (AST).</p>
 *
 * <p>This module uses the {@link java.nio.file.spi.FileSystemProvider
 * FileSystemProvider} API to locate file system providers. In particular,
 * this means that a jar file system provider, such as that in the
 * {@code jdk.zipfs} module, must be available if the compiler is to be able
 * to read JAR files.
 *
 * @toolGuide javac
 *
 * @provides java.util.spi.ToolProvider
 * @provides com.sun.tools.javac.platform.PlatformProvider
 * @provides javax.tools.JavaCompiler
 * @provides javax.tools.Tool
 *
 * @uses javax.annotation.processing.Processor
 * @uses com.sun.source.util.Plugin
 * @uses com.sun.tools.javac.platform.PlatformProvider
 *
 * @moduleGraph
 * @since 9
 */
module jdk.compiler {
    requires transitive java.compiler;

    exports com.sun.source.doctree;
    exports com.sun.source.tree;
    exports com.sun.source.util;
    exports com.sun.tools.javac;

    exports com.sun.tools.doclint to
        jdk.javadoc;
    exports com.sun.tools.javac.api to
        jdk.javadoc,
        jdk.jshell;
    exports com.sun.tools.javac.resources to
        jdk.jshell;
    exports com.sun.tools.javac.code to
        jdk.javadoc,
        jdk.jshell;
    exports com.sun.tools.javac.comp to
        jdk.javadoc,
        jdk.jshell;
    exports com.sun.tools.javac.file to
        jdk.jdeps,
        jdk.javadoc;
    exports com.sun.tools.javac.jvm to
        jdk.javadoc;
    exports com.sun.tools.javac.main to
        jdk.javadoc,
        jdk.jshell;
    exports com.sun.tools.javac.model to
        jdk.javadoc;
    exports com.sun.tools.javac.parser to
        jdk.jshell;
    exports com.sun.tools.javac.platform to
        jdk.jdeps,
        jdk.javadoc;
    exports com.sun.tools.javac.tree to
        jdk.javadoc,
        jdk.jshell;
    exports com.sun.tools.javac.util to
        jdk.jdeps,
        jdk.javadoc,
        jdk.jshell;
    exports jdk.internal.shellsupport.doc to
        jdk.jshell;

    uses javax.annotation.processing.Processor;
    uses com.sun.source.util.Plugin;
    uses com.sun.tools.doclint.DocLint;
    uses com.sun.tools.javac.platform.PlatformProvider;

    provides java.util.spi.ToolProvider with
        com.sun.tools.javac.main.JavacToolProvider;

    provides com.sun.tools.javac.platform.PlatformProvider with
        com.sun.tools.javac.platform.JDKPlatformProvider;

    provides javax.tools.JavaCompiler with
        com.sun.tools.javac.api.JavacTool;

    provides javax.tools.Tool with
        com.sun.tools.javac.api.JavacTool;
}

