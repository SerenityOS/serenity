package validation;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;


import com.sun.org.apache.xerces.internal.dom.PSVIElementNSImpl;
import com.sun.org.apache.xerces.internal.impl.Constants;
import com.sun.org.apache.xerces.internal.impl.xs.SchemaGrammar;
import com.sun.org.apache.xerces.internal.xs.ElementPSVI;
import com.sun.org.apache.xerces.internal.xs.ItemPSVI;
import com.sun.org.apache.xerces.internal.xs.XSElementDeclaration;
import com.sun.org.apache.xerces.internal.xs.XSTypeDefinition;
import java.security.Policy;
import javax.xml.transform.stream.StreamSource;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

public abstract class BaseTest {
    protected final static String ROOT_TYPE = Constants.XERCES_PROPERTY_PREFIX
            + Constants.ROOT_TYPE_DEFINITION_PROPERTY;

    protected final static String IGNORE_XSI_TYPE = Constants.XERCES_FEATURE_PREFIX
            + Constants.IGNORE_XSI_TYPE_FEATURE;

    protected final static String ID_IDREF_CHECKING = Constants.XERCES_FEATURE_PREFIX
            + Constants.ID_IDREF_CHECKING_FEATURE;

    protected final static String IDC_CHECKING = Constants.XERCES_FEATURE_PREFIX
            + Constants.IDC_CHECKING_FEATURE;

    protected final static String UNPARSED_ENTITY_CHECKING = Constants.XERCES_FEATURE_PREFIX
            + Constants.UNPARSED_ENTITY_CHECKING_FEATURE;

    protected final static String USE_GRAMMAR_POOL_ONLY = Constants.XERCES_FEATURE_PREFIX
            + Constants.USE_GRAMMAR_POOL_ONLY_FEATURE;

    protected final static String DYNAMIC_VALIDATION = Constants.XERCES_FEATURE_PREFIX
            + Constants.DYNAMIC_VALIDATION_FEATURE;

    protected final static String DOCUMENT_CLASS_NAME = Constants.XERCES_PROPERTY_PREFIX
            + Constants.DOCUMENT_CLASS_NAME_PROPERTY;

    public static boolean isWindows = false;
    static {
        if (System.getProperty("os.name").indexOf("Windows")>-1) {
            isWindows = true;
        }
    };

    protected Schema schema;
    protected Validator fValidator;

    protected SpecialCaseErrorHandler fErrorHandler;

    protected DocumentBuilder builder;
    protected Document fDocument;

    protected ElementPSVI fRootNode;
    protected URL fDocumentURL;
    protected URL fSchemaURL;

    static String errMessage;

    int passed = 0, failed = 0;
    private boolean hasSM;
    private Policy orig;

    protected abstract String getSchemaFile();

    protected abstract String getXMLDocument();

    public BaseTest(String name) {
        fErrorHandler = new SpecialCaseErrorHandler(getRelevantErrorIDs());
    }

    protected void setUp() throws Exception {
        if (System.getSecurityManager() != null) {
            hasSM = true;
            System.setSecurityManager(null);
        }

        orig = Policy.getPolicy();

        DocumentBuilderFactory docFactory = DocumentBuilderFactory
                .newInstance();
        docFactory.setAttribute(DOCUMENT_CLASS_NAME,
                "com.sun.org.apache.xerces.internal.dom.PSVIDocumentImpl");
        docFactory.setNamespaceAware(true);
        builder = docFactory.newDocumentBuilder();
        // build the location URL of the document
        String filepath = System.getProperty("test.src", ".");
        String packageDir = this.getClass().getPackage().getName().replace('.',
                '/');
        String documentPath = filepath + "/" + packageDir + "/" + getXMLDocument();
        String schemaPath = filepath + "/" + packageDir + "/" + getSchemaFile();

        if (isWindows) {
            fDocumentURL = new URL("file:/" + documentPath);
            fSchemaURL = new URL("file:/" + schemaPath);
        } else {
            fDocumentURL = new URL("file:" + documentPath);
            fSchemaURL = new URL("file:" + schemaPath);
        }
        if (fDocumentURL == null) {
            throw new FileNotFoundException("Couldn't find xml file for test: " + documentPath);
        }

        SchemaFactory sf = SchemaFactory
                .newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        sf.setFeature(USE_GRAMMAR_POOL_ONLY, getUseGrammarPoolOnly());

        if (fSchemaURL == null) {
            throw new FileNotFoundException("Couldn't find schema file for test: " + schemaPath);
        }
        schema = sf.newSchema(fSchemaURL);

//        String schemaPath = "./jaxp-ri/src/unit-test/apache/xerces/jdk8037819/" + getSchemaFile();
//        Schema schema = sf.newSchema(new StreamSource(new File(schemaPath)));
    }

    protected void tearDown() throws Exception {
        System.setSecurityManager(null);
        Policy.setPolicy(orig);
        if (hasSM) {
            System.setSecurityManager(new SecurityManager());
        }

        builder = null;
        schema = null;
        fRootNode = null;
        fErrorHandler.reset();
        System.out.println("\nNumber of tests passed: " + passed);
        System.out.println("Number of tests failed: " + failed + "\n");

        if (errMessage != null) {
            throw new RuntimeException(errMessage);
        }
    }

    protected void validateDocument() throws Exception {
        Source source = new DOMSource(fDocument);
        source.setSystemId(fDocumentURL.toExternalForm());
        Result result = new DOMResult(fDocument);

        fValidator.validate(source, result);
    }

    protected void validateFragment() throws Exception {
        Source source = new DOMSource((Node) fRootNode);
        source.setSystemId(fDocumentURL.toExternalForm());
        Result result = new DOMResult((Node) fRootNode);
        fValidator.validate(source, result);
    }

    protected void reset() throws Exception {
//        fDocument = builder.parse(new File("./jaxp-ri/src/unit-test/apache/xerces/jdk8037819/" + getXMLDocument()));
        fDocument = builder.parse(fDocumentURL.toExternalForm());
        fRootNode = (ElementPSVI) fDocument.getDocumentElement();
        fValidator = schema.newValidator();
        fErrorHandler.reset();
        fValidator.setErrorHandler(fErrorHandler);
        fValidator.setFeature(DYNAMIC_VALIDATION, false);
    }

    protected PSVIElementNSImpl getChild(int n) {
        int numFound = 0;
        Node child = ((Node) fRootNode).getFirstChild();
        while (child != null) {
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                numFound++;
                if (numFound == n) {
                    return (PSVIElementNSImpl) child;
                }
            }
            child = child.getNextSibling();
        }
        return null;
    }

    protected String[] getRelevantErrorIDs() {
        return new String[] {};
    }

    protected boolean getUseGrammarPoolOnly() {
        return false;
    }

    // specialized asserts

    protected void assertValidity(short expectedValidity, short actualValidity) {
        String expectedString = expectedValidity == ItemPSVI.VALIDITY_VALID ? "valid"
                : (expectedValidity == ItemPSVI.VALIDITY_INVALID ? "invalid"
                        : "notKnown");
        String actualString = actualValidity == ItemPSVI.VALIDITY_VALID ? "valid"
                : (actualValidity == ItemPSVI.VALIDITY_INVALID ? "invalid"
                        : "notKnown");
        String message = "{validity} was <" + actualString
                + "> but it should have been <" + expectedString + ">";
        assertEquals(message, expectedValidity, actualValidity);
    }

    protected void assertValidationAttempted(short expectedAttempted,
            short actualAttempted) {
        String expectedString = expectedAttempted == ItemPSVI.VALIDATION_FULL ? "full"
                : (expectedAttempted == ItemPSVI.VALIDATION_PARTIAL ? "partial"
                        : "none");
        String actualString = actualAttempted == ItemPSVI.VALIDATION_FULL ? "full"
                : (actualAttempted == ItemPSVI.VALIDATION_PARTIAL ? "partial"
                        : "none");
        String message = "{validity} was <" + actualString
                + "> but it should have been <" + expectedString + ">";
        assertEquals(message, expectedAttempted, actualAttempted);
    }

    protected void assertElementName(String expectedName, String actualName) {
        assertEquals("Local name of element declaration is wrong.",
                expectedName, actualName);
    }

    protected void assertElementNull(XSElementDeclaration elem) {
        assertNull("Element declaration should be null.", elem);
    }

    protected void assertElementNamespace(String expectedName, String actualName) {
        assertEquals("Namespace of element declaration is wrong.",
                expectedName, actualName);
    }

    protected void assertElementNamespaceNull(String actualName) {
        assertNull("Local name of element declaration should be null.",
                actualName);
    }

    protected void assertTypeName(String expectedName, String actualName) {
        assertEquals("Local name of type definition is wrong.", expectedName,
                actualName);
    }

    protected void assertTypeNull(XSTypeDefinition type) {
        assertNull("Type definition should be null.", type);
    }

    protected void assertTypeNamespace(String expectedName, String actualName) {
        assertEquals("Namespace of type definition is wrong.", expectedName,
                actualName);
    }

    protected void assertTypeNamespaceNull(String actualName) {
        assertNull("Namespace of type definition should be null.", actualName);
    }

    protected void assertError(String error) {
        assertTrue("Error <" + error + "> should have occured, but did not.",
                fErrorHandler.specialCaseFound(error));
    }

    protected void assertNoError(String error) {
        assertFalse("Error <" + error
                + "> should not have occured (but it did)", fErrorHandler
                .specialCaseFound(error));
    }

    protected void assertAnyType(XSTypeDefinition type) {
        assertEquals("Type is supposed to be anyType", SchemaGrammar.fAnyType,
                type);
    }

    void assertEquals(String msg, Object expected, Object actual) {
        if (!expected.equals(actual)) {
            fail(msg + " Expected: " + expected + " Actual: " + actual);
        } else {
            success(null);
        }
    }
    void assertNull(String msg, Object value) {
        if (value != null) {
            fail(msg);
        } else {
            success(null);
        }
    }
    public void assertTrue(String msg, boolean value) {
        if (!value) {
            fail(msg);
        } else {
            success(null);
        }
    }
    public void assertFalse(String msg, boolean value) {
        if (value) {
            fail(msg);
        } else {
            success(null);
        }
    }
    public void fail(String errMsg) {
        if (errMessage == null) {
            errMessage = errMsg;
        } else {
            errMessage = errMessage + "\n" + errMsg;
        }
        failed++;
    }

    public void success(String msg) {
        passed++;
        if (msg != null) {
            if (msg.length() != 0) {
                System.out.println(msg);
            }
        }
    }
}
