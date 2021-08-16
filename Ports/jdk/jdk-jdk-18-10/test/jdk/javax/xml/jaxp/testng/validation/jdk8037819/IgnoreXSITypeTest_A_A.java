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

public class IgnoreXSITypeTest_A_A extends BaseTest {

    protected String getXMLDocument() {
        return "xsitype_A_A.xml";
    }

    protected String getSchemaFile() {
        return "base.xsd";
    }

    public IgnoreXSITypeTest_A_A(String name) {
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
    public void testDefaultDocument() {
        try {
            reset();
            validateDocument();
        } catch (Exception e) {
            fail("Validation failed: " + e.getMessage());
        }

        // default value of the feature is false
        checkFalseResult();
    }

    @Test
    public void testDefaultFragment() {
        try {
            reset();
            validateFragment();
        } catch (Exception e) {
            fail("Validation failed: " + e.getMessage());
        }

        // default value of the feature is false
        checkFalseResult();
    }

    @Test
    public void testSetFalseDocument() {
        try {
            reset();
            fValidator.setFeature(IGNORE_XSI_TYPE, false);
            validateDocument();
        } catch (Exception e) {
            fail("Validation failed: " + e.getMessage());
        }

        checkFalseResult();
    }

    @Test
    public void testSetFalseFragment() {
        try {
            reset();
            fValidator.setFeature(IGNORE_XSI_TYPE, false);
            validateFragment();
        } catch (Exception e) {
            fail("Validation failed: " + e.getMessage());
        }

        checkFalseResult();
    }

    @Test
    public void testSetTrueDocument() {
        try {
            reset();
            fValidator.setFeature(IGNORE_XSI_TYPE, true);
            validateDocument();
        } catch (Exception e) {
            fail("Validation failed: " + e.getMessage());
        }

        checkTrueResult();
    }

    @Test
    public void testSetTrueFragment() {
        try {
            reset();
            fValidator.setFeature(IGNORE_XSI_TYPE, true);
            validateFragment();
        } catch (Exception e) {
            fail("Validation failed: " + e.getMessage());
        }

        checkTrueResult();
    }

    private void checkTrueResult() {
        checkResult();
    }

    private void checkFalseResult() {
        checkResult();
    }

    private void checkResult() {
        assertValidity(ItemPSVI.VALIDITY_VALID, fRootNode.getValidity());
        assertValidationAttempted(ItemPSVI.VALIDATION_FULL, fRootNode
                .getValidationAttempted());
        assertElementName("A", fRootNode.getElementDeclaration().getName());
        assertTypeName("Y", fRootNode.getTypeDefinition().getName());
        assertTypeNamespaceNull(fRootNode.getTypeDefinition().getNamespace());

        PSVIElementNSImpl child = super.getChild(1);
        assertValidity(ItemPSVI.VALIDITY_VALID, child.getValidity());
        assertValidationAttempted(ItemPSVI.VALIDATION_FULL, child
                .getValidationAttempted());
        assertElementName("A", child.getElementDeclaration().getName());
        assertTypeName("Y", child.getTypeDefinition().getName());
        assertTypeNamespaceNull(child.getTypeDefinition().getNamespace());
    }
}
