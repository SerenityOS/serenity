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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import com.sun.org.apache.xml.internal.security.c14n.CanonicalizationException;
import com.sun.org.apache.xml.internal.security.c14n.implementations.Canonicalizer20010315Excl;
import com.sun.org.apache.xml.internal.security.c14n.implementations.Canonicalizer20010315ExclOmitComments;
import com.sun.org.apache.xml.internal.security.exceptions.XMLSecurityException;
import com.sun.org.apache.xml.internal.security.signature.XMLSignatureInput;
import com.sun.org.apache.xml.internal.security.transforms.TransformSpi;
import com.sun.org.apache.xml.internal.security.transforms.Transforms;
import com.sun.org.apache.xml.internal.security.transforms.params.InclusiveNamespaces;
import com.sun.org.apache.xml.internal.security.utils.XMLUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Class TransformC14NExclusive
 *
 */
public class TransformC14NExclusive extends TransformSpi {

    /**
     * {@inheritDoc}
     */
    @Override
    protected String engineGetURI() {
        return Transforms.TRANSFORM_C14N_EXCL_OMIT_COMMENTS;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected XMLSignatureInput enginePerformTransform(
        XMLSignatureInput input, OutputStream os, Element transformElement,
        String baseURI, boolean secureValidation
    ) throws CanonicalizationException {
        try {
            String inclusiveNamespaces = null;

            if (length(transformElement,
                InclusiveNamespaces.ExclusiveCanonicalizationNamespace,
                InclusiveNamespaces._TAG_EC_INCLUSIVENAMESPACES) == 1
            ) {
                Element inclusiveElement =
                    XMLUtils.selectNode(
                        transformElement.getFirstChild(),
                        InclusiveNamespaces.ExclusiveCanonicalizationNamespace,
                        InclusiveNamespaces._TAG_EC_INCLUSIVENAMESPACES,
                        0
                    );

                inclusiveNamespaces =
                    new InclusiveNamespaces(
                        inclusiveElement, baseURI).getInclusiveNamespaces();
            }

            Canonicalizer20010315Excl c14n = getCanonicalizer();

            if (os == null) {
                try (ByteArrayOutputStream writer = new ByteArrayOutputStream()) {
                    c14n.engineCanonicalize(input, inclusiveNamespaces, writer, secureValidation);
                    writer.flush();
                    XMLSignatureInput output = new XMLSignatureInput(writer.toByteArray());
                    output.setSecureValidation(secureValidation);
                    return output;
                } catch (IOException ex) {
                    throw new CanonicalizationException("empty", new Object[] {ex.getMessage()});
                }
            } else {
                c14n.engineCanonicalize(input, inclusiveNamespaces, os, secureValidation);
                XMLSignatureInput output = new XMLSignatureInput((byte[])null);
                output.setSecureValidation(secureValidation);
                output.setOutputStream(os);
                return output;
            }
        } catch (XMLSecurityException ex) {
            throw new CanonicalizationException(ex);
        }
    }

    protected Canonicalizer20010315Excl getCanonicalizer() {
        return new Canonicalizer20010315ExclOmitComments();
    }

    /**
     * Method length
     *
     * @param namespace
     * @param localname
     * @return the number of elements {namespace}:localname under this element
     */
    private int length(Element element, String namespace, String localname) {
        int number = 0;
        Node sibling = element.getFirstChild();
        while (sibling != null) {
            if (localname.equals(sibling.getLocalName())
                && namespace.equals(sibling.getNamespaceURI())) {
                number++;
            }
            sibling = sibling.getNextSibling();
        }
        return number;
    }
}
