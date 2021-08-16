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

package catalog;

import static catalog.CatalogTestUtils.DEFER_FALSE;
import static catalog.CatalogTestUtils.FEATURE_DEFER;
import static catalog.CatalogTestUtils.FEATURE_FILES;
import static catalog.CatalogTestUtils.FEATURE_PREFER;
import static catalog.CatalogTestUtils.FEATURE_RESOLVE;
import static catalog.CatalogTestUtils.PREFER_SYSTEM;
import static catalog.CatalogTestUtils.RESOLVE_CONTINUE;
import static catalog.CatalogTestUtils.catalogResolver;
import static catalog.CatalogTestUtils.catalogUriResolver;
import static catalog.CatalogTestUtils.createPropsContent;
import static catalog.CatalogTestUtils.deleteJAXPProps;
import static catalog.CatalogTestUtils.generateJAXPProps;
import static catalog.CatalogTestUtils.getCatalogPath;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.catalog.CatalogResolver;

import org.testng.Assert;
import org.testng.annotations.Test;

/*
 * @test
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/isolatedjdk
 * @run shell/timeout=600 ../IsolatedJDK.sh JAXP_PROPS
 * @run testng catalog.PropertiesTest
 * @run shell/timeout=600 ../IsolatedJDK.sh JAXP_PROPS remove
 * @summary This test case tests if the properties FILES, DEFER, PREFER,
 *          RESOLVE in jaxp.properties and system properties are used.
 *          It needs to run in a copied JDK as it modifies the JDK's
 *          jaxp.properties file.
 * @bug 8077931
 */
public class PropertiesTest {

    private static final String CATALOG_PROPERTIES = "properties.xml";

    @Test
    /*
     * Run main in a child process as it will modify the JDK.
     */
    public void test() throws Exception {
        // get required properties and do some assertions
        String javaclasspath = System.getProperty("java.class.path");
        Assert.assertNotNull(javaclasspath, "Test class path is null");
        String testclasspath = System.getProperty("test.class.path");
        Assert.assertNotNull(testclasspath, "Test class path is null");
        String testsourcepath = System.getProperty("test.src");
        Assert.assertNotNull(testsourcepath, "Test source path is null");

        // start the child process
        List<String> testCall = new ArrayList<>(6);
        testCall.add(Paths.get("ISOLATED_JDK_JAXP_PROPS", "/bin", "java").toString());
        testCall.add("-cp");
        testCall.add(javaclasspath);
        testCall.add("-Dtest.class.path=" + testclasspath);
        testCall.add("-Dtest.src=" + testsourcepath);
        testCall.add("catalog.PropertiesTest");
        System.out.println("Starting child process: " + Arrays.toString(testCall.toArray()));
        Process test = new ProcessBuilder(testCall).start();

        // wait for it to finish
        boolean interrupted = false;
        do {
            try {
                test.waitFor();
                interrupted = false;
            } catch (InterruptedException ie) {
                interrupted = true;
            }
        } while (interrupted);

        // trace system.out of child process
        System.out.println("Proccess Out:");
        BufferedReader br = new BufferedReader(new InputStreamReader(test.getInputStream()));
        String line;
        while ((line = br.readLine()) != null) {
            System.out.println(line);
        }
        br.close();

        // trace system.err of child process
        System.out.println("Proccess Err:");
        br = new BufferedReader(new InputStreamReader(test.getErrorStream()));
        while ((line = br.readLine()) != null) {
            System.out.println(line);
        }
        br.close();

        // trace exit value and assert 0
        int exitValue = test.exitValue();
        System.out.println("Process Exit code: " + exitValue);
        Assert.assertEquals(exitValue, 0, "PropertiesTest returned nonzero exit code.");
    }

    public static void main(String[] args) throws Exception {
        System.out.println("testJAXPProperties started");
        testJAXPProperties();
        System.out.println("testJAXPProperties ended");

        System.out.println("testSystemProperties started");
        testSystemProperties();
        System.out.println("testSystemProperties ended");

        System.out.println("Test passed");
    }

    /*
     * Tests how jaxp.properties affects the resolution.
     * Be careful: This test modifies jaxp.properties in the used JDK.
     */
    private static void testJAXPProperties() throws IOException {
        generateJAXPProps(createJAXPPropsContent());
        testProperties();
        deleteJAXPProps();
    }

    /*
     * Tests how system properties affects the resolution.
     */
    private static void testSystemProperties() {
        setSystemProperties();
        testProperties();
    }

    private static void testProperties() {
        testPropertiesOnEntityResolver();
        testPropertiesOnUriResolver();
    }

    private static void testPropertiesOnEntityResolver() {
        CatalogResolver entityResolver = catalogResolver((String[]) null);
        entityResolver.resolveEntity("-//REMOTE//DTD DOCDUMMY XML//EN",
                "http://remote/sys/dtd/docDummy.dtd");
        "http://local/base/dtd/docSys.dtd".equals(
                entityResolver.resolveEntity("-//REMOTE//DTD DOC XML//EN",
                        "http://remote/dtd/doc.dtd").getSystemId());
    }

    private static void testPropertiesOnUriResolver() {
        CatalogResolver uriResolver = catalogUriResolver((String[]) null);
        uriResolver.resolve("http://remote/uri/dtd/docDummy.dtd", null);
        "http://local/base/dtd/docURI.dtd".equals(uriResolver.resolve(
                "http://remote/dtd/doc.dtd", null).getSystemId());
    }

    // The properties in jaxp.properties don't use default values
    private static String createJAXPPropsContent() {
        Map<String, String> props = new HashMap<>();
        props.put(FEATURE_FILES, getCatalogPath(CATALOG_PROPERTIES).toString());
        props.put(FEATURE_DEFER, DEFER_FALSE);
        props.put(FEATURE_PREFER, PREFER_SYSTEM);
        props.put(FEATURE_RESOLVE, RESOLVE_CONTINUE);
        return createPropsContent(props);
    }

    // The system properties don't use default values
    private static void setSystemProperties() {
        System.setProperty(FEATURE_FILES, getCatalogPath(CATALOG_PROPERTIES).toString());
        System.setProperty(FEATURE_DEFER, DEFER_FALSE);
        System.setProperty(FEATURE_PREFER, PREFER_SYSTEM);
        System.setProperty(FEATURE_RESOLVE, RESOLVE_CONTINUE);
    }
}
