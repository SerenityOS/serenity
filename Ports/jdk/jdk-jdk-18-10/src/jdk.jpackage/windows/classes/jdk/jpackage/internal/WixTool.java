/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * WiX tool.
 */
public enum WixTool {
    Candle, Light;

    static final class ToolInfo {
        ToolInfo(Path path, String version) {
            this.path = path;
            this.version = new DottedVersion(version);
        }

        final Path path;
        final DottedVersion version;
    }

    static Map<WixTool, ToolInfo> toolset() throws ConfigException {
        Map<WixTool, ToolInfo> toolset = new HashMap<>();
        for (var tool : values()) {
            toolset.put(tool, tool.find());
        }
        return toolset;
    }

    ToolInfo find() throws ConfigException {
        final Path toolFileName = IOUtils.addSuffix(
                Path.of(name().toLowerCase()), ".exe");

        String[] version = new String[1];
        ConfigException reason = createToolValidator(toolFileName, version).get();
        if (version[0] != null) {
            if (reason == null) {
                // Found in PATH.
                return new ToolInfo(toolFileName, version[0]);
            }

            // Found in PATH, but something went wrong.
            throw reason;
        }

        for (var dir : findWixInstallDirs()) {
            Path path = dir.resolve(toolFileName);
            if (Files.exists(path)) {
                reason = createToolValidator(path, version).get();
                if (reason != null) {
                    throw reason;
                }
                return new ToolInfo(path, version[0]);
            }
        }

        throw reason;
    }

    private static Supplier<ConfigException> createToolValidator(Path toolPath,
            String[] versionCtnr) {
        return new ToolValidator(toolPath)
                .setCommandLine("/?")
                .setMinimalVersion(MINIMAL_VERSION)
                .setToolNotFoundErrorHandler(
                        (name, ex) -> new ConfigException(
                                I18N.getString("error.no-wix-tools"),
                                I18N.getString("error.no-wix-tools.advice")))
                .setToolOldVersionErrorHandler(
                        (name, version) -> new ConfigException(
                                MessageFormat.format(I18N.getString(
                                        "message.wrong-tool-version"), name,
                                        version, MINIMAL_VERSION),
                                I18N.getString("error.no-wix-tools.advice")))
                .setVersionParser(output -> {
                    versionCtnr[0] = "";
                    String firstLineOfOutput = output.findFirst().orElse("");
                    int separatorIdx = firstLineOfOutput.lastIndexOf(' ');
                    if (separatorIdx == -1) {
                        return null;
                    }
                    versionCtnr[0] = firstLineOfOutput.substring(separatorIdx + 1);
                    return versionCtnr[0];
                })::validate;
    }

    private static final DottedVersion MINIMAL_VERSION = DottedVersion.lazy("3.0");

    static Path getSystemDir(String envVar, String knownDir) {
        return Optional
                .ofNullable(getEnvVariableAsPath(envVar))
                .orElseGet(() -> Optional
                        .ofNullable(getEnvVariableAsPath("SystemDrive"))
                        .orElseGet(() -> Path.of("C:")).resolve(knownDir));
    }

    private static Path getEnvVariableAsPath(String envVar) {
        String path = System.getenv(envVar);
        if (path != null) {
            try {
                return Path.of(path);
            } catch (InvalidPathException ex) {
                Log.error(MessageFormat.format(I18N.getString(
                        "error.invalid-envvar"), envVar));
            }
        }
        return null;
    }

    private static List<Path> findWixInstallDirs() {
        PathMatcher wixInstallDirMatcher = FileSystems.getDefault().getPathMatcher(
                "glob:WiX Toolset v*");

        Path programFiles = getSystemDir("ProgramFiles", "\\Program Files");
        Path programFilesX86 = getSystemDir("ProgramFiles(x86)",
                "\\Program Files (x86)");

        // Returns list of WiX install directories ordered by WiX version number.
        // Newer versions go first.
        return Stream.of(programFiles, programFilesX86).map(path -> {
            List<Path> result;
            try (var paths = Files.walk(path, 1)) {
                result = paths.toList();
            } catch (IOException ex) {
                Log.verbose(ex);
                result = Collections.emptyList();
            }
            return result;
        }).flatMap(List::stream)
        .filter(path -> wixInstallDirMatcher.matches(path.getFileName()))
        .sorted(Comparator.comparing(Path::getFileName).reversed())
        .map(path -> path.resolve("bin"))
        .toList();
    }
}
