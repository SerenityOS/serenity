/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;


public final class ToolValidator {

    ToolValidator(String tool) {
        this(Path.of(tool));
    }

    ToolValidator(Path toolPath) {
        this.toolPath = toolPath;
        args = new ArrayList<>();

        if (Platform.getPlatform() == Platform.LINUX) {
            setCommandLine("--version");
        }

        setToolNotFoundErrorHandler(null);
        setToolOldVersionErrorHandler(null);
    }

    ToolValidator setCommandLine(String... args) {
        this.args = List.of(args);
        return this;
    }

    ToolValidator setMinimalVersion(Comparable<String> v) {
        minimalVersion = v;
        return this;
    }

    ToolValidator setVersionParser(Function<Stream<String>, String> v) {
        versionParser = v;
        return this;
    }

    ToolValidator setToolNotFoundErrorHandler(
            BiFunction<String, IOException, ConfigException> v) {
        toolNotFoundErrorHandler = v;
        return this;
    }

    ToolValidator setToolOldVersionErrorHandler(BiFunction<String, String, ConfigException> v) {
        toolOldVersionErrorHandler = v;
        return this;
    }

    ConfigException validate() {
        List<String> cmdline = new ArrayList<>();
        cmdline.add(toolPath.toString());
        cmdline.addAll(args);

        String name = IOUtils.getFileName(toolPath).toString();
        try {
            ProcessBuilder pb = new ProcessBuilder(cmdline);
            AtomicBoolean canUseTool = new AtomicBoolean();
            if (minimalVersion == null) {
                // No version check.
                canUseTool.setPlain(true);
            }

            String[] version = new String[1];
            Executor.of(pb).setQuiet(true).setOutputConsumer(lines -> {
                if (versionParser != null && minimalVersion != null) {
                    version[0] = versionParser.apply(lines);
                    if (minimalVersion.compareTo(version[0]) < 0) {
                        canUseTool.setPlain(true);
                    }
                }
            }).execute();

            if (!canUseTool.getPlain()) {
                if (toolOldVersionErrorHandler != null) {
                    return toolOldVersionErrorHandler.apply(name, version[0]);
                }
                return new ConfigException(MessageFormat.format(I18N.getString(
                        "error.tool-old-version"), name, minimalVersion),
                        MessageFormat.format(I18N.getString(
                                "error.tool-old-version.advice"), name,
                                minimalVersion));
            }
        } catch (IOException e) {
            if (toolNotFoundErrorHandler != null) {
                return toolNotFoundErrorHandler.apply(name, e);
            }
            return new ConfigException(MessageFormat.format(I18N.getString(
                    "error.tool-not-found"), name, e.getMessage()),
                    MessageFormat.format(I18N.getString(
                            "error.tool-not-found.advice"), name), e);
        }

        // All good. Tool can be used.
        return null;
    }

    private final Path toolPath;
    private List<String> args;
    private Comparable<String> minimalVersion;
    private Function<Stream<String>, String> versionParser;
    private BiFunction<String, IOException, ConfigException> toolNotFoundErrorHandler;
    private BiFunction<String, String, ConfigException> toolOldVersionErrorHandler;
}
