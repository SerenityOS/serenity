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
package com.sun.org.apache.xml.internal.security.utils.resolver.implementations;

import com.sun.org.apache.xml.internal.security.signature.XMLSignatureInput;
import com.sun.org.apache.xml.internal.security.utils.XMLUtils;
import com.sun.org.apache.xml.internal.security.utils.resolver.ResourceResolverContext;
import com.sun.org.apache.xml.internal.security.utils.resolver.ResourceResolverException;
import com.sun.org.apache.xml.internal.security.utils.resolver.ResourceResolverSpi;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Handles barename XPointer Reference URIs.
 * <p></p>
 * To retain comments while selecting an element by an identifier ID,
 * use the following full XPointer: URI='#xpointer(id('ID'))'.
 * <p></p>
 * To retain comments while selecting the entire document,
 * use the following full XPointer: URI='#xpointer(/)'.
 * This XPointer contains a simple XPath expression that includes
 * the root node, which the second to last step above replaces with all
 * nodes of the parse tree (all descendants, plus all attributes,
 * plus all namespaces nodes).
 *
 */
public class ResolverXPointer extends ResourceResolverSpi {

    private static final com.sun.org.slf4j.internal.Logger LOG =
        com.sun.org.slf4j.internal.LoggerFactory.getLogger(ResolverXPointer.class);

    private static final String XP = "#xpointer(id(";
    private static final int XP_LENGTH = XP.length();

    /**
     * {@inheritDoc}
     */
    @Override
    public XMLSignatureInput engineResolveURI(ResourceResolverContext context)
        throws ResourceResolverException {

        Node resultNode = null;
        Document doc = context.attr.getOwnerElement().getOwnerDocument();

        if (isXPointerSlash(context.uriToResolve)) {
            resultNode = doc;
        } else if (isXPointerId(context.uriToResolve)) {
            String id = getXPointerId(context.uriToResolve);
            resultNode = doc.getElementById(id);

            if (context.secureValidation) {
                Element start = context.attr.getOwnerDocument().getDocumentElement();
                if (!XMLUtils.protectAgainstWrappingAttack(start, id)) {
                    Object[] exArgs = { id };
                    throw new ResourceResolverException(
                        "signature.Verification.MultipleIDs", exArgs, context.uriToResolve, context.baseUri
                    );
                }
            }

            if (resultNode == null) {
                Object[] exArgs = { id };

                throw new ResourceResolverException(
                    "signature.Verification.MissingID", exArgs, context.uriToResolve, context.baseUri
                );
            }
        }

        XMLSignatureInput result = new XMLSignatureInput(resultNode);
        result.setSecureValidation(context.secureValidation);

        result.setMIMEType("text/xml");
        if (context.baseUri != null && context.baseUri.length() > 0) {
            result.setSourceURI(context.baseUri.concat(context.uriToResolve));
        } else {
            result.setSourceURI(context.uriToResolve);
        }

        return result;
    }

    /**
     * {@inheritDoc}
     */
    public boolean engineCanResolveURI(ResourceResolverContext context) {
        return isXPointerSlash(context.uriToResolve) || isXPointerId(context.uriToResolve);
    }

    /**
     * Method isXPointerSlash
     *
     * @param uri
     * @return true if begins with xpointer
     */
    private static boolean isXPointerSlash(String uri) {
        return "#xpointer(/)".equals(uri);
    }

    /**
     * Method isXPointerId
     *
     * @param uri
     * @return whether it has an xpointer id
     */
    private static boolean isXPointerId(String uri) {
        if (uri != null && uri.startsWith(XP) && uri.endsWith("))")) {
            String idPlusDelim = uri.substring(XP_LENGTH, uri.length() - 2);

            int idLen = idPlusDelim.length() -1;
            if (idPlusDelim.charAt(0) == '"' && idPlusDelim.charAt(idLen) == '"'
                || idPlusDelim.charAt(0) == '\'' && idPlusDelim.charAt(idLen) == '\'') {
                LOG.debug("Id = {}", idPlusDelim.substring(1, idLen));
                return true;
            }
        }

        return false;
    }

    /**
     * Method getXPointerId
     *
     * @param uri
     * @return xpointerId to search.
     */
    private static String getXPointerId(String uri) {
        if (uri.startsWith(XP) && uri.endsWith("))")) {
            String idPlusDelim = uri.substring(XP_LENGTH, uri.length() - 2);

            int idLen = idPlusDelim.length() -1;
            if (idPlusDelim.charAt(0) == '"' && idPlusDelim.charAt(idLen) == '"'
                || idPlusDelim.charAt(0) == '\'' && idPlusDelim.charAt(idLen) == '\'') {
                return idPlusDelim.substring(1, idLen);
            }
        }

        return null;
    }
}
