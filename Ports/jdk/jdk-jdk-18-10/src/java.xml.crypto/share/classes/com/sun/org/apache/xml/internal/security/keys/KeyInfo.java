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
package com.sun.org.apache.xml.internal.security.keys;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.crypto.SecretKey;

import com.sun.org.apache.xml.internal.security.exceptions.XMLSecurityException;
import com.sun.org.apache.xml.internal.security.keys.content.DEREncodedKeyValue;
import com.sun.org.apache.xml.internal.security.keys.content.KeyInfoReference;
import com.sun.org.apache.xml.internal.security.keys.content.KeyName;
import com.sun.org.apache.xml.internal.security.keys.content.KeyValue;
import com.sun.org.apache.xml.internal.security.keys.content.MgmtData;
import com.sun.org.apache.xml.internal.security.keys.content.PGPData;
import com.sun.org.apache.xml.internal.security.keys.content.RetrievalMethod;
import com.sun.org.apache.xml.internal.security.keys.content.SPKIData;
import com.sun.org.apache.xml.internal.security.keys.content.X509Data;
import com.sun.org.apache.xml.internal.security.keys.content.keyvalues.DSAKeyValue;
import com.sun.org.apache.xml.internal.security.keys.content.keyvalues.RSAKeyValue;
import com.sun.org.apache.xml.internal.security.keys.keyresolver.KeyResolver;
import com.sun.org.apache.xml.internal.security.keys.keyresolver.KeyResolverException;
import com.sun.org.apache.xml.internal.security.keys.keyresolver.KeyResolverSpi;
import com.sun.org.apache.xml.internal.security.keys.storage.StorageResolver;
import com.sun.org.apache.xml.internal.security.transforms.Transforms;
import com.sun.org.apache.xml.internal.security.utils.Constants;
import com.sun.org.apache.xml.internal.security.utils.ElementProxy;
import com.sun.org.apache.xml.internal.security.utils.SignatureElementProxy;
import com.sun.org.apache.xml.internal.security.utils.XMLUtils;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * This class stand for KeyInfo Element that may contain keys, names,
 * certificates and other public key management information,
 * such as in-band key distribution or key agreement data.
 * <p></p>
 * KeyInfo Element has two basic functions:
 * One is KeyResolve for getting the public key in signature validation processing.
 * the other one is toElement for getting the element in signature generation processing.
 * <p></p>
 * The {@code lengthXXX()} methods provide access to the internal Key
 * objects:
 * <UL>
 * <LI>If the {@code KeyInfo} was constructed from an Element
 * (Signature verification), the {@code lengthXXX()} methods searches
 * for child elements of {@code ds:KeyInfo} for known types. </LI>
 * <LI>If the {@code KeyInfo} was constructed from scratch (during
 * Signature generation), the {@code lengthXXX()} methods return the number
 * of {@code XXXs} objects already passed to the KeyInfo</LI>
 * </UL>
 * <p></p>
 * The {@code addXXX()} methods are used for adding Objects of the
 * appropriate type to the {@code KeyInfo}. This is used during signature
 * generation.
 * <p></p>
 * The {@code itemXXX(int i)} methods return the i'th object of the
 * corresponding type.
 * <p></p>
 * The {@code containsXXX()} methods return <I>whether</I> the KeyInfo
 * contains the corresponding type.
 *
 */
public class KeyInfo extends SignatureElementProxy {

    private static final com.sun.org.slf4j.internal.Logger LOG =
        com.sun.org.slf4j.internal.LoggerFactory.getLogger(KeyInfo.class);

    // We need at least one StorageResolver otherwise
    // the KeyResolvers would not be called.
    // The default StorageResolver is null.

    private List<X509Data> x509Datas;

    private static final List<StorageResolver> nullList;
    static {
        List<StorageResolver> list = new ArrayList<>(1);
        list.add(null);
        nullList = java.util.Collections.unmodifiableList(list);
    }

    /** Field storageResolvers */
    private List<StorageResolver> storageResolvers = nullList;

    /**
     * Stores the individual (per-KeyInfo) {@link KeyResolverSpi}s
     */
    private List<KeyResolverSpi> internalKeyResolvers = new ArrayList<>();

    private boolean secureValidation;

    /**
     * Constructor KeyInfo
     * @param doc
     */
    public KeyInfo(Document doc) {
        super(doc);
        addReturnToSelf();

        String prefix = ElementProxy.getDefaultPrefix(this.getBaseNamespace());
        if (prefix != null && prefix.length() > 0) {
            getElement().setAttributeNS(Constants.NamespaceSpecNS, "xmlns:" + prefix,
                                        this.getBaseNamespace());
        }

    }

    /**
     * Constructor KeyInfo
     *
     * @param element
     * @param baseURI
     * @throws XMLSecurityException
     */
    public KeyInfo(Element element, String baseURI) throws XMLSecurityException {
        super(element, baseURI);

        Attr attr = element.getAttributeNodeNS(null, "Id");
        if (attr != null) {
            element.setIdAttributeNode(attr, true);
        }
    }

    /**
     * Set whether secure processing is enabled or not. The default is false.
     */
    public void setSecureValidation(boolean secureValidation) {
        this.secureValidation = secureValidation;
    }

    /**
     * Sets the {@code Id} attribute
     *
     * @param id ID
     */
    public void setId(String id) {
        if (id != null) {
            setLocalIdAttribute(Constants._ATT_ID, id);
        }
    }

    /**
     * Returns the {@code Id} attribute
     *
     * @return the {@code Id} attribute
     */
    public String getId() {
        return getLocalAttribute(Constants._ATT_ID);
    }

    /**
     * Method addKeyName
     *
     * @param keynameString
     */
    public void addKeyName(String keynameString) {
        this.add(new KeyName(getDocument(), keynameString));
    }

    /**
     * Method add
     *
     * @param keyname
     */
    public void add(KeyName keyname) {
        appendSelf(keyname);
        addReturnToSelf();
    }

    /**
     * Method addKeyValue
     *
     * @param pk
     */
    public void addKeyValue(PublicKey pk) {
        this.add(new KeyValue(getDocument(), pk));
    }

    /**
     * Method addKeyValue
     *
     * @param unknownKeyValueElement
     */
    public void addKeyValue(Element unknownKeyValueElement) {
        this.add(new KeyValue(getDocument(), unknownKeyValueElement));
    }

    /**
     * Method add
     *
     * @param dsakeyvalue
     */
    public void add(DSAKeyValue dsakeyvalue) {
        this.add(new KeyValue(getDocument(), dsakeyvalue));
    }

    /**
     * Method add
     *
     * @param rsakeyvalue
     */
    public void add(RSAKeyValue rsakeyvalue) {
        this.add(new KeyValue(getDocument(), rsakeyvalue));
    }

    /**
     * Method add
     *
     * @param pk
     */
    public void add(PublicKey pk) {
        this.add(new KeyValue(getDocument(), pk));
    }

    /**
     * Method add
     *
     * @param keyvalue
     */
    public void add(KeyValue keyvalue) {
        appendSelf(keyvalue);
        addReturnToSelf();
    }

    /**
     * Method addMgmtData
     *
     * @param mgmtdata
     */
    public void addMgmtData(String mgmtdata) {
        this.add(new MgmtData(getDocument(), mgmtdata));
    }

    /**
     * Method add
     *
     * @param mgmtdata
     */
    public void add(MgmtData mgmtdata) {
        appendSelf(mgmtdata);
        addReturnToSelf();
    }

    /**
     * Method addPGPData
     *
     * @param pgpdata
     */
    public void add(PGPData pgpdata) {
        appendSelf(pgpdata);
        addReturnToSelf();
    }

    /**
     * Method addRetrievalMethod
     *
     * @param uri
     * @param transforms
     * @param Type
     */
    public void addRetrievalMethod(String uri, Transforms transforms, String Type) {
        this.add(new RetrievalMethod(getDocument(), uri, transforms, Type));
    }

    /**
     * Method add
     *
     * @param retrievalmethod
     */
    public void add(RetrievalMethod retrievalmethod) {
        appendSelf(retrievalmethod);
        addReturnToSelf();
    }

    /**
     * Method add
     *
     * @param spkidata
     */
    public void add(SPKIData spkidata) {
        appendSelf(spkidata);
        addReturnToSelf();
    }

    /**
     * Method addX509Data
     *
     * @param x509data
     */
    public void add(X509Data x509data) {
        if (x509Datas == null) {
            x509Datas = new ArrayList<>();
        }
        x509Datas.add(x509data);
        appendSelf(x509data);
        addReturnToSelf();
    }

    /**
     * Method addDEREncodedKeyValue
     *
     * @param pk
     * @throws XMLSecurityException
     */
    public void addDEREncodedKeyValue(PublicKey pk) throws XMLSecurityException {
        this.add(new DEREncodedKeyValue(getDocument(), pk));
    }

    /**
     * Method add
     *
     * @param derEncodedKeyValue
     */
    public void add(DEREncodedKeyValue derEncodedKeyValue) {
        appendSelf(derEncodedKeyValue);
        addReturnToSelf();
    }

    /**
     * Method addKeyInfoReference
     *
     * @param URI
     * @throws XMLSecurityException
     */
    public void addKeyInfoReference(String URI) throws XMLSecurityException {
        this.add(new KeyInfoReference(getDocument(), URI));
    }

    /**
     * Method add
     *
     * @param keyInfoReference
     */
    public void add(KeyInfoReference keyInfoReference) {
        appendSelf(keyInfoReference);
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
     * Method lengthKeyName
     *
     * @return the number of the KeyName tags
     */
    public int lengthKeyName() {
        return this.length(Constants.SignatureSpecNS, Constants._TAG_KEYNAME);
    }

    /**
     * Method lengthKeyValue
     *
     *@return the number of the KeyValue tags
     */
    public int lengthKeyValue() {
        return this.length(Constants.SignatureSpecNS, Constants._TAG_KEYVALUE);
    }

    /**
     * Method lengthMgmtData
     *
     *@return the number of the MgmtData tags
     */
    public int lengthMgmtData() {
        return this.length(Constants.SignatureSpecNS, Constants._TAG_MGMTDATA);
    }

    /**
     * Method lengthPGPData
     *
     *@return the number of the PGPDat. tags
     */
    public int lengthPGPData() {
        return this.length(Constants.SignatureSpecNS, Constants._TAG_PGPDATA);
    }

    /**
     * Method lengthRetrievalMethod
     *
     *@return the number of the RetrievalMethod tags
     */
    public int lengthRetrievalMethod() {
        return this.length(Constants.SignatureSpecNS, Constants._TAG_RETRIEVALMETHOD);
    }

    /**
     * Method lengthSPKIData
     *
     *@return the number of the SPKIData tags
     */
    public int lengthSPKIData() {
        return this.length(Constants.SignatureSpecNS, Constants._TAG_SPKIDATA);
    }

    /**
     * Method lengthX509Data
     *
     *@return the number of the X509Data tags
     */
    public int lengthX509Data() {
        if (x509Datas != null) {
            return x509Datas.size();
        }
        return this.length(Constants.SignatureSpecNS, Constants._TAG_X509DATA);
    }

    /**
     * Method lengthDEREncodedKeyValue
     *
     *@return the number of the DEREncodedKeyValue tags
     */
    public int lengthDEREncodedKeyValue() {
        return this.length(Constants.SignatureSpec11NS, Constants._TAG_DERENCODEDKEYVALUE);
    }

    /**
     * Method lengthKeyInfoReference
     *
     *@return the number of the KeyInfoReference tags
     */
    public int lengthKeyInfoReference() {
        return this.length(Constants.SignatureSpec11NS, Constants._TAG_KEYINFOREFERENCE);
    }

    /**
     * Method lengthUnknownElement
     * NOTE possibly buggy.
     * @return the number of the UnknownElement tags
     */
    public int lengthUnknownElement() {
        int res = 0;
        Node childNode = getElement().getFirstChild();
        while (childNode != null) {
            /**
             * $todo$ using this method, we don't see unknown Elements
             *  from Signature NS; revisit
             */
            if (childNode.getNodeType() == Node.ELEMENT_NODE
                && childNode.getNamespaceURI().equals(Constants.SignatureSpecNS)) {
                res++;
            }
            childNode = childNode.getNextSibling();
        }

        return res;
    }

    /**
     * Method itemKeyName
     *
     * @param i
     * @return the asked KeyName element, null if the index is too big
     * @throws XMLSecurityException
     */
    public KeyName itemKeyName(int i) throws XMLSecurityException {
        Element e =
            XMLUtils.selectDsNode(
                getFirstChild(), Constants._TAG_KEYNAME, i);

        if (e != null) {
            return new KeyName(e, this.baseURI);
        }
        return null;
    }

    /**
     * Method itemKeyValue
     *
     * @param i
     * @return the asked KeyValue element, null if the index is too big
     * @throws XMLSecurityException
     */
    public KeyValue itemKeyValue(int i) throws XMLSecurityException {
        Element e =
            XMLUtils.selectDsNode(
                getFirstChild(), Constants._TAG_KEYVALUE, i);

        if (e != null) {
            return new KeyValue(e, this.baseURI);
        }
        return null;
    }

    /**
     * Method itemMgmtData
     *
     * @param i
     * @return the asked MgmtData element, null if the index is too big
     * @throws XMLSecurityException
     */
    public MgmtData itemMgmtData(int i) throws XMLSecurityException {
        Element e =
            XMLUtils.selectDsNode(
                getFirstChild(), Constants._TAG_MGMTDATA, i);

        if (e != null) {
            return new MgmtData(e, this.baseURI);
        }
        return null;
    }

    /**
     * Method itemPGPData
     *
     * @param i
     * @return the asked PGPData element, null if the index is too big
     * @throws XMLSecurityException
     */
    public PGPData itemPGPData(int i) throws XMLSecurityException {
        Element e =
            XMLUtils.selectDsNode(
                getFirstChild(), Constants._TAG_PGPDATA, i);

        if (e != null) {
            return new PGPData(e, this.baseURI);
        }
        return null;
    }

    /**
     * Method itemRetrievalMethod
     *
     * @param i
     *@return the asked RetrievalMethod element, null if the index is too big
     * @throws XMLSecurityException
     */
    public RetrievalMethod itemRetrievalMethod(int i) throws XMLSecurityException {
        Element e =
            XMLUtils.selectDsNode(
                getFirstChild(), Constants._TAG_RETRIEVALMETHOD, i);

        if (e != null) {
            return new RetrievalMethod(e, this.baseURI);
        }
        return null;
    }

    /**
     * Method itemSPKIData
     *
     * @param i
     * @return the asked SPKIData element, null if the index is too big
     * @throws XMLSecurityException
     */
    public SPKIData itemSPKIData(int i) throws XMLSecurityException {
        Element e =
            XMLUtils.selectDsNode(
                getFirstChild(), Constants._TAG_SPKIDATA, i);

        if (e != null) {
            return new SPKIData(e, this.baseURI);
        }
        return null;
    }

    /**
     * Method itemX509Data
     *
     * @param i
     * @return the asked X509Data element, null if the index is too big
     * @throws XMLSecurityException
     */
    public X509Data itemX509Data(int i) throws XMLSecurityException {
        if (x509Datas != null) {
            return x509Datas.get(i);
        }
        Element e =
            XMLUtils.selectDsNode(
                getFirstChild(), Constants._TAG_X509DATA, i);

        if (e != null) {
            return new X509Data(e, this.baseURI);
        }
        return null;
    }

    /**
     * Method itemDEREncodedKeyValue
     *
     * @param i
     * @return the asked DEREncodedKeyValue element, null if the index is too big
     * @throws XMLSecurityException
     */
    public DEREncodedKeyValue itemDEREncodedKeyValue(int i) throws XMLSecurityException {
        Element e =
            XMLUtils.selectDs11Node(
                getFirstChild(), Constants._TAG_DERENCODEDKEYVALUE, i);

        if (e != null) {
            return new DEREncodedKeyValue(e, this.baseURI);
        }
        return null;
    }

    /**
     * Method itemKeyInfoReference
     *
     * @param i
     * @return the asked KeyInfoReference element, null if the index is too big
     * @throws XMLSecurityException
     */
    public KeyInfoReference itemKeyInfoReference(int i) throws XMLSecurityException {
        Element e =
            XMLUtils.selectDs11Node(
                getFirstChild(), Constants._TAG_KEYINFOREFERENCE, i);

        if (e != null) {
            return new KeyInfoReference(e, this.baseURI);
        }
        return null;
    }

    /**
     * Method itemUnknownElement
     *
     * @param i index
     * @return the element number of the unknown elements
     */
    public Element itemUnknownElement(int i) {
        int res = 0;
        Node childNode = getElement().getFirstChild();
        while (childNode != null) {
            /**
             * $todo$ using this method, we don't see unknown Elements
             *  from Signature NS; revisit
             */
            if (childNode.getNodeType() == Node.ELEMENT_NODE
                && childNode.getNamespaceURI().equals(Constants.SignatureSpecNS)) {
                res++;

                if (res == i) {
                    return (Element) childNode;
                }
            }
            childNode = childNode.getNextSibling();
        }

        return null;
    }

    /**
     * Method isEmpty
     *
     * @return true if the element has no descendants.
     */
    public boolean isEmpty() {
        return getFirstChild() == null;
    }

    /**
     * Method containsKeyName
     *
     * @return If the KeyInfo contains a KeyName node
     */
    public boolean containsKeyName() {
        return this.lengthKeyName() > 0;
    }

    /**
     * Method containsKeyValue
     *
     * @return If the KeyInfo contains a KeyValue node
     */
    public boolean containsKeyValue() {
        return this.lengthKeyValue() > 0;
    }

    /**
     * Method containsMgmtData
     *
     * @return If the KeyInfo contains a MgmtData node
     */
    public boolean containsMgmtData() {
        return this.lengthMgmtData() > 0;
    }

    /**
     * Method containsPGPData
     *
     * @return If the KeyInfo contains a PGPData node
     */
    public boolean containsPGPData() {
        return this.lengthPGPData() > 0;
    }

    /**
     * Method containsRetrievalMethod
     *
     * @return If the KeyInfo contains a RetrievalMethod node
     */
    public boolean containsRetrievalMethod() {
        return this.lengthRetrievalMethod() > 0;
    }

    /**
     * Method containsSPKIData
     *
     * @return If the KeyInfo contains a SPKIData node
     */
    public boolean containsSPKIData() {
        return this.lengthSPKIData() > 0;
    }

    /**
     * Method containsUnknownElement
     *
     * @return If the KeyInfo contains a UnknownElement node
     */
    public boolean containsUnknownElement() {
        return this.lengthUnknownElement() > 0;
    }

    /**
     * Method containsX509Data
     *
     * @return If the KeyInfo contains a X509Data node
     */
    public boolean containsX509Data() {
        return this.lengthX509Data() > 0;
    }

    /**
     * Method containsDEREncodedKeyValue
     *
     * @return If the KeyInfo contains a DEREncodedKeyValue node
     */
    public boolean containsDEREncodedKeyValue() {
        return this.lengthDEREncodedKeyValue() > 0;
    }

    /**
     * Method containsKeyInfoReference
     *
     * @return If the KeyInfo contains a KeyInfoReference node
     */
    public boolean containsKeyInfoReference() {
        return this.lengthKeyInfoReference() > 0;
    }

    /**
     * This method returns the public key.
     *
     * @return If the KeyInfo contains a PublicKey node
     * @throws KeyResolverException
     */
    public PublicKey getPublicKey() throws KeyResolverException {
        PublicKey pk = this.getPublicKeyFromInternalResolvers();

        if (pk != null) {
            LOG.debug("I could find a key using the per-KeyInfo key resolvers");

            return pk;
        }
        LOG.debug("I couldn't find a key using the per-KeyInfo key resolvers");

        pk = this.getPublicKeyFromStaticResolvers();

        if (pk != null) {
            LOG.debug("I could find a key using the system-wide key resolvers");

            return pk;
        }
        LOG.debug("I couldn't find a key using the system-wide key resolvers");

        return null;
    }

    /**
     * Searches the library wide KeyResolvers for public keys
     *
     * @return The public key contained in this Node.
     * @throws KeyResolverException
     */
    PublicKey getPublicKeyFromStaticResolvers() throws KeyResolverException {
        Iterator<KeyResolverSpi> it = KeyResolver.iterator();
        while (it.hasNext()) {
            KeyResolverSpi keyResolver = it.next();
            Node currentChild = getFirstChild();
            String uri = this.getBaseURI();
            while (currentChild != null) {
                if (currentChild.getNodeType() == Node.ELEMENT_NODE) {
                    for (StorageResolver storage : storageResolvers) {
                        PublicKey pk =
                            keyResolver.engineLookupAndResolvePublicKey(
                                (Element) currentChild, uri, storage, secureValidation
                            );

                        if (pk != null) {
                            return pk;
                        }
                    }
                }
                currentChild = currentChild.getNextSibling();
            }
        }
        return null;
    }

    /**
     * Searches the per-KeyInfo KeyResolvers for public keys
     *
     * @return The public key contained in this Node.
     * @throws KeyResolverException
     */
    PublicKey getPublicKeyFromInternalResolvers() throws KeyResolverException {
        for (KeyResolverSpi keyResolver : internalKeyResolvers) {
            LOG.debug("Try {}", keyResolver.getClass().getName());
            Node currentChild = getFirstChild();
            String uri = this.getBaseURI();
            while (currentChild != null)      {
                if (currentChild.getNodeType() == Node.ELEMENT_NODE) {
                    for (StorageResolver storage : storageResolvers) {
                        PublicKey pk =
                            keyResolver.engineLookupAndResolvePublicKey(
                                (Element) currentChild, uri, storage, secureValidation
                            );

                        if (pk != null) {
                            return pk;
                        }
                    }
                }
                currentChild = currentChild.getNextSibling();
            }
        }

        return null;
    }

    /**
     * Method getX509Certificate
     *
     * @return The certificate contained in this KeyInfo
     * @throws KeyResolverException
     */
    public X509Certificate getX509Certificate() throws KeyResolverException {
        // First search using the individual resolvers from the user
        X509Certificate cert = this.getX509CertificateFromInternalResolvers();

        if (cert != null) {
            LOG.debug("I could find a X509Certificate using the per-KeyInfo key resolvers");

            return cert;
        }
        LOG.debug("I couldn't find a X509Certificate using the per-KeyInfo key resolvers");

        // Then use the system-wide Resolvers
        cert = this.getX509CertificateFromStaticResolvers();

        if (cert != null) {
            LOG.debug("I could find a X509Certificate using the system-wide key resolvers");

            return cert;
        }
        LOG.debug("I couldn't find a X509Certificate using the system-wide key resolvers");

        return null;
    }

    /**
     * This method uses each System-wide {@link KeyResolver} to search the
     * child elements. Each combination of {@link KeyResolver} and child element
     * is checked against all {@link StorageResolver}s.
     *
     * @return The certificate contained in this KeyInfo
     * @throws KeyResolverException
     */
    X509Certificate getX509CertificateFromStaticResolvers()
        throws KeyResolverException {
        LOG.debug(
            "Start getX509CertificateFromStaticResolvers() with {} resolvers", KeyResolver.length()
        );
        String uri = this.getBaseURI();
        Iterator<KeyResolverSpi> it = KeyResolver.iterator();
        while (it.hasNext()) {
            KeyResolverSpi keyResolver = it.next();
            X509Certificate cert = applyCurrentResolver(uri, keyResolver);
            if (cert != null) {
                return cert;
            }
        }
        return null;
    }

    private X509Certificate applyCurrentResolver(
        String uri, KeyResolverSpi keyResolver
    ) throws KeyResolverException {
        Node currentChild = getFirstChild();
        while (currentChild != null)      {
            if (currentChild.getNodeType() == Node.ELEMENT_NODE) {
                for (StorageResolver storage : storageResolvers) {
                    X509Certificate cert =
                        keyResolver.engineLookupResolveX509Certificate(
                            (Element) currentChild, uri, storage, secureValidation
                        );

                    if (cert != null) {
                        return cert;
                    }
                }
            }
            currentChild = currentChild.getNextSibling();
        }
        return null;
    }

    /**
     * Method getX509CertificateFromInternalResolvers
     *
     * @return The certificate contained in this KeyInfo
     * @throws KeyResolverException
     */
    X509Certificate getX509CertificateFromInternalResolvers()
        throws KeyResolverException {
        LOG.debug(
            "Start getX509CertificateFromInternalResolvers() with {} resolvers",
            + this.lengthInternalKeyResolver()
        );
        String uri = this.getBaseURI();
        for (KeyResolverSpi keyResolver : internalKeyResolvers) {
            LOG.debug("Try {}", keyResolver.getClass().getName());
            X509Certificate cert = applyCurrentResolver(uri, keyResolver);
            if (cert != null) {
                return cert;
            }
        }

        return null;
    }

    /**
     * This method returns a secret (symmetric) key. This is for XML Encryption.
     * @return the secret key contained in this KeyInfo
     * @throws KeyResolverException
     */
    public SecretKey getSecretKey() throws KeyResolverException {
        SecretKey sk = this.getSecretKeyFromInternalResolvers();

        if (sk != null) {
            LOG.debug("I could find a secret key using the per-KeyInfo key resolvers");

            return sk;
        }
        LOG.debug("I couldn't find a secret key using the per-KeyInfo key resolvers");

        sk = this.getSecretKeyFromStaticResolvers();

        if (sk != null) {
            LOG.debug("I could find a secret key using the system-wide key resolvers");

            return sk;
        }
        LOG.debug("I couldn't find a secret key using the system-wide key resolvers");

        return null;
    }

    /**
     * Searches the library wide KeyResolvers for Secret keys
     *
     * @return the secret key contained in this KeyInfo
     * @throws KeyResolverException
     */
    SecretKey getSecretKeyFromStaticResolvers() throws KeyResolverException {
        Iterator<KeyResolverSpi> it = KeyResolver.iterator();
        while (it.hasNext()) {
            KeyResolverSpi keyResolver = it.next();

            Node currentChild = getFirstChild();
            String uri = this.getBaseURI();
            while (currentChild != null)      {
                if (currentChild.getNodeType() == Node.ELEMENT_NODE) {
                    for (StorageResolver storage : storageResolvers) {
                        SecretKey sk =
                            keyResolver.engineLookupAndResolveSecretKey(
                                (Element) currentChild, uri, storage, secureValidation
                            );

                        if (sk != null) {
                            return sk;
                        }
                    }
                }
                currentChild = currentChild.getNextSibling();
            }
        }
        return null;
    }

    /**
     * Searches the per-KeyInfo KeyResolvers for secret keys
     *
     * @return the secret key contained in this KeyInfo
     * @throws KeyResolverException
     */

    SecretKey getSecretKeyFromInternalResolvers() throws KeyResolverException {
        for (KeyResolverSpi keyResolver : internalKeyResolvers) {
            LOG.debug("Try {}", keyResolver.getClass().getName());
            Node currentChild = getFirstChild();
            String uri = this.getBaseURI();
            while (currentChild != null)      {
                if (currentChild.getNodeType() == Node.ELEMENT_NODE) {
                    for (StorageResolver storage : storageResolvers) {
                        SecretKey sk =
                            keyResolver.engineLookupAndResolveSecretKey(
                                (Element) currentChild, uri, storage, secureValidation
                            );

                        if (sk != null) {
                            return sk;
                        }
                    }
                }
                currentChild = currentChild.getNextSibling();
            }
        }

        return null;
    }

    /**
     * This method returns a private key. This is for Key Transport in XML Encryption.
     * @return the private key contained in this KeyInfo
     * @throws KeyResolverException
     */
    public PrivateKey getPrivateKey() throws KeyResolverException {
        PrivateKey pk = this.getPrivateKeyFromInternalResolvers();

        if (pk != null) {
            LOG.debug("I could find a private key using the per-KeyInfo key resolvers");
            return pk;
        }
        LOG.debug("I couldn't find a secret key using the per-KeyInfo key resolvers");

        pk = this.getPrivateKeyFromStaticResolvers();
        if (pk != null) {
            LOG.debug("I could find a private key using the system-wide key resolvers");
            return pk;
        }
        LOG.debug("I couldn't find a private key using the system-wide key resolvers");

        return null;
    }

    /**
     * Searches the library wide KeyResolvers for Private keys
     *
     * @return the private key contained in this KeyInfo
     * @throws KeyResolverException
     */
    PrivateKey getPrivateKeyFromStaticResolvers() throws KeyResolverException {
        Iterator<KeyResolverSpi> it = KeyResolver.iterator();
        while (it.hasNext()) {
            KeyResolverSpi keyResolver = it.next();

            Node currentChild = getFirstChild();
            String uri = this.getBaseURI();
            while (currentChild != null)      {
                if (currentChild.getNodeType() == Node.ELEMENT_NODE) {
                    // not using StorageResolvers at the moment
                    // since they cannot return private keys
                    PrivateKey pk =
                        keyResolver.engineLookupAndResolvePrivateKey(
                            (Element) currentChild, uri, null, secureValidation
                        );

                    if (pk != null) {
                        return pk;
                    }
                }
                currentChild = currentChild.getNextSibling();
            }
        }
        return null;
    }

    /**
     * Searches the per-KeyInfo KeyResolvers for private keys
     *
     * @return the private key contained in this KeyInfo
     * @throws KeyResolverException
     */
    PrivateKey getPrivateKeyFromInternalResolvers() throws KeyResolverException {
        for (KeyResolverSpi keyResolver : internalKeyResolvers) {
            LOG.debug("Try {}", keyResolver.getClass().getName());
            Node currentChild = getFirstChild();
            String uri = this.getBaseURI();
            while (currentChild != null) {
                if (currentChild.getNodeType() == Node.ELEMENT_NODE) {
                    // not using StorageResolvers at the moment
                    // since they cannot return private keys
                    PrivateKey pk =
                        keyResolver.engineLookupAndResolvePrivateKey(
                            (Element) currentChild, uri, null, secureValidation
                        );

                    if (pk != null) {
                        return pk;
                    }
                }
                currentChild = currentChild.getNextSibling();
            }
        }

        return null;
    }

    /**
     * This method is used to add a custom {@link KeyResolverSpi} to a KeyInfo
     * object.
     *
     * @param realKeyResolver
     */
    public void registerInternalKeyResolver(KeyResolverSpi realKeyResolver) {
        this.internalKeyResolvers.add(realKeyResolver);
    }

    /**
     * Method lengthInternalKeyResolver
     * @return the length of the key
     */
    int lengthInternalKeyResolver() {
        return this.internalKeyResolvers.size();
    }

    /**
     * Method itemInternalKeyResolver
     *
     * @param i the index
     * @return the KeyResolverSpi for the index.
     */
    KeyResolverSpi itemInternalKeyResolver(int i) {
        return this.internalKeyResolvers.get(i);
    }

    /**
     * Method addStorageResolver
     *
     * @param storageResolver
     */
    public void addStorageResolver(StorageResolver storageResolver) {
        if (storageResolvers == nullList) {
            // Replace the default null StorageResolver
            storageResolvers = new ArrayList<>();
        }
        this.storageResolvers.add(storageResolver);
    }


    /** {@inheritDoc} */
    public String getBaseLocalName() {
        return Constants._TAG_KEYINFO;
    }
}
