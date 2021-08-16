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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import jdk.tools.jlink.plugin.ResourcePool;
import jdk.tools.jlink.plugin.ResourcePoolBuilder;
import jdk.tools.jlink.internal.PluginRepository;
import jdk.tools.jlink.plugin.Plugin;

import tests.Helper;

/*
 * @test
 * @summary Test jlink options
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
 * @run main JLinkOptionsTest
 */
public class JLinkOptionsTest {

    private static class TestPlugin implements Plugin {
        private final String name;
        private final String option;

        private TestPlugin(String name, String option) {
            this.name = name;
            this.option = option;
        }


        @Override
        public String getOption() {
            return option;
        }

        @Override
        public ResourcePool transform(ResourcePool in, ResourcePoolBuilder out) {
            return out.build();
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getDescription() {
            return name;
        }
    }

    public static void main(String[] args) throws Exception {
        Helper helper = Helper.newHelper();
        if (helper == null) {
            System.err.println("Test not run");
            return;
        }
        helper.generateDefaultModules();
        {
            // multiple plugins with same option

            PluginRepository.
                    registerPlugin(new TestPlugin("test1", "test1"));
            PluginRepository.
                    registerPlugin(new TestPlugin("test2", "test1"));
            helper.generateDefaultImage("composite2").assertFailure("Error: More than one plugin enabled by test1 option");
            PluginRepository.unregisterPlugin("test1");
            PluginRepository.unregisterPlugin("test2");
        }
    }
}
