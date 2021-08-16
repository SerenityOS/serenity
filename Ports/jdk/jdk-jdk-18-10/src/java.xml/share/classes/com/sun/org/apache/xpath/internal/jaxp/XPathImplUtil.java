/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package com.sun.org.apache.xpath.internal.jaxp;

import com.sun.org.apache.xalan.internal.res.XSLMessages;
import com.sun.org.apache.xml.internal.dtm.DTM;
import com.sun.org.apache.xpath.internal.axes.LocPathIterator;
import com.sun.org.apache.xpath.internal.objects.XObject;
import com.sun.org.apache.xpath.internal.res.XPATHErrorResources;
import java.io.IOException;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathEvaluationResult;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFunctionResolver;
import javax.xml.xpath.XPathNodes;
import javax.xml.xpath.XPathVariableResolver;
import jdk.xml.internal.JdkXmlFeatures;
import jdk.xml.internal.JdkXmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.traversal.NodeIterator;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * This class contains several utility methods used by XPathImpl and
 * XPathExpressionImpl
 */
class XPathImplUtil {
    XPathFunctionResolver functionResolver;
    XPathVariableResolver variableResolver;
    JAXPPrefixResolver prefixResolver;
    boolean overrideDefaultParser;
    // By default Extension Functions are allowed in XPath Expressions. If
    // Secure Processing Feature is set on XPathFactory then the invocation of
    // extensions function need to throw XPathFunctionException
    boolean featureSecureProcessing = false;
    JdkXmlFeatures featureManager;

    /**
     * Evaluate an XPath context using the internal XPath engine
     * @param contextItem The XPath context
     * @param xpath The internal XPath engine
     * @return an XObject
     * @throws javax.xml.transform.TransformerException If the expression cannot be evaluated.
     */
    XObject eval(Object contextItem, com.sun.org.apache.xpath.internal.XPath xpath)
            throws javax.xml.transform.TransformerException {
        com.sun.org.apache.xpath.internal.XPathContext xpathSupport;
        if (contextItem == null && xpath.getExpression() instanceof LocPathIterator) {
            // the operation must have no dependency on the context that is null
            throw new TransformerException(XSLMessages.createXPATHMessage(
                    XPATHErrorResources.ER_CONTEXT_CAN_NOT_BE_NULL,
                    new Object[] {}));
        }
        if (functionResolver != null) {
            JAXPExtensionsProvider jep = new JAXPExtensionsProvider(
                    functionResolver, featureSecureProcessing, featureManager);
            xpathSupport = new com.sun.org.apache.xpath.internal.XPathContext(jep);
        } else {
            xpathSupport = new com.sun.org.apache.xpath.internal.XPathContext();
        }

        xpathSupport.setVarStack(new JAXPVariableStack(variableResolver));
        XObject xobj;

        Node contextNode = (Node)contextItem;
        // We always need to have a ContextNode with Xalan XPath implementation
        // To allow simple expression evaluation like 1+1 we are setting
        // dummy Document as Context Node
        if (contextNode == null) {
            xobj = xpath.execute(xpathSupport, DTM.NULL, prefixResolver);
        } else {
            xobj = xpath.execute(xpathSupport, contextNode, prefixResolver);
        }

        return xobj;
    }

    /**
     * Parse the input source and return a Document.
     * @param source The {@code InputSource} of the document
     * @return a DOM Document
     * @throws XPathExpressionException if there is an error parsing the source.
     */
    Document getDocument(InputSource source)
        throws XPathExpressionException {
        requireNonNull(source, "Source");
        try {
            // we'd really like to cache those DocumentBuilders, but we can't because:
            // 1. thread safety. parsers are not thread-safe, so at least
            //    we need one instance per a thread.
            // 2. parsers are non-reentrant, so now we are looking at having a
            // pool of parsers.
            // 3. then the class loading issue. The look-up procedure of
            //    DocumentBuilderFactory.newInstance() depends on context class loader
            //    and system properties, which may change during the execution of JVM.
            //
            // so we really have to create a fresh DocumentBuilder every time we need one
            // - KK
            DocumentBuilderFactory dbf = JdkXmlUtils.getDOMFactory(overrideDefaultParser);
            return dbf.newDocumentBuilder().parse(source);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new XPathExpressionException (e);
        }
    }

    /**
     * Get result depending on the QName type defined in XPathConstants
     * @param resultObject the result of an evaluation
     * @param returnType the return type
     * @return result per the return type
     * @throws TransformerException if the result can not be converted to
     * the specified return type.
     */
    Object getResultAsType(XObject resultObject, QName returnType)
        throws TransformerException {
        // XPathConstants.STRING
        if (returnType.equals(XPathConstants.STRING)) {
            return resultObject.str();
        }
        // XPathConstants.NUMBER
        if (returnType.equals(XPathConstants.NUMBER)) {
            return resultObject.num();
        }
        // XPathConstants.BOOLEAN
        if (returnType.equals(XPathConstants.BOOLEAN)) {
            return resultObject.bool();
        }
        // XPathConstants.NODESET ---ORdered, UNOrdered???
        if (returnType.equals(XPathConstants.NODESET)) {
            return resultObject.nodelist();
        }
        // XPathConstants.NODE
        if (returnType.equals(XPathConstants.NODE)) {
            NodeIterator ni = resultObject.nodeset();
            //Return the first node, or null
            return ni.nextNode();
        }
        // If isSupported check is already done then the execution path
        // shouldn't come here. Being defensive
        String fmsg = XSLMessages.createXPATHMessage(
                XPATHErrorResources.ER_UNSUPPORTED_RETURN_TYPE,
                new Object[] { returnType.toString()});
        throw new IllegalArgumentException (fmsg);
    }

    /**
     * Construct an XPathExpressionResult object based on the result of the
     * evaluation and cast to the specified class type.
     * @param <T> The class type
     * @param resultObject the result of an evaluation
     * @param type The class type expected to be returned by the XPath expression.
     * @return an instance of the specified type or null if the XObject returned
     * an UNKNOWN object type.
     * @throws TransformerException if there is an error converting the result
     * to the specified type. It's unlikely to happen. This is mostly needed
     * by the internal XPath engine.
     */
    <T> T getXPathResult(XObject resultObject, Class<T> type)
            throws TransformerException {
        int resultType = resultObject.getType();

        switch (resultType) {
            case XObject.CLASS_BOOLEAN :
                return type.cast(new XPathResultImpl<>(resultObject, Boolean.class));
            case XObject.CLASS_NUMBER :
                return type.cast(new XPathResultImpl<>(resultObject, Double.class));
            case XObject.CLASS_STRING :
                return type.cast(new XPathResultImpl<>(resultObject, String.class));
            case XObject.CLASS_NODESET :
                return type.cast(new XPathResultImpl<>(resultObject, XPathNodes.class));
            case XObject.CLASS_RTREEFRAG :  //NODE
                return type.cast(new XPathResultImpl<>(resultObject, Node.class));
        }

        return null;
    }

    /**
     * Check whether or not the specified type is supported
     * @param <T> The class type
     * @param type The type to be checked
     * @throws IllegalArgumentException if the type is not supported
     */
    <T> void isSupportedClassType(Class<T> type) {
        requireNonNull(type, "The class type");
        if (type.isAssignableFrom(Boolean.class) ||
                type.isAssignableFrom(Double.class) ||
                type.isAssignableFrom(Integer.class) ||
                type.isAssignableFrom(Long.class) ||
                type.isAssignableFrom(String.class) ||
                type.isAssignableFrom(XPathNodes.class) ||
                type.isAssignableFrom(Node.class) ||
                type.isAssignableFrom(XPathEvaluationResult.class)) {
            return;
        }
        String fmsg = XSLMessages.createXPATHMessage(
                XPATHErrorResources.ER_UNSUPPORTED_RETURN_TYPE,
                new Object[] { type.toString() });
        throw new IllegalArgumentException (fmsg);
    }

    /**
     * Check if the requested returnType is supported.
     * @param returnType the return type
     * @throws IllegalArgumentException if the return type is not supported
     */
    void isSupported(QName returnType) {
        requireNonNull(returnType, "returnType");
        if (returnType.equals(XPathConstants.STRING) ||
                returnType.equals(XPathConstants.NUMBER) ||
                returnType.equals(XPathConstants.BOOLEAN) ||
                returnType.equals(XPathConstants.NODE) ||
                returnType.equals(XPathConstants.NODESET)) {
            return;
        }
        String fmsg = XSLMessages.createXPATHMessage(
                XPATHErrorResources.ER_UNSUPPORTED_RETURN_TYPE,
                new Object[] { returnType.toString() });
        throw new IllegalArgumentException (fmsg);
     }

    /**
     * Checks that the specified parameter is not {@code null}.
     *
     * @param <T> the type of the reference
     * @param param the parameter to check for nullity
     * @param paramName the parameter name
     * @throws NullPointerException if {@code param} is {@code null}
     */
    <T> void requireNonNull(T param, String paramName) {
        if (param == null) {
            String fmsg = XSLMessages.createXPATHMessage(
                    XPATHErrorResources.ER_ARG_CANNOT_BE_NULL,
                    new Object[] {paramName});
            throw new NullPointerException (fmsg);
        }
    }
}
