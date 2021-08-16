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

import java.io.IOException;
import java.io.OutputStream;

import com.sun.org.apache.xml.internal.security.c14n.CanonicalizationException;
import com.sun.org.apache.xml.internal.security.signature.XMLSignatureInput;
import com.sun.org.apache.xml.internal.security.transforms.TransformSpi;
import com.sun.org.apache.xml.internal.security.transforms.TransformationException;
import com.sun.org.apache.xml.internal.security.transforms.Transforms;
import com.sun.org.apache.xml.internal.security.utils.XMLUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

import com.sun.org.apache.xml.internal.security.utils.JavaUtils;

/**
 * Implements the {@code http://www.w3.org/2000/09/xmldsig#base64} decoding
 * transform.
 *
 * <p>The normative specification for base64 decoding transforms is
 * <A HREF="http://www.w3.org/TR/2001/CR-xmldsig-core-20010419/#ref-MIME">[MIME]</A>.
 * The base64 Transform element has no content. The input
 * is decoded by the algorithms. This transform is useful if an
 * application needs to sign the raw data associated with the encoded
 * content of an element. </p>
 *
 * <p>This transform requires an octet stream for input.
 * If an XPath node-set (or sufficiently functional alternative) is
 * given as input, then it is converted to an octet stream by
 * performing operations LOGically equivalent to 1) applying an XPath
 * transform with expression self::text(), then 2) taking the string-value
 * of the node-set. Thus, if an XML element is identified by a barename
 * XPointer in the Reference URI, and its content consists solely of base64
 * encoded character data, then this transform automatically strips away the
 * start and end tags of the identified element and any of its descendant
 * elements as well as any descendant comments and processing instructions.
 * The output of this transform is an octet stream.</p>
 *
 */
public class TransformBase64Decode extends TransformSpi {

    /**
     * {@inheritDoc}
     */
    @Override
    protected String engineGetURI() {
        return Transforms.TRANSFORM_BASE64_DECODE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected XMLSignatureInput enginePerformTransform(
        XMLSignatureInput input, OutputStream os, Element transformElement,
        String baseURI, boolean secureValidation
    ) throws IOException, CanonicalizationException, TransformationException {
        if (input.isElement()) {
            Node el = input.getSubNode();
            if (input.getSubNode().getNodeType() == Node.TEXT_NODE) {
                el = el.getParentNode();
            }
            StringBuilder sb = new StringBuilder();
            traverseElement((Element)el, sb);
            if (os == null) {
                byte[] decodedBytes = XMLUtils.decode(sb.toString());
                XMLSignatureInput output = new XMLSignatureInput(decodedBytes);
                output.setSecureValidation(secureValidation);
                return output;
            }
            byte[] bytes = XMLUtils.decode(sb.toString());
            os.write(bytes);
            XMLSignatureInput output = new XMLSignatureInput((byte[])null);
            output.setSecureValidation(secureValidation);
            output.setOutputStream(os);
            return output;
        } else if (input.isOctetStream() || input.isNodeSet()) {
            if (os == null) {
                byte[] base64Bytes = input.getBytes();
                byte[] decodedBytes = XMLUtils.decode(base64Bytes);
                XMLSignatureInput output = new XMLSignatureInput(decodedBytes);
                output.setSecureValidation(secureValidation);
                return output;
            }
            if (input.isByteArray() || input.isNodeSet()) {
                byte[] bytes = XMLUtils.decode(input.getBytes());
                os.write(bytes);
            } else {
                byte[] inputBytes = JavaUtils.getBytesFromStream(input.getOctetStreamReal());
                byte[] bytes = XMLUtils.decode(inputBytes);
                os.write(bytes);
            }
            XMLSignatureInput output = new XMLSignatureInput((byte[])null);
            output.setSecureValidation(secureValidation);
            output.setOutputStream(os);
            return output;
        }

        throw new TransformationException("empty", new Object[] {"Unrecognized XMLSignatureInput state"});
    }

    private void traverseElement(Element node, StringBuilder sb) {
        Node sibling = node.getFirstChild();
        while (sibling != null) {
            if (Node.ELEMENT_NODE == sibling.getNodeType()) {
                traverseElement((Element)sibling, sb);
            } else if (Node.TEXT_NODE == sibling.getNodeType()) {
                sb.append(((Text)sibling).getData());
            }
            sibling = sibling.getNextSibling();
        }
    }
}
