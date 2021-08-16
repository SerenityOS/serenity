/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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
import java.io.*;
import java.nio.file.*;
import java.util.Arrays;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jdk.test.lib.Platform;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.dcmd.*;
import org.testng.annotations.Test;

/*
 * Test to attach JVMTI java agent.
 *
 * @test
 * @bug 8147388
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.compiler
 *          java.instrument
 *          java.management
 *          jdk.internal.jvmstat/sun.jvmstat.monitor
 * @build SimpleJvmtiAgent
 * @run driver jdk.test.lib.helpers.ClassFileInstaller SimpleJvmtiAgent
 * @run testng/othervm LoadAgentDcmdTest
 */
public class LoadAgentDcmdTest {

    public String getLibInstrumentPath() throws FileNotFoundException {
        String jdkPath = System.getProperty("test.jdk");

        if (jdkPath == null) {
            throw new RuntimeException(
                      "System property 'test.jdk' not set. " +
                      "This property is normally set by jtreg. " +
                      "When running test separately, set this property using " +
                      "'-Dtest.jdk=/path/to/jdk'.");
        }

        Path libpath = Paths.get(jdkPath, jdkLibPath(), sharedObjectName("instrument"));

        if (!libpath.toFile().exists()) {
            throw new FileNotFoundException(
                      "Could not find " + libpath.toAbsolutePath());
        }

        return libpath.toAbsolutePath().toString();
    }


    public void createJarFileForAgent()
      throws IOException {

        final String jarName = "agent.jar";
        final String agentClass = "SimpleJvmtiAgent";

        Manifest manifest = new Manifest();

        manifest.getMainAttributes().put(
             Attributes.Name.MANIFEST_VERSION, "1.0");

        manifest.getMainAttributes().put(
             new Attributes.Name("Agent-Class"), agentClass);

        JarOutputStream target = null;

        try {
            target = new
              JarOutputStream(new FileOutputStream(jarName), manifest);
            JarEntry entry = new JarEntry(agentClass + ".class");
            target.putNextEntry(entry);
            target.closeEntry();
        } finally {
            target.close();
        }
    }

    static void checkWarningsOnly(OutputAnalyzer out) {
        // stderr should be empty except for VM warnings.
        if (!out.getStderr().isEmpty()) {
            List<String> lines = Arrays.asList(out.getStderr().split("(\\r\\n|\\n|\\r)"));
            Pattern p = Pattern.compile(".*VM warning.*");
            for (String line : lines) {
                Matcher m = p.matcher(line);
                if (!m.matches()) {
                    throw new RuntimeException("Stderr has output other than VM warnings");
                }
            }
        }
    }

    public void run(CommandExecutor executor)  {
        try{

            createJarFileForAgent();

            String libpath = getLibInstrumentPath();
            OutputAnalyzer output = null;

            // Test 1: Native agent, no arguments
            output = executor.execute("JVMTI.agent_load " +
                                          libpath + " agent.jar");
            checkWarningsOnly(output);

            // Test 2: Native agent, with arguments
            output = executor.execute("JVMTI.agent_load " +
                                          libpath + " \"agent.jar=foo=bar\"");
            checkWarningsOnly(output);

            // Test 3: Java agent, no arguments
            output = executor.execute("JVMTI.agent_load " +
                                          "agent.jar");
            checkWarningsOnly(output);

            // Test 4: Java agent, with arguments
            output = executor.execute("JVMTI.agent_load " +
                                          "\"agent.jar=foo=bar\"");
            checkWarningsOnly(output);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    /**
     * return path to library inside jdk tree
     */
    public static String jdkLibPath() {
        if (Platform.isWindows()) {
            return "bin";
        }
        return "lib";
    }

    /**
     * Build name of shared object according to platform rules
     */
    public static String sharedObjectName(String name) {
        if (Platform.isWindows()) {
            return name + ".dll";
        }
        if (Platform.isOSX()) {
            return "lib" + name + ".dylib";
        }
        return "lib" + name + ".so";
    }

    @Test
    public void jmx() throws Throwable {
        run(new JMXExecutor());
    }

    @Test
    public void cli() throws Throwable {
        run(new PidJcmdExecutor());
    }
}
