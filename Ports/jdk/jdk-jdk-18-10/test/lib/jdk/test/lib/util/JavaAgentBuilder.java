/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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

package jdk.test.lib.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import jdk.test.lib.Utils;
import jdk.test.lib.util.JarUtils;

/**
 * A builder for a common Java agent.
 * Can be used directly from the jtreg test header to
 * build a java agent before the test is executed.
 *
 * E.g.:
 * @run driver jdk.test.lib.util.JavaAgentBuilder
 *             jdk.jfr.javaagent.EventEmitterAgent EventEmitterAgent.jar
 *
 */
public class JavaAgentBuilder {

    /**
     * Build a java agent jar file with a given agent class.
     *
     * @param args[0]    fully qualified name of an agent class
     * @param args[1]    file name of the agent jar to be created
     * @throws IOException
     */
    public static void main(String... args) throws Exception {
        String agentClass = args[0];
        String agentJar = args[1];
        System.out.println("Building " + agentJar + " with agent class " + agentClass);

        build(agentClass, agentJar, parseExtraAttrs(args));
    }

    private static Map<String,String> parseExtraAttrs(String[] args) throws Exception {
        Map<String,String> attrs = new HashMap<>();
        for (int i = 2; i < args.length; i++) {
            String[] parts = args[i].split(":");
            if (parts.length != 2) {
                throw new IllegalArgumentException("Extra attributes should be of format 'key:value'");
            }
            attrs.put(parts[0],parts[1]);
        }
        return attrs;
    }

    /**
     * Build a java agent jar file with a given agent class.
     * The agent class will be added as both premain class and agent class.
     *
     * @param agentClass fully qualified name of an agent class
     * @param agentJar   file name of the agent jar to be created
     *                   the file will be placed in a current work directory
     * @throws IOException
     */
    public static void build(String agentClass, String agentJar) throws IOException {
        build(agentClass, agentJar, new HashMap<String, String>());
    }

    /**
     * Build a java agent jar file with a given agent class.
     * The agent class will be added as both premain class and agent class.
     *
     * @param agentClass fully qualified name of an agent class
     * @param agentJar   file name of the agent jar to be created
     *                   the file will be placed in a current work directory
     * @param extraAttrs additional manifest attributes
     * @throws IOException
     */
    public static void build(String agentClass, String agentJar,
                             Map<String, String> extraAttrs) throws IOException {
        Manifest mf = new Manifest();
        Attributes attrs = mf.getMainAttributes();
        attrs.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        attrs.putValue("Can-Redefine-Classes", "true");
        attrs.putValue("Can-Retransform-Classes", "true");
        attrs.putValue("Premain-Class", agentClass);
        attrs.putValue("Agent-Class", agentClass);

        extraAttrs.forEach( (k,v) -> attrs.putValue(k,v));

        Path jarFile = Paths.get(".", agentJar);
        String testClasses = Utils.TEST_CLASSES;
        String agentPath = agentClass.replace(".", File.separator) + ".class";
        Path agentFile = Paths.get(testClasses, agentPath);
        Path dir = Paths.get(testClasses);
        JarUtils.createJarFile(jarFile, mf, dir, agentFile);
        System.out.println("Agent built:" + jarFile.toAbsolutePath());
    }
}
