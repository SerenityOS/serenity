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
package com.sun.org.apache.xml.internal.security.keys.content.x509;

import com.sun.org.apache.xml.internal.security.exceptions.XMLSecurityException;
import com.sun.org.apache.xml.internal.security.utils.Constants;
import com.sun.org.apache.xml.internal.security.utils.SignatureElementProxy;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class XMLX509CRL extends SignatureElementProxy implements XMLX509DataContent {

    /**
     * Constructor XMLX509CRL
     *
     * @param element
     * @param baseURI
     * @throws XMLSecurityException
     */
    public XMLX509CRL(Element element, String baseURI) throws XMLSecurityException {
        super(element, baseURI);
    }

    /**
     * Constructor X509CRL
     *
     * @param doc
     * @param crlBytes
     */
    public XMLX509CRL(Document doc, byte[] crlBytes) {
        super(doc);

        this.addBase64Text(crlBytes);
    }

    /**
     * Method getCRLBytes
     *
     * @return the CRL bytes
     * @throws XMLSecurityException
     */
    public byte[] getCRLBytes() throws XMLSecurityException {
        return this.getBytesFromTextChild();
    }

    /** {@inheritDoc} */
    public String getBaseLocalName() {
        return Constants._TAG_X509CRL;
    }
}
