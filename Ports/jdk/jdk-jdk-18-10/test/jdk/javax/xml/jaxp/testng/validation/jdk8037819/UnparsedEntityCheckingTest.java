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

import com.sun.org.apache.xerces.internal.dom.PSVIElementNSImpl;
import com.sun.org.apache.xerces.internal.xs.ItemPSVI;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import validation.BaseTest;

public class UnparsedEntityCheckingTest extends BaseTest {
    public static final String UNDECLARED_ENTITY = "UndeclaredEntity";

    protected String getXMLDocument() {
        return "unparsedEntity.xml";
    }

    protected String getSchemaFile() {
        return "base.xsd";
    }

    protected String[] getRelevantErrorIDs() {
        return new String[] { UNDECLARED_ENTITY };
    }

    public UnparsedEntityCheckingTest(String name) {
        super(name);
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
    public void testDefaultValid() {
        try {
            reset();
            validateDocument();
        } catch (Exception e) {
            fail("Validation failed: " + e.getMessage());
        }

        checkDefault();
    }

    @Test
    public void testSetFalseValid() {
        try {
            reset();
            fValidator.setFeature(UNPARSED_ENTITY_CHECKING, false);
            validateDocument();
        } catch (Exception e) {
            fail("Validation failed: " + e.getMessage());
        }

        checkDefault();
    }

    @Test
    public void testSetTrueValid() {
        try {
            reset();
            fValidator.setFeature(UNPARSED_ENTITY_CHECKING, true);
            validateDocument();
        } catch (Exception e) {
            fail("Validation failed: " + e.getMessage());
        }

        checkDefault();
    }

    @Test
    public void testDefaultInvalid() {
        try {
            reset();
            ((PSVIElementNSImpl) fRootNode).setAttributeNS(null,
                    "unparsedEntityAttr", "invalid");
            validateDocument();
        } catch (Exception e) {
            fail("Validation failed: " + e.getMessage());
        }

        checkInvalid();
    }

    @Test
    public void testSetFalseInvalid() {
        try {
            reset();
            ((PSVIElementNSImpl) fRootNode).setAttributeNS(null,
                    "unparsedEntityAttr", "invalid");
            fValidator.setFeature(UNPARSED_ENTITY_CHECKING, false);
            validateDocument();
        } catch (Exception e) {
            fail("Validation failed: " + e.getMessage());
        }

        checkDefault();
    }

    @Test
    public void testSetTrueInvalid() {
        try {
            reset();
            ((PSVIElementNSImpl) fRootNode).setAttributeNS(null,
                    "unparsedEntityAttr", "invalid");
            fValidator.setFeature(UNPARSED_ENTITY_CHECKING, true);
            validateDocument();
        } catch (Exception e) {
            fail("Validation failed: " + e.getMessage());
        }

        checkInvalid();
    }

    private void checkDefault() {
        assertNoError(UNDECLARED_ENTITY);
        assertValidity(ItemPSVI.VALIDITY_VALID, fRootNode.getValidity());
        assertValidationAttempted(ItemPSVI.VALIDATION_FULL, fRootNode
                .getValidationAttempted());
        assertElementName("A", fRootNode.getElementDeclaration().getName());
        assertTypeName("X", fRootNode.getTypeDefinition().getName());
    }

    private void checkInvalid() {
        assertError(UNDECLARED_ENTITY);
        assertValidity(ItemPSVI.VALIDITY_INVALID, fRootNode.getValidity());
        assertValidationAttempted(ItemPSVI.VALIDATION_FULL, fRootNode
                .getValidationAttempted());
        assertElementName("A", fRootNode.getElementDeclaration().getName());
        assertTypeName("X", fRootNode.getTypeDefinition().getName());
    }
}
