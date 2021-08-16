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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import static jdk.jpackage.internal.StandardBundlerParam.RESOURCE_DIR;
import jdk.jpackage.internal.resources.ResourceLocator;


/**
 * Resource file that may have the default value supplied by jpackage. It can be
 * overridden by a file from resource directory set with {@code --resource-dir}
 * jpackage parameter.
 *
 * Resource has default name and public name. Default name is the name of a file
 * in {@code jdk.jpackage.internal.resources} package that provides the default
 * value of the resource.
 *
 * Public name is a path relative to resource directory to a file with custom
 * value of the resource.
 *
 * Use #setPublicName to set the public name.
 *
 * If #setPublicName was not called, name of file passed in #saveToFile function
 * will be used as a public name.
 *
 * Use #setExternal to set arbitrary file as a source of resource. If non-null
 * value was passed in #setExternal call that value will be used as a path to file
 * to copy in the destination file passed in #saveToFile function call.
 */
final class OverridableResource {

    OverridableResource(String defaultName) {
        this.defaultName = defaultName;
        setSourceOrder(Source.values());
    }

    OverridableResource setSubstitutionData(Map<String, String> v) {
        if (v != null) {
            // Disconnect `v`
            substitutionData = new HashMap<>(v);
        } else {
            substitutionData = null;
        }
        return this;
    }

    OverridableResource setCategory(String v) {
        category = v;
        return this;
    }

    OverridableResource setResourceDir(Path v) {
        resourceDir = v;
        return this;
    }

    OverridableResource setResourceDir(File v) {
        return setResourceDir(toPath(v));
    }

    enum Source { External, ResourceDir, DefaultResource };

    OverridableResource setSourceOrder(Source... v) {
        sources = Stream.of(v)
                .map(source -> Map.entry(source, getHandler(source)))
                .toList();
        return this;
    }

    /**
     * Set name of file to look for in resource dir.
     *
     * @return this
     */
    OverridableResource setPublicName(Path v) {
        publicName = v;
        return this;
    }

    OverridableResource setPublicName(String v) {
        return setPublicName(Path.of(v));
    }

    /**
     * Set name of file to look for in resource dir to put in verbose log.
     *
     * @return this
     */
    OverridableResource setLogPublicName(Path v) {
        logPublicName = v;
        return this;
    }

    OverridableResource setLogPublicName(String v) {
        return setLogPublicName(Path.of(v));
    }

    OverridableResource setExternal(Path v) {
        externalPath = v;
        return this;
    }

    OverridableResource setExternal(File v) {
        return setExternal(toPath(v));
    }

    Source saveToStream(OutputStream dest) throws IOException {
        if (dest == null) {
            return sendToConsumer(null);
        }
        return sendToConsumer(new ResourceConsumer() {
            @Override
            public Path publicName() {
                throw new UnsupportedOperationException();
            }

            @Override
            public void consume(InputStream in) throws IOException {
                in.transferTo(dest);
            }
        });
    }

    Source saveToFile(Path dest) throws IOException {
        if (dest == null) {
            return sendToConsumer(null);
        }
        return sendToConsumer(new ResourceConsumer() {
            @Override
            public Path publicName() {
                return dest.getFileName();
            }

            @Override
            public void consume(InputStream in) throws IOException {
                Files.createDirectories(IOUtils.getParent(dest));
                Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
            }
        });
    }

    Source saveToFile(File dest) throws IOException {
        return saveToFile(toPath(dest));
    }

    static InputStream readDefault(String resourceName) {
        return ResourceLocator.class.getResourceAsStream(resourceName);
    }

    static OverridableResource createResource(String defaultName,
            Map<String, ? super Object> params) {
        return new OverridableResource(defaultName).setResourceDir(
                RESOURCE_DIR.fetchFrom(params));
    }

    private Source sendToConsumer(ResourceConsumer consumer) throws IOException {
        for (var source: sources) {
            if (source.getValue().apply(consumer)) {
                return source.getKey();
            }
        }
        return null;
    }

    private String getPrintableCategory() {
        if (category != null) {
            return String.format("[%s]", category);
        }
        return "";
    }

    private boolean useExternal(ResourceConsumer dest) throws IOException {
        boolean used = externalPath != null && Files.exists(externalPath);
        if (used && dest != null) {
            Log.verbose(MessageFormat.format(I18N.getString(
                    "message.using-custom-resource-from-file"),
                    getPrintableCategory(),
                    externalPath.toAbsolutePath().normalize()));

            try (InputStream in = Files.newInputStream(externalPath)) {
                processResourceStream(in, dest);
            }
        }
        return used;
    }

    private boolean useResourceDir(ResourceConsumer dest) throws IOException {
        boolean used = false;

        if (dest == null && publicName == null) {
            throw new IllegalStateException();
        }

        final Path resourceName = Optional.ofNullable(publicName).orElseGet(
                () -> dest.publicName());

        if (resourceDir != null) {
            final Path customResource = resourceDir.resolve(resourceName);
            used = Files.exists(customResource);
            if (used && dest != null) {
                final Path logResourceName;
                if (logPublicName != null) {
                    logResourceName = logPublicName.normalize();
                } else {
                    logResourceName = resourceName.normalize();
                }

                Log.verbose(MessageFormat.format(I18N.getString(
                        "message.using-custom-resource"), getPrintableCategory(),
                        logResourceName));

                try (InputStream in = Files.newInputStream(customResource)) {
                    processResourceStream(in, dest);
                }
            }
        }

        return used;
    }

    private boolean useDefault(ResourceConsumer dest) throws IOException {
        boolean used = defaultName != null;
        if (used && dest != null) {
            final Path resourceName = Optional
                    .ofNullable(logPublicName)
                    .orElse(Optional
                            .ofNullable(publicName)
                            .orElseGet(() -> dest.publicName()));
            Log.verbose(MessageFormat.format(
                    I18N.getString("message.using-default-resource"),
                    defaultName, getPrintableCategory(), resourceName));

            try (InputStream in = readDefault(defaultName)) {
                processResourceStream(in, dest);
            }
        }
        return used;
    }

    private static Stream<String> substitute(Stream<String> lines,
            Map<String, String> substitutionData) {
        return lines.map(line -> {
            String result = line;
            for (var entry : substitutionData.entrySet()) {
                result = result.replace(entry.getKey(), Optional.ofNullable(
                        entry.getValue()).orElse(""));
            }
            return result;
        });
    }

    private static Path toPath(File v) {
        if (v != null) {
            return v.toPath();
        }
        return null;
    }

    private void processResourceStream(InputStream rawResource,
            ResourceConsumer dest) throws IOException {
        if (substitutionData == null) {
            dest.consume(rawResource);
        } else {
            // Utf8 in and out
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(rawResource, StandardCharsets.UTF_8))) {
                String data = substitute(reader.lines(), substitutionData).collect(
                        Collectors.joining("\n", "", "\n"));
                try (InputStream in = new ByteArrayInputStream(data.getBytes(
                        StandardCharsets.UTF_8))) {
                    dest.consume(in);
                }
            }
        }
    }

    private SourceHandler getHandler(Source sourceType) {
        switch (sourceType) {
            case DefaultResource:
                return this::useDefault;

            case External:
                return this::useExternal;

            case ResourceDir:
                return this::useResourceDir;

            default:
                throw new IllegalArgumentException();
        }
    }

    private Map<String, String> substitutionData;
    private String category;
    private Path resourceDir;
    private Path publicName;
    private Path logPublicName;
    private Path externalPath;
    private final String defaultName;
    private List<Map.Entry<Source, SourceHandler>> sources;

    @FunctionalInterface
    private static interface SourceHandler {
        public boolean apply(ResourceConsumer dest) throws IOException;
    }

    private static interface ResourceConsumer {
        public Path publicName();
        public void consume(InputStream in) throws IOException;
    }
}
