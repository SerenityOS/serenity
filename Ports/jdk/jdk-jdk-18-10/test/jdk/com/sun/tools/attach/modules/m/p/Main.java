/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

package p;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import com.sun.tools.attach.VirtualMachine;

public class Main {

    public static void main(String[] args) throws Exception {
        System.out.println("#modules loaded: " + moduleInfoCont());

        String vmid = "" + ProcessHandle.current().pid();
        VirtualMachine vm = VirtualMachine.attach(vmid);

        for (String test : args) {
            switch (test) {
                case "jmx" :
                    startJMXAgent(vm);
                    break;
                case "javaagent" :
                    startJavaAgent(vm, createAgentJar());
                    break;
            }

            System.out.println("#modules loaded: " + moduleInfoCont());
        }
    }

    /**
     * Locates module-info.class resources to get a count of the module of system
     * modules.
     */
    static long moduleInfoCont() {
        ClassLoader scl = ClassLoader.getSystemClassLoader();
        return scl.resources("module-info.class").count();
    }

    /**
     * Starts a JMX agent and checks that java.management is loaded.
     */
    static void startJMXAgent(VirtualMachine vm) throws Exception {
        System.out.println("Start JMX agent");
        vm.startLocalManagementAgent();

        // types in java.management should be visible
        Class.forName("javax.management.MXBean");
    }

    /**
     * Loads a java agent into the VM and checks that java.instrument is loaded.
     */
    static void startJavaAgent(VirtualMachine vm, Path agent) throws Exception {
        System.out.println("Load java agent ...");
        vm.loadAgent(agent.toString());

        // the Agent class should be visible
        Class.forName("Agent");

        // types in java.instrument should be visible
        Class.forName("java.lang.instrument.Instrumentation");
    }

    /**
     * Creates a java agent, return the file path to the agent JAR file.
     */
    static Path createAgentJar() throws IOException {
        Manifest man = new Manifest();
        Attributes attrs = man.getMainAttributes();
        attrs.put(Attributes.Name.MANIFEST_VERSION, "1.0.0");
        attrs.put(new Attributes.Name("Agent-Class"), "Agent");
        Path agent = Paths.get("agent.jar");
        Path dir = Paths.get(System.getProperty("test.classes"));
        createJarFile(agent, man, dir, "Agent.class");
        return agent;
    }

    /**
     * Creates a JAR file.
     *
     * Equivalent to {@code jar cfm <jarfile> <manifest> -C <dir> file...}
     *
     * The input files are resolved against the given directory. Any input
     * files that are directories are processed recursively.
     */
    static void createJarFile(Path jarfile, Manifest man, Path dir, String... files)
        throws IOException
    {
        // create the target directory
        Path parent = jarfile.getParent();
        if (parent != null)
            Files.createDirectories(parent);

        List<Path> entries = new ArrayList<>();
        for (String file : files) {
            Files.find(dir.resolve(file), Integer.MAX_VALUE,
                    (p, attrs) -> attrs.isRegularFile())
                    .map(e -> dir.relativize(e))
                    .forEach(entries::add);
        }

        try (OutputStream out = Files.newOutputStream(jarfile);
             JarOutputStream jos = new JarOutputStream(out))
        {
            if (man != null) {
                JarEntry je = new JarEntry(JarFile.MANIFEST_NAME);
                jos.putNextEntry(je);
                man.write(jos);
                jos.closeEntry();
            }

            for (Path entry : entries) {
                String name = toJarEntryName(entry);
                jos.putNextEntry(new JarEntry(name));
                Files.copy(dir.resolve(entry), jos);
                jos.closeEntry();
            }
        }
    }

    /**
     * Map a file path to the equivalent name in a JAR file
     */
    static String toJarEntryName(Path file) {
        Path normalized = file.normalize();
        return normalized.subpath(0, normalized.getNameCount())
                .toString()
                .replace(File.separatorChar, '/');
    }
}
