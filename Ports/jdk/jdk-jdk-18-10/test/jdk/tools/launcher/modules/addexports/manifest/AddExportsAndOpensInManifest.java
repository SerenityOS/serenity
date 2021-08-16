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

/**
 * @test
 * @library /test/lib
 * @modules jdk.compiler
 * @build AddExportsAndOpensInManifest Test2
 *        jdk.test.lib.util.JarUtils
 * @compile --add-exports=java.base/jdk.internal.misc=ALL-UNNAMED Test1.java
 * @run testng AddExportsAndOpensInManifest
 * @summary Basic test for Add-Exports and Add-Opens attributes in the
 *          manifest of a main application JAR
 */

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.util.JarUtils;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import static org.testng.Assert.*;


@Test
public class AddExportsAndOpensInManifest {

    private String testName;
    private int testCaseNum;

    @BeforeMethod
    public void getTestName(Method m){
        testName = m.getName();
        testCaseNum = 0;
    }

    /**
     * Package Test1 and Test2 into a JAR file with the given attributes
     * in the JAR manifest, then execute the JAR file with `java -jar`.
     */
    private OutputAnalyzer runTest(String attributes) throws Exception {
        Manifest man = new Manifest();
        Attributes attrs = man.getMainAttributes();
        attrs.put(Attributes.Name.MANIFEST_VERSION, "1.0");

        for (String nameAndValue : attributes.split(",")) {
            String[] s = nameAndValue.split("=");
            if (s.length != 2)
                throw new RuntimeException("Malformed: " + nameAndValue);
            String name = s[0];
            String value = s[1];
            attrs.put(new Attributes.Name(name), value);
        }

        // create the JAR file with Test1 and Test2
        Path jarfile = Paths.get(String.format("%s-%s.jar", testName, ++testCaseNum));
        Files.deleteIfExists(jarfile);

        Path classes = Paths.get(System.getProperty("test.classes", ""));
        JarUtils.createJarFile(jarfile, man, classes,
                Paths.get("Test1.class"), Paths.get("Test2.class"));

        // java -jar test.jar
        return ProcessTools.executeTestJava("-jar", jarfile.toString())
                .outputTo(System.out)
                .errorTo(System.out);
    }

    /**
     * Run test with the given JAR attributes, expecting the test to pass
     */
    private void runExpectingPass(String attrs) throws Exception {
        int exitValue = runTest(attrs).getExitValue();
        assertTrue(exitValue == 0);
    }

    /**
     * Run test with the given JAR attributes, expecting the test to fail
     * with at least the given output
     */
    private void runExpectingFail(String attrs, String errorString) throws Exception {
        int exitValue = runTest(attrs).shouldContain(errorString).getExitValue();
        assertTrue(exitValue != 0);
    }


    /**
     * Run tests to make sure that they fail in the expected way.
     */
    public void testSanity() throws Exception {
        runExpectingFail("Main-Class=Test1", "IllegalAccessError");
        runExpectingFail("Main-Class=Test2", "InaccessibleObjectException");
    }

    /**
     * Run tests with the Add-Exports attribute in the main manifest.
     */
    public void testWithAddExports() throws Exception {
        runExpectingPass("Main-Class=Test1,Add-Exports=java.base/jdk.internal.misc");
        runExpectingFail("Main-Class=Test2,Add-Exports=java.base/jdk.internal.misc",
                         "InaccessibleObjectException");

        // run with leading and trailing spaces
        runExpectingPass("Main-Class=Test1,Add-Exports=  java.base/jdk.internal.misc");
        runExpectingPass("Main-Class=Test1,Add-Exports=java.base/jdk.internal.misc  ");

        // run with multiple values
        runExpectingPass("Main-Class=Test1,Add-Exports=java.base/jdk.internal.misc"
                + " java.base/jdk.internal.loader");
        runExpectingPass("Main-Class=Test1,Add-Exports=java.base/jdk.internal.loader"
                + " java.base/jdk.internal.misc");

        // run with duplicate values
        runExpectingPass("Main-Class=Test1,Add-Exports=java.base/jdk.internal.misc"
                + " java.base/jdk.internal.misc");
    }

    /**
     * Run tests with the Add-Opens attribute in the main manifest.
     */
    public void testWithAddOpens() throws Exception {
        runExpectingPass("Main-Class=Test1,Add-Opens=java.base/jdk.internal.misc");
        runExpectingPass("Main-Class=Test2,Add-Opens=java.base/jdk.internal.misc");

        // run with leading and trailing spaces
        runExpectingPass("Main-Class=Test1,Add-Opens=  java.base/jdk.internal.misc");
        runExpectingPass("Main-Class=Test1,Add-Opens=java.base/jdk.internal.misc  ");

        // run with multiple values
        runExpectingPass("Main-Class=Test1,Add-Opens=java.base/jdk.internal.misc"
                + " java.base/jdk.internal.loader");
        runExpectingPass("Main-Class=Test1,Add-Opens=java.base/jdk.internal.loader"
                + " java.base/jdk.internal.misc");

        // run with duplicate values
        runExpectingPass("Main-Class=Test1,Add-Opens=java.base/jdk.internal.misc"
                + " java.base/jdk.internal.misc");
    }

    /**
     * Run tests a bad module or package name
     */
    public void testWithBadModuleOrPackage() throws Exception {
        // Add-Exports with bad module name
        String attrs = "Main-Class=Test1,Add-Exports=java.DoesNotExist/jdk.internal.misc";
        runExpectingFail(attrs, "IllegalAccessError");

        // Add-Exports with bad package name
        attrs = "Main-Class=Test1,Add-Exports=java.base/jdk.internal.DoesNotExit";
        runExpectingFail(attrs, "IllegalAccessError");

        // Add-Opens with bad module name
        attrs = "Main-Class=Test1,Add-Opens=java.DoesNotExist/jdk.internal.misc";
        runExpectingFail(attrs, "IllegalAccessError");

        // Add-Opens with bad package name
        attrs = "Main-Class=Test1,Add-Opens=java.base/jdk.internal.DoesNotExit";
        runExpectingFail(attrs, "IllegalAccessError");
    }
}
