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

import java.security.cert.X509Certificate;

import com.sun.org.apache.xml.internal.security.exceptions.XMLSecurityException;
import com.sun.org.apache.xml.internal.security.utils.Constants;
import com.sun.org.apache.xml.internal.security.utils.RFC2253Parser;
import com.sun.org.apache.xml.internal.security.utils.SignatureElementProxy;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 */
public class XMLX509SubjectName extends SignatureElementProxy implements XMLX509DataContent {

    /**
     * Constructor X509SubjectName
     *
     * @param element
     * @param baseURI
     * @throws XMLSecurityException
     */
    public XMLX509SubjectName(Element element, String baseURI)
        throws XMLSecurityException {
        super(element, baseURI);
    }

    /**
     * Constructor X509SubjectName
     *
     * @param doc
     * @param X509SubjectNameString
     */
    public XMLX509SubjectName(Document doc, String X509SubjectNameString) {
        super(doc);

        this.addText(X509SubjectNameString);
    }

    /**
     * Constructor XMLX509SubjectName
     *
     * @param doc
     * @param x509certificate
     */
    public XMLX509SubjectName(Document doc, X509Certificate x509certificate) {
        this(doc, x509certificate.getSubjectX500Principal().getName());
    }

    /**
     * Method getSubjectName
     *
     *
     * @return the subject name
     */
    public String getSubjectName() {
        return RFC2253Parser.normalize(this.getTextFromTextChild());
    }

    /** {@inheritDoc} */
    public boolean equals(Object obj) {
        if (!(obj instanceof XMLX509SubjectName)) {
            return false;
        }

        XMLX509SubjectName other = (XMLX509SubjectName) obj;
        String otherSubject = other.getSubjectName();
        String thisSubject = this.getSubjectName();

        return thisSubject.equals(otherSubject);
    }

    public int hashCode() {
        int result = 17;
        result = 31 * result + this.getSubjectName().hashCode();
        return result;
    }

    /** {@inheritDoc} */
    public String getBaseLocalName() {
        return Constants._TAG_X509SUBJECTNAME;
    }
}
