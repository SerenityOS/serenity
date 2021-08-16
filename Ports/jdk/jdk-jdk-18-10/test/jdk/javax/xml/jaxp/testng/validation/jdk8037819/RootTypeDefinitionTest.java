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
package validation.jdk8037819;

import javax.xml.namespace.QName;
import com.sun.org.apache.xerces.internal.dom.PSVIElementNSImpl;
import com.sun.org.apache.xerces.internal.impl.xs.SchemaSymbols;
import com.sun.org.apache.xerces.internal.xs.ItemPSVI;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import validation.BaseTest;

public class RootTypeDefinitionTest extends BaseTest {
    private QName unknownType;

    private QName typeX;

    private QName typeY;

    private QName typeZ;

    private QName typeOtherNamespace;

    private final static String UNKNOWN_TYPE_ERROR = "cvc-type.1";

    private final static String INVALID_DERIVATION_ERROR = "cvc-elt.4.3";

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


    @BeforeClass
    protected void setUp() throws Exception {
        super.setUp();
    }

    @AfterClass
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testDefault() {
        try {
            reset();
            validateDocument();
        } catch (Exception e) {
            fail("Validation failed: " + e.getMessage());
        }

        checkDefault();
    }

    @Test
    public void testSettingNull() {
        try {
            reset();
            fValidator.setProperty(ROOT_TYPE, null);
            validateDocument();
        } catch (Exception e) {
            fail("Validation failed: " + e.getMessage());
        }

        checkDefault();
    }

    @Test
    public void testSettingToUnknownType() {
        try {
            reset();
            fValidator.setProperty(ROOT_TYPE, unknownType);
            validateDocument();
        } catch (Exception e) {
            fail("Validation failed: " + e.getMessage());
        }

        assertError(UNKNOWN_TYPE_ERROR);
        checkDefault();
    }

    @Test
    public void testSettingToEqualType() {
        try {
            reset();
            fValidator.setProperty(ROOT_TYPE, typeX);
            validateDocument();
        } catch (Exception e) {
            fail("Validation failed: " + e.getMessage());
        }

        assertValidity(ItemPSVI.VALIDITY_VALID, fRootNode.getValidity());
        assertValidationAttempted(ItemPSVI.VALIDATION_FULL, fRootNode
                .getValidationAttempted());
        assertElementNull(fRootNode.getElementDeclaration());
        assertTypeName("X", fRootNode.getTypeDefinition().getName());
    }

    @Test
    public void testSettingToDerivedType() {
        try {
            reset();
            // this is required to make it a valid type Y node
            ((PSVIElementNSImpl) fRootNode).setAttributeNS(null, "attr", "typeY");
            fValidator.setProperty(ROOT_TYPE, typeY);
            validateDocument();
        } catch (Exception e) {
            fail("Validation failed: " + e.getMessage());
        }

        assertValidity(ItemPSVI.VALIDITY_VALID, fRootNode.getValidity());
        assertValidationAttempted(ItemPSVI.VALIDATION_FULL, fRootNode
                .getValidationAttempted());
        assertElementNull(fRootNode.getElementDeclaration());
        assertTypeName("Y", fRootNode.getTypeDefinition().getName());
    }

    @Test
    public void testSettingToNonDerivedType() {
        try {
            reset();
            fValidator.setProperty(ROOT_TYPE, typeZ);
            validateDocument();
        } catch (Exception e) {
            fail("Validation failed: " + e.getMessage());
        }

        assertValidity(ItemPSVI.VALIDITY_VALID, fRootNode.getValidity());
        assertValidationAttempted(ItemPSVI.VALIDATION_FULL, fRootNode
                .getValidationAttempted());
        assertElementNull(fRootNode.getElementDeclaration());
        assertTypeName("Z", fRootNode.getTypeDefinition().getName());
    }

    @Test
    public void testSettingToOtherSchemaType() {
        try {
            reset();
            ((PSVIElementNSImpl) fRootNode).setAttributeNS(SchemaSymbols.URI_XSI,
                SchemaSymbols.XSI_SCHEMALOCATION,
                "xslt.unittests otherNamespace.xsd");
            fValidator.setProperty(ROOT_TYPE, typeOtherNamespace);
            validateDocument();
        } catch (Exception e) {
            fail("Validation failed: " + e.getMessage());
        }

        assertValidity(ItemPSVI.VALIDITY_VALID, fRootNode.getValidity());
        assertValidationAttempted(ItemPSVI.VALIDATION_FULL, fRootNode
                .getValidationAttempted());
        assertElementNull(fRootNode.getElementDeclaration());
        assertTypeName("W", fRootNode.getTypeDefinition().getName());
        assertTypeNamespace("xslt.unittests", fRootNode.getTypeDefinition()
                .getNamespace());
    }

    @Test
    public void testSettingTypeAndXSIType() {
        try {
            reset();
            // this is required to make it a valid type Y node
            ((PSVIElementNSImpl) fRootNode).setAttributeNS(null, "attr", "typeY");
            ((PSVIElementNSImpl) fRootNode).setAttributeNS(SchemaSymbols.URI_XSI,
                    SchemaSymbols.XSI_TYPE, "Y");
            fValidator.setProperty(ROOT_TYPE, typeX);
            validateDocument();
        } catch (Exception e) {
            fail("Validation failed: " + e.getMessage());
        }

        assertValidity(ItemPSVI.VALIDITY_VALID, fRootNode.getValidity());
        assertValidationAttempted(ItemPSVI.VALIDATION_FULL, fRootNode
                .getValidationAttempted());
        assertElementNull(fRootNode.getElementDeclaration());
        assertTypeName("Y", fRootNode.getTypeDefinition().getName());
    }

    @Test
    public void testSettingTypeAndInvalidXSIType() {
        try {
            reset();
            ((PSVIElementNSImpl) fRootNode).setAttributeNS(SchemaSymbols.URI_XSI,
                    SchemaSymbols.XSI_TYPE, "Z");
            fValidator.setProperty(ROOT_TYPE, typeX);
            validateDocument();
        } catch (Exception e) {
            fail("Validation failed: " + e.getMessage());
        }

        assertError(INVALID_DERIVATION_ERROR);
        assertValidity(ItemPSVI.VALIDITY_INVALID, fRootNode.getValidity());
        assertValidationAttempted(ItemPSVI.VALIDATION_FULL, fRootNode
                .getValidationAttempted());
        assertElementNull(fRootNode.getElementDeclaration());
        assertTypeName("Z", fRootNode.getTypeDefinition().getName());
    }

    private void checkDefault() {
        assertValidity(ItemPSVI.VALIDITY_VALID, fRootNode.getValidity());
        assertValidationAttempted(ItemPSVI.VALIDATION_FULL, fRootNode
                .getValidationAttempted());
        assertElementName("A", fRootNode.getElementDeclaration().getName());
        assertTypeName("X", fRootNode.getTypeDefinition().getName());
    }
}
