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

package catalog;

import static jaxp.library.JAXPTestUtilities.clearSystemProperty;
import static jaxp.library.JAXPTestUtilities.setSystemProperty;

import java.io.File;
import java.io.StringReader;
import javax.xml.stream.XMLResolver;
import javax.xml.transform.Source;
import javax.xml.transform.URIResolver;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stax.StAXSource;
import javax.xml.transform.stream.StreamSource;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.InputSource;

/**
 * @test
 * @bug 8158084 8162438 8162442 8166220
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow catalog.CatalogSupport4
 * @run testng/othervm catalog.CatalogSupport4
 * @summary verifies the overriding over of the USE_CATALOG feature. Extending
 * CatalogSupport tests, the USE_CATALOG is turned off system-wide, however,
 * a JAXP processor may decide to use Catalog by enabling it through the factory
 * or a processor created by the factory.
 */

/**
 *
 * Test notes:
 * Same set of data as in CatalogSupport without the ones with resolvers.
 * The set-use-catalog is set to true.
 *
 * @author huizhe.wang@oracle.com
 */
@Listeners({jaxp.library.FilePolicy.class, jaxp.library.NetAccessPolicy.class})
public class CatalogSupport4 extends CatalogSupportBase {
    /*
     * Initializing fields
     */
    @BeforeClass
    public void setUpClass() throws Exception {
        setUp();
        //turn off USE_CATALOG system-wide
        setSystemProperty(SP_USE_CATALOG, "false");
    }

    @AfterClass
    public void tearDownClass() throws Exception {
        clearSystemProperty(SP_USE_CATALOG);
    }

    /*
       Verifies the Catalog support on SAXParser.
    */
    @Test(dataProvider = "data_SAXA")
    public void testSAXA(boolean setUseCatalog, boolean useCatalog, String catalog,
            String xml, MyHandler handler, String expected) throws Exception {
        testSAX(setUseCatalog, useCatalog, catalog, xml, handler, expected);
    }

    /*
       Verifies the Catalog support on XMLReader.
    */
    @Test(dataProvider = "data_SAXA")
    public void testXMLReaderA(boolean setUseCatalog, boolean useCatalog, String catalog,
            String xml, MyHandler handler, String expected) throws Exception {
        testXMLReader(setUseCatalog, useCatalog, catalog, xml, handler, expected);
    }

    /*
       Verifies the Catalog support on XInclude.
    */
    @Test(dataProvider = "data_XIA")
    public void testXIncludeA(boolean setUseCatalog, boolean useCatalog, String catalog,
            String xml, MyHandler handler, String expected) throws Exception {
        testXInclude(setUseCatalog, useCatalog, catalog, xml, handler, expected);
    }

    /*
       Verifies the Catalog support on DOM parser.
    */
    @Test(dataProvider = "data_DOMA")
    public void testDOMA(boolean setUseCatalog, boolean useCatalog, String catalog,
            String xml, MyHandler handler, String expected) throws Exception {
        testDOM(setUseCatalog, useCatalog, catalog, xml, handler, expected);
    }

    /*
       Verifies the Catalog support on XMLStreamReader.
    */
    @Test(dataProvider = "data_StAXA")
    public void testStAXA(boolean setUseCatalog, boolean useCatalog, String catalog,
            String xml, XMLResolver resolver, String expected) throws Exception {
        testStAX(setUseCatalog, useCatalog, catalog, xml, resolver, expected);
    }

    /*
       Verifies the Catalog support on resolving DTD, xsd import and include in
    Schema files.
    */
    @Test(dataProvider = "data_SchemaA")
    public void testValidationA(boolean setUseCatalog, boolean useCatalog,
            String catalog, String xsd, LSResourceResolver resolver)
            throws Exception {

        testValidation(setUseCatalog, useCatalog, catalog, xsd, resolver) ;
    }

    /*
       @bug 8158084 8162438 these tests also verifies the fix for 8162438
       Verifies the Catalog support on the Schema Validator.
    */
    @Test(dataProvider = "data_ValidatorA")
    public void testValidatorA(boolean setUseCatalog1, boolean setUseCatalog2, boolean useCatalog,
            Source source, LSResourceResolver resolver1, LSResourceResolver resolver2,
            String catalog1, String catalog2)
            throws Exception {
        testValidator(setUseCatalog1, setUseCatalog2, useCatalog, source,
                resolver1, resolver2, catalog1, catalog2);
    }

    /*
       Verifies the Catalog support on resolving DTD, xsl import and include in
    XSL files.
    */
    @Test(dataProvider = "data_XSLA")
    public void testXSLImportA(boolean setUseCatalog, boolean useCatalog, String catalog,
            SAXSource xsl, StreamSource xml, URIResolver resolver, String expected)
            throws Exception {

        testXSLImport(setUseCatalog, useCatalog, catalog, xsl, xml, resolver, expected);
    }

    /*
       @bug 8158084 8162442
       Verifies the Catalog support on resolving DTD, xsl import and include in
    XSL files.
    */
    @Test(dataProvider = "data_XSLA")
    public void testXSLImportWTemplatesA(boolean setUseCatalog, boolean useCatalog,
            String catalog, SAXSource xsl, StreamSource xml, URIResolver resolver, String expected)
            throws Exception {
        testXSLImportWTemplates(setUseCatalog, useCatalog, catalog, xsl, xml, resolver, expected);
    }

    /*
       DataProvider: for testing the SAX parser
       Data: set use_catalog, use_catalog, catalog file, xml file, handler, expected result string
     */
    @DataProvider(name = "data_SAXA")
    public Object[][] getDataSAX() {
        return new Object[][]{
            {true, true, xml_catalog, xml_system, new MyHandler(elementInSystem), expectedWCatalog},
        };
    }

    /*
       DataProvider: for testing XInclude
       Data: set use_catalog, use_catalog, catalog file, xml file, handler, expected result string
     */
    @DataProvider(name = "data_XIA")
    public Object[][] getDataXI() {
        return new Object[][]{
            {true, true, xml_catalog, xml_xInclude, new MyHandler(elementInXISimple), contentInUIutf8Catalog},
        };
    }

    /*
       DataProvider: for testing DOM parser
       Data: set use_catalog, use_catalog, catalog file, xml file, handler, expected result string
     */
    @DataProvider(name = "data_DOMA")
    public Object[][] getDataDOM() {
        return new Object[][]{
            {true, true, xml_catalog, xml_system, new MyHandler(elementInSystem), expectedWCatalog},
        };
    }

    /*
       DataProvider: for testing the StAX parser
       Data: set use_catalog, use_catalog, catalog file, xml file, handler, expected result string
     */
    @DataProvider(name = "data_StAXA")
    public Object[][] getDataStAX() {

        return new Object[][]{
            {true, true, xml_catalog, xml_system, null, expectedWCatalog},
        };
    }

    MyEntityHandler getMyEntityHandler(String elementName, String[] systemIds, InputSource... returnValues) {
       return new MyEntityHandler(systemIds, returnValues, elementName);
    }

    /*
       DataProvider: for testing Schema validation
       Data: set use_catalog, use_catalog, catalog file, xsd file, a LSResourceResolver
     */
    @DataProvider(name = "data_SchemaA")
    public Object[][] getDataSchema() {
        return new Object[][]{
            // for resolving DTD in xsd
            {true, true, xml_catalog, xsd_xmlSchema, null},
            // for resolving xsd import
            {true, true, xml_catalog, xsd_xmlSchema_import, null},
            // for resolving xsd include
            {true, true, xml_catalog, xsd_include_company, null},
        };
    }

    /*
       DataProvider: for testing Schema Validator
       Data: source, resolver1, resolver2, catalog1, a catalog2
     */
    @DataProvider(name = "data_ValidatorA")
    public Object[][] getDataValidator() {
        DOMSource ds = getDOMSource(xml_val_test, xml_val_test_id, true, true, xml_catalog);

        SAXSource ss = new SAXSource(new InputSource(xml_val_test));
        ss.setSystemId(xml_val_test_id);

        StAXSource stax = getStaxSource(xml_val_test, xml_val_test_id, true, true, xml_catalog);
        StAXSource stax1 = getStaxSource(xml_val_test, xml_val_test_id, true, true, xml_catalog);

        StreamSource source = new StreamSource(new File(xml_val_test));

        return new Object[][]{
            // use catalog
            {true, false, true, ds, null, null, xml_catalog, null},
            {false, true, true, ds, null, null, null, xml_catalog},
            {true, false, true, ss, null, null, xml_catalog, null},
            {false, true, true, ss, null, null, null, xml_catalog},
            {true, false, true, stax, null, null, xml_catalog, xml_catalog},
            {false, true, true, stax1, null, null, xml_catalog, xml_catalog},
            {true, false, true, source, null, null, xml_catalog, null},
            {false, true, true, source, null, null, null, xml_catalog},
        };
    }

    /*
       DataProvider: for testing XSL import and include
       Data: set use_catalog, use_catalog, catalog file, xsl file, xml file, a URIResolver, expected result
     */
    @DataProvider(name = "data_XSLA")
    public Object[][] getDataXSL() {
        // XSLInclude.xsl has one import XSLImport_html.xsl and two includes,
        // XSLInclude_header.xsl and XSLInclude_footer.xsl;
        SAXSource xslSourceDTD = new SAXSource(new InputSource(new StringReader(xsl_includeDTD)));
        StreamSource xmlSourceDTD = new StreamSource(new StringReader(xml_xslDTD));

        SAXSource xslDocSource = new SAXSource(new InputSource(new File(xsl_doc).toURI().toASCIIString()));
        StreamSource xmlDocSource = new StreamSource(new File(xml_doc));
        return new Object[][]{
            // for resolving DTD, import and include in xsl
            {true, true, xml_catalog, xslSourceDTD, xmlSourceDTD, null, ""},
            // for resolving reference by the document function
            {true, true, xml_catalog, xslDocSource, xmlDocSource, null, "Resolved by a catalog"},
        };
    }
}
