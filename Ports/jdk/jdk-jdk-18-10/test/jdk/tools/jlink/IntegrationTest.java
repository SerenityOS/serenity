/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;
import jdk.tools.jlink.internal.Jlink;
import jdk.tools.jlink.internal.JlinkTask;
import jdk.tools.jlink.builder.DefaultImageBuilder;
import jdk.tools.jlink.plugin.ResourcePool;
import jdk.tools.jlink.plugin.ResourcePoolBuilder;
import jdk.tools.jlink.plugin.Plugin;
import jdk.tools.jlink.internal.ExecutableImage;
import jdk.tools.jlink.internal.Jlink.JlinkConfiguration;
import jdk.tools.jlink.internal.Jlink.PluginsConfiguration;
import jdk.tools.jlink.internal.PostProcessor;
import jdk.tools.jlink.internal.plugins.DefaultCompressPlugin;
import jdk.tools.jlink.internal.plugins.DefaultStripDebugPlugin;

import tests.Helper;
import tests.JImageGenerator;

/*
 * @test
 * @summary Test integration API
 * @author Jean-Francois Denise
 * @library ../lib
 * @modules java.base/jdk.internal.jimage
 *          jdk.jdeps/com.sun.tools.classfile
 *          jdk.jlink/jdk.tools.jlink.builder
 *          jdk.jlink/jdk.tools.jlink.internal
 *          jdk.jlink/jdk.tools.jlink.internal.plugins
 *          jdk.jlink/jdk.tools.jlink.plugin
 *          jdk.jlink/jdk.tools.jmod
 *          jdk.jlink/jdk.tools.jimage
 *          jdk.compiler
 * @build tests.*
 * @run main/othervm -Xmx1g IntegrationTest
 */
public class IntegrationTest {

    private static final List<Integer> ordered = new ArrayList<>();

    public static class MyPostProcessor implements PostProcessor, Plugin {

        public static final String NAME = "mypostprocessor";

        @Override
        public List<String> process(ExecutableImage image) {
            try {
                Files.createFile(image.getHome().resolve("toto.txt"));
                return null;
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        }

        @Override
        public String getName() {
            return NAME;
        }

        @Override
        public Category getType() {
            return Category.PROCESSOR;
        }

        @Override
        public void configure(Map<String, String> config) {
            throw new UnsupportedOperationException("Shouldn't be called");
        }

        @Override
        public ResourcePool transform(ResourcePool in, ResourcePoolBuilder out) {
            in.transformAndCopy(Function.identity(), out);
            return out.build();
        }
    }

    public static void main(String[] args) throws Exception {

        Helper helper = Helper.newHelper();
        if (helper == null) {
            System.err.println("Test not run");
            return;
        }
        apitest();
        test();
    }

    private static void apitest() throws Exception {
        boolean failed = false;
        Jlink jl = new Jlink();

        try {
            jl.build(null);
            failed = true;
        } catch (Exception ex) {
            // XXX OK
        }
        if (failed) {
            throw new Exception("Should have failed");
        }
        System.out.println(jl);

        Plugin p = Jlink.newPlugin("toto", Collections.emptyMap(), null);
        if (p != null) {
            throw new Exception("Plugin should be null");
        }

        Plugin p2 = Jlink.newPlugin("compress", Map.of("compress", "1"), null);
        if (p2 == null) {
            throw new Exception("Plugin should not be null");
        }
    }

    private static void test() throws Exception {
        Jlink jlink = new Jlink();
        Path output = Paths.get("integrationout");
        List<Path> modulePaths = new ArrayList<>();
        File jmods
                = JImageGenerator.getJModsDir(new File(System.getProperty("test.jdk")));
        modulePaths.add(jmods.toPath());
        Set<String> mods = new HashSet<>();
        mods.add("java.management");
        Set<String> limits = new HashSet<>();
        limits.add("java.management");
        JlinkConfiguration config = new Jlink.JlinkConfiguration(output,
                mods, ByteOrder.nativeOrder(),
                JlinkTask.newModuleFinder(modulePaths, limits, mods));

        List<Plugin> lst = new ArrayList<>();

        //Strip debug
        {
            Map<String, String> config1 = new HashMap<>();
            Plugin strip = Jlink.newPlugin("strip-debug", config1, null);
            config1.put(strip.getName(), "");
            lst.add(strip);
        }
        // compress
        {
            Map<String, String> config1 = new HashMap<>();
            String pluginName = "compress";
            config1.put(pluginName, "2");
            Plugin compress
                    = Jlink.newPlugin(pluginName, config1, null);
            if(!pluginName.equals(compress.getName())) {
                throw new AssertionError("compress plugin name doesn't match test constant");
            }
            lst.add(compress);
        }
        // Post processor
        {
            lst.add(new MyPostProcessor());
        }
        // Image builder
        DefaultImageBuilder builder = new DefaultImageBuilder(output, Collections.emptyMap());
        PluginsConfiguration plugins
                = new Jlink.PluginsConfiguration(lst, builder, null);

        jlink.build(config, plugins);

        if (!Files.exists(output)) {
            throw new AssertionError("Directory not created");
        }
        File jimage = new File(output.toString(), "lib" + File.separator + "modules");
        if (!jimage.exists()) {
            throw new AssertionError("jimage not generated");
        }
        File release = new File(output.toString(), "release");
        if (!release.exists()) {
            throw new AssertionError("release not generated");
        }

        Properties props = new Properties();
        try (FileReader reader = new FileReader(release)) {
            props.load(reader);
        }

        checkReleaseProperty(props, "JAVA_VERSION");

        if (!Files.exists(output.resolve("toto.txt"))) {
            throw new AssertionError("Post processing not called");
        }

    }

    static void checkReleaseProperty(Properties props, String name) {
        if (! props.containsKey(name)) {
            throw new AssertionError("release file does not contain property : " + name);
        }

        // property value is of min. length 3 and double quoted at the ends.
        String value = props.getProperty(name);
        if (value.length() < 3 ||
            value.charAt(0) != '"' ||
            value.charAt(value.length() - 1) != '"') {
            throw new AssertionError("release property " + name + " is not quoted property");
        }
    }
}
