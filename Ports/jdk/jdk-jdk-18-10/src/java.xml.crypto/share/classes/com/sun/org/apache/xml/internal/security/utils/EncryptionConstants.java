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

public final class EncryptionConstants {
    // Attributes that exist in XML Signature in the same way
    /** Tag of Attr Algorithm **/
    public static final String _ATT_ALGORITHM = Constants._ATT_ALGORITHM;

    /** Tag of Attr Id**/
    public static final String _ATT_ID = Constants._ATT_ID;

    /** Tag of Attr Target **/
    public static final String _ATT_TARGET = Constants._ATT_TARGET;

    /** Tag of Attr Type **/
    public static final String _ATT_TYPE = Constants._ATT_TYPE;

    /** Tag of Attr URI **/
    public static final String _ATT_URI = Constants._ATT_URI;

    // Attributes new in XML Encryption
    /** Tag of Attr encoding **/
    public static final String _ATT_ENCODING = "Encoding";

    /** Tag of Attr recipient **/
    public static final String _ATT_RECIPIENT = "Recipient";

    /** Tag of Attr mimetype **/
    public static final String _ATT_MIMETYPE = "MimeType";

    /** Tag of Element CarriedKeyName **/
    public static final String _TAG_CARRIEDKEYNAME = "CarriedKeyName";

    /** Tag of Element CipherData **/
    public static final String _TAG_CIPHERDATA = "CipherData";

    /** Tag of Element CipherReference **/
    public static final String _TAG_CIPHERREFERENCE = "CipherReference";

    /** Tag of Element CipherValue **/
    public static final String _TAG_CIPHERVALUE = "CipherValue";

    /** Tag of Element DataReference **/
    public static final String _TAG_DATAREFERENCE = "DataReference";

    /** Tag of Element EncryptedData **/
    public static final String _TAG_ENCRYPTEDDATA = "EncryptedData";

    /** Tag of Element EncryptedKey **/
    public static final String _TAG_ENCRYPTEDKEY = "EncryptedKey";

    /** Tag of Element EncryptionMethod **/
    public static final String _TAG_ENCRYPTIONMETHOD = "EncryptionMethod";

    /** Tag of Element EncryptionProperties **/
    public static final String _TAG_ENCRYPTIONPROPERTIES = "EncryptionProperties";

    /** Tag of Element EncryptionProperty **/
    public static final String _TAG_ENCRYPTIONPROPERTY = "EncryptionProperty";

    /** Tag of Element KeyReference **/
    public static final String _TAG_KEYREFERENCE = "KeyReference";

    /** Tag of Element KeySize **/
    public static final String _TAG_KEYSIZE = "KeySize";

    /** Tag of Element OAEPparams **/
    public static final String _TAG_OAEPPARAMS = "OAEPparams";

    /** Tag of Element MGF **/
    public static final String _TAG_MGF = "MGF";

    /** Tag of Element ReferenceList **/
    public static final String _TAG_REFERENCELIST = "ReferenceList";

    /** Tag of Element Transforms **/
    public static final String _TAG_TRANSFORMS = "Transforms";

    /** Tag of Element AgreementMethod **/
    public static final String _TAG_AGREEMENTMETHOD = "AgreementMethod";

    /** Tag of Element KA-Nonce **/
    public static final String _TAG_KA_NONCE = "KA-Nonce";

    /** Tag of Element OriginatorKeyInfo **/
    public static final String _TAG_ORIGINATORKEYINFO = "OriginatorKeyInfo";

    /** Tag of Element RecipientKeyInfo **/
    public static final String _TAG_RECIPIENTKEYINFO = "RecipientKeyInfo";

    /** Field ENCRYPTIONSPECIFICATION_URL */
    public static final String ENCRYPTIONSPECIFICATION_URL =
        "http://www.w3.org/TR/2001/WD-xmlenc-core-20010626/";

    /** The namespace of the
     * <A HREF="http://www.w3.org/TR/2001/WD-xmlenc-core-20010626/">
     * XML Encryption Syntax and Processing</A> */
    public static final String EncryptionSpecNS =
        "http://www.w3.org/2001/04/xmlenc#";

    /**
     * The namespace of the XML Encryption 1.1 specification
     */
    public static final String EncryptionSpec11NS =
        "http://www.w3.org/2009/xmlenc11#";

    /** URI for content*/
    public static final String TYPE_CONTENT = EncryptionSpecNS + "Content";

    /** URI for element*/
    public static final String TYPE_ELEMENT = EncryptionSpecNS + "Element";

    /** URI for mediatype*/
    public static final String TYPE_MEDIATYPE =
        "http://www.isi.edu/in-notes/iana/assignments/media-types/";

    /** Block Encryption - REQUIRED TRIPLEDES */
    public static final String ALGO_ID_BLOCKCIPHER_TRIPLEDES =
        EncryptionConstants.EncryptionSpecNS + "tripledes-cbc";

    /** Block Encryption - REQUIRED AES-128 */
    public static final String ALGO_ID_BLOCKCIPHER_AES128 =
        EncryptionConstants.EncryptionSpecNS + "aes128-cbc";

    /** Block Encryption - REQUIRED AES-256 */
    public static final String ALGO_ID_BLOCKCIPHER_AES256 =
        EncryptionConstants.EncryptionSpecNS + "aes256-cbc";

    /** Block Encryption - OPTIONAL AES-192 */
    public static final String ALGO_ID_BLOCKCIPHER_AES192 =
        EncryptionConstants.EncryptionSpecNS + "aes192-cbc";

    /** Block Encryption - OPTIONAL AES-128-GCM */
    public static final String ALGO_ID_BLOCKCIPHER_AES128_GCM =
        "http://www.w3.org/2009/xmlenc11#aes128-gcm";

    /** Block Encryption - OPTIONAL AES-192-GCM */
    public static final String ALGO_ID_BLOCKCIPHER_AES192_GCM =
        "http://www.w3.org/2009/xmlenc11#aes192-gcm";

    /** Block Encryption - OPTIONAL AES-256-GCM */
    public static final String ALGO_ID_BLOCKCIPHER_AES256_GCM =
        "http://www.w3.org/2009/xmlenc11#aes256-gcm";

    /** Block Encryption - OPTIONAL SEED-128 */
    public static final String ALGO_ID_BLOCKCIPHER_SEED128 =
        "http://www.w3.org/2007/05/xmldsig-more#seed128-cbc";

    /** Block Encryption - OPTIONAL CAMELLIA-128 */
    public static final String ALGO_ID_BLOCKCIPHER_CAMELLIA128 =
        "http://www.w3.org/2001/04/xmldsig-more#camellia128-cbc";

    /** Block Encryption - OPTIONAL CAMELLIA-128 */
    public static final String ALGO_ID_BLOCKCIPHER_CAMELLIA192 =
        "http://www.w3.org/2001/04/xmldsig-more#camellia192-cbc";

    /** Block Encryption - OPTIONAL CAMELLIA-128 */
    public static final String ALGO_ID_BLOCKCIPHER_CAMELLIA256 =
        "http://www.w3.org/2001/04/xmldsig-more#camellia256-cbc";

    /** Key Transport - REQUIRED RSA-v1.5*/
    public static final String ALGO_ID_KEYTRANSPORT_RSA15 =
        EncryptionConstants.EncryptionSpecNS + "rsa-1_5";

    /** Key Transport - REQUIRED RSA-OAEP */
    public static final String ALGO_ID_KEYTRANSPORT_RSAOAEP =
        EncryptionConstants.EncryptionSpecNS + "rsa-oaep-mgf1p";

    /** Key Transport - OPTIONAL RSA-OAEP_11 */
    public static final String ALGO_ID_KEYTRANSPORT_RSAOAEP_11 =
        EncryptionConstants.EncryptionSpec11NS + "rsa-oaep";

    /** Key Agreement - OPTIONAL Diffie-Hellman */
    public static final String ALGO_ID_KEYAGREEMENT_DH =
        EncryptionConstants.EncryptionSpecNS + "dh";

    /** Symmetric Key Wrap - REQUIRED TRIPLEDES KeyWrap */
    public static final String ALGO_ID_KEYWRAP_TRIPLEDES =
        EncryptionConstants.EncryptionSpecNS + "kw-tripledes";

    /** Symmetric Key Wrap - REQUIRED AES-128 KeyWrap */
    public static final String ALGO_ID_KEYWRAP_AES128 =
        EncryptionConstants.EncryptionSpecNS + "kw-aes128";

    /** Symmetric Key Wrap - REQUIRED AES-256 KeyWrap */
    public static final String ALGO_ID_KEYWRAP_AES256 =
        EncryptionConstants.EncryptionSpecNS + "kw-aes256";

    /** Symmetric Key Wrap - OPTIONAL AES-192 KeyWrap */
    public static final String ALGO_ID_KEYWRAP_AES192 =
        EncryptionConstants.EncryptionSpecNS + "kw-aes192";

    /** Symmetric Key Wrap - OPTIONAL CAMELLIA-128 KeyWrap */
    public static final String ALGO_ID_KEYWRAP_CAMELLIA128 =
        "http://www.w3.org/2001/04/xmldsig-more#kw-camellia128";

    /** Symmetric Key Wrap - OPTIONAL CAMELLIA-192 KeyWrap */
    public static final String ALGO_ID_KEYWRAP_CAMELLIA192 =
        "http://www.w3.org/2001/04/xmldsig-more#kw-camellia192";

    /** Symmetric Key Wrap - OPTIONAL CAMELLIA-256 KeyWrap */
    public static final String ALGO_ID_KEYWRAP_CAMELLIA256 =
        "http://www.w3.org/2001/04/xmldsig-more#kw-camellia256";

    /** Symmetric Key Wrap - OPTIONAL SEED-128 KeyWrap */
    public static final String ALGO_ID_KEYWRAP_SEED128 =
        "http://www.w3.org/2007/05/xmldsig-more#kw-seed128";

    /** Message Authentication - RECOMMENDED XML Digital Signature */
    public static final String ALGO_ID_AUTHENTICATION_XMLSIGNATURE =
        "http://www.w3.org/TR/2001/CR-xmldsig-core-20010419/";

    /** Canonicalization - OPTIONAL Canonical XML with Comments */
    public static final String ALGO_ID_C14N_WITHCOMMENTS =
        "http://www.w3.org/TR/2001/REC-xml-c14n-20010315#WithComments";

    /** Canonicalization - OPTIONAL Canonical XML (omits comments) */
    public static final String ALGO_ID_C14N_OMITCOMMENTS =
        "http://www.w3.org/TR/2001/REC-xml-c14n-20010315";

    /** Encoding - REQUIRED base64 */
    public static final String ALGO_ID_ENCODING_BASE64 =
        "http://www.w3.org/2000/09/xmldsig#base64";

    /** MGF1 with SHA-1 */
    public static final String MGF1_SHA1 =
        EncryptionConstants.EncryptionSpec11NS + "mgf1sha1";

    /** MGF1 with SHA-224 */
    public static final String MGF1_SHA224 =
        EncryptionConstants.EncryptionSpec11NS + "mgf1sha224";

    /** MGF1 with SHA-256 */
    public static final String MGF1_SHA256 =
        EncryptionConstants.EncryptionSpec11NS + "mgf1sha256";

    /** MGF1 with SHA-384 */
    public static final String MGF1_SHA384 =
        EncryptionConstants.EncryptionSpec11NS + "mgf1sha384";

    /** MGF1 with SHA-512 */
    public static final String MGF1_SHA512 =
        EncryptionConstants.EncryptionSpec11NS + "mgf1sha512";


    private EncryptionConstants() {
        // we don't allow instantiation
    }

}
