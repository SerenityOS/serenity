/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * Defines the Java Packaging tool, jpackage.
 *
 * <p>jpackage is a tool for generating self-contained application bundles.
 *
 * <p> This module provides the equivalent of command-line access to <em>jpackage</em>
 * via the {@link java.util.spi.ToolProvider ToolProvider} SPI.
 * Instances of the tool can be obtained by calling
 * {@link java.util.spi.ToolProvider#findFirst ToolProvider.findFirst}
 * or the {@link java.util.ServiceLoader service loader} with the name
 * {@code "jpackage"}.
 *
 * @implNote The {@code jpackage} tool is not thread-safe. An application
 * should not call either of the
 * {@link java.util.spi.ToolProvider ToolProvider} {@code run} methods
 * concurrently, even with separate {@code "jpackage"} {@code ToolProvider}
 * instances, or undefined behavior may result.
 *
 *
 * @moduleGraph
 * @since 16
 */

module jdk.jpackage {
    requires jdk.jlink;

    requires java.desktop;

    uses jdk.jpackage.internal.Bundler;
    uses jdk.jpackage.internal.Bundlers;

    provides jdk.jpackage.internal.Bundlers with
        jdk.jpackage.internal.BasicBundlers;

    provides java.util.spi.ToolProvider
        with jdk.jpackage.internal.JPackageToolProvider;
}
