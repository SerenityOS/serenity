/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import jdk.tools.jlink.internal.PluginRepository;
import jdk.tools.jlink.plugin.Plugin;
import jdk.tools.jlink.plugin.PluginException;
import jdk.tools.jlink.plugin.ResourcePool;
import jdk.tools.jlink.plugin.ResourcePoolBuilder;
import tests.Helper;

/*
 * @test
 * @summary Test plugins enabled by default
 * @author Jean-Francois Denise
 * @requires vm.compMode != "Xcomp"
 * @library ../lib
 * @modules java.base/jdk.internal.jimage
 *          jdk.jdeps/com.sun.tools.classfile
 *          jdk.jlink/jdk.tools.jlink.internal
 *          jdk.jlink/jdk.tools.jlink.plugin
 *          jdk.jlink/jdk.tools.jmod
 *          jdk.jlink/jdk.tools.jimage
 *          jdk.compiler
 * @build tests.*
 * @run main/othervm DefaultProviderTest
 */
public class DefaultProviderTest {
    private static final String NAME = "disable-toto";
    private final static Map<String, Object> expectedOptions = new HashMap<>();

    static {
        expectedOptions.put("disable-toto", "false");
        expectedOptions.put("option1", "value1");
        expectedOptions.put("option2", "value2");
    }

    private static class Custom implements Plugin {
        private boolean enabled = true;

        @Override
        public Set<State> getState() {
             return enabled ? EnumSet.of(State.AUTO_ENABLED, State.FUNCTIONAL)
                : EnumSet.of(State.DISABLED);
        }

        @Override
        public ResourcePool transform(ResourcePool in, ResourcePoolBuilder out) {
            if (!enabled) {
                throw new PluginException(NAME + " was set");
            }

            DefaultProviderTest.isNewPluginsCalled = true;
            in.transformAndCopy(content -> {
                return content;
            }, out);

            return out.build();
        }

        @Override
        public String getName() {
            return NAME;
        }

        @Override
        public String getDescription() {
            return NAME;
        }

        @Override
        public boolean hasArguments() {
            return true;
        }

        @Override
        public void configure(Map<String, String> config) {
            if (config.containsKey(NAME)) {
                enabled = !Boolean.parseBoolean(config.get(NAME));
            }

            if (enabled) {
                DefaultProviderTest.receivedOptions = config;
            } else {
                DefaultProviderTest.receivedOptions = null;
            }
        }
    }

    private static boolean isNewPluginsCalled;
    private static Map<String, String> receivedOptions;

    private static void reset() {
        isNewPluginsCalled = false;
        receivedOptions = null;
    }

    public static void main(String[] args) throws Exception {
        Helper helper = Helper.newHelper();
        if (helper == null) {
            System.err.println("Test not run");
            return;
        }
        helper.generateDefaultModules();
        test(helper, new Custom());
    }

    private static void test(Helper helper, Plugin plugin) throws Exception {
        PluginRepository.registerPlugin(plugin);

        {
            String[] userOptions = {};
            Path imageDir = helper.generateDefaultImage(userOptions, "composite2").assertSuccess();
            helper.checkImage(imageDir, "composite2", null, null);
            if (!isNewPluginsCalled) {
                throw new Exception("Should have been called");
            }
            reset();
        }

        {
            String[] userOptions = {"--disable-toto=false:option1=value1:option2=value2"};
            Path imageDir = helper.generateDefaultImage(userOptions, "composite2").assertSuccess();
            helper.checkImage(imageDir, "composite2", null, null);
            if (!isNewPluginsCalled) {
                throw new Exception("Should have been called");
            }
            if (!receivedOptions.equals(expectedOptions)) {
                throw new Exception("Optional options " + receivedOptions + " are not expected one "
                        + expectedOptions);
            }
            System.err.println("OPTIONS " + receivedOptions);
            reset();
        }

        {
            String[] userOptions = {"--disable-toto=true:option1=value1"};
            Path imageDir = helper.generateDefaultImage(userOptions, "composite2").assertSuccess();
            helper.checkImage(imageDir, "composite2", null, null);
            if (isNewPluginsCalled) {
                throw new Exception("Should not have been called");
            }
            if (receivedOptions != null) {
                throw new Exception("Optional options are not expected");
            }
            reset();
        }
    }
}
