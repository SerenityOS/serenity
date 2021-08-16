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
package com.sun.org.apache.xml.internal.security.keys.content;

import java.math.BigInteger;
import java.security.cert.X509Certificate;

import com.sun.org.apache.xml.internal.security.exceptions.XMLSecurityException;
import com.sun.org.apache.xml.internal.security.keys.content.x509.XMLX509CRL;
import com.sun.org.apache.xml.internal.security.keys.content.x509.XMLX509Certificate;
import com.sun.org.apache.xml.internal.security.keys.content.x509.XMLX509Digest;
import com.sun.org.apache.xml.internal.security.keys.content.x509.XMLX509IssuerSerial;
import com.sun.org.apache.xml.internal.security.keys.content.x509.XMLX509SKI;
import com.sun.org.apache.xml.internal.security.keys.content.x509.XMLX509SubjectName;
import com.sun.org.apache.xml.internal.security.utils.Constants;
import com.sun.org.apache.xml.internal.security.utils.SignatureElementProxy;
import com.sun.org.apache.xml.internal.security.utils.XMLUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class X509Data extends SignatureElementProxy implements KeyInfoContent {

    private static final com.sun.org.slf4j.internal.Logger LOG =
        com.sun.org.slf4j.internal.LoggerFactory.getLogger(X509Data.class);

    /**
     * Constructor X509Data
     *
     * @param doc
     */
    public X509Data(Document doc) {
        super(doc);

        addReturnToSelf();
    }

    /**
     * Constructor X509Data
     *
     * @param element
     * @param baseURI
     * @throws XMLSecurityException
     */
    public X509Data(Element element, String baseURI) throws XMLSecurityException {
        super(element, baseURI);

        Node sibling = getFirstChild();
        while (sibling != null && sibling.getNodeType() != Node.ELEMENT_NODE) {
            sibling = sibling.getNextSibling();
        }
        if (sibling == null || sibling.getNodeType() != Node.ELEMENT_NODE) {
            /* No Elements found */
            Object[] exArgs = { "Elements", Constants._TAG_X509DATA };
            throw new XMLSecurityException("xml.WrongContent", exArgs);
        }
    }

    /**
     * Method addIssuerSerial
     *
     * @param X509IssuerName
     * @param X509SerialNumber
     */
    public void addIssuerSerial(String X509IssuerName, BigInteger X509SerialNumber) {
        this.add(new XMLX509IssuerSerial(getDocument(), X509IssuerName, X509SerialNumber));
    }

    /**
     * Method addIssuerSerial
     *
     * @param X509IssuerName
     * @param X509SerialNumber
     */
    public void addIssuerSerial(String X509IssuerName, String X509SerialNumber) {
        this.add(new XMLX509IssuerSerial(getDocument(), X509IssuerName, X509SerialNumber));
    }

    /**
     * Method addIssuerSerial
     *
     * @param X509IssuerName
     * @param X509SerialNumber
     */
    public void addIssuerSerial(String X509IssuerName, int X509SerialNumber) {
        this.add(new XMLX509IssuerSerial(getDocument(), X509IssuerName, X509SerialNumber));
    }

    /**
     * Method add
     *
     * @param xmlX509IssuerSerial
     */
    public void add(XMLX509IssuerSerial xmlX509IssuerSerial) {

        appendSelf(xmlX509IssuerSerial);
        addReturnToSelf();
    }

    /**
     * Method addSKI
     *
     * @param skiBytes
     */
    public void addSKI(byte[] skiBytes) {
        this.add(new XMLX509SKI(getDocument(), skiBytes));
    }

    /**
     * Method addSKI
     *
     * @param x509certificate
     * @throws XMLSecurityException
     */
    public void addSKI(X509Certificate x509certificate)
        throws XMLSecurityException {
        this.add(new XMLX509SKI(getDocument(), x509certificate));
    }

    /**
     * Method add
     *
     * @param xmlX509SKI
     */
    public void add(XMLX509SKI xmlX509SKI) {
        appendSelf(xmlX509SKI);
        addReturnToSelf();
    }

    /**
     * Method addSubjectName
     *
     * @param subjectName
     */
    public void addSubjectName(String subjectName) {
        this.add(new XMLX509SubjectName(getDocument(), subjectName));
    }

    /**
     * Method addSubjectName
     *
     * @param x509certificate
     */
    public void addSubjectName(X509Certificate x509certificate) {
        this.add(new XMLX509SubjectName(getDocument(), x509certificate));
    }

    /**
     * Method add
     *
     * @param xmlX509SubjectName
     */
    public void add(XMLX509SubjectName xmlX509SubjectName) {
        appendSelf(xmlX509SubjectName);
        addReturnToSelf();
    }

    /**
     * Method addCertificate
     *
     * @param x509certificate
     * @throws XMLSecurityException
     */
    public void addCertificate(X509Certificate x509certificate)
        throws XMLSecurityException {
        this.add(new XMLX509Certificate(getDocument(), x509certificate));
    }

    /**
     * Method addCertificate
     *
     * @param x509certificateBytes
     */
    public void addCertificate(byte[] x509certificateBytes) {
        this.add(new XMLX509Certificate(getDocument(), x509certificateBytes));
    }

    /**
     * Method add
     *
     * @param xmlX509Certificate
     */
    public void add(XMLX509Certificate xmlX509Certificate) {
        appendSelf(xmlX509Certificate);
        addReturnToSelf();
    }

    /**
     * Method addCRL
     *
     * @param crlBytes
     */
    public void addCRL(byte[] crlBytes) {
        this.add(new XMLX509CRL(getDocument(), crlBytes));
    }

    /**
     * Method add
     *
     * @param xmlX509CRL
     */
    public void add(XMLX509CRL xmlX509CRL) {
        appendSelf(xmlX509CRL);
        addReturnToSelf();
    }

    /**
     * Method addDigest
     *
     * @param x509certificate
     * @param algorithmURI
     * @throws XMLSecurityException
     */
    public void addDigest(X509Certificate x509certificate, String algorithmURI)
        throws XMLSecurityException {
        this.add(new XMLX509Digest(getDocument(), x509certificate, algorithmURI));
    }

    /**
     * Method addDigest
     *
     * @param x509CertificateDigestBytes
     * @param algorithmURI
     */
    public void addDigest(byte[] x509CertificateDigestBytes, String algorithmURI) {
        this.add(new XMLX509Digest(getDocument(), x509CertificateDigestBytes, algorithmURI));
    }

    /**
     * Method add
     *
     * @param xmlX509Digest
     */
    public void add(XMLX509Digest xmlX509Digest) {
        appendSelf(xmlX509Digest);
        addReturnToSelf();
    }

    /**
     * Method addUnknownElement
     *
     * @param element
     */
    public void addUnknownElement(Element element) {
        appendSelf(element);
        addReturnToSelf();
    }

    /**
     * Method lengthIssuerSerial
     *
     * @return the number of IssuerSerial elements in this X509Data
     */
    public int lengthIssuerSerial() {
        return this.length(Constants.SignatureSpecNS, Constants._TAG_X509ISSUERSERIAL);
    }

    /**
     * Method lengthSKI
     *
     * @return the number of SKI elements in this X509Data
     */
    public int lengthSKI() {
        return this.length(Constants.SignatureSpecNS, Constants._TAG_X509SKI);
    }

    /**
     * Method lengthSubjectName
     *
     * @return the number of SubjectName elements in this X509Data
     */
    public int lengthSubjectName() {
        return this.length(Constants.SignatureSpecNS, Constants._TAG_X509SUBJECTNAME);
    }

    /**
     * Method lengthCertificate
     *
     * @return the number of Certificate elements in this X509Data
     */
    public int lengthCertificate() {
        return this.length(Constants.SignatureSpecNS, Constants._TAG_X509CERTIFICATE);
    }

    /**
     * Method lengthCRL
     *
     * @return the number of CRL elements in this X509Data
     */
    public int lengthCRL() {
        return this.length(Constants.SignatureSpecNS, Constants._TAG_X509CRL);
    }

    /**
     * Method lengthDigest
     *
     * @return the number of X509Digest elements in this X509Data
     */
    public int lengthDigest() {
        return this.length(Constants.SignatureSpec11NS, Constants._TAG_X509DIGEST);
    }

    /**
     * Method lengthUnknownElement
     *
     * @return the number of UnknownElement elements in this X509Data
     */
    public int lengthUnknownElement() {
        int result = 0;
        Node n = getFirstChild();
        while (n != null) {
            if (n.getNodeType() == Node.ELEMENT_NODE
                && !n.getNamespaceURI().equals(Constants.SignatureSpecNS)) {
                result++;
            }
            n = n.getNextSibling();
        }

        return result;
    }

    /**
     * Method itemIssuerSerial
     *
     * @param i
     * @return the X509IssuerSerial, null if not present
     * @throws XMLSecurityException
     */
    public XMLX509IssuerSerial itemIssuerSerial(int i) throws XMLSecurityException {
        Element e =
            XMLUtils.selectDsNode(
                getFirstChild(), Constants._TAG_X509ISSUERSERIAL, i);

        if (e != null) {
            return new XMLX509IssuerSerial(e, this.baseURI);
        }
        return null;
    }

    /**
     * Method itemSKI
     *
     * @param i
     * @return the X509SKI, null if not present
     * @throws XMLSecurityException
     */
    public XMLX509SKI itemSKI(int i) throws XMLSecurityException {

        Element e =
            XMLUtils.selectDsNode(
                getFirstChild(), Constants._TAG_X509SKI, i);

        if (e != null) {
            return new XMLX509SKI(e, this.baseURI);
        }
        return null;
    }

    /**
     * Method itemSubjectName
     *
     * @param i
     * @return the X509SubjectName, null if not present
     * @throws XMLSecurityException
     */
    public XMLX509SubjectName itemSubjectName(int i) throws XMLSecurityException {

        Element e =
            XMLUtils.selectDsNode(
                getFirstChild(), Constants._TAG_X509SUBJECTNAME, i);

        if (e != null) {
            return new XMLX509SubjectName(e, this.baseURI);
        }
        return null;
    }

    /**
     * Method itemCertificate
     *
     * @param i
     * @return the X509Certificate, null if not present
     * @throws XMLSecurityException
     */
    public XMLX509Certificate itemCertificate(int i) throws XMLSecurityException {

        Element e =
            XMLUtils.selectDsNode(
                getFirstChild(), Constants._TAG_X509CERTIFICATE, i);

        if (e != null) {
            return new XMLX509Certificate(e, this.baseURI);
        }
        return null;
    }

    /**
     * Method itemCRL
     *
     * @param i
     * @return the X509CRL, null if not present
     * @throws XMLSecurityException
     */
    public XMLX509CRL itemCRL(int i) throws XMLSecurityException {

        Element e =
            XMLUtils.selectDsNode(
                getFirstChild(), Constants._TAG_X509CRL, i);

        if (e != null) {
            return new XMLX509CRL(e, this.baseURI);
        }
        return null;
    }

    /**
     * Method itemDigest
     *
     * @param i
     * @return the X509Digest, null if not present
     * @throws XMLSecurityException
     */
    public XMLX509Digest itemDigest(int i) throws XMLSecurityException {

        Element e =
            XMLUtils.selectDs11Node(
                getFirstChild(), Constants._TAG_X509DIGEST, i);

        if (e != null) {
            return new XMLX509Digest(e, this.baseURI);
        }
        return null;
    }

    /**
     * Method itemUnknownElement
     *
     * @param i
     * @return the Unknown Element at i
     * TODO implement
     **/
    public Element itemUnknownElement(int i) {
        LOG.debug("itemUnknownElement not implemented: {}", i);
        return null;
    }

    /**
     * Method containsIssuerSerial
     *
     * @return true if this X509Data contains a IssuerSerial
     */
    public boolean containsIssuerSerial() {
        return this.lengthIssuerSerial() > 0;
    }

    /**
     * Method containsSKI
     *
     * @return true if this X509Data contains a SKI
     */
    public boolean containsSKI() {
        return this.lengthSKI() > 0;
    }

    /**
     * Method containsSubjectName
     *
     * @return true if this X509Data contains a SubjectName
     */
    public boolean containsSubjectName() {
        return this.lengthSubjectName() > 0;
    }

    /**
     * Method containsCertificate
     *
     * @return true if this X509Data contains a Certificate
     */
    public boolean containsCertificate() {
        return this.lengthCertificate() > 0;
    }

    /**
     * Method containsDigest
     *
     * @return true if this X509Data contains an X509Digest
     */
    public boolean containsDigest() {
        return this.lengthDigest() > 0;
    }

    /**
     * Method containsCRL
     *
     * @return true if this X509Data contains a CRL
     */
    public boolean containsCRL() {
        return this.lengthCRL() > 0;
    }

    /**
     * Method containsUnknownElement
     *
     * @return true if this X509Data contains an UnknownElement
     */
    public boolean containsUnknownElement() {
        return this.lengthUnknownElement() > 0;
    }

    /** {@inheritDoc} */
    public String getBaseLocalName() {
        return Constants._TAG_X509DATA;
    }
}
