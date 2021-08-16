/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package validation.jdk8036951;


import com.sun.org.apache.xerces.internal.xs.ElementPSVI;
import com.sun.org.apache.xerces.internal.xs.ItemPSVI;
import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.validation.SchemaFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import validation.BaseTest;

/**
 * @author Peter McCracken, IBM
 * @version $Id$
 */
public class RootTypeDefinitionTest extends BaseTest {

    private QName unknownType;
    private QName typeX;
    private QName typeY;
    private QName typeZ;
    private QName typeOtherNamespace;

    private final static String UNKNOWN_TYPE_ERROR = "cvc-type.1";

    private final static String INVALID_DERIVATION_ERROR = "cvc-elt.4.3";

    @BeforeClass
    protected void setUp() throws Exception {
        super.setUp();
    }

    @AfterClass
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    protected String getXMLDocument() {
        return "base.xml";
    }

    protected String getSchemaFile() {
        return "base.xsd";
    }

    protected String[] getRelevantErrorIDs() {
        return new String[] { UNKNOWN_TYPE_ERROR, INVALID_DERIVATION_ERROR };
    }

    public RootTypeDefinitionTest(String name) {
        super(name);
        unknownType = new QName("W");
        typeX = new QName("X");
        typeY = new QName("Y");
        typeZ = new QName("Z");
        typeOtherNamespace = new QName("xslt.unittests", "W", "unit");
    }


    /**
     * XERCESJ-1141 root-type-definition property not read by XMLSchemaValidator during reset()
     */
    @Test
    public void testUsingDocumentBuilderFactory() throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setAttribute(ROOT_TYPE, typeX);
        dbf.setAttribute(DOCUMENT_CLASS_NAME,"com.sun.org.apache.xerces.internal.dom.PSVIDocumentImpl");
        dbf.setNamespaceAware(true);
        dbf.setValidating(false);

        SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        dbf.setSchema(sf.newSchema(fSchemaURL));

        DocumentBuilder db = dbf.newDocumentBuilder();
        Document document = db.parse(fDocumentURL.toExternalForm());
        ElementPSVI rootNode = (ElementPSVI) document.getDocumentElement();

        assertValidity(ItemPSVI.VALIDITY_VALID, rootNode.getValidity());
        assertValidationAttempted(ItemPSVI.VALIDATION_FULL, rootNode
                .getValidationAttempted());
        assertElementNull(rootNode.getElementDeclaration());
        assertTypeName("X", rootNode.getTypeDefinition().getName());
    }

    private void checkDefault() {
        assertValidity(ItemPSVI.VALIDITY_VALID, fRootNode.getValidity());
        assertValidationAttempted(ItemPSVI.VALIDATION_FULL, fRootNode
                .getValidationAttempted());
        assertElementName("A", fRootNode.getElementDeclaration().getName());
        assertTypeName("X", fRootNode.getTypeDefinition().getName());
    }
}
