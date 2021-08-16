/*
 * reserved comment block
 * DO NOT REMOVE OR ALTER!
 */
/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.sun.org.apache.xml.internal.security.utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.SourceLocator;
import javax.xml.transform.TransformerException;

import com.sun.org.apache.xml.internal.security.transforms.implementations.FuncHere;
import com.sun.org.apache.xml.internal.utils.PrefixResolver;
import com.sun.org.apache.xml.internal.utils.PrefixResolverDefault;
import com.sun.org.apache.xpath.internal.Expression;
import com.sun.org.apache.xpath.internal.XPath;
import com.sun.org.apache.xpath.internal.XPathContext;
import com.sun.org.apache.xpath.internal.compiler.FunctionTable;
import com.sun.org.apache.xpath.internal.objects.XObject;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * An implementation of XPathAPI using Xalan. This supports the "here()" function defined in the digital
 * signature spec.
 */
class XalanXPathAPI implements XPathAPI {

    private static final com.sun.org.slf4j.internal.Logger LOG =
        com.sun.org.slf4j.internal.LoggerFactory.getLogger(XalanXPathAPI.class);

    private String xpathStr;

    private XPath xpath;

    private static FunctionTable funcTable;

    private static boolean installed;

    private XPathContext context;

    static {
        fixupFunctionTable();
    }


    /**
     *  Use an XPath string to select a nodelist.
     *  XPath namespace prefixes are resolved from the namespaceNode.
     *
     *  @param contextNode The node to start searching from.
     *  @param xpathnode
     *  @param str
     *  @param namespaceNode The node from which prefixes in the XPath will be resolved to namespaces.
     *  @return A NodeIterator, should never be null.
     *
     * @throws TransformerException
     */
    public NodeList selectNodeList(
        Node contextNode, Node xpathnode, String str, Node namespaceNode
    ) throws TransformerException {

        // Execute the XPath, and have it return the result
        XObject list = eval(contextNode, xpathnode, str, namespaceNode);

        // Return a NodeList.
        return list.nodelist();
    }

    /**
     * Evaluate an XPath string and return true if the output is to be included or not.
     *  @param contextNode The node to start searching from.
     *  @param xpathnode The XPath node
     *  @param str The XPath expression
     *  @param namespaceNode The node from which prefixes in the XPath will be resolved to namespaces.
     */
    public boolean evaluate(Node contextNode, Node xpathnode, String str, Node namespaceNode)
        throws TransformerException {
        XObject object = eval(contextNode, xpathnode, str, namespaceNode);
        return object.bool();
    }

    /**
     * Clear any context information from this object
     */
    public void clear() {
        xpathStr = null;
        xpath = null;
        context = null;
    }

    public static boolean isInstalled() {
        return installed;
    }

    private XObject eval(Node contextNode, Node xpathnode, String str, Node namespaceNode)
        throws TransformerException {
        if (context == null) {
            context = new XPathContext(xpathnode);
            context.setSecureProcessing(true);
        }

        // Create an object to resolve namespace prefixes.
        // XPath namespaces are resolved from the input context node's document element
        // if it is a root node, or else the current context node (for lack of a better
        // resolution space, given the simplicity of this sample code).
        Node resolverNode =
            (namespaceNode.getNodeType() == Node.DOCUMENT_NODE)
                ? ((Document) namespaceNode).getDocumentElement() : namespaceNode;
        PrefixResolverDefault prefixResolver = new PrefixResolverDefault(resolverNode);

        if (!str.equals(xpathStr)) {
            if (str.indexOf("here()") > 0) {
                context.reset();
            }
            xpath = createXPath(str, prefixResolver);
            xpathStr = str;
        }

        // Execute the XPath, and have it return the result
        int ctxtNode = context.getDTMHandleFromNode(contextNode);

        return xpath.execute(context, ctxtNode, prefixResolver);
    }

    private XPath createXPath(String str, PrefixResolver prefixResolver) throws TransformerException {
        XPath xpath = null;
        Class<?>[] classes = new Class<?>[]{String.class, SourceLocator.class, PrefixResolver.class, int.class,
                                      ErrorListener.class, FunctionTable.class};
        Object[] objects =
            new Object[]{str, null, prefixResolver, XPath.SELECT, null, funcTable};
        try {
            Constructor<?> constructor = XPath.class.getConstructor(classes);
            xpath = (XPath) constructor.newInstance(objects);
        } catch (Exception ex) {
            LOG.debug(ex.getMessage(), ex);
        }
        if (xpath == null) {
            xpath = new XPath(str, null, prefixResolver, XPath.SELECT, null);
        }
        return xpath;
    }

    private static synchronized void fixupFunctionTable() {
        installed = false;
        if (new FunctionTable().functionAvailable("here")) {
            LOG.debug("Here function already registered");
            installed = true;
            return;
        }
        LOG.debug("Registering Here function");
        /**
         * Try to register our here() implementation as internal function.
         */
        try {
            Class<?>[] args = {String.class, Expression.class};
            Method installFunction = FunctionTable.class.getMethod("installFunction", args);
            if ((installFunction.getModifiers() & Modifier.STATIC) != 0) {
                Object[] params = {"here", new FuncHere()};
                installFunction.invoke(null, params);
                installed = true;
            }
        } catch (Exception ex) {
            LOG.debug("Error installing function using the static installFunction method", ex);
        }
        if (!installed) {
            try {
                funcTable = new FunctionTable();
                Class<?>[] args = {String.class, Class.class};
                Method installFunction = FunctionTable.class.getMethod("installFunction", args);
                Object[] params = {"here", FuncHere.class};
                installFunction.invoke(funcTable, params);
                installed = true;
            } catch (Exception ex) {
                LOG.debug("Error installing function using the static installFunction method", ex);
            }
        }
        if (installed) {
            LOG.debug("Registered class {} for XPath function 'here()' function in internal table", FuncHere.class.getName());
        } else {
            LOG.debug("Unable to register class {} for XPath function 'here()' function in internal table", FuncHere.class.getName());
        }
    }

}
