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

/**
 * Provides all constants and some translation functions for i18n.
 *
 * For the used Algorithm identifiers and Namespaces, look at the
 * <A HREF="http://www.w3.org/TR/xmldsig-core/#sec-TransformAlg">XML
 * Signature specification</A>.
 *
 */
public final class Constants {

    /** Field configurationFile */
    public static final String configurationFile = "data/websig.conf";

    /** Field configurationFileNew */
    public static final String configurationFileNew = ".xmlsecurityconfig";

    /** Field exceptionMessagesResourceBundleDir */
    public static final String exceptionMessagesResourceBundleDir =
        "com.sun.org.apache.xml.internal.security/resource";

    /** Field exceptionMessagesResourceBundleBase is the location of the {@code ResourceBundle} */
    public static final String exceptionMessagesResourceBundleBase =
        exceptionMessagesResourceBundleDir + "/" + "xmlsecurity";

    /**
     * The URL of the
     * <A HREF="http://www.w3.org/TR/2001/CR-xmldsig-core-20010419/">XML Signature specification</A>
     */
    public static final String SIGNATURESPECIFICATION_URL =
        "http://www.w3.org/TR/2001/CR-xmldsig-core-20010419/";

    /**
     * The namespace of the
     * <A HREF="http://www.w3.org/TR/2001/CR-xmldsig-core-20010419/">XML Signature specification</A>
     */
    public static final String SignatureSpecNS = "http://www.w3.org/2000/09/xmldsig#";

    /**
     * The namespace of the
     * <A HREF="http://www.w3.org/TR/xmldsig-core1/">XML Signature specification</A>
     */
    public static final String SignatureSpec11NS = "http://www.w3.org/2009/xmldsig11#";

    /** The URL for more algorithms **/
    public static final String MoreAlgorithmsSpecNS = "http://www.w3.org/2001/04/xmldsig-more#";

    /** The (newer) URL for more algorithms **/
    public static final String XML_DSIG_NS_MORE_07_05 = "http://www.w3.org/2007/05/xmldsig-more#";

    /** The URI for XML spec*/
    public static final String XML_LANG_SPACE_SpecNS = "http://www.w3.org/XML/1998/namespace";

    /** The URI for XMLNS spec*/
    public static final String NamespaceSpecNS = "http://www.w3.org/2000/xmlns/";

    /** Tag of Attr Algorithm**/
    public static final String _ATT_ALGORITHM = "Algorithm";

    /** Tag of Attr URI**/
    public static final String _ATT_URI = "URI";

    /** Tag of Attr Type**/
    public static final String _ATT_TYPE = "Type";

    /** Tag of Attr Id**/
    public static final String _ATT_ID = "Id";

    /** Tag of Attr MimeType**/
    public static final String _ATT_MIMETYPE = "MimeType";

    /** Tag of Attr Encoding**/
    public static final String _ATT_ENCODING = "Encoding";

    /** Tag of Attr Target**/
    public static final String _ATT_TARGET = "Target";

    // KeyInfo (KeyName|KeyValue|RetrievalMethod|X509Data|PGPData|SPKIData|MgmtData)
    // KeyValue (DSAKeyValue|RSAKeyValue)
    // DSAKeyValue (P, Q, G, Y, J?, (Seed, PgenCounter)?)
    // RSAKeyValue (Modulus, Exponent)
    // RetrievalMethod (Transforms?)
    // X509Data ((X509IssuerSerial | X509SKI | X509SubjectName | X509Certificate)+ | X509CRL)
    // X509IssuerSerial (X509IssuerName, X509SerialNumber)
    // PGPData ((PGPKeyID, PGPKeyPacket?) | (PGPKeyPacket))
    // SPKIData (SPKISexp)

    /** Tag of Element CanonicalizationMethod **/
    public static final String _TAG_CANONICALIZATIONMETHOD = "CanonicalizationMethod";

    /** Tag of Element DigestMethod **/
    public static final String _TAG_DIGESTMETHOD = "DigestMethod";

    /** Tag of Element DigestValue **/
    public static final String _TAG_DIGESTVALUE = "DigestValue";

    /** Tag of Element Manifest **/
    public static final String _TAG_MANIFEST = "Manifest";

    /** Tag of Element Methods **/
    public static final String _TAG_METHODS = "Methods";

    /** Tag of Element Object **/
    public static final String _TAG_OBJECT = "Object";

    /** Tag of Element Reference **/
    public static final String _TAG_REFERENCE = "Reference";

    /** Tag of Element Signature **/
    public static final String _TAG_SIGNATURE = "Signature";

    /** Tag of Element SignatureMethod **/
    public static final String _TAG_SIGNATUREMETHOD = "SignatureMethod";

    /** Tag of Element HMACOutputLength **/
    public static final String _TAG_HMACOUTPUTLENGTH = "HMACOutputLength";

    /** Tag of Element RSAPSSParams **/
    public static final String _TAG_RSAPSSPARAMS = "RSAPSSParams";

    /** Tag of Element MaskGenerationFunction **/
    public static final String _TAG_MGF = "MaskGenerationFunction";

    /** Tag of Element SaltLength **/
    public static final String _TAG_SALTLENGTH = "SaltLength";

    /** Tag of Element TrailerField **/
    public static final String _TAG_TRAILERFIELD = "TrailerField";

    /** Tag of Element SignatureProperties **/
    public static final String _TAG_SIGNATUREPROPERTIES = "SignatureProperties";

    /** Tag of Element SignatureProperty **/
    public static final String _TAG_SIGNATUREPROPERTY = "SignatureProperty";

    /** Tag of Element SignatureValue **/
    public static final String _TAG_SIGNATUREVALUE = "SignatureValue";

    /** Tag of Element SignedInfo **/
    public static final String _TAG_SIGNEDINFO = "SignedInfo";

    /** Tag of Element Transform **/
    public static final String _TAG_TRANSFORM = "Transform";

    /** Tag of Element Transforms **/
    public static final String _TAG_TRANSFORMS = "Transforms";

    /** Tag of Element XPath **/
    public static final String _TAG_XPATH = "XPath";

    /** Tag of Element KeyInfo **/
    public static final String _TAG_KEYINFO = "KeyInfo";

    /** Tag of Element KeyName **/
    public static final String _TAG_KEYNAME = "KeyName";

    /** Tag of Element KeyValue **/
    public static final String _TAG_KEYVALUE = "KeyValue";

    /** Tag of Element RetrievalMethod **/
    public static final String _TAG_RETRIEVALMETHOD = "RetrievalMethod";

    /** Tag of Element X509Data **/
    public static final String _TAG_X509DATA = "X509Data";

    /** Tag of Element PGPData **/
    public static final String _TAG_PGPDATA = "PGPData";

    /** Tag of Element SPKIData **/
    public static final String _TAG_SPKIDATA = "SPKIData";

    /** Tag of Element MgmtData **/
    public static final String _TAG_MGMTDATA = "MgmtData";

    /** Tag of Element RSAKeyValue **/
    public static final String _TAG_RSAKEYVALUE = "RSAKeyValue";

    /** Tag of Element Exponent **/
    public static final String _TAG_EXPONENT = "Exponent";

    /** Tag of Element Modulus **/
    public static final String _TAG_MODULUS = "Modulus";

    /** Tag of Element DSAKeyValue **/
    public static final String _TAG_DSAKEYVALUE = "DSAKeyValue";

    /** Tag of Element P **/
    public static final String _TAG_P = "P";

    /** Tag of Element Q **/
    public static final String _TAG_Q = "Q";

    /** Tag of Element G **/
    public static final String _TAG_G = "G";

    /** Tag of Element Y **/
    public static final String _TAG_Y = "Y";

    /** Tag of Element J **/
    public static final String _TAG_J = "J";

    /** Tag of Element ECKeyValue **/
    public static final String _TAG_ECKEYVALUE = "ECKeyValue";

    /** Tag of Element Seed **/
    public static final String _TAG_SEED = "Seed";

    /** Tag of Element PgenCounter **/
    public static final String _TAG_PGENCOUNTER = "PgenCounter";

    /** Tag of Element rawX509Certificate **/
    public static final String _TAG_RAWX509CERTIFICATE = "rawX509Certificate";

    /** Tag of Element X509IssuerSerial **/
    public static final String _TAG_X509ISSUERSERIAL= "X509IssuerSerial";

    /** Tag of Element X509SKI **/
    public static final String _TAG_X509SKI = "X509SKI";

    /** Tag of Element X509SubjectName **/
    public static final String _TAG_X509SUBJECTNAME = "X509SubjectName";

    /** Tag of Element X509Certificate **/
    public static final String _TAG_X509CERTIFICATE = "X509Certificate";

    /** Tag of Element X509CRL **/
    public static final String _TAG_X509CRL = "X509CRL";

    /** Tag of Element X509IssuerName **/
    public static final String _TAG_X509ISSUERNAME = "X509IssuerName";

    /** Tag of Element X509SerialNumber **/
    public static final String _TAG_X509SERIALNUMBER = "X509SerialNumber";

    /** Tag of Element PGPKeyID **/
    public static final String _TAG_PGPKEYID = "PGPKeyID";

    /** Tag of Element PGPKeyPacket **/
    public static final String _TAG_PGPKEYPACKET = "PGPKeyPacket";

    /** Tag of Element DEREncodedKeyValue **/
    public static final String _TAG_DERENCODEDKEYVALUE = "DEREncodedKeyValue";

    /** Tag of Element KeyInfoReference **/
    public static final String _TAG_KEYINFOREFERENCE = "KeyInfoReference";

    /** Tag of Element X509Digest **/
    public static final String _TAG_X509DIGEST = "X509Digest";

    /** Tag of Element SPKISexp **/
    public static final String _TAG_SPKISEXP = "SPKISexp";

    /** Digest - Required SHA1 */
    public static final String ALGO_ID_DIGEST_SHA1 = SignatureSpecNS + "sha1";

    /**
     * @see <A HREF="http://www.ietf.org/internet-drafts/draft-blake-wilson-xmldsig-ecdsa-02.txt">
     *  draft-blake-wilson-xmldsig-ecdsa-02.txt</A>
     */
    public static final String ALGO_ID_SIGNATURE_ECDSA_CERTICOM =
        "http://www.certicom.com/2000/11/xmlecdsig#ecdsa-sha1";

    private Constants() {
        // we don't allow instantiation
    }

}
