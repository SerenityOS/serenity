/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
package jdk.tools.jlink.internal.plugins;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.lang.module.ModuleDescriptor;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import jdk.tools.jlink.internal.ModuleSorter;
import jdk.tools.jlink.internal.Utils;
import jdk.tools.jlink.plugin.PluginException;
import jdk.tools.jlink.plugin.ResourcePool;
import jdk.tools.jlink.plugin.ResourcePoolBuilder;
import jdk.tools.jlink.plugin.ResourcePoolEntry;
import jdk.tools.jlink.plugin.ResourcePoolModule;

/**
 * This plugin adds/deletes information for 'release' file.
 */
public final class ReleaseInfoPlugin extends AbstractPlugin {
    // option name
    public static final String KEYS = "keys";
    private final Map<String, String> release = new HashMap<>();

    public ReleaseInfoPlugin() {
        super("release-info");
    }

    @Override
    public Category getType() {
        return Category.METAINFO_ADDER;
    }

    @Override
    public Set<State> getState() {
        return EnumSet.of(State.AUTO_ENABLED, State.FUNCTIONAL);
    }

    @Override
    public boolean hasArguments() {
        return true;
    }

    @Override
    public void configure(Map<String, String> config) {
        String operation = config.get(getName());
        if (operation == null) {
            return;
        }

        switch (operation) {
            case "add": {
                // leave it to open-ended! source, java_version, java_full_version
                // can be passed via this option like:
                //
                //     --release-info add:build_type=fastdebug,source=openjdk,java_version=9
                // and put whatever value that was passed in command line.

                config.keySet().stream()
                      .filter(s -> !getName().equals(s))
                      .forEach(s -> release.put(s, config.get(s)));
            }
            break;

            case "del": {
                // --release-info del:keys=openjdk,java_version
                String keys = config.get(KEYS);
                if (keys == null || keys.isEmpty()) {
                    throw new IllegalArgumentException("No key specified for delete");
                }
                Utils.parseList(keys).stream().forEach((k) -> {
                    release.remove(k);
                });
            }
            break;

            default: {
                // --release-info <file>
                Properties props = new Properties();
                try (FileInputStream fis = new FileInputStream(operation)) {
                    props.load(fis);
                } catch (IOException exp) {
                    throw new UncheckedIOException(exp);
                }
                props.forEach((k, v) -> release.put(k.toString(), v.toString()));
            }
            break;
        }
    }

    @Override
    public ResourcePool transform(ResourcePool in, ResourcePoolBuilder out) {
        in.transformAndCopy(Function.identity(), out);

        ResourcePoolModule javaBase = in.moduleView().findModule("java.base")
                                                     .orElse(null);
        if (javaBase == null || javaBase.targetPlatform() == null) {
            throw new PluginException("ModuleTarget attribute is missing for java.base module");
        }

        // fill release information available from transformed "java.base" module!
        ModuleDescriptor desc = javaBase.descriptor();
        desc.version().ifPresent(v -> release.put("JAVA_VERSION",
                                                  quote(parseVersion(v))));

        // put topological sorted module names separated by space
        release.put("MODULES",  new ModuleSorter(in.moduleView())
               .sorted().map(ResourcePoolModule::name)
               .collect(Collectors.joining(" ", "\"", "\"")));

        // create a TOP level ResourcePoolEntry for "release" file.
        out.add(ResourcePoolEntry.create("/java.base/release",
                                         ResourcePoolEntry.Type.TOP,
                                         releaseFileContent()));
        return out.build();
    }

    // Parse version string and return a string that includes only version part
    // leaving "pre", "build" information. See also: java.lang.Runtime.Version.
    private static String parseVersion(ModuleDescriptor.Version v) {
        return Runtime.Version.parse(v.toString())
                      .version()
                      .stream()
                      .map(Object::toString)
                      .collect(Collectors.joining("."));
    }

    private static String quote(String str) {
        return "\"" + str + "\"";
    }

    private byte[] releaseFileContent() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PrintWriter pw = new PrintWriter(baos)) {
            release.entrySet().stream()
                   .sorted(Map.Entry.comparingByKey())
                   .forEach(e -> pw.format("%s=%s%n", e.getKey(), e.getValue()));
        }
        return baos.toByteArray();
    }
}
