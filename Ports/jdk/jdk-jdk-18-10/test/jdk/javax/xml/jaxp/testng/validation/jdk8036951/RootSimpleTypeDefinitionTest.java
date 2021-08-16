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

import com.sun.org.apache.xerces.internal.xs.ItemPSVI;
import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;
import validation.BaseTest;

/**
 * @author Peter McCracken, IBM
 * @version $Id$
 */
public class RootSimpleTypeDefinitionTest extends BaseTest {

    private QName typeString;
    private QName typeNonNegInt;

    private final static String INVALID_TYPE_ERROR = "cvc-type.3.1.3";
    private final static String MININCLUSIVE_DERIVATION_ERROR = "cvc-minInclusive-valid";

    protected String getXMLDocument() {
        return "simpleType.xml";
    }

    protected String getSchemaFile() {
        return "base.xsd";
    }

    protected String[] getRelevantErrorIDs() {
        return new String[] { INVALID_TYPE_ERROR, MININCLUSIVE_DERIVATION_ERROR };
    }

    @BeforeClass
    protected void setUp() throws Exception {
        super.setUp();
    }

    @AfterClass
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public RootSimpleTypeDefinitionTest(String name) {
        super(name);
        // This is a roundabout way of making sure that we're not using an
        // interned string (so that == doesn't work)
        String ns = "x" + XMLConstants.W3C_XML_SCHEMA_NS_URI;
        ns = ns.substring(1);
        typeString = new QName(ns, "string", "xsd");
        typeNonNegInt = new QName(ns, "nonNegativeInteger", "xsd");
    }

    @Test
    public void testSettingSimpleType() throws Exception {
        try {
            reset();
            fValidator.setProperty(ROOT_TYPE, typeString);
        } catch (SAXException e1) {
            fail("Problem setting property: " + e1.getMessage());
        }

        try {
            validateDocument();
        } catch (Exception e) {
            fail("Validation failed: " + e.getMessage());
        }

        assertValidity(ItemPSVI.VALIDITY_VALID, fRootNode.getValidity());
        assertValidationAttempted(ItemPSVI.VALIDATION_FULL, fRootNode
                .getValidationAttempted());
        assertElementNull(fRootNode.getElementDeclaration());
        assertTypeName("string", fRootNode.getTypeDefinition().getName());
    }

    @Test
    public void testSettingInvalidSimpleType() throws Exception {
        try {
            reset();
            fValidator.setProperty(ROOT_TYPE, typeNonNegInt);
        } catch (SAXException e1) {
            fail("Problem setting property: " + e1.getMessage());
        }

        try {
            validateDocument();
        } catch (Exception e) {
            fail("Validation failed: " + e.getMessage());
        }

        assertError(INVALID_TYPE_ERROR);
        assertError(MININCLUSIVE_DERIVATION_ERROR);
        assertValidity(ItemPSVI.VALIDITY_INVALID, fRootNode.getValidity());
        assertValidationAttempted(ItemPSVI.VALIDATION_FULL, fRootNode
                .getValidationAttempted());
        assertElementNull(fRootNode.getElementDeclaration());
        assertTypeName("nonNegativeInteger", fRootNode.getTypeDefinition().getName());
    }
}
