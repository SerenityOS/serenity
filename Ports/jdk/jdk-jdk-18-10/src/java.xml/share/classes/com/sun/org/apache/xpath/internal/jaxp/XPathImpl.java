/*
 * Copyright (c) 2003, 2017, Oracle and/or its affiliates. All rights reserved.
 */
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

package com.sun.org.apache.xpath.internal.jaxp;

import com.sun.org.apache.xpath.internal.*;
import com.sun.org.apache.xpath.internal.objects.XObject;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathEvaluationResult;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFunctionResolver;
import javax.xml.xpath.XPathVariableResolver;
import jdk.xml.internal.JdkXmlFeatures;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

/**
 * The XPathImpl class provides implementation for the methods defined  in
 * javax.xml.xpath.XPath interface. This provides simple access to the results
 * of an XPath expression.
 *
 * @author  Ramesh Mandava
 *
 * Updated 12/04/2014:
 * New methods: evaluateExpression
 * Refactored to share code with XPathExpressionImpl.
 */
public class XPathImpl extends XPathImplUtil implements javax.xml.xpath.XPath {

    // Private variables
    private XPathVariableResolver origVariableResolver;
    private XPathFunctionResolver origFunctionResolver;
    private NamespaceContext namespaceContext=null;

    XPathImpl(XPathVariableResolver vr, XPathFunctionResolver fr) {
        this(vr, fr, false, new JdkXmlFeatures(false));
    }

    XPathImpl(XPathVariableResolver vr, XPathFunctionResolver fr,
            boolean featureSecureProcessing, JdkXmlFeatures featureManager) {
        this.origVariableResolver = this.variableResolver = vr;
        this.origFunctionResolver = this.functionResolver = fr;
        this.featureSecureProcessing = featureSecureProcessing;
        this.featureManager = featureManager;
        overrideDefaultParser = featureManager.getFeature(
                JdkXmlFeatures.XmlFeature.JDK_OVERRIDE_PARSER);

    }


    //-Override-
    public void setXPathVariableResolver(XPathVariableResolver resolver) {
        requireNonNull(resolver, "XPathVariableResolver");
        this.variableResolver = resolver;
    }

    //-Override-
    public XPathVariableResolver getXPathVariableResolver() {
        return variableResolver;
    }

    //-Override-
    public void setXPathFunctionResolver(XPathFunctionResolver resolver) {
        requireNonNull(resolver, "XPathFunctionResolver");
        this.functionResolver = resolver;
    }

    //-Override-
    public XPathFunctionResolver getXPathFunctionResolver() {
        return functionResolver;
    }

    //-Override-
    public void setNamespaceContext(NamespaceContext nsContext) {
        requireNonNull(nsContext, "NamespaceContext");
        this.namespaceContext = nsContext;
        this.prefixResolver = new JAXPPrefixResolver (nsContext);
    }

    //-Override-
    public NamespaceContext getNamespaceContext() {
        return namespaceContext;
    }

    /**
     * Evaluate an {@code XPath} expression in the specified context.
     * @param expression The XPath expression.
     * @param contextItem The starting context.
     * @return an XObject as the result of evaluating the expression
     * @throws TransformerException if evaluating fails
     */
    private XObject eval(String expression, Object contextItem)
        throws TransformerException {
        requireNonNull(expression, "XPath expression");
        com.sun.org.apache.xpath.internal.XPath xpath = new com.sun.org.apache.xpath.internal.XPath(expression,
            null, prefixResolver, com.sun.org.apache.xpath.internal.XPath.SELECT);

        return eval(contextItem, xpath);
    }

    //-Override-
    public Object evaluate(String expression, Object item, QName returnType)
            throws XPathExpressionException {
        //this check is necessary before calling eval to maintain binary compatibility
        requireNonNull(expression, "XPath expression");
        isSupported(returnType);

        try {

            XObject resultObject = eval(expression, item);
            return getResultAsType(resultObject, returnType);
        } catch (java.lang.NullPointerException npe) {
            // If VariableResolver returns null Or if we get
            // NullPointerException at this stage for some other reason
            // then we have to reurn XPathException
            throw new XPathExpressionException (npe);
        } catch (TransformerException te) {
            Throwable nestedException = te.getException();
            if (nestedException instanceof javax.xml.xpath.XPathFunctionException) {
                throw (javax.xml.xpath.XPathFunctionException)nestedException;
            } else {
                // For any other exceptions we need to throw
                // XPathExpressionException (as per spec)
                throw new XPathExpressionException (te);
            }
        }

    }

    //-Override-
    public String evaluate(String expression, Object item)
        throws XPathExpressionException {
        return (String)this.evaluate(expression, item, XPathConstants.STRING);
    }

    //-Override-
    public XPathExpression compile(String expression)
        throws XPathExpressionException {
        requireNonNull(expression, "XPath expression");
        try {
            com.sun.org.apache.xpath.internal.XPath xpath = new XPath (expression, null,
                    prefixResolver, com.sun.org.apache.xpath.internal.XPath.SELECT);
            // Can have errorListener
            XPathExpressionImpl ximpl = new XPathExpressionImpl (xpath,
                    prefixResolver, functionResolver, variableResolver,
                    featureSecureProcessing, featureManager);
            return ximpl;
        } catch (TransformerException te) {
            throw new XPathExpressionException (te) ;
        }
    }

    //-Override-
    public Object evaluate(String expression, InputSource source,
            QName returnType) throws XPathExpressionException {
        isSupported(returnType);

        try {
            Document document = getDocument(source);
            XObject resultObject = eval(expression, document);
            return getResultAsType(resultObject, returnType);
        } catch (TransformerException te) {
            Throwable nestedException = te.getException();
            if (nestedException instanceof javax.xml.xpath.XPathFunctionException) {
                throw (javax.xml.xpath.XPathFunctionException)nestedException;
            } else {
                throw new XPathExpressionException (te);
            }
        }
    }

    //-Override-
    public String evaluate(String expression, InputSource source)
        throws XPathExpressionException {
        return (String)this.evaluate(expression, source, XPathConstants.STRING);
    }

    //-Override-
    public void reset() {
        this.variableResolver = this.origVariableResolver;
        this.functionResolver = this.origFunctionResolver;
        this.namespaceContext = null;
    }

    //-Override-
    public <T> T evaluateExpression(String expression, Object item, Class<T> type)
            throws XPathExpressionException {
        isSupportedClassType(type);
        try {
            XObject resultObject = eval(expression, item);
            if (type.isAssignableFrom(XPathEvaluationResult.class)) {
                return getXPathResult(resultObject, type);
            } else {
                return XPathResultImpl.getValue(resultObject, type);
            }
        } catch (TransformerException te) {
            throw new XPathExpressionException (te);
        }
    }

    //-Override-
    public XPathEvaluationResult<?> evaluateExpression(String expression, Object item)
            throws XPathExpressionException {
        return evaluateExpression(expression, item, XPathEvaluationResult.class);
    }

    //-Override-
    public <T> T evaluateExpression(String expression, InputSource source, Class<T> type)
            throws XPathExpressionException {
        Document document = getDocument(source);
        return evaluateExpression(expression, document, type);
    }

    //-Override-
    public XPathEvaluationResult<?> evaluateExpression(String expression, InputSource source)
            throws XPathExpressionException {
        return evaluateExpression(expression, source, XPathEvaluationResult.class);
    }
}
