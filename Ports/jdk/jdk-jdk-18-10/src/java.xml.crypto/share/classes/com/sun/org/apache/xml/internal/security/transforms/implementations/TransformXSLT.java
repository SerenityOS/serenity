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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import com.sun.org.apache.xml.internal.security.exceptions.XMLSecurityException;
import com.sun.org.apache.xml.internal.security.signature.XMLSignatureInput;
import com.sun.org.apache.xml.internal.security.transforms.TransformSpi;
import com.sun.org.apache.xml.internal.security.transforms.TransformationException;
import com.sun.org.apache.xml.internal.security.transforms.Transforms;
import com.sun.org.apache.xml.internal.security.utils.XMLUtils;
import org.w3c.dom.Element;

/**
 * Class TransformXSLT
 *
 * Implements the {@code http://www.w3.org/TR/1999/REC-xslt-19991116}
 * transform.
 *
 */
public class TransformXSLT extends TransformSpi {

    static final String XSLTSpecNS = "http://www.w3.org/1999/XSL/Transform";
    static final String defaultXSLTSpecNSprefix = "xslt";
    static final String XSLTSTYLESHEET = "stylesheet";

    private static final com.sun.org.slf4j.internal.Logger LOG =
        com.sun.org.slf4j.internal.LoggerFactory.getLogger(TransformXSLT.class);

    /**
     * {@inheritDoc}
     */
    @Override
    protected String engineGetURI() {
        return Transforms.TRANSFORM_XSLT;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected XMLSignatureInput enginePerformTransform(
        XMLSignatureInput input, OutputStream baos, Element transformElement,
        String baseURI, boolean secureValidation
    ) throws IOException, TransformationException {
        try {
            Element xsltElement =
                XMLUtils.selectNode(transformElement.getFirstChild(), XSLTSpecNS, "stylesheet", 0);
            if (xsltElement == null) {
                xsltElement =
                    XMLUtils.selectNode(transformElement.getFirstChild(), XSLTSpecNS, "transform", 0);
            }
            if (xsltElement == null) {
                Object[] exArgs = { "xslt:stylesheet", "Transform" };

                throw new TransformationException("xml.WrongContent", exArgs);
            }

            TransformerFactory tFactory = TransformerFactory.newInstance();
            // Process XSLT stylesheets in a secure manner
            tFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, Boolean.TRUE);
            if (secureValidation) {
                try {
                    tFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
                    tFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
                } catch (IllegalArgumentException ex) {
                    // ignore
                }
            }

            /*
             * This transform requires an octet stream as input. If the actual
             * input is an XPath node-set, then the signature application should
             * attempt to convert it to octets (apply Canonical XML]) as described
             * in the Reference Processing Model (section 4.3.3.2).
             */
            Source stylesheet;

            /*
             * This complicated transformation of the stylesheet itself is necessary
             * because of the need to get the pure style sheet. If we simply say
             * Source stylesheet = new DOMSource(this.xsltElement);
             * whereby this.xsltElement is not the rootElement of the Document,
             * this causes problems;
             * so we convert the stylesheet to byte[] and use this as input stream
             */
            {
                try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
                    Transformer transformer = tFactory.newTransformer();
                    DOMSource source = new DOMSource(xsltElement);
                    StreamResult result = new StreamResult(os);

                    transformer.transform(source, result);

                    stylesheet =
                        new StreamSource(new ByteArrayInputStream(os.toByteArray()));
                }
            }

            Transformer transformer = tFactory.newTransformer(stylesheet);

            // Force Xalan to use \n as line separator on all OSes. This
            // avoids OS specific signature validation failures due to line
            // separator differences in the transformed output. Unfortunately,
            // this is not a standard JAXP property so will not work with non-Xalan
            // implementations.
            try {
                transformer.setOutputProperty("{http://xml.apache.org/xalan}line-separator", "\n");
            } catch (Exception e) {
                LOG.warn("Unable to set Xalan line-separator property: " + e.getMessage());
            }

            try (InputStream is = new ByteArrayInputStream(input.getBytes())) {
                Source xmlSource = new StreamSource(is);
                if (baos == null) {
                    try (ByteArrayOutputStream baos1 = new ByteArrayOutputStream()) {
                        StreamResult outputTarget = new StreamResult(baos1);
                        transformer.transform(xmlSource, outputTarget);
                        XMLSignatureInput output = new XMLSignatureInput(baos1.toByteArray());
                        output.setSecureValidation(secureValidation);
                        return output;
                    }
                }
                StreamResult outputTarget = new StreamResult(baos);

                transformer.transform(xmlSource, outputTarget);
            }
            XMLSignatureInput output = new XMLSignatureInput((byte[])null);
            output.setSecureValidation(secureValidation);
            output.setOutputStream(baos);
            return output;
        } catch (XMLSecurityException | TransformerException ex) {
            throw new TransformationException(ex);
        }
    }
}
