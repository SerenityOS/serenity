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

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;
import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import validation.BaseTest;

/**
 * @author Peter McCracken, IBM
 * @version $Id$
 */
public class FeaturePropagationTest extends BaseTest {

    public final String FEATURE_STRING_DEFAULT_FALSE = "http://apache.org/xml/features/honour-all-schemaLocations";
    public final String FEATURE_STRING_DEFAULT_TRUE = "http://apache.org/xml/features/validation/schema-full-checking";
    public final String SECURITY_MANAGER = "http://apache.org/xml/properties/security-manager";

    public FeaturePropagationTest(String name) {
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
    public void testPropertyReset() throws Exception {
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema = makeSchema(factory, null);
        Validator validator = schema.newValidator();
        Object beforeReset = validator.getProperty(SECURITY_MANAGER);
        validator.setProperty(SECURITY_MANAGER, null);
        Object changed = validator.getProperty(SECURITY_MANAGER);
        //for JDK, this is changed since by default the security manager is set
        assertTrue("Property value should have changed after calling setProperty().", beforeReset != changed);
        validator.reset();
        Object afterReset = validator.getProperty(SECURITY_MANAGER);
        assertTrue("Property value should be the same after calling reset()", beforeReset == afterReset);
    }

    @Test
    public void testFeatureReset() throws Exception {
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema = makeSchema(factory, null);
        Validator validator = schema.newValidator();
        validator.setFeature(FEATURE_STRING_DEFAULT_TRUE, false);
        validator.setFeature(FEATURE_STRING_DEFAULT_FALSE, true);
        validator.reset();
        boolean value = validator.getFeature(FEATURE_STRING_DEFAULT_TRUE);
        assertTrue("After reset, value of feature on Validator should be true.", value);
        value = validator.getFeature(FEATURE_STRING_DEFAULT_FALSE);
        assertFalse("After reset, value of feature on Validator should be false.", value);
    }

    @Test
    public void testSecureProcessingFeaturePropagationAndReset() throws Exception {
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        boolean value;
        value = factory.getFeature(XMLConstants.FEATURE_SECURE_PROCESSING);
        //default is true for JDK
        //assertFalse("Default value of feature on SchemaFactory should have been false.", value);
        assertTrue("Default value of feature on SchemaFactory should have been false.", value);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        Schema schema = makeSchema(factory, null);
        Validator validator = schema.newValidator();
        value = validator.getFeature(XMLConstants.FEATURE_SECURE_PROCESSING);
        assertTrue("Value of feature on Validator should have been true.", value);
        validator.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, false);
        value = validator.getFeature(XMLConstants.FEATURE_SECURE_PROCESSING);
        assertFalse("Value of feature on Validator should have been false.", value);
        validator.reset();
        value = validator.getFeature(XMLConstants.FEATURE_SECURE_PROCESSING);
        assertTrue("After reset, value of feature on Validator should be true.", value);
    }
    /*
     * Using four basically identical tests to try out the different
     * instance classes of Schema.  They shouldn't differ, because the relevant
     * code is in a common base class.
     */

    @Test
    public void testFeaturePropagationNull() throws Exception {
        checkFeaturesOnValidator(null);
    }

    @Test
    public void testFeaturePropagationEmpty() throws Exception {
        checkFeaturesOnValidator(new Source[] {});
    }

    @Test
    public void testFeaturePropagationSingle() throws Exception {
        checkFeaturesOnValidator(new Source[] {makeSource("base.xsd")});
    }

    @Test
    public void testFeaturePropagationMultiple() throws Exception {
        checkFeaturesOnValidator(new Source[] {makeSource("base.xsd"), makeSource("idc.xsd")});
    }

    private void checkFeaturesOnValidator(Source[] sources) throws Exception {
        try {
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = makeSchema(factory, sources);
            Validator validator = schema.newValidator();
            boolean value;
            value = validator.getFeature(FEATURE_STRING_DEFAULT_TRUE);
            assertTrue("Default value of feature on Validator should have been true.", value);
            value = validator.getFeature(FEATURE_STRING_DEFAULT_FALSE);
            assertFalse("Default value of feature on Validator should have been false.", value);

            // checking that the value propagates to the validator
            factory.setFeature(FEATURE_STRING_DEFAULT_TRUE, false);
            factory.setFeature(FEATURE_STRING_DEFAULT_FALSE, true);
            schema = makeSchema(factory, sources);
            validator = schema.newValidator();
            value = validator.getFeature(FEATURE_STRING_DEFAULT_TRUE);
            assertFalse("Value of feature on Validator should have been false.", value);
            value = validator.getFeature(FEATURE_STRING_DEFAULT_FALSE);
            assertTrue("Value of feature on Validator should have been true.", value);

            // checking that the validator contains a copy of the features, not a reference
            factory.setFeature(FEATURE_STRING_DEFAULT_TRUE, true);
            factory.setFeature(FEATURE_STRING_DEFAULT_FALSE, false);
            value = validator.getFeature(FEATURE_STRING_DEFAULT_TRUE);
            assertFalse("Value of feature on Validator should have stayed false.", value);
            value = validator.getFeature(FEATURE_STRING_DEFAULT_FALSE);
            assertTrue("Value of feature on Validator should have stayed true.", value);
        }
        catch (SAXNotRecognizedException e) {
            fail(e.getMessage());
        }
        catch (SAXNotSupportedException e) {
            fail(e.getMessage());
        }
    }

    private Schema makeSchema(SchemaFactory factory, Source[] sources) throws SAXException {
        if (sources == null) {
            return factory.newSchema();
        }
        else {
            return factory.newSchema(sources);
        }
    }

    private Source makeSource(String xsd) throws FileNotFoundException {
        return new StreamSource(fSchemaURL.toExternalForm());
    }

    @Override
    protected String getSchemaFile() {
        return "base.xsd";
    }

    @Override
    protected String getXMLDocument() {
        //not needed
        return null;
    }
}
