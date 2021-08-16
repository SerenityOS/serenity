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
package com.sun.org.apache.xml.internal.security.transforms.implementations;

import javax.xml.transform.TransformerException;

import com.sun.org.apache.xml.internal.dtm.DTM;
import com.sun.org.apache.xml.internal.security.utils.I18n;
import com.sun.org.apache.xml.internal.security.utils.XMLUtils;
import com.sun.org.apache.xml.internal.utils.QName;
import com.sun.org.apache.xpath.internal.NodeSetDTM;
import com.sun.org.apache.xpath.internal.XPathContext;
import com.sun.org.apache.xpath.internal.functions.Function;
import com.sun.org.apache.xpath.internal.objects.XNodeSet;
import com.sun.org.apache.xpath.internal.objects.XObject;
import com.sun.org.apache.xpath.internal.res.XPATHErrorResources;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.util.List;

/**
 * The 'here()' function returns a node-set containing the attribute or
 * processing instruction node or the parent element of the text node
 * that directly bears the XPath expression.  This expression results
 * in an error if the containing XPath expression does not appear in the
 * same XML document against which the XPath expression is being evaluated.
 *
 * Mainpart is stolen from FuncId.java
 *
 * This does crash under Xalan2.2.D7 and works under Xalan2.2.D9
 *
 * To get this baby to work, a special trick has to be used. The function needs
 * access to the Node where the XPath expression has been defined. This is done
 * by constructing a {@link FuncHere} which has this Node as 'owner'.
 *
 * @see "http://www.w3.org/Signature/Drafts/xmldsig-core/Overview.html#function-here"
 */
public class FuncHere extends Function {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /**
     * The here function returns a node-set containing the attribute or
     * processing instruction node or the parent element of the text node
     * that directly bears the XPath expression.  This expression results
     * in an error if the containing XPath expression does not appear in the
     * same XML document against which the XPath expression is being evaluated.
     *
     * @param xctxt
     * @return the xobject
     * @throws javax.xml.transform.TransformerException
     */
    public XObject execute(XPathContext xctxt) throws TransformerException {

        Node xpathOwnerNode = (Node) xctxt.getOwnerObject();

        if (xpathOwnerNode == null) {
            return null;
        }

        int xpathOwnerNodeDTM = xctxt.getDTMHandleFromNode(xpathOwnerNode);

        int currentNode = xctxt.getCurrentNode();
        DTM dtm = xctxt.getDTM(currentNode);
        int docContext = dtm.getDocument();

        if (DTM.NULL == docContext) {
            error(xctxt, XPATHErrorResources.ER_CONTEXT_HAS_NO_OWNERDOC, null);
        }

        {
            // check whether currentNode and the node containing the XPath expression
            // are in the same document
            Document currentDoc =
                XMLUtils.getOwnerDocument(dtm.getNode(currentNode));
            Document xpathOwnerDoc = XMLUtils.getOwnerDocument(xpathOwnerNode);

            if (currentDoc != xpathOwnerDoc) {
                throw new TransformerException(I18n.translate("xpath.funcHere.documentsDiffer"));
            }
        }

        XNodeSet nodes = new XNodeSet(xctxt.getDTMManager());
        NodeSetDTM nodeSet = nodes.mutableNodeset();

        {
            int hereNode = DTM.NULL;

            switch (dtm.getNodeType(xpathOwnerNodeDTM)) {

            case Node.ATTRIBUTE_NODE :
            case Node.PROCESSING_INSTRUCTION_NODE : {
                // returns a node-set containing the attribute /  processing instruction node
                hereNode = xpathOwnerNodeDTM;

                nodeSet.addNode(hereNode);

                break;
            }
            case Node.TEXT_NODE : {
                // returns a node-set containing the parent element of the
                // text node that directly bears the XPath expression
                hereNode = dtm.getParent(xpathOwnerNodeDTM);

                nodeSet.addNode(hereNode);

                break;
            }
            default :
                break;
            }
        }

        /** $todo$ Do I have to do this detach() call? */
        nodeSet.detach();

        return nodes;
    }

    /**
     * No arguments to process, so this does nothing.
     * @param vars
     * @param globalsSize
     */
    public void fixupVariables(List<QName> vars, int globalsSize) { //NOPMD
        // do nothing
    }
}
