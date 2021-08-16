/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

import static org.testng.Assert.assertTrue;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import static jdk.test.lib.process.ProcessTools.executeTestJava;
import jdk.test.lib.compiler.CompilerUtils;

import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

/*
 * @test
 * @library /test/lib
 * @run testng BasicModularXMLParserTest
 * @bug 8078820 8156119
 * @summary Tests JAXP lib can instantiate the following interfaces
 *          with customized provider module on boot layer
 *
 *          javax.xml.datatype.DatatypeFactory
 *          javax.xml.parsers.DocumentBuilderFactory
 *          javax.xml.parsers.SAXParserFactory
 *          javax.xml.stream.XMLEventFactory
 *          javax.xml.stream.XMLInputFactory
 *          javax.xml.stream.XMLOutputFactory
 *          javax.xml.transform.TransformerFactory
 *          javax.xml.validation.SchemaFactory
 *          javax.xml.xpath.XPathFactory
 *          org.xml.sax.XMLReader
 */

@Test
public class BasicModularXMLParserTest {

    private static final String TEST_SRC = System.getProperty("test.src");

    private static final Path SRC_DIR = Paths.get(TEST_SRC, "src");
    private static final Path MOD_DIR1 = Paths.get("mod1");
    private static final Path MOD_DIR2 = Paths.get("mod2");
    private static final Path CLASSES_DIR = Paths.get("classes");

    /*
     * Compiles all modules used by the test
     */
    @BeforeTest
    public void compileAll() throws Exception {
        assertTrue(CompilerUtils.compile(SRC_DIR.resolve("xmlprovider1"), MOD_DIR1.resolve("xmlprovider1")));
        assertTrue(CompilerUtils.compile(SRC_DIR.resolve("xmlprovider2"), MOD_DIR2.resolve("xmlprovider2")));
        assertTrue(CompilerUtils.compile(SRC_DIR.resolve("unnamed"), CLASSES_DIR));
    }

    /*
     * test the default JAXP implementation
     */
    public void testDefault() throws Exception {
        int exitValue
            = executeTestJava("-cp", CLASSES_DIR.toString(),
                              "Main")
                .outputTo(System.out)
                .errorTo(System.out)
                .getExitValue();

        assertTrue(exitValue == 0);
    }

    /*
     * test loading one provider module
     */
    public void testWithOneProvider() throws Exception {
        int exitValue
            = executeTestJava("--module-path", MOD_DIR1.toString(),
                              "-cp", CLASSES_DIR.toString(),
                              "Main", "xmlprovider1")
                .outputTo(System.out)
                .errorTo(System.out)
                .getExitValue();

        assertTrue(exitValue == 0);
    }

    /*
     * test loading both provider modules
     */
    public void testWithTwoProvider() throws Exception {
        int exitValue
            = executeTestJava("--module-path", MOD_DIR1.toString() + File.pathSeparator + MOD_DIR2.toString(),
                              "-cp", CLASSES_DIR.toString(),
                              "Main", "xmlprovider1", "xmlprovider2")
                .outputTo(System.out)
                .errorTo(System.out)
                .getExitValue();

        assertTrue(exitValue == 0);
    }

}
