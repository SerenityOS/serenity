/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

package transform;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.xml.sax.InputSource;

/*
 * @test
 * @bug 8157830 8228854
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm transform.ErrorListenerTest
 * @summary Verifies that ErrorListeners are handled properly
 */
public class ErrorListenerTest {
    static final int SYSTEM_ERR = 1;
    static final int SYSTEM_OUT = 2;
    static final String ERR_STDERR = "Msg sent to stderr";
    static final String ERR_STDOUT = "Msg sent to stdout";

    static final private String INVALID_STYLESHEET = "xxx";
    static final private String SYSTEM_ID = "http://openjdk_java_net/xsl/dummy.xsl";

    final private String INCLUDE_NOT_EXIST = "<?xml version=\"1.1\" encoding=\"UTF-8\"?>" +
        "<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" version=\"1.0\">" +
        "    <xsl:import href=\"NOSUCHFILE.xsl\"/>" +
        "</xsl:stylesheet>";

    final private String VAR_UNDEFINED = "<?xml version=\"1.1\" encoding=\"ISO-8859-1\"?>" +
        "<xsl:stylesheet version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\">" +
        "    <xsl:template match=\"/\"> " +
        "        <test1><xsl:apply-templates select=\"$ids\"/></test1>" +
        "        <test2><xsl:apply-templates select=\"$dummy//ids/id\"/></test2>" +
        "    </xsl:template>" +
        "</xsl:stylesheet>";
    final private String XSL_DOC_FUNCTION = "<?xml version=\"1.1\" encoding=\"ISO-8859-1\"?>" +
        "<xsl:stylesheet version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\">" +
        "    <xsl:output method=\"xml\" indent=\"yes\"/>" +
        "    <xsl:variable name=\"ids\" select=\"//ids//id\"/>" +
        "    <xsl:variable name=\"dummy\" select=\"document('NOSUCHFILE.xml')\"/>" +
        "    <xsl:template match=\"/\"> " +
        "        <test1><xsl:apply-templates select=\"$ids\"/></test1>" +
        "        <test2><xsl:apply-templates select=\"$dummy//ids/id\"/></test2>" +
        "    </xsl:template>" +
        "    <xsl:template match=\"id\">" +
        "        <xsl:variable name=\"entity\" select=\"id(@value)\"/> " +
        "        <must-be-one><xsl:value-of select=\"count($entity)\"/></must-be-one>" +
        "    </xsl:template>" +
        "</xsl:stylesheet>";
    final private String XML_DOC_FUNCTION = "<?xml version=\"1.1\" encoding=\"ISO-8859-1\" standalone=\"no\"?>" +
        "<organization2>" +
        "    <company id=\"xca\" count=\"2\">" +
        "        <department id=\"xda\"/>" +
        "    </company>" +
        "    <company id=\"xcb\" count=\"0\"/>" +
        "    <company id=\"xcc\" count=\"5\"/>" +
        "    <ids>" +
        "        <id value=\"xca\"/>" +
        "        <id value=\"xcb\"/>" +
        "    </ids>" +
        "</organization2>";

    PrintStream originalErr, originalOut;
    List<String> testMsgs = new ArrayList<>();

    @BeforeClass
    public void setUpClass() throws Exception {
        // save the PrintStream
        originalErr = System.err;
        originalOut = System.out;
    }

    @AfterClass
    protected void tearDown() throws Exception {
        // set back to the original
        System.setErr(originalErr);
        System.setOut(originalOut);
        // print out test messages
        testMsgs.stream().forEach((msg) -> {
            System.out.println(msg);
        });
    }

    /*
       DataProvider: for ErrorListenner tests
       Data: xsl, xml, setListener(true/false), output channel(stderr/stdout),
             expected console output, expected listener output
     */
    @DataProvider(name = "testCreatingTransformer")
    public Object[][] getTransformer() {
        return new Object[][]{
            /*
             * Verifies that the default implementation does not print out
             * warnings and errors to stderr.
             */
            {INCLUDE_NOT_EXIST, false, ""},
            {VAR_UNDEFINED, false, ""},
            /*
             * Verifies that the registered listener is used.
             */
            {INCLUDE_NOT_EXIST, true, "NOSUCHFILE.xsl"},
            {VAR_UNDEFINED, true, "'ids' is undefined"},
            /*
             * The original test for JDK8157830
             * Verifies that when an ErrorListener is registered, parser errors
             * are passed onto the listener without other output.
            */
            {INVALID_STYLESHEET, true, "Content is not allowed in prolog"},
        };
    }
    /*
       DataProvider: for ErrorListenner tests
       Data: xsl, xml, setListener(true/false), output channel(stderr/stdout),
             expected console output, expected listener output
     */
    @DataProvider(name = "testTransform")
    public Object[][] getTransform() {
        return new Object[][]{
            /*
             * Verifies that the default implementation does not print out
             * warnings and errors to stderr.
             */
            {XSL_DOC_FUNCTION, XML_DOC_FUNCTION, false, ""},
            /*
             * Verifies that the default implementation does not print out
             * warnings and errors to stderr.
             */
            {XSL_DOC_FUNCTION, XML_DOC_FUNCTION, true, "NOSUCHFILE.xml"}
        };
    }

    /*
       DataProvider: for ErrorListenner tests
       Data: xsl, xml, setListener(true/false), expected listener output
     */
    @DataProvider(name = "testEncoding")
    public Object[][] getData() {
        return new Object[][]{
            {"<foo><bar></bar></foo>", false, ""},
            {"<foo><bar></bar></foo>", true, "'dummy' is not supported"}
        };
    }

    /**
     * Verifies that ErrorListeners are properly set and propagated, or the
     * default ErrorListener does not send messages to stderr/stdout.
     *
     * @param xsl the stylesheet
     * @param setListener a flag indicating whether a listener should be set
     * @param msgL the expected listener output
     * @throws Exception if the test fails
     */
    @Test(dataProvider = "testCreatingTransformer")
    public void testTransformer(String xsl, boolean setListener, String msgL)
            throws Exception {
        ErrListener listener = setListener ? new ErrListener("test") : null;
        String msgConsole = getTransformerErr("testTransformer", xsl, listener);
        evalResult(listener, msgConsole, setListener, msgL);
    }

    /**
     * Verifies that ErrorListeners are properly set and propagated, or the
     * default ErrorListener does not send messages to stderr/stdout.
     *
     * @param xsl the stylesheet
     * @param xml the XML
     * @param setListener a flag indicating whether a listener should be set
     * @param msgL the expected listener output
     * @throws Exception if the test fails
     */
    //@Test(dataProvider = "testTransform")
    public void testTransform(String xsl, String xml, boolean setListener, String msgL)
            throws Exception {
        ErrListener listener = setListener ? new ErrListener("test") : null;
        Transformer t = getTransformer("testDocFunc", xsl, listener);
        String msgConsole = transform("testDocFunc", xml, t);
        evalResult(listener, msgConsole, setListener, msgL);
    }

    /**
     * Verifies that the default implementation does not print out warnings and
     * errors to the console when an invalid encoding is set.
     *
     * @throws Exception if the test fails
     */
    //@Test(dataProvider = "testEncoding")
    public void testEncoding(String xml, boolean setListener, String msgL)
            throws Exception {
        ErrListener listener = setListener ? new ErrListener("test") : null;
        Transformer t = TransformerFactory.newInstance().newTransformer();
        if (setListener) {
            t.setErrorListener(listener);
        }
        t.setOutputProperty(OutputKeys.ENCODING, "dummy");
        String msgConsole = transform("testEncoding", "<foo><bar></bar></foo>", t);
        evalResult(listener, msgConsole, setListener, msgL);
    }

    private void evalResult(ErrListener l, String m, boolean setListener, String msgL)
            throws Exception{
        Assert.assertTrue(!m.contains(ERR_STDERR), "no output to stderr");
        Assert.assertTrue(!m.contains(ERR_STDOUT), "no output to stdout");
        if (setListener) {
            testMsgs.add("l.errMsg=" + l.errMsg);
            testMsgs.add("evalResult.msgL=" + msgL);
            Assert.assertTrue(l.errMsg.contains(msgL),
                    "The registered listener shall be used.");
        }
    }

    /**
     * Obtains a Transformer.
     *
     * @param test the name of the test
     * @param xsl the stylesheet
     * @param setListener a flag indicating whether to set a listener
     * @return the Transformer, null if error occurs
     * @throws Exception
     */
    private Transformer getTransformer(String test, String xsl, ErrorListener listener)
            throws Exception {
        Transformer f = null;
        InputSource source = new InputSource(new ByteArrayInputStream(xsl.getBytes()));
        TransformerFactory factory = TransformerFactory.newInstance();
        if (listener != null) {
            factory.setErrorListener(listener);
        }

        try {
            f = factory.newTransformer(new SAXSource(source));
            if (listener != null) {
                f.setErrorListener(listener);
            }
        } catch (TransformerConfigurationException e) {
            testMsgs.add(test + "::catch: " + e.getMessage());
        }

        return f;
    }

    /**
     * Attempts to capture messages sent to stderr/stdout during the creation
     * of a Transformer.
     *
     * @param test the name of the test
     * @param xsl the stylesheet
     * @param setListener a flag indicating whether to set a listener
     * @return message sent to stderr/stdout, null if none
     * @throws Exception
     */
    private String getTransformerErr(String test, String xsl, ErrorListener listener)
            throws Exception {
        InputStream is = new ByteArrayInputStream(xsl.getBytes());
        InputSource source = new InputSource(is);

        ByteArrayOutputStream baos1 = setOutput(SYSTEM_ERR);
        ByteArrayOutputStream baos2 = setOutput(SYSTEM_OUT);

        TransformerFactory factory = TransformerFactory.newInstance();
        if (listener != null) {
            factory.setErrorListener(listener);
        }

        try {
            factory.newTransformer(new SAXSource(source));
        } catch (TransformerConfigurationException e) {
            testMsgs.add(test + "::catch: " + e.getMessage());
        }
        reset();
        String msg = !"".equals(baos1.toString()) ? ERR_STDERR : "";
        msg = !"".equals(baos2.toString()) ? msg + ERR_STDOUT : msg;
        return msg;
    }

    /**
     * Transforms an XML file. Attempts to capture stderr/stdout as the Transformer
     * may direct messages to stdout.
     *
     * @param test the name of the test
     * @param xml the XML file
     * @param t the Transformer
     * @param type the flag indicating which output channel to capture
     * @return message sent to stdout, null if none
     * @throws Exception
     */
    private String transform(String test, String xml, Transformer t)
            throws Exception {
        StreamSource source = new StreamSource(new StringReader(xml));
        StreamResult result = new StreamResult(new StringWriter());
        ByteArrayOutputStream baos1 = setOutput(SYSTEM_ERR);
        ByteArrayOutputStream baos2 = setOutput(SYSTEM_OUT);
        try {
            t.transform(source, result);
        } catch (Exception e) {
            testMsgs.add(test + "::catch: " + e.getMessage());
        }
        reset();
        String msg = !"".equals(baos1.toString()) ? ERR_STDERR : "";
        msg = !"".equals(baos2.toString()) ? msg + ERR_STDOUT : msg;
        return msg;
    }

    private ByteArrayOutputStream setOutput(int type) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        if (type == SYSTEM_ERR) {
            System.setErr(ps);
        } else {
            System.setOut(ps);
        }
        return baos;
    }

    private void reset() {
        System.setErr(originalErr);
        System.setOut(originalOut);
    }

    class ErrListener implements ErrorListener {
        String testName;
        String errMsg = "";
        ErrListener(String test) {
            testName = test;
        }

        @Override
        public void error(TransformerException e)
                throws TransformerException {
            errMsg = errMsg + "#error: " + e.getMessage();
        }

        @Override
        public void fatalError(TransformerException e)
                throws TransformerException {
            errMsg = errMsg + "#fatalError: " + e.getMessage();
        }

        @Override
        public void warning(TransformerException e)
                throws TransformerException {
            errMsg = errMsg + "#warning: " + e.getMessage();
        }
    }
}
