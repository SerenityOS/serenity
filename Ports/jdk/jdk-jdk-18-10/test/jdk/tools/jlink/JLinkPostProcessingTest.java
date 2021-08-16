/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import jdk.tools.jlink.plugin.Plugin;
import jdk.tools.jlink.plugin.ResourcePool;
import jdk.tools.jlink.plugin.ResourcePoolBuilder;
import jdk.tools.jlink.internal.PluginRepository;
import jdk.tools.jlink.internal.PostProcessor;
import jdk.tools.jlink.internal.ExecutableImage;
import tests.Helper;

/*
 * @test
 * @summary Test post processing
 * @author Jean-Francois Denise
 * @library ../lib
 * @modules java.base/jdk.internal.jimage
 *          jdk.jdeps/com.sun.tools.classfile
 *          jdk.jlink/jdk.tools.jlink.internal
 *          jdk.jlink/jdk.tools.jlink.plugin
 *          jdk.jlink/jdk.tools.jmod
 *          jdk.jlink/jdk.tools.jimage
 *          jdk.compiler
 * @build tests.*
 * @run main/othervm JLinkPostProcessingTest
 */
public class JLinkPostProcessingTest {

    private static class PPPlugin implements PostProcessor, Plugin {

        private static ExecutableImage called;
        private static final String NAME = "pp";

        @Override
        public List<String> process(ExecutableImage image) {
            called = image;
            Path gen = image.getHome().resolve("lib").resolve("toto.txt");
            try {
                Files.createFile(gen);
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
            return null;
        }

        @Override
        public ResourcePool transform(ResourcePool in, ResourcePoolBuilder out) {
            in.transformAndCopy(Function.identity(), out);
            return out.build();
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
        public String getDescription() {
            return NAME;
        }
    }

    public static void main(String[] args) throws Exception {

        Helper helper = Helper.newHelper();
        if (helper == null) {
            System.err.println("Test not run");
            return;
        }
        helper.generateDefaultModules();

        PluginRepository.registerPlugin(new PPPlugin());

        // Generate an image and post-process in same jlink execution.
        {
            String[] userOptions = {"--pp"};
            String moduleName = "postprocessing1";
            helper.generateDefaultJModule(moduleName, "composite2");
            String[] res = {};
            String[] files = {};
            Path imageDir = helper.generateDefaultImage(userOptions, moduleName).assertSuccess();
            helper.checkImage(imageDir, moduleName, res, files);

            test(imageDir);
        }

        // Generate an image, post-process in 2 jlink executions.
        {
            String[] userOptions = {};
            String moduleName = "postprocessing2";
            helper.generateDefaultJModule(moduleName, "composite2");
            String[] res = {};
            String[] files = {};
            Path imageDir = helper.generateDefaultImage(userOptions, moduleName).assertSuccess();
            helper.checkImage(imageDir, moduleName, res, files);

            String[] ppOptions = {"--pp"};
            helper.postProcessImage(imageDir, ppOptions);
            test(imageDir);
        }
    }

    private static void test(Path imageDir)
            throws Exception {
        if (PPPlugin.called == null) {
            throw new Exception("Post processor not called.");
        }
        if (!PPPlugin.called.getHome().equals(imageDir)) {
            throw new Exception("Not right imageDir " + PPPlugin.called.getHome());
        }
        if (PPPlugin.called.getExecutionArgs().isEmpty()) {
            throw new Exception("No arguments to run java...");
        }
        Path gen = imageDir.resolve("lib").resolve("toto.txt");
        if (!Files.exists(gen)) {
            throw new Exception("Generated file doesn;t exist");
        }
        PPPlugin.called = null;
    }
}
