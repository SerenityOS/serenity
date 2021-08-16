/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

package util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;

import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLResolver;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;

import org.testng.Assert;

/**
 * Base class for all StaxTest unit test classes. Contains shared
 * functionality for many common set up tasks, as well as for
 * outputting diagnostics.
 *
 */
public class BaseStAXUT implements XMLStreamConstants {
    /**
     * This is the de facto standard property that enables accurate reporting of
     * CDATA events.
     */
    final static String PROP_REPORT_CDATA = "http://java.sun.com/xml/stream/properties/report-cdata-event";

    final static HashMap mTokenTypes = new HashMap();
    static {
        mTokenTypes.put(new Integer(START_ELEMENT), "START_ELEMENT");
        mTokenTypes.put(new Integer(END_ELEMENT), "END_ELEMENT");
        mTokenTypes.put(new Integer(START_DOCUMENT), "START_DOCUMENT");
        mTokenTypes.put(new Integer(END_DOCUMENT), "END_DOCUMENT");
        mTokenTypes.put(new Integer(CHARACTERS), "CHARACTERS");
        mTokenTypes.put(new Integer(CDATA), "CDATA");
        mTokenTypes.put(new Integer(COMMENT), "COMMENT");
        mTokenTypes.put(new Integer(PROCESSING_INSTRUCTION), "PROCESSING_INSTRUCTION");
        mTokenTypes.put(new Integer(DTD), "DTD");
        mTokenTypes.put(new Integer(SPACE), "SPACE");
        mTokenTypes.put(new Integer(ENTITY_REFERENCE), "ENTITY_REFERENCE");
        mTokenTypes.put(new Integer(NAMESPACE), "NAMESPACE_DECLARATION");
        mTokenTypes.put(new Integer(NOTATION_DECLARATION), "NOTATION_DECLARATION");
        mTokenTypes.put(new Integer(ENTITY_DECLARATION), "ENTITY_DECLARATION");
    }

    /*
     * /////////////////////////////////////////////////// // Consts for
     * expected values ///////////////////////////////////////////////////
     */

    /**
     * Expected return value for streamReader.getNamespaceURI() in
     * non-namespace-aware mode.
     */
    protected final String DEFAULT_URI_NON_NS = "";

    protected final String DEFAULT_URI_NS = "";

    /*
     * /////////////////////////////////////////////////// // Other consts
     * ///////////////////////////////////////////////////
     */

    /*
     * /////////////////////////////////////////////////// // Cached instances
     * ///////////////////////////////////////////////////
     */

    XMLInputFactory mInputFactory;
    XMLOutputFactory mOutputFactory;
    XMLEventFactory mEventFactory;

    protected XMLInputFactory getInputFactory() {
        if (mInputFactory == null) {
            mInputFactory = getNewInputFactory();
        }
        return mInputFactory;
    }

    protected static XMLInputFactory getNewInputFactory() {
        return XMLInputFactory.newInstance();
    }

    protected XMLOutputFactory getOutputFactory() {
        if (mOutputFactory == null) {
            mOutputFactory = getNewOutputFactory();
        }
        return mOutputFactory;
    }

    protected static XMLOutputFactory getNewOutputFactory() {
        return XMLOutputFactory.newInstance();
    }

    protected XMLEventFactory getEventFactory() {
        if (mEventFactory == null) {
            mEventFactory = XMLEventFactory.newInstance();
        }
        return mEventFactory;
    }

    protected static XMLStreamReader constructStreamReader(XMLInputFactory f, String content) throws XMLStreamException {
        // return f.createXMLStreamReader(new StringReader(content));
        try {
            byte[] data = content.getBytes("UTF-8");
            return constructStreamReader(f, data);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    protected static XMLStreamReader constructStreamReader(XMLInputFactory f, byte[] b) throws XMLStreamException {
        return f.createXMLStreamReader(new ByteArrayInputStream(b));
    }

    protected static XMLStreamReader constructStreamReaderForFile(XMLInputFactory f, String filename) throws IOException, XMLStreamException {
        File inf = new File(filename);
        XMLStreamReader sr = f.createXMLStreamReader(inf.toURL().toString(), new FileReader(inf));
        Assert.assertEquals(START_DOCUMENT, sr.getEventType());
        return sr;
    }

    protected XMLStreamReader constructNsStreamReader(String content) throws XMLStreamException {
        XMLInputFactory f = getInputFactory();
        setNamespaceAware(f, true);
        return f.createXMLStreamReader(new StringReader(content));
    }

    protected XMLStreamReader constructNsStreamReader(String content, boolean coal) throws XMLStreamException {
        XMLInputFactory f = getInputFactory();
        setNamespaceAware(f, true);
        setCoalescing(f, coal);
        return f.createXMLStreamReader(new StringReader(content));
    }

    /*
     * ////////////////////////////////////////////////// // Configuring input
     * factory //////////////////////////////////////////////////
     */

    protected static boolean isCoalescing(XMLInputFactory f) throws XMLStreamException {
        return ((Boolean) f.getProperty(XMLInputFactory.IS_COALESCING)).booleanValue();
    }

    protected static void setCoalescing(XMLInputFactory f, boolean state) throws XMLStreamException {
        Boolean b = state ? Boolean.TRUE : Boolean.FALSE;
        f.setProperty(XMLInputFactory.IS_COALESCING, b);
        // Let's just double-check it...
        Assert.assertEquals(state, isCoalescing(f));
    }

    protected static boolean isValidating(XMLInputFactory f) throws XMLStreamException {
        return ((Boolean) f.getProperty(XMLInputFactory.IS_VALIDATING)).booleanValue();
    }

    protected static void setValidating(XMLInputFactory f, boolean state) throws XMLStreamException {
        try {
            Boolean b = state ? Boolean.TRUE : Boolean.FALSE;
            f.setProperty(XMLInputFactory.IS_VALIDATING, b);
        } catch (IllegalArgumentException iae) {
            Assert.fail("Could not set DTD validating mode to " + state + ": " + iae);
            // throw new XMLStreamException(iae.getMessage(), iae);
        }
        Assert.assertEquals(state, isValidating(f));
    }

    protected static boolean isNamespaceAware(XMLInputFactory f) throws XMLStreamException {
        return ((Boolean) f.getProperty(XMLInputFactory.IS_NAMESPACE_AWARE)).booleanValue();
    }

    /**
     * @return True if setting succeeded, and property supposedly was
     *         succesfully set to the value specified; false if there was a
     *         problem.
     */
    protected static boolean setNamespaceAware(XMLInputFactory f, boolean state) throws XMLStreamException {
        try {
            f.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, state ? Boolean.TRUE : Boolean.FALSE);

            /*
             * 07-Sep-2005, TSa: Let's not assert, but instead let's see if it
             * sticks. Some implementations might choose to silently ignore
             * setting, at least for 'false'?
             */
            return (isNamespaceAware(f) == state);
        } catch (IllegalArgumentException e) {
            /*
             * Let's assume, then, that the property (or specific value for it)
             * is NOT supported...
             */
            return false;
        }
    }

    protected static void setReplaceEntities(XMLInputFactory f, boolean state) throws XMLStreamException {
        Boolean b = state ? Boolean.TRUE : Boolean.FALSE;
        f.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, b);
        Assert.assertEquals(b, f.getProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES));
    }

    protected static void setSupportDTD(XMLInputFactory f, boolean state) throws XMLStreamException {
        Boolean b = state ? Boolean.TRUE : Boolean.FALSE;
        f.setProperty(XMLInputFactory.SUPPORT_DTD, b);
        Assert.assertEquals(b, f.getProperty(XMLInputFactory.SUPPORT_DTD));
    }

    protected static boolean setSupportExternalEntities(XMLInputFactory f, boolean state) throws XMLStreamException {
        Boolean b = state ? Boolean.TRUE : Boolean.FALSE;
        try {
            f.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, b);
            Object act = f.getProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES);
            return (act instanceof Boolean) && ((Boolean) act).booleanValue() == state;
        } catch (IllegalArgumentException e) {
            /*
             * Let's assume, then, that the property (or specific value for it)
             * is NOT supported...
             */
            return false;
        }
    }

    protected static void setResolver(XMLInputFactory f, XMLResolver resolver) throws XMLStreamException {
        f.setProperty(XMLInputFactory.RESOLVER, resolver);
    }

    protected static boolean setReportCData(XMLInputFactory f, boolean state) throws XMLStreamException {

        Boolean b = state ? Boolean.TRUE : Boolean.FALSE;
        if (f.isPropertySupported(PROP_REPORT_CDATA)) {
            f.setProperty(PROP_REPORT_CDATA, b);
            return true;
        }
        return false;
    }

    /*
     * ////////////////////////////////////////////////// // Stream reader
     * accessors //////////////////////////////////////////////////
     */

    /**
     * Method that not only gets currently available text from the reader, but
     * also checks that its consistenly accessible using different StAX methods.
     */
    protected static String getAndVerifyText(XMLStreamReader sr) throws XMLStreamException {
        String text = sr.getText();

        /*
         * 05-Apr-2006, TSa: Although getText() is available for DTD and
         * ENTITY_REFERENCE, getTextXxx() are not. Thus, can not do more checks
         * for those types.
         */
        int type = sr.getEventType();
        if (type != ENTITY_REFERENCE && type != DTD) {
            Assert.assertNotNull("getText() should never return null.", text);
            int expLen = sr.getTextLength();
            /*
             * Hmmh. Can only return empty text for CDATA (since empty blocks
             * are legal).
             */
            /*
             * !!! 01-Sep-2004, TSa: note: theoretically, in coalescing mode, it
             * could be possible to have empty CDATA section(s) get converted to
             * CHARACTERS, which would be empty... may need to enhance this to
             * check that mode is not coalescing? Or something
             */
            if (sr.getEventType() == CHARACTERS) {
                if (expLen == 0) {
                    Assert.fail("Stream reader should never return empty Strings.");
                }
            }
            Assert.assertEquals(expLen, text.length(), "Expected text length of " + expLen + ", got " + text.length());
            char[] textChars = sr.getTextCharacters();
            int start = sr.getTextStart();
            String text2 = new String(textChars, start, expLen);
            Assert.assertEquals("Expected getText() and getTextCharacters() to return same value for event of type (" + tokenTypeDesc(sr.getEventType()) + ")",
                    text, text2);
        } else { // DTD or ENTITY_REFERENCE
            // not sure if null is legal for these either, but...
            if (text == null) { // let's prevent an NPE at caller
                text = "";
            }
        }
        return text;
    }

    protected static String getAllText(XMLStreamReader sr) throws XMLStreamException {
        StringBuffer sb = new StringBuffer();
        while (true) {
            int tt = sr.getEventType();
            if (tt != CHARACTERS && tt != SPACE) {
                break;
            }
            sb.append(getAndVerifyText(sr));
            sr.next();
        }
        return sb.toString();
    }

    protected static String getAllCData(XMLStreamReader sr) throws XMLStreamException {
        StringBuffer sb = new StringBuffer();
        while (true) {
            /*
             * Note: CDATA sections CAN be reported as CHARACTERS, but not as
             * SPACE
             */
            int tt = sr.getEventType();
            if (tt != CHARACTERS && tt != CDATA) {
                break;
            }
            sb.append(getAndVerifyText(sr));
            sr.next();
        }
        return sb.toString();
    }

    /*
     * ////////////////////////////////////////////////// // Derived assert/fail
     * methods //////////////////////////////////////////////////
     */

    protected static void assertTokenType(int expType, int actType) {
        if (expType == actType) {
            return;
        }
        Assert.fail("Expected token " + tokenTypeDesc(expType) + "; got " + tokenTypeDesc(actType) + ".");
    }

    protected static void assertTokenType(int expType, int actType, XMLStreamReader sr) {
        if (expType == actType) {
            return;
        }
        Assert.fail("Expected token " + tokenTypeDesc(expType) + "; got " + tokenTypeDesc(actType, sr) + ".");
    }

    protected static void assertTextualTokenType(int actType) {
        if (actType != CHARACTERS && actType != SPACE && actType != CDATA) {
            Assert.fail("Expected textual token (CHARACTERS, SPACE or CDATA)" + "; got " + tokenTypeDesc(actType) + ".");
        }
    }

    protected static void failStrings(String msg, String exp, String act) {
        // !!! TODO: Indicate position where Strings differ
        Assert.fail(msg + ": expected " + quotedPrintable(exp) + ", got " + quotedPrintable(act));
    }

    /**
     * Specific method makes sense, since earlier it was not clear whether null
     * or empty string (or perhaps both) would be the right answer when there is
     * no prefix.
     * <p>
     * However: as per javadocs of {@link XMLStreamReader#getPrefix}, from JDK
     * 1.6 indicate, the current understanding is that <b>null</b> is the
     * ultimate right answer here.
     */
    protected static void assertNoPrefix(XMLStreamReader sr) throws XMLStreamException {
        String prefix = sr.getPrefix();
        if (prefix != null) {
            if (prefix.length() != 0) {
                Assert.fail("Current element should not have a prefix: got '" + prefix + "'");
            } else {
                Assert.fail("Expected null to signify missing prefix (see XMLStreamReader#getPrefix() JavaDocs): got empty String");
            }
        }
    }

    protected static void assertNoAttrPrefix(String attrPrefix) throws XMLStreamException {
        if (attrPrefix != null) {
            if (attrPrefix.length() != 0) {
                Assert.fail("Attribute should not have a prefix: got '" + attrPrefix + "'");
            } else {
                Assert.fail("Expected null to signify missing attribute prefix (see XMLStreamReader#getAttributePrefix() JavaDocs): got empty String");
            }
        }
    }

    /**
     * Similar to {@link #assertNoPrefix}, but here we do know that unbound
     * namespace URI should be indicated as empty String.
     */
    protected static void assertNoNsURI(XMLStreamReader sr) throws XMLStreamException {
        String uri = sr.getNamespaceURI();
        if (uri == null) {
            Assert.fail("Expected empty String to indicate \"no namespace\": got null");
        } else if (uri.length() != 0) {
            Assert.fail("Expected empty String to indicate \"no namespace\": got '" + uri + "'");
        }
    }

    protected static void assertNoAttrNamespace(String attrNsURI) throws XMLStreamException {
        if (attrNsURI == null) {
            // refer to 6903561; accept null for now.
            // fail("Expected empty String to indicate \"no namespace\" (for attribute): got null");
        } else if (attrNsURI.length() != 0) {
            Assert.fail("Expected empty String to indicate \"no namespace\" (for attribute): got '" + attrNsURI + "'");
        }
    }

    protected static void assertNoPrefixOrNs(XMLStreamReader sr) throws XMLStreamException {
        assertNoPrefix(sr);
        assertNoNsURI(sr);
    }

    /**
     * Helper assertion that assert that the String is either null or empty
     * ("").
     */
    protected static void assertNullOrEmpty(String str) {
        if (str != null && str.length() > 0) {
            Assert.fail("Expected String to be empty or null; was '" + str + "' (length " + str.length() + ")");
        }
    }

    /*
     * ////////////////////////////////////////////////// // Debug/output
     * helpers //////////////////////////////////////////////////
     */

    protected static String tokenTypeDesc(int tt) {
        String desc = (String) mTokenTypes.get(new Integer(tt));
        if (desc == null) {
            return "[" + tt + "]";
        }
        return desc;
    }

    protected static String tokenTypeDesc(XMLEvent evt) {
        return tokenTypeDesc(evt.getEventType());
    }

    final static int MAX_DESC_TEXT_CHARS = 8;

    protected static String tokenTypeDesc(int tt, XMLStreamReader sr) {
        String desc = tokenTypeDesc(tt);
        // Let's show first 8 chars or so...
        if (tt == CHARACTERS || tt == SPACE || tt == CDATA) {
            String str = sr.getText();
            if (str.length() > MAX_DESC_TEXT_CHARS) {
                desc = "\"" + str.substring(0, MAX_DESC_TEXT_CHARS) + "\"[...]";
            } else {
                desc = "\"" + desc + "\"";
            }
            desc = " (" + desc + ")";
        }
        return desc;
    }

    protected static String valueDesc(String value) {
        if (value == null) {
            return "[NULL]";
        }
        return "\"" + value + "\"";
    }

    protected static String printable(char ch) {
        if (ch == '\n') {
            return "\\n";
        }
        if (ch == '\r') {
            return "\\r";
        }
        if (ch == '\t') {
            return "\\t";
        }
        if (ch == ' ') {
            return "_";
        }
        if (ch > 127 || ch < 32) {
            StringBuffer sb = new StringBuffer(6);
            sb.append("\\u");
            String hex = Integer.toHexString((int) ch);
            for (int i = 0, len = 4 - hex.length(); i < len; i++) {
                sb.append('0');
            }
            sb.append(hex);
            return sb.toString();
        }
        return null;
    }

    protected static String printable(String str) {
        if (str == null || str.length() == 0) {
            return str;
        }

        int len = str.length();
        StringBuffer sb = new StringBuffer(len + 64);
        for (int i = 0; i < len; ++i) {
            char c = str.charAt(i);
            String res = printable(c);
            if (res == null) {
                sb.append(c);
            } else {
                sb.append(res);
            }
        }
        return sb.toString();
    }

    protected static String quotedPrintable(String str) {
        if (str == null || str.length() == 0) {
            return "[0]''";
        }
        return "[len: " + str.length() + "] '" + printable(str) + "'";
    }

    protected void reportNADueToProperty(String method, String prop) {
        String clsName = getClass().getName();
        /*
         * 27-Sep-2005, TSa: Should probably use some other mechanism for
         * reporting this. Does JUnit have something applicable?
         */
        System.err.println("Skipping " + clsName + "#" + method + ": property '" + prop + "' (or one of its values) not supported.");
    }

    protected void reportNADueToNS(String method) {
        reportNADueToProperty(method, "IS_NAMESPACE_AWARE");
    }

    protected void reportNADueToExtEnt(String method) {
        reportNADueToProperty(method, "IS_SUPPORTING_EXTERNAL_ENTITIES");
    }

    protected void reportNADueToEntityExpansion(String method, int type) {
        String clsName = getClass().getName();
        String msg = (type > 0) ? " (next event: " + tokenTypeDesc(type) + ")" : "";
        System.err.println("Skipping " + clsName + "#" + method + ": entity expansion does not seem to be functioning properly" + msg + ".");
    }
}
