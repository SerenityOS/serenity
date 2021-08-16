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

import com.sun.org.apache.xerces.internal.xs.ItemPSVI;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import validation.BaseTest;

public class UseGrammarPoolOnlyTest_True extends BaseTest {

    protected String getXMLDocument() {
        return "otherNamespace.xml";
    }

    protected String getSchemaFile() {
        return "base.xsd";
    }

    protected boolean getUseGrammarPoolOnly() {
        return true;
    }

    public UseGrammarPoolOnlyTest_True(String name) {
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

    /**
     * The purpose of this test is to check if setting the USE_GRAMMAR_POOL_ONLY
     * feature to true causes external schemas to not be read. This
     * functionality already existed prior to adding the XSLT 2.0 validation
     * features; however, because the class that controlled it changed, this
     * test simply ensures that the existing functionality did not disappear.
     * -PM
     */
    @Test
    public void testUsingOnlyGrammarPool() {
        try {
            reset();
            validateDocument();
        } catch (Exception e) {
            fail("Validation failed: " + e.getMessage());
        }

        assertValidity(ItemPSVI.VALIDITY_NOTKNOWN, fRootNode.getValidity());
        assertValidationAttempted(ItemPSVI.VALIDATION_NONE, fRootNode
                .getValidationAttempted());
        assertElementNull(fRootNode.getElementDeclaration());
        assertAnyType(fRootNode.getTypeDefinition());
    }
}
