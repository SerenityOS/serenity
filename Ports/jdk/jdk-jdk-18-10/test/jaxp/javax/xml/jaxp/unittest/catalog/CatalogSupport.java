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

import java.io.File;
import java.io.StringReader;
import javax.xml.stream.XMLResolver;
import javax.xml.transform.Source;
import javax.xml.transform.URIResolver;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stax.StAXSource;
import javax.xml.transform.stream.StreamSource;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.InputSource;

/**
 * @test
 * @bug 8158084 8162438 8162442 8166220 8166398
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow catalog.CatalogSupport
 * @run testng/othervm catalog.CatalogSupport
 * @summary verifies the use of Catalog in SAX/DOM/StAX/Validation/Transform.
 * The two main scenarios for all processors are:
 * A custom resolver is used whether or not there's a Catalog;
 * A Catalog is used when there's no custom resolver, and the USE_CATALOG
 * is true (which is the case by default).
*/

/**
 * Support Catalog:
 * With this patch, the Catalog features are supported by all of the JAXP processors.
 * The support is enabled by default. Using Catalog is as simple as setting a
 * path to a catalog, through the API, or System property, or jaxp.properties.
 *
 * Test notes:
 * For all DataProviders, the 1st and 2nd columns determine whether to set USE_CATALOG
 * through the API and to use Catalog. When a custom resolver is specified, these
 * settings should not affect the operation, thus the tests are repeated for both
 * false and true.
 *
 * @author huizhe.wang@oracle.com
 */
@Listeners({jaxp.library.FilePolicy.class, jaxp.library.NetAccessPolicy.class})
public class CatalogSupport extends CatalogSupportBase {
    /*
     * Initializing fields
     */
    @BeforeClass
    public void setUpClass() throws Exception {
        setUp();
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
        String[] systemIds = {"system.dtd"};
        return new Object[][]{
            {false, true, xml_catalog, xml_system, new MyHandler(elementInSystem), expectedWCatalog},
            {false, true, xml_catalog, xml_system, getMyEntityHandler(elementInSystem, systemIds,
                    new InputSource(new StringReader(dtd_systemResolved))), expectedWResolver},
            {true, true, xml_catalog, xml_system, getMyEntityHandler(elementInSystem, systemIds,
                    new InputSource(new StringReader(dtd_systemResolved))), expectedWResolver}
        };
    }

    /*
       DataProvider: for testing XInclude
       Data: set use_catalog, use_catalog, catalog file, xml file, handler, expected result string
     */
    @DataProvider(name = "data_XIA")
    public Object[][] getDataXI() {
        String[] systemIds = {"XI_simple.xml"};
        InputSource[] returnValues = {new InputSource(xml_xIncludeSimple)};
        MyEntityHandler entityHandler = new MyEntityHandler(systemIds, returnValues, elementInXISimple);
        return new Object[][]{
            {false, true, xml_catalog, xml_xInclude, new MyHandler(elementInXISimple), contentInUIutf8Catalog},
            {false, true, xml_catalog, xml_xInclude, entityHandler, contentInXIutf8},
            {true, true, xml_catalog, xml_xInclude, entityHandler, contentInXIutf8}
        };
    }

    /*
       DataProvider: for testing DOM parser
       Data: set use_catalog, use_catalog, catalog file, xml file, handler, expected result string
     */
    @DataProvider(name = "data_DOMA")
    public Object[][] getDataDOM() {
        String[] systemIds = {"system.dtd"};
        InputSource[] returnValues = {new InputSource(new StringReader(dtd_systemResolved))};
        MyEntityHandler entityHandler = new MyEntityHandler(systemIds, returnValues, elementInSystem);
        return new Object[][]{
            {false, true, xml_catalog, xml_system, new MyHandler(elementInSystem), expectedWCatalog},
            {false, true, xml_catalog, xml_system, getMyEntityHandler(elementInSystem, systemIds,
                    new InputSource(new StringReader(dtd_systemResolved))), expectedWResolver},
            {true, true, xml_catalog, xml_system, getMyEntityHandler(elementInSystem, systemIds,
                    new InputSource(new StringReader(dtd_systemResolved))), expectedWResolver}
        };
    }

    /*
       DataProvider: for testing the StAX parser
       Data: set use_catalog, use_catalog, catalog file, xml file, handler, expected result string
     */
    @DataProvider(name = "data_StAXA")
    public Object[][] getDataStAX() {

        return new Object[][]{
            {false, true, xml_catalog, xml_system, null, expectedWCatalog},
            {false, true, xml_catalog, xml_system, new MyStaxEntityResolver(), expectedWResolver},
            {true, true, xml_catalog, xml_system, new MyStaxEntityResolver(), expectedWResolver}
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
        String[] systemIds = {"pathto/XMLSchema.dtd", "datatypes.dtd"};
        XmlInput[] returnValues = {new XmlInput(null, dtd_xmlSchema, null), new XmlInput(null, dtd_datatypes, null)};
        LSResourceResolver resolver = new SourceResolver(null, systemIds, returnValues);

        String[] systemIds1 = {"xml.xsd"};
        XmlInput[] returnValues1 = {new XmlInput(null, xsd_xml, null)};
        LSResourceResolver resolverImport = new SourceResolver(null, systemIds1, returnValues1);

        String[] systemIds2 = {"XSDInclude_person.xsd", "XSDInclude_product.xsd"};
        XmlInput[] returnValues2 = {new XmlInput(null, xsd_include_person, null),
                        new XmlInput(null, xsd_include_product, null)};
        LSResourceResolver resolverInclude = new SourceResolver(null, systemIds2, returnValues2);

        return new Object[][]{
            // for resolving DTD in xsd
            {false, true, xml_catalog, xsd_xmlSchema, null},
            {false, true, xml_bogus_catalog, xsd_xmlSchema, resolver},
            {true, true, xml_bogus_catalog, xsd_xmlSchema, resolver},
            // for resolving xsd import
            {false, true, xml_catalog, xsd_xmlSchema_import, null},
            {false, true, xml_bogus_catalog, xsd_xmlSchema_import, resolverImport},
            {true, true, xml_bogus_catalog, xsd_xmlSchema_import, resolverImport},
            // for resolving xsd include
            {false, true, xml_catalog, xsd_include_company, null},
            {false, true, xml_bogus_catalog, xsd_include_company, resolverInclude},
            {true, true, xml_bogus_catalog, xsd_include_company, resolverInclude}
        };
    }

    /*
       DataProvider: for testing Schema Validator
       Data: source, resolver1, resolver2, catalog1, a catalog2
     */
    @DataProvider(name = "data_ValidatorA")
    public Object[][] getDataValidator() {
        DOMSource ds = getDOMSource(xml_val_test, xml_val_test_id, false, true, xml_catalog);

        SAXSource ss = new SAXSource(new InputSource(xml_val_test));
        ss.setSystemId(xml_val_test_id);

        StAXSource stax = getStaxSource(xml_val_test, xml_val_test_id, false, true, xml_catalog);
        StAXSource stax1 = getStaxSource(xml_val_test, xml_val_test_id, false, true, xml_catalog);

        StreamSource source = new StreamSource(new File(xml_val_test));

        String[] systemIds = {"system.dtd", "val_test.xsd"};
        XmlInput[] returnValues = {new XmlInput(null, dtd_system, null), new XmlInput(null, xsd_val_test, null)};
        LSResourceResolver resolver = new SourceResolver(null, systemIds, returnValues);

        StAXSource stax2 = getStaxSource(xml_val_test, xml_val_test_id, false, true, xml_catalog);

        return new Object[][]{
            // use catalog
            {false, false, true, ds, null, null, xml_catalog, null},
            {false, false, true, ds, null, null, null, xml_catalog},
            {false, false, true, ss, null, null, xml_catalog, null},
            {false, false, true, ss, null, null, null, xml_catalog},
            {false, false, true, stax, null, null, xml_catalog, null},
            {false, false, true, stax1, null, null, null, xml_catalog},
            {false, false, true, source, null, null, xml_catalog, null},
            {false, false, true, source, null, null, null, xml_catalog},
            // use resolver
            {false, false, true, ds, resolver, resolver, xml_bogus_catalog, xml_bogus_catalog},
            {false, false, true, ss, resolver, resolver, xml_bogus_catalog, xml_bogus_catalog},
            {false, false, true, stax2, resolver, resolver, xml_bogus_catalog, xml_bogus_catalog},
            {false, false, true, source, resolver, resolver, xml_bogus_catalog, xml_bogus_catalog}
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
        String[] hrefs = {"XSLImport_html.xsl", "XSLInclude_header.xsl", "XSLInclude_footer.xsl"};
        Source[] returnValues = {new StreamSource(xsl_import_html),
                        new StreamSource(xsl_include_header),
                        new StreamSource(xsl_include_footer)};
        URIResolver resolver = new XslResolver(hrefs, returnValues);
        SAXSource xslSourceDTD = new SAXSource(new InputSource(new StringReader(xsl_includeDTD)));
        StreamSource xmlSourceDTD = new StreamSource(new StringReader(xml_xslDTD));

        String[] hrefs1 = {"pathto/DocFunc2.xml"};
        Source[] returnValues1 = {new StreamSource(xml_doc2)};
        URIResolver docResolver = new XslResolver(hrefs1, returnValues1);
        SAXSource xslDocSource = new SAXSource(new InputSource(new File(xsl_doc).toURI().toASCIIString()));
        StreamSource xmlDocSource = new StreamSource(new File(xml_doc));
        return new Object[][]{
            // for resolving DTD, import and include in xsl
            {false, true, xml_catalog, xslSourceDTD, xmlSourceDTD, null, ""},
            {false, true, xml_bogus_catalog, new SAXSource(new InputSource(new StringReader(xsl_include))),
                new StreamSource(new StringReader(xml_xsl)), resolver, ""},
            {true, true, xml_bogus_catalog, new SAXSource(new InputSource(new StringReader(xsl_include))),
                new StreamSource(new StringReader(xml_xsl)), resolver, ""},
            // for resolving reference by the document function
            {false, true, xml_catalog, xslDocSource, xmlDocSource, null, "Resolved by a catalog"},
            {false, true, xml_bogus_catalog, xslDocSource, xmlDocSource, docResolver, "Resolved by a resolver"},
            {true, true, xml_bogus_catalog, xslDocSource, xmlDocSource, docResolver, "Resolved by a resolver"}
        };
    }
}
