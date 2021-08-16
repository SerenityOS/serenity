/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

/**
 * WiX pipeline. Compiles and links WiX sources.
 */
public class WixPipeline {
    WixPipeline() {
        sources = new ArrayList<>();
        lightOptions = new ArrayList<>();
    }

    WixPipeline setToolset(Map<WixTool, Path> v) {
        toolset = v;
        return this;
    }

    WixPipeline setWixVariables(Map<String, String> v) {
        wixVariables = v;
        return this;
    }

    WixPipeline setWixObjDir(Path v) {
        wixObjDir = v;
        return this;
    }

    WixPipeline setWorkDir(Path v) {
        workDir = v;
        return this;
    }

    WixPipeline addSource(Path source, Map<String, String> wixVariables) {
        WixSource entry = new WixSource();
        entry.source = source;
        entry.variables = wixVariables;
        sources.add(entry);
        return this;
    }

    WixPipeline addLightOptions(String ... v) {
        lightOptions.addAll(List.of(v));
        return this;
    }

    void buildMsi(Path msi) throws IOException {
        List<Path> wixObjs = new ArrayList<>();
        for (var source : sources) {
            wixObjs.add(compile(source));
        }

        List<String> lightCmdline = new ArrayList<>(List.of(
                toolset.get(WixTool.Light).toString(),
                "-nologo",
                "-spdb",
                "-ext", "WixUtilExtension",
                "-out", msi.toString()
        ));

        lightCmdline.addAll(lightOptions);
        wixObjs.stream().map(Path::toString).forEach(lightCmdline::add);

        Files.createDirectories(IOUtils.getParent(msi));
        execute(lightCmdline);
    }

    private Path compile(WixSource wixSource) throws IOException {
        UnaryOperator<Path> adjustPath = path -> {
            return workDir != null ? path.toAbsolutePath() : path;
        };

        Path wixObj = adjustPath.apply(wixObjDir).resolve(IOUtils.replaceSuffix(
                IOUtils.getFileName(wixSource.source), ".wixobj"));

        List<String> cmdline = new ArrayList<>(List.of(
                toolset.get(WixTool.Candle).toString(),
                "-nologo",
                adjustPath.apply(wixSource.source).toString(),
                "-ext", "WixUtilExtension",
                "-arch", WixFragmentBuilder.is64Bit() ? "x64" : "x86",
                "-out", wixObj.toAbsolutePath().toString()
        ));

        Map<String, String> appliedVaribales = new HashMap<>();
        Stream.of(wixVariables, wixSource.variables)
                .filter(Objects::nonNull)
                .forEachOrdered(appliedVaribales::putAll);

        appliedVaribales.entrySet().stream().map(wixVar -> String.format("-d%s=%s",
                wixVar.getKey(), wixVar.getValue())).forEachOrdered(
                cmdline::add);

        execute(cmdline);

        return wixObj;
    }

    private void execute(List<String> cmdline) throws IOException {
        Executor.of(new ProcessBuilder(cmdline).directory(
                workDir != null ? workDir.toFile() : null)).executeExpectSuccess();
    }

    private static final class WixSource {
        Path source;
        Map<String, String> variables;
    }

    private Map<WixTool, Path> toolset;
    private Map<String, String> wixVariables;
    private List<String> lightOptions;
    private Path wixObjDir;
    private Path workDir;
    private List<WixSource> sources;
}
