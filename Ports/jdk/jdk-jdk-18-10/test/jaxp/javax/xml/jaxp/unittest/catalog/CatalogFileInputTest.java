/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.xml.catalog.Catalog;
import javax.xml.catalog.CatalogException;
import javax.xml.catalog.CatalogFeatures;
import javax.xml.catalog.CatalogManager;
import javax.xml.catalog.CatalogResolver;

import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;
import static jaxp.library.JAXPTestUtilities.getSystemProperty;

import jdk.test.lib.util.JarUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.xml.sax.InputSource;
import jdk.test.lib.net.SimpleHttpServer;

/*
 * @test
 * @bug 8151154 8171243
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest /test/lib
 * @build jdk.test.lib.net.SimpleHttpServer
 * @run testng/othervm catalog.CatalogFileInputTest
 * @summary Verifies that the Catalog API accepts valid URIs only;
 *          Verifies that the CatalogFeatures' builder throws
 *          IllegalArgumentException on invalid file inputs.
 *          This test was splitted from CatalogTest.java due to
 *          JDK-8168968, it has to only run without SecurityManager
 *          because an ACE will be thrown for invalid path.
 */
@Listeners({jaxp.library.FilePolicy.class, jaxp.library.NetAccessPolicy.class})
public class CatalogFileInputTest extends CatalogSupportBase {

    static final CatalogFeatures FEATURES = CatalogFeatures.builder().
            with(CatalogFeatures.Feature.PREFER, "system").build();
    static String CLS_DIR = getSystemProperty("test.classes");
    static String SRC_DIR = System.getProperty("test.src");
    static String JAR_CONTENT = "META-INF";
    final static String SCHEME_JARFILE = "jar:";
    static final String REMOTE_FILE_LOCATION = "/jar/META-INF";
    static final String DOCROOT = SRC_DIR;
    static final String TESTCONTEXT = REMOTE_FILE_LOCATION;  //mapped to local file path
    private SimpleHttpServer httpserver;
    private String remoteFilePath;

    /*
     * Initializing fields
     */
    @BeforeClass
    public void setUpClass() throws Exception {
        super.setUp();
        // set up HttpServer
        httpserver = new SimpleHttpServer(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), TESTCONTEXT, DOCROOT);
        httpserver.start();
        remoteFilePath = httpserver.getAddress() + REMOTE_FILE_LOCATION;
    }

    @AfterClass
    protected void tearDown() {
        if (httpserver != null) {
            httpserver.stop();
        }
    }

    /*
     * Verifies that the Catalog can be created with file system paths including JAR
     * and http URL, and used to resolve a systemId as expected.
     */
    @Test(dataProvider = "acceptedURI")
    public void testMatch(final String uri, final String sysId, final String pubId,
            final String expectedId, final String msg) {
        CatalogResolver cr = CatalogManager.catalogResolver(FEATURES, URI.create(uri));
        InputSource is = cr.resolveEntity(pubId, sysId);
        Assert.assertNotNull(is, msg);
        Assert.assertEquals(expectedId, is.getSystemId(), msg);
    }

    @Test(dataProvider = "invalidCatalog")
    public void testEmptyCatalog(final String uri, final String publicId, final String msg) {
        Catalog c = CatalogManager.catalog(FEATURES, uri != null ? URI.create(uri) : null);
        Assert.assertNull(c.matchSystem(publicId), msg);
    }

    @Test(dataProvider = "invalidCatalog", expectedExceptions = CatalogException.class)
    public void testCatalogResolverWEmptyCatalog(final String uri, final String publicId, final String msg) {
        CatalogResolver cr = CatalogManager.catalogResolver(
                CatalogFeatures.builder().with(CatalogFeatures.Feature.RESOLVE, "strict").build(),
                uri != null ? URI.create(uri) : null);
        InputSource is = cr.resolveEntity(publicId, "");
    }

    @Test(dataProvider = "invalidCatalog")
    public void testCatalogResolverWEmptyCatalog1(final String uri, final String publicId, final String msg) {
        CatalogResolver cr = CatalogManager.catalogResolver(
                CatalogFeatures.builder().with(CatalogFeatures.Feature.RESOLVE, "continue").build(),
                uri != null ? URI.create(uri) : null);
        Assert.assertNull(cr.resolveEntity(publicId, ""), msg);
    }

    @Test(dataProvider = "invalidInput", expectedExceptions = IllegalArgumentException.class)
    public void testFileInput(final String file) {
        CatalogFeatures features = CatalogFeatures.builder()
            .with(CatalogFeatures.Feature.FILES, file)
            .build();
    }

    @Test(dataProvider = "invalidInput", expectedExceptions = IllegalArgumentException.class)
    public void testInvalidUri(final String file) {
        CatalogResolver cr = CatalogManager.catalogResolver(FEATURES, file != null ? URI.create(file) : null);
    }

    @Test(dataProvider = "invalidInput", expectedExceptions = IllegalArgumentException.class)
    public void testInvalidUri1(final String file) {
        Catalog c = CatalogManager.catalog(FEATURES, file != null ? URI.create(file) : null);
        System.err.println("Catalog =" + c);
    }


    @Test(expectedExceptions = NullPointerException.class)
    public void testNullFileInput() {
        CatalogFeatures features = CatalogFeatures.builder()
            .with(CatalogFeatures.Feature.FILES, null)
            .build();
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testNullUri() {
        URI uri = null;
        CatalogResolver cr = CatalogManager.catalogResolver(FEATURES, uri);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testNullUri1() {
        URI uri = null;
        Catalog c = CatalogManager.catalog(FEATURES, uri);
    }

    private String systemId = "http://www.sys00test.com/rewrite.dtd";
    private String publicId = "PUB-404";
    private String expected = "http://www.groupxmlbase.com/dtds/rewrite.dtd";
    private String errMsg = "Relative rewriteSystem with xml:base at group level failed";

    /*
        DataProvider: used to verify CatalogResolver's resolveEntity function.
        Data columns:
        catalog, systemId, publicId, expectedUri, msg
     */
    @DataProvider(name = "acceptedURI")
    Object[][] getData() throws Exception {
        String filename = "rewriteSystem_id.xml";
        String urlFile = getClass().getResource(filename).toExternalForm();
        String urlHttp = remoteFilePath + "/jax-ws-catalog.xml";
        String remoteXSD = remoteFilePath + "/catalog/ws-addr.xsd";
        File file = new File(CLS_DIR + "/JDK8171243.jar!/META-INF/jax-ws-catalog.xml");
        String jarPath = SCHEME_JARFILE + file.toURI().toString();
        String xsd = jarPath.substring(0, jarPath.lastIndexOf("/")) + "/catalog/ws-addr.xsd";

        // create JAR file
        try {
            JarUtils.createJarFile(Paths.get(CLS_DIR + "/JDK8171243.jar"),
                    Paths.get(SRC_DIR + "/jar"), JAR_CONTENT);
        } catch (IOException ex) {
            Assert.fail("Failed to create JAR: " + ex.getMessage());
        }

        return new Object[][]{
            // URL
            {urlFile, systemId, publicId, expected, errMsg},
            {urlHttp, "http://www.w3.org/2006/03/addressing/ws-addr.xsd", "", remoteXSD, "http test failed."},
            // JAR file
            {jarPath, "http://www.w3.org/2006/03/addressing/ws-addr.xsd", "", xsd, "jar file test failed."},
        };
    }

    /*
     *  DataProvider: invalid catalog result in empty catalog
     *  Note: the difference from invalidInput is that invalidInput is syntactically
     *  rejected with an IAE.
     */
    @DataProvider(name = "invalidCatalog")
    public Object[][] getInvalidCatalog() {
        String catalogUri = getClass().getResource("catalog_invalid.xml").toExternalForm();
        return new Object[][]{
            {catalogUri, "-//W3C//DTD XHTML 1.0 Strict//EN",
                "The catalog is invalid, attempting to match the public entry shall return null."},
            {"file:/../../..", "-//W3C//DTD XHTML 1.0 Strict//EN",
                "The catalog is invalid, attempting to match the public entry shall return null."}
        };
    }

    /*
     *  DataProvider: a list of invalid inputs, expects IAE
     *  Note: exclude null since NPE would have been expected
     */
    @DataProvider(name = "invalidInput")
    public Object[][] getFiles() throws Exception {
        String filename = "rewriteSystem_id.xml";
        copyFile(Paths.get(SRC_DIR + "/" + filename), Paths.get(filename));
        String absolutePath = getClass().getResource(filename).getFile();

        return new Object[][]{
            {""},
            {"file:a/b\\c"},
            {"c:/te:t"},
            {"c:/te?t"},
            {"c/te*t"},
            {"in|valid.txt"},
            {"shema:invalid.txt"},
            // relative file path
            {filename},
            // absolute file path
            {absolutePath}
        };
    }

    /*
       DataProvider: a list of invalid inputs
     */
    @DataProvider(name = "nullTest")
    public Object[][] getNull() {
        return new Object[][]{
            {null},
        };
    }

    void copyFile(final Path src, final Path target) throws Exception {
        try (InputStream in = Files.newInputStream(src);
                BufferedReader reader
                = new BufferedReader(new InputStreamReader(in));
                OutputStream out = new BufferedOutputStream(
                        Files.newOutputStream(target, CREATE, APPEND));
                BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(out))) {
            String line;
            while ((line = reader.readLine()) != null) {
                bw.write(line);
            }
        } catch (IOException x) {
            throw new Exception(x.getMessage());
        }
    }
}
