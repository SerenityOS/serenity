/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @library /test/lib
 * @build ExecJarWithAgent Main Agent AgentHelper
 *        jdk.test.lib.util.JarUtils
 * @run testng ExecJarWithAgent
 * @summary Test starting agents in executable JAR files
 */

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.stream.Stream;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.util.JarUtils;

@Test
public class ExecJarWithAgent {

    /**
     * Basic test of java -jar with agent in the executable JAR
     */
    public void testBasic() throws Exception {
        Manifest man = new Manifest();
        Attributes attrs = man.getMainAttributes();
        attrs.put(Attributes.Name.MANIFEST_VERSION, "1.0.0");
        attrs.put(Attributes.Name.MAIN_CLASS, "Main");
        attrs.put(new Attributes.Name("Launcher-Agent-Class"), "Agent");

        // require all capabilities
        attrs.put(new Attributes.Name("Can-Redefine-Classes"), "true");
        attrs.put(new Attributes.Name("Can-Retransform-Classes"), "true");
        attrs.put(new Attributes.Name("Can-Set-Native-Method-Prefix"), "true");
        attrs.put(new Attributes.Name("Boot-Class-Path"), "helper.jar");

        Path app = Paths.get("app.jar");
        Path dir = Paths.get(System.getProperty("test.classes"));

        Path[] paths = Stream.of("Main.class", "Agent.class")
                .map(Paths::get)
                .toArray(Path[]::new);

        JarUtils.createJarFile(app, man, dir, paths);

        // helper API to test that the BCP has been extended
        Path helper = Paths.get("helper.jar");
        JarUtils.createJarFile(helper, dir, "AgentHelper.class");

        // java -jar app.jar
        assertEquals(exec(app).getExitValue(), 0);
    }

    /**
     * Test that java -jar fails when the executable JAR has the
     * Launcher-Agent-Class attribute but the class cannot be loaded.
     */
    public void testBadAgentClass() throws Exception {
        Manifest man = new Manifest();
        Attributes attrs = man.getMainAttributes();
        attrs.put(Attributes.Name.MANIFEST_VERSION, "1.0.0");
        attrs.put(Attributes.Name.MAIN_CLASS, "Main");

        // agent class does not exist
        attrs.put(new Attributes.Name("Launcher-Agent-Class"), "BadAgent");

        Path app = Paths.get("app.jar");
        Path dir = Paths.get(System.getProperty("test.classes"));

        JarUtils.createJarFile(app, man, dir, Paths.get("Main.class"));

        // java -jar app.jar
        int exitCode = exec(app).shouldContain("ClassNotFoundException").getExitValue();
        assertNotEquals(exitCode, 0);
    }

    /**
     * Test that java -jar fails when the executable JAR has the
     * Launcher-Agent-Class attribute and the class does not define an
     * agentmain method.
     */
    public void testNoAgentMain() throws Exception {
        // manifest for the executable JAR
        Manifest man = new Manifest();
        Attributes attrs = man.getMainAttributes();
        attrs.put(Attributes.Name.MANIFEST_VERSION, "1.0.0");
        attrs.put(Attributes.Name.MAIN_CLASS, "Main");

        // the main class does not define the agentmain method
        attrs.put(new Attributes.Name("Launcher-Agent-Class"), "Main");

        Path app = Paths.get("app.jar");
        Path dir = Paths.get(System.getProperty("test.classes"));

        JarUtils.createJarFile(app, man, dir, Paths.get("Main.class"));

        // java -jar app.jar
        int exitCode = exec(app).shouldContain("NoSuchMethodException").getExitValue();
        assertNotEquals(exitCode, 0);
    }

    /**
     * java -jar app.jar, returning the OutputAnalyzer to analyze the output
     */
    private OutputAnalyzer exec(Path appJar) throws Exception {
        return ProcessTools.executeTestJava("-jar", appJar.toString())
                .outputTo(System.out)
                .errorTo(System.out);
    }


}
