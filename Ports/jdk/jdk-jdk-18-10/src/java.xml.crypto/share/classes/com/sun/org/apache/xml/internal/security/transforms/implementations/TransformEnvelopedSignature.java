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

import java.io.OutputStream;

import com.sun.org.apache.xml.internal.security.signature.NodeFilter;
import com.sun.org.apache.xml.internal.security.signature.XMLSignatureInput;
import com.sun.org.apache.xml.internal.security.transforms.TransformSpi;
import com.sun.org.apache.xml.internal.security.transforms.TransformationException;
import com.sun.org.apache.xml.internal.security.transforms.Transforms;
import com.sun.org.apache.xml.internal.security.utils.Constants;
import com.sun.org.apache.xml.internal.security.utils.XMLUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Implements the {@code http://www.w3.org/2000/09/xmldsig#enveloped-signature}
 * transform.
 *
 */
public class TransformEnvelopedSignature extends TransformSpi {

    /**
     * {@inheritDoc}
     */
    @Override
    protected String engineGetURI() {
        return Transforms.TRANSFORM_ENVELOPED_SIGNATURE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected XMLSignatureInput enginePerformTransform(
        XMLSignatureInput input, OutputStream os, Element transformElement,
        String baseURI, boolean secureValidation
    ) throws TransformationException {
        /**
         * If the actual input is an octet stream, then the application MUST
         * convert the octet stream to an XPath node-set suitable for use by
         * Canonical XML with Comments. (A subsequent application of the
         * REQUIRED Canonical XML algorithm would strip away these comments.)
         *
         * ...
         *
         * The evaluation of this expression includes all of the document's nodes
         * (including comments) in the node-set representing the octet stream.
         */

        Node signatureElement = searchSignatureElement(transformElement);
        input.setExcludeNode(signatureElement);
        input.addNodeFilter(new EnvelopedNodeFilter(signatureElement));
        return input;
    }

    /**
     * @param signatureElement
     * @return the node that is the signature
     * @throws TransformationException
     */
    private static Node searchSignatureElement(Node signatureElement)
        throws TransformationException {
        boolean found = false;

        while (true) {
            if (signatureElement == null
                || signatureElement.getNodeType() == Node.DOCUMENT_NODE) {
                break;
            }
            Element el = (Element) signatureElement;
            if (el.getNamespaceURI().equals(Constants.SignatureSpecNS)
                && el.getLocalName().equals(Constants._TAG_SIGNATURE)) {
                found = true;
                break;
            }

            signatureElement = signatureElement.getParentNode();
        }

        if (!found) {
            throw new TransformationException(
                "transform.envelopedSignatureTransformNotInSignatureElement");
        }
        return signatureElement;
    }

    static class EnvelopedNodeFilter implements NodeFilter {

        private final Node exclude;

        EnvelopedNodeFilter(Node n) {
            exclude = n;
        }

        public int isNodeIncludeDO(Node n, int level) {
            if (n == exclude) {
                return -1;
            }
            return 1;
        }

        /**
         * @see com.sun.org.apache.xml.internal.security.signature.NodeFilter#isNodeInclude(org.w3c.dom.Node)
         */
        public int isNodeInclude(Node n) {
            if (n == exclude || XMLUtils.isDescendantOrSelf(exclude, n)) {
                return -1;
            }
            return 1;
            //return !XMLUtils.isDescendantOrSelf(exclude, n);
        }
    }
}
