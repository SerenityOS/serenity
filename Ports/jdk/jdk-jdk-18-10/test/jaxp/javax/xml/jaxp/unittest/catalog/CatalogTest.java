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
package catalog;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
import java.nio.file.Paths;
import javax.xml.XMLConstants;
import javax.xml.catalog.Catalog;
import javax.xml.catalog.CatalogException;
import javax.xml.catalog.CatalogFeatures;
import javax.xml.catalog.CatalogFeatures.Feature;
import javax.xml.catalog.CatalogManager;
import javax.xml.catalog.CatalogResolver;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import static jaxp.library.JAXPTestUtilities.clearSystemProperty;
import static jaxp.library.JAXPTestUtilities.setSystemProperty;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.xml.sax.Attributes;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.DefaultHandler2;

/*
 * @test
 * @bug 8081248 8144966 8146606 8146237 8150969 8151162 8152527 8154220 8163232
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow catalog.CatalogTest
 * @run testng/othervm catalog.CatalogTest
 * @summary Tests basic Catalog functions.
 */
@Listeners({jaxp.library.FilePolicy.class})
public class CatalogTest extends CatalogSupportBase {
    static final String KEY_FILES = "javax.xml.catalog.files";


    /*
     * Initializing fields
     */
    @BeforeClass
    public void setUpClass() throws Exception {
        super.setUp();
    }

    /*
     * @bug 8163232
     * Verifies that the CatalogResolver supports the following XML Resolvers:
          javax.xml.stream.XMLResolver
          javax.xml.transform.URIResolver
          org.w3c.dom.ls.LSResourceResolver
          org.xml.sax.EntityResolver
     *
     * Plus, system and uri entries can equally be used.
     */

    /*
     * Verifies the support for org.xml.sax.EntityResolver.
     * Expected: the parser returns the expected string.
    */
    @Test(dataProvider = "supportXMLResolver")
    public void supportEntityResolver(URI catalogFile, String xml, String expected) throws Exception {
        String xmlSource = getClass().getResource(xml).getFile();

        CatalogResolver cr = CatalogManager.catalogResolver(CatalogFeatures.defaults(), catalogFile);
        MyCatalogHandler handler = new MyCatalogHandler(cr, elementInSystem);
        SAXParser parser = getSAXParser(false, true, null);
        parser.parse(xmlSource, handler);

        Assert.assertEquals(handler.getResult().trim(), expected);
    }

    /*
     * Verifies the support for javax.xml.stream.XMLResolver.
     * Expected: the parser returns the expected string.
    */
    @Test(dataProvider = "supportXMLResolver")
    public void supportXMLResolver(URI catalogFile, String xml, String expected) throws Exception {
        String xmlSource = getClass().getResource(xml).getFile();

        CatalogResolver cr = CatalogManager.catalogResolver(CatalogFeatures.defaults(), catalogFile);

        XMLInputFactory xifactory = XMLInputFactory.newInstance();
        xifactory.setProperty(XMLInputFactory.IS_COALESCING, true);
        xifactory.setProperty(XMLInputFactory.RESOLVER, cr);
        File file = new File(xmlSource);
        String systemId = file.toURI().toASCIIString();
        InputStream entityxml = new FileInputStream(file);
        XMLStreamReader streamReader = xifactory.createXMLStreamReader(systemId, entityxml);
        String result = null;
        while (streamReader.hasNext()) {
            int eventType = streamReader.next();
            if (eventType == XMLStreamConstants.START_ELEMENT) {
                eventType = streamReader.next();
                if (eventType == XMLStreamConstants.CHARACTERS) {
                    result = streamReader.getText();
                }
            }
        }
        System.out.println(": expected [" + expected + "] <> actual [" + result.trim() + "]");

        Assert.assertEquals(result.trim(), expected);
    }

    /*
     * Verifies the support for org.w3c.dom.ls.LSResourceResolver by ShemaFactory.
     * Success: parsing goes through with no error
     * Fail: throws Exception if references are not resolved (by the CatalogResolver)
    */
    @Test(dataProvider = "supportLSResourceResolver")
    public void supportLSResourceResolver(URI catalogFile, Source schemaSource) throws SAXException {

        CatalogResolver cr = CatalogManager.catalogResolver(CatalogFeatures.defaults(), catalogFile);

        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        factory.setResourceResolver(cr);
        Schema schema = factory.newSchema(schemaSource);

    }

    /*
     * Verifies the support for org.w3c.dom.ls.LSResourceResolver by Validator.
     * Success: parsing goes through with no error
     * Fail: throws Exception if references are not resolved (by the CatalogResolver)
    */
    @Test(dataProvider = "supportLSResourceResolver1")
    public void supportLSResourceResolver1(URI catalogFile, Source source) throws Exception {

        CatalogResolver cr = CatalogManager.catalogResolver(CatalogFeatures.defaults(), catalogFile);

        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Validator validator = factory.newSchema().newValidator();
        validator.setResourceResolver(cr);
        validator.validate(source);
    }

    /*
     * Verifies the support for javax.xml.transform.URIResolver.
     * Success: parsing goes through with no error
     * Fail: throws Exception if references are not resolved (by the CatalogResolver)
    */
    @Test(dataProvider = "supportURIResolver")
    public void supportURIResolver(URI catalogFile, Source xsl, Source xml, String expected) throws Exception {

        CatalogResolver cr = CatalogManager.catalogResolver(CatalogFeatures.defaults(), catalogFile);

            TransformerFactory factory = TransformerFactory.newInstance();
            factory.setURIResolver(cr);
            Transformer transformer = factory.newTransformer(xsl);
            StringWriter out = new StringWriter();
            transformer.transform(xml, new StreamResult(out));
            if (expected != null) {
                Assert.assertTrue(out.toString().contains(expected), "supportURIResolver");
            }
    }

    /*
       DataProvider: used to verify the support of XML Resolvers.
        Data columns:
        catalog filepath, xml source file, expected result
     */
    @DataProvider(name = "supportXMLResolver")
    public Object[][] supportXMLResolver() throws Exception {
        URI catalogFile = getClass().getResource("catalog.xml").toURI();
        URI catalogFileUri = getClass().getResource("catalog_uri.xml").toURI();

        return new Object[][]{
            {catalogFile, "system.xml", "Test system entry"},
            {catalogFile, "rewritesystem.xml", "Test rewritesystem entry"},
            {catalogFile, "rewritesystem1.xml", "Test rewritesystem entry"},
            {catalogFile, "systemsuffix.xml", "Test systemsuffix entry"},
            {catalogFile, "delegatesystem.xml", "Test delegatesystem entry"},
            {catalogFile, "public.xml", "Test public entry"},
            {catalogFile, "delegatepublic.xml", "Test delegatepublic entry"},
            // using uri entries
            {catalogFileUri, "system.xml", "Test system entry"},
            {catalogFileUri, "rewritesystem.xml", "Test rewritesystem entry"},
            {catalogFileUri, "rewritesystem1.xml", "Test rewritesystem entry"},
            {catalogFileUri, "systemsuffix.xml", "Test systemsuffix entry"},
            {catalogFileUri, "delegateuri.xml", "Test delegateuri entry"},
            {catalogFileUri, "public.xml", "Test public entry"},
         };
    }

    /*
       DataProvider: used to verify the support of LSResourceResolver by SchemaFactory.
        Data columns:
        catalog filepath, schema source file
     */
    @DataProvider(name = "supportLSResourceResolver")
    public Object[][] supportLSResourceResolver() throws Exception {
        URI catalogFile = getClass().getResource("CatalogSupport.xml").toURI();
        URI catalogFileUri = getClass().getResource("CatalogSupport_uri.xml").toURI();

        /*
         * XMLSchema.xsd has a reference to XMLSchema.dtd which in turn refers to
         * datatypes.dtd
        */
        return new Object[][]{
            {catalogFile, new StreamSource(new StringReader(xsd_xmlSchema))},
            {catalogFile, new StreamSource(new StringReader(xsd_xmlSchema_import))},
            {catalogFile, new StreamSource(new StringReader(xsd_include_company))},
            {catalogFileUri, new StreamSource(new StringReader(xsd_xmlSchema))},
            {catalogFileUri, new StreamSource(new StringReader(xsd_xmlSchema_import))},
            {catalogFileUri, new StreamSource(new StringReader(xsd_include_company))},
         };
    }

    /*
       DataProvider: used to verify the support of LSResourceResolver by Validator.
        Data columns:
        catalog filepath, source file
     */
    @DataProvider(name = "supportLSResourceResolver1")
    public Object[][] supportLSResourceResolver1() throws Exception {
        URI catalogFile = getClass().getResource("CatalogSupport.xml").toURI();
        URI catalogFileUri = getClass().getResource("CatalogSupport_uri.xml").toURI();

        /*
         * val_test.xml has a reference to system.dtd and val_test.xsd
        */
        SAXSource ss = new SAXSource(new InputSource(xml_val_test));
        ss.setSystemId(xml_val_test_id);

        return new Object[][]{
            {catalogFile, ss},
            {catalogFileUri, ss},
         };
    }


    /*
       DataProvider: used to verify the support of LSResourceResolver by Validator.
        Data columns:
        catalog filepath, xsl source, xml source file
     */
    @DataProvider(name = "supportURIResolver")
    public Object[][] supportURIResolver() throws Exception {
        URI catalogFile = getClass().getResource("CatalogSupport.xml").toURI();
        URI catalogFileUri = getClass().getResource("CatalogSupport_uri.xml").toURI();
        SAXSource xslSource = new SAXSource(new InputSource(new File(xsl_doc).toURI().toASCIIString()));

        /*
         * val_test.xml has a reference to system.dtd and val_test.xsd
        */
        SAXSource ss = new SAXSource(new InputSource(xml_val_test));
        ss.setSystemId(xml_val_test_id);

        return new Object[][]{
            {catalogFile, new SAXSource(new InputSource(new File(xsl_doc).toURI().toASCIIString())),
                new StreamSource(new File(xml_doc)), "Resolved by a catalog"},
            {catalogFileUri, new SAXSource(new InputSource(new StringReader(xsl_include))),
                new StreamSource(new StringReader(xml_xsl)), null},
         };
    }

    /*
     * @bug 8150187
     * NPE is expected if the systemId is null. The specification for systemId
     * is as follows:
     * A system identifier is required on all external entities. XML
     * requires a system identifier on all external entities, so this value is
     * always specified.
     */
    @Test(expectedExceptions = NullPointerException.class)
    public void sysIdCantBeNull() {
        CatalogResolver catalogResolver = CatalogManager.catalogResolver(CatalogFeatures.defaults());
        InputSource is = catalogResolver.resolveEntity("-//FOO//DTD XML Dummy V0.0//EN", null);
    }

    /*
     * @bug 8156845
     * Verifies that an URI reference with a urn:publicid is correctly resolved
     * with an uri entry with a publicId.
     *
     * @param expectedFile is not used in this test, it's kept since we're
     * copying the JCK test and its dataProvider. This test may be reused for
     * other cases in that test.
     */
    @Test(dataProvider = "resolveUri")
    public void testMatch1(String cFile, String href, String expectedFile,
            String expectedUri, String msg) throws Exception {
        URI catalogFile = getClass().getResource(cFile).toURI();
        CatalogResolver cur = CatalogManager.catalogResolver(CatalogFeatures.defaults(), catalogFile);
        Source source = cur.resolve(href, null);
        Assert.assertNotNull(source, "Source returned is null");
        Assert.assertEquals(expectedUri, source.getSystemId(), msg);
    }

    /*
     * @bug 8154220
     * Verifies that the file input is validated properly. Valid input includes
     * multiple file paths separated by semicolon.
     */
    @Test(dataProvider = "hierarchyOfCatFilesData")
    public void hierarchyOfCatFiles2(String systemId, String expectedUri) {
        String file1 = getClass().getResource("first_cat.xml").toExternalForm();
        String file2 = getClass().getResource("second_cat.xml").toExternalForm();
        String files = file1 + ";" + file2;

        try {
            setSystemProperty(KEY_FILES, files);
            CatalogResolver catalogResolver = CatalogManager.catalogResolver(CatalogFeatures.defaults());
            String sysId = catalogResolver.resolveEntity(null, systemId).getSystemId();
            Assert.assertEquals(sysId, Paths.get(filepath + expectedUri).toUri().toString().replace("///", "/"),
                    "System ID match not right");
        } finally {
            clearSystemProperty(KEY_FILES);
        }

    }

    /*
     * @bug 8152527
     * This test is the same as the JDK test ResolveEntityTests:testMatch1.
     * Verifies that the CatalogResolver resolves a publicId and/or systemId as
     * expected.
     */
    @Test(dataProvider = "resolveEntity")
    public void testMatch1(String cfile, String prefer, String sysId, String pubId,
            String expectedUri, String expectedFile, String msg) throws Exception {
        URI catalogFile = getClass().getResource(cfile).toURI();
        CatalogFeatures features = CatalogFeatures.builder().with(CatalogFeatures.Feature.PREFER, prefer).build();
        CatalogResolver catalogResolver = CatalogManager.catalogResolver(features, catalogFile);
        InputSource is = catalogResolver.resolveEntity(pubId, sysId);
        Assert.assertNotNull(is, msg);
        String expected = (expectedUri == null) ? expectedFile : expectedUri;
        Assert.assertEquals(expected, is.getSystemId(), msg);
    }

    /*
     * @bug 8151162
     * Verifies that the Catalog matches specified publicId or systemId and returns
     * results as expected.
     */
    @Test(dataProvider = "matchWithPrefer")
    public void matchWithPrefer(String prefer, String cfile, String publicId,
            String systemId, String expected) throws Exception {
        URI catalogFile = getClass().getResource(cfile).toURI();
        Catalog c = CatalogManager.catalog(
                CatalogFeatures.builder().with(CatalogFeatures.Feature.PREFER, prefer).build(),
                catalogFile);
        String result;
        if (publicId != null && publicId.length() > 0) {
            result = c.matchPublic(publicId);
        } else {
            result = c.matchSystem(systemId);
        }
        Assert.assertEquals(expected, result);
    }

    /*
     * @bug 8151162
     * Verifies that the CatalogResolver resolves specified publicId or systemId
     * in accordance with the prefer setting.
     * prefer "system": resolves with a system entry.
     *                  Exception: use the public entry when the catalog contains
     *                  only public entry and only publicId is specified.
     * prefer "public": attempts to resolve with a system entry;
     *                  attempts to resolve with a public entry if no matching
     *                  system entry is found.
     */
    @Test(dataProvider = "resolveWithPrefer")
    public void resolveWithPrefer(String prefer, String cfile, String publicId,
            String systemId, String expected) throws Exception {
        URI catalogFile = getClass().getResource(cfile).toURI();
        CatalogFeatures f = CatalogFeatures.builder()
                .with(CatalogFeatures.Feature.PREFER, prefer)
                .with(CatalogFeatures.Feature.RESOLVE, "ignore")
                .build();
        CatalogResolver catalogResolver = CatalogManager.catalogResolver(f, catalogFile);
        String result = catalogResolver.resolveEntity(publicId, systemId).getSystemId();
        Assert.assertEquals(expected, result);
    }

    /**
     * @bug 8150969
     * Verifies that the defer attribute set in the catalog file takes precedence
     * over other settings, in which case, whether next and delegate Catalogs will
     * be loaded is determined by the defer attribute.
     */
    @Test(dataProvider = "invalidAltCatalogs", expectedExceptions = CatalogException.class)
    public void testDeferAltCatalogs(String file) throws Exception {
        URI catalogFile = getClass().getResource(file).toURI();
        CatalogFeatures features = CatalogFeatures.builder().
                with(CatalogFeatures.Feature.DEFER, "true")
                .build();
        /*
          Since the defer attribute is set to false in the specified catalog file,
          the parent catalog will try to load the alt catalog, which will fail
          since it points to an invalid catalog.
        */
        Catalog catalog = CatalogManager.catalog(features, catalogFile);
    }


    /**
     * @bug 8146237
     * PREFER from Features API taking precedence over catalog file
     */
    @Test
    public void testJDK8146237() throws Exception {
        URI catalogFile = getClass().getResource("JDK8146237_catalog.xml").toURI();

        try {
            CatalogFeatures features = CatalogFeatures.builder()
                    .with(CatalogFeatures.Feature.PREFER, "system")
                    .build();
            Catalog catalog = CatalogManager.catalog(features, catalogFile);
            CatalogResolver catalogResolver = CatalogManager.catalogResolver(catalog);
            String actualSystemId = catalogResolver.resolveEntity(
                    "-//FOO//DTD XML Dummy V0.0//EN",
                    "http://www.oracle.com/alt1sys.dtd")
                    .getSystemId();
            Assert.assertTrue(actualSystemId.contains("dummy.dtd"),
                    "Resulting id should contain dummy.dtd, indicating a match by publicId");

        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    /*
       @bug 8146606
       Verifies that the resulting systemId does not contain duplicate slashes
    */
    @Test
    public void testRewriteSystem() throws Exception {
        URI catalog = getClass().getResource("rewriteCatalog.xml").toURI();

        try {
            CatalogResolver resolver = CatalogManager.catalogResolver(CatalogFeatures.defaults(), catalog);
            String actualSystemId = resolver.resolveEntity(null, "http://remote.com/dtd/book.dtd").getSystemId();
            Assert.assertTrue(!actualSystemId.contains("//"), "result contains duplicate slashes");
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }

    }

    /*
       @bug 8146606
       Verifies that the resulting systemId does not contain duplicate slashes
    */
    @Test
    public void testRewriteUri() throws Exception {
        URI catalog = getClass().getResource("rewriteCatalog.xml").toURI();

        try {

            CatalogResolver resolver = CatalogManager.catalogResolver(CatalogFeatures.defaults(), catalog);
            String actualSystemId = resolver.resolve("http://remote.com/import/import.xsl", null).getSystemId();
            Assert.assertTrue(!actualSystemId.contains("//"), "result contains duplicate slashes");
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    /*
       @bug 8144966
       Verifies that passing null as CatalogFeatures will result in a NPE.
    */
    @Test(expectedExceptions = NullPointerException.class)
    public void testFeatureNull() {
        CatalogResolver resolver = CatalogManager.catalogResolver(null, null);

    }

    /*
       @bug 8144966
       Verifies that passing null as the URI will result in a NPE.
    */
    @Test(expectedExceptions = NullPointerException.class)
    public void testPathNull() {
        URI uri = null;
        CatalogResolver resolver = CatalogManager.catalogResolver(CatalogFeatures.defaults(), uri);
    }

    /*
       Tests basic catalog feature by using a CatalogResolver instance to
    resolve a DTD reference to a locally specified DTD file. If the resolution
    is successful, the Handler shall return the value of the entity reference
    that matches the expected value.
     */
    @Test(dataProvider = "catalog")
    public void testCatalogResolver(String test, String expected, String catalogFile,
            String xml, SAXParser saxParser) throws Exception {
        URI catalog = null;
        if (catalogFile != null) {
            catalog = getClass().getResource(catalogFile).toURI();
        }
        String url = getClass().getResource(xml).getFile();
        try {
            CatalogResolver cr = CatalogManager.catalogResolver(CatalogFeatures.defaults(), catalog);
            XMLReader reader = saxParser.getXMLReader();
            reader.setEntityResolver(cr);
            MyHandler handler = new MyHandler(saxParser);
            reader.setContentHandler(handler);
            reader.parse(url);
            System.out.println(test + ": expected [" + expected + "] <> actual [" + handler.getResult() + "]");
            Assert.assertEquals(handler.getResult(), expected);
        } catch (SAXException | IOException e) {
            Assert.fail(e.getMessage());
        }
    }

    /*
       Verifies that when there's no match, in this case only an invalid
    catalog is provided, the resolver will throw an exception by default.
    */
    @Test
    public void testInvalidCatalog() throws Exception {
        String expectedMsgId = "JAXP09040001";
        URI catalog = getClass().getResource("catalog_invalid.xml").toURI();

        try {
            CatalogResolver resolver = CatalogManager.catalogResolver(
                    CatalogFeatures.defaults(), catalog);
            String actualSystemId = resolver.resolveEntity(
                    null,
                    "http://remote/xml/dtd/sys/alice/docAlice.dtd")
                    .getSystemId();
        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg != null) {
                Assert.assertTrue(msg.contains(expectedMsgId),
                        "Message shall contain the corrent message ID " + expectedMsgId);
            }
        }
    }

    /*
       Verifies that if resolve is "ignore", an empty InputSource will be returned
    when there's no match. The systemId is then null.
    */
    @Test
    public void testIgnoreInvalidCatalog() {
        String catalog = getClass().getResource("catalog_invalid.xml").toExternalForm();
        CatalogFeatures f = CatalogFeatures.builder()
                .with(Feature.FILES, catalog)
                .with(Feature.PREFER, "public")
                .with(Feature.DEFER, "true")
                .with(Feature.RESOLVE, "ignore")
                .build();

        String test = "testInvalidCatalog";
        try {
            CatalogResolver resolver = CatalogManager.catalogResolver(f);
            String actualSystemId = resolver.resolveEntity(
                    null,
                    "http://remote/xml/dtd/sys/alice/docAlice.dtd")
                    .getSystemId();
            System.out.println("testIgnoreInvalidCatalog: expected [null]");
            System.out.println("testIgnoreInvalidCatalog: expected [null]");
            System.out.println("actual [" + actualSystemId + "]");
            Assert.assertEquals(actualSystemId, null);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }


    /*
        DataProvider: used to verify CatalogResolver's resolve function.
        Data columns:
        catalog, uri or publicId, expectedFile, expectedUri, msg

        This DataProvider is copied from JCK ResolveTests' dataMatch1
     */
    @DataProvider(name = "resolveUri")
    public Object[][] getDataForUriResolver() {
        return new Object[][]{
            {"uri.xml",
                "urn:publicid:-:Acme,+Inc.:DTD+Book+Version+1.0",
                null,
                "http://local/base/dtd/book.dtd",
                "Uri in publicId namespace is incorrectly unwrapped"},
        };
    }

    /*
        DataProvider: used to verify hierarchical catalogs. Refer to JCK test
    hierarchyOfCatFiles2.
     */
    @DataProvider(name = "hierarchyOfCatFilesData")
    public Object[][] getHierarchyOfCatFilesData() {
        return new Object[][]{
            {"http://www.oracle.com/sequence.dtd", "first.dtd"},
            {"http://www.oracle.com/sequence_next.dtd", "next.dtd"},
            {"http://www.oracle.com/sequence_second.dtd", "second.dtd"}
        };
    }

    /*
        DataProvider: used to verify CatalogResolver's resolveEntity function.
        Data columns:
        catalog, prefer, systemId, publicId, expectedUri, expectedFile, msg
     */
    @DataProvider(name = "resolveEntity")
    public Object[][] getDataForMatchingBothIds() {
        String expected = "http://www.groupxmlbase.com/dtds/rewrite.dtd";
        return new Object[][]{
            {"rewriteSystem_id.xml",
                "system",
                "http://www.sys00test.com/rewrite.dtd",
                "PUB-404",
                expected,
                expected,
                "Relative rewriteSystem with xml:base at group level failed"},
        };
    }

    static String id = "http://openjdk_java_net/xml/catalog/dtd/system.dtd";
    /*
       DataProvider: used to verify how prefer settings affect the result of the
        Catalog's matching operation.
        Data columns:
        prefer, catalog, publicId, systemId, expected result
     */
    @DataProvider(name = "matchWithPrefer")
    public Object[][] getDataForMatch() {
        return new Object[][]{
            {"public", "pubOnly.xml", id, "", "http://local/base/dtd/public.dtd"},
            {"public", "sysOnly.xml", id, "", null},
            {"public", "sysAndPub.xml", id, "", "http://local/base/dtd/public.dtd"},
            {"system", "pubOnly.xml", id, "", "http://local/base/dtd/public.dtd"},
            {"system", "sysOnly.xml", id, "", null},
            {"system", "sysAndPub.xml", id, "", "http://local/base/dtd/public.dtd"},
            {"public", "pubOnly.xml", "", id, null},
            {"public", "sysOnly.xml", "", id, "http://local/base/dtd/system.dtd"},
            {"public", "sysAndPub.xml", "", id, "http://local/base/dtd/system.dtd"},
            {"system", "pubOnly.xml", "", id, null},
            {"system", "sysOnly.xml", "", id, "http://local/base/dtd/system.dtd"},
            {"system", "sysAndPub.xml", "", id, "http://local/base/dtd/system.dtd"},
        };
    }

    /*
       DataProvider: used to verify how prefer settings affect the result of the
        CatalogResolver's resolution operation.
        Data columns:
        prefer, catalog, publicId, systemId, expected result
     */
    @DataProvider(name = "resolveWithPrefer")
    public Object[][] getDataForResolve() {
        return new Object[][]{
            {"system", "pubOnly.xml", id, "", "http://local/base/dtd/public.dtd"},
            {"system", "pubOnly.xml", "", id, null},
            {"system", "pubOnly.xml", id, id, null},
            {"public", "pubOnly.xml", id, "", "http://local/base/dtd/public.dtd"},
            {"public", "pubOnly.xml", "", id, null},
            {"public", "pubOnly.xml", id, id, "http://local/base/dtd/public.dtd"},
            {"system", "sysOnly.xml", id, "", null},
            {"system", "sysOnly.xml", "", id, "http://local/base/dtd/system.dtd"},
            {"system", "sysOnly.xml", id, id, "http://local/base/dtd/system.dtd"},
            {"public", "sysOnly.xml", id, "", null},
            {"public", "sysOnly.xml", "", id, "http://local/base/dtd/system.dtd"},
            {"public", "sysOnly.xml", id, id, "http://local/base/dtd/system.dtd"},
            {"system", "sysAndPub.xml", id, "", "http://local/base/dtd/public.dtd"},
            {"system", "sysAndPub.xml", "", id, "http://local/base/dtd/system.dtd"},
            {"system", "sysAndPub.xml", id, id, "http://local/base/dtd/system.dtd"},
            {"public", "sysAndPub.xml", id, "", "http://local/base/dtd/public.dtd"},
            {"public", "sysAndPub.xml", "", id, "http://local/base/dtd/system.dtd"},
            {"public", "sysAndPub.xml", id, id, "http://local/base/dtd/system.dtd"},
        };
    }
    /*
       DataProvider: catalogs that contain invalid next or delegate catalogs.
                     The defer attribute is set to false.
     */
    @DataProvider(name = "invalidAltCatalogs")
    public Object[][] getCatalogs() {
        return new Object[][]{
            {"defer_false_2.xml"},
            {"defer_del_false.xml"}
        };
    }


    /*
       DataProvider: provides test name, expected string, the catalog, and XML
       document.
     */
    @DataProvider(name = "catalog")
    public Object[][] getCatalog() {
        return new Object[][]{
            {"testSystem", "Test system entry", "catalog.xml", "system.xml", getParser()},
            {"testRewriteSystem", "Test rewritesystem entry", "catalog.xml", "rewritesystem.xml", getParser()},
            {"testRewriteSystem1", "Test rewritesystem entry", "catalog.xml", "rewritesystem1.xml", getParser()},
            {"testSystemSuffix", "Test systemsuffix entry", "catalog.xml", "systemsuffix.xml", getParser()},
            {"testDelegateSystem", "Test delegatesystem entry", "catalog.xml", "delegatesystem.xml", getParser()},
            {"testPublic", "Test public entry", "catalog.xml", "public.xml", getParser()},
            {"testDelegatePublic", "Test delegatepublic entry", "catalog.xml", "delegatepublic.xml", getParser()},
        };
    }

    SAXParser getParser() {
        SAXParser saxParser = null;
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            saxParser = factory.newSAXParser();
        } catch (ParserConfigurationException | SAXException e) {
        }

        return saxParser;
    }

    /**
     * SAX handler
     */
    public class MyHandler extends DefaultHandler2 implements ErrorHandler {

        StringBuilder textContent = new StringBuilder();
        SAXParser saxParser;

        MyHandler(SAXParser saxParser) {
            textContent.setLength(0);
            this.saxParser = saxParser;
        }

        String getResult() {
            return textContent.toString();
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes)
                throws SAXException {
            textContent.delete(0, textContent.length());
            try {
                System.out.println("Element: " + uri + ":" + localName + " " + qName);
            } catch (Exception e) {
                throw new SAXException(e);
            }

        }

        @Override
        public void characters(char ch[], int start, int length) throws SAXException {
            textContent.append(ch, start, length);
        }
    }
}
