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
package com.sun.org.apache.xml.internal.security.signature;

import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sun.org.apache.xml.internal.security.c14n.CanonicalizationException;
import com.sun.org.apache.xml.internal.security.c14n.InvalidCanonicalizerException;
import com.sun.org.apache.xml.internal.security.exceptions.XMLSecurityException;
import com.sun.org.apache.xml.internal.security.parser.XMLParserException;
import com.sun.org.apache.xml.internal.security.transforms.Transforms;
import com.sun.org.apache.xml.internal.security.utils.Constants;
import com.sun.org.apache.xml.internal.security.utils.I18n;
import com.sun.org.apache.xml.internal.security.utils.SignatureElementProxy;
import com.sun.org.apache.xml.internal.security.utils.XMLUtils;
import com.sun.org.apache.xml.internal.security.utils.resolver.ResourceResolverSpi;
import org.w3c.dom.Attr;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Handles {@code &lt;ds:Manifest&gt;} elements.
 * <p> This element holds the {@code Reference} elements</p>
 */
public class Manifest extends SignatureElementProxy {

    /**
     * The default maximum number of references per Manifest, if secure validation is enabled.
     */
    public static final int MAXIMUM_REFERENCE_COUNT = 30;

    private static final com.sun.org.slf4j.internal.Logger LOG =
        com.sun.org.slf4j.internal.LoggerFactory.getLogger(Manifest.class);

    @SuppressWarnings("removal")
    private static Integer referenceCount =
        AccessController.doPrivileged(
            (PrivilegedAction<Integer>) () -> Integer.parseInt(System.getProperty("com.sun.org.apache.xml.internal.security.maxReferences",
                                                                Integer.toString(MAXIMUM_REFERENCE_COUNT))));

    /** Field references */
    private List<Reference> references;
    private Element[] referencesEl;

    /** Field verificationResults[] */
    private List<VerifiedReference> verificationResults;

    /** Field resolverProperties */
    private Map<String, String> resolverProperties;

    /** Field perManifestResolvers */
    private List<ResourceResolverSpi> perManifestResolvers;

    private boolean secureValidation;

    /**
     * Constructs {@link Manifest}
     *
     * @param doc the {@link Document} in which {@code XMLsignature} is placed
     */
    public Manifest(Document doc) {
        super(doc);

        addReturnToSelf();

        this.references = new ArrayList<>();
    }

    /**
     * Constructor Manifest
     *
     * @param element
     * @param baseURI
     * @throws XMLSecurityException
     */
    public Manifest(Element element, String baseURI) throws XMLSecurityException {
        this(element, baseURI, true);

    }
    /**
     * Constructor Manifest
     *
     * @param element
     * @param baseURI
     * @param secureValidation
     * @throws XMLSecurityException
     */
    public Manifest(
        Element element, String baseURI, boolean secureValidation
    ) throws XMLSecurityException {
        super(element, baseURI);

        Attr attr = element.getAttributeNodeNS(null, "Id");
        if (attr != null) {
            element.setIdAttributeNode(attr, true);
        }
        this.secureValidation = secureValidation;

        // check out Reference children
        this.referencesEl =
            XMLUtils.selectDsNodes(
                getFirstChild(), Constants._TAG_REFERENCE
            );
        int le = this.referencesEl.length;
        if (le == 0) {
            // At least one Reference must be present. Bad.
            Object[] exArgs = { Constants._TAG_REFERENCE, Constants._TAG_MANIFEST };

            throw new DOMException(DOMException.WRONG_DOCUMENT_ERR,
                                   I18n.translate("xml.WrongContent", exArgs));
        }

        if (secureValidation && le > referenceCount) {
            Object[] exArgs = { le, referenceCount };

            throw new XMLSecurityException("signature.tooManyReferences", exArgs);
        }

        // create List
        this.references = new ArrayList<>(le);

        for (int i = 0; i < le; i++) {
            Element refElem = referencesEl[i];
            Attr refAttr = refElem.getAttributeNodeNS(null, "Id");
            if (refAttr != null) {
                refElem.setIdAttributeNode(refAttr, true);
            }
            this.references.add(null);
        }
    }

    /**
     * This {@code addDocument} method is used to add a new resource to the
     * signed info. A {@link com.sun.org.apache.xml.internal.security.signature.Reference} is built
     * from the supplied values.
     *
     * @param baseURI the URI of the resource where the XML instance was stored
     * @param referenceURI {@code URI} attribute in {@code Reference} for specifying
     * where data is
     * @param transforms com.sun.org.apache.xml.internal.security.signature.Transforms object with an ordered
     * list of transformations to be performed.
     * @param digestURI The digest algorithm URI to be used.
     * @param referenceId
     * @param referenceType
     * @throws XMLSignatureException
     */
    public void addDocument(
        String baseURI, String referenceURI, Transforms transforms,
        String digestURI, String referenceId, String referenceType
    ) throws XMLSignatureException {
        // the this.doc is handed implicitly by the this.getOwnerDocument()
        Reference ref =
            new Reference(getDocument(), baseURI, referenceURI, this, transforms, digestURI);

        if (referenceId != null) {
            ref.setId(referenceId);
        }

        if (referenceType != null) {
            ref.setType(referenceType);
        }

        // add Reference object to our cache vector
        this.references.add(ref);

        // add the Element of the Reference object to the Manifest/SignedInfo
        appendSelf(ref);
        addReturnToSelf();
    }

    /**
     * The calculation of the DigestValues in the References must be after the
     * References are already added to the document and during the signing
     * process. This ensures that all necessary data is in place.
     *
     * @throws ReferenceNotInitializedException
     * @throws XMLSignatureException
     */
    public void generateDigestValues()
        throws XMLSignatureException, ReferenceNotInitializedException {
        for (int i = 0; i < this.getLength(); i++) {
            // update the cached Reference object, the Element content is automatically updated
            Reference currentRef = this.references.get(i);
            currentRef.generateDigestValue();
        }
    }

    /**
     * Return the nonnegative number of added references.
     *
     * @return the number of references
     */
    public int getLength() {
        return this.references.size();
    }

    /**
     * Return the <i>i</i><sup>th</sup> reference. Valid {@code i}
     * values are 0 to {@code {link@ getSize}-1}.
     *
     * @param i Index of the requested {@link Reference}
     * @return the <i>i</i><sup>th</sup> reference
     * @throws XMLSecurityException
     */
    public Reference item(int i) throws XMLSecurityException {
        if (this.references.get(i) == null) {
            // not yet constructed, so _we_ have to
            Reference ref =
                new Reference(referencesEl[i], this.baseURI, this, secureValidation);

            this.references.set(i, ref);
        }

        return this.references.get(i);
    }

    /**
     * Sets the {@code Id} attribute
     *
     * @param Id the {@code Id} attribute in {@code ds:Manifest}
     */
    public void setId(String Id) {
        if (Id != null) {
            setLocalIdAttribute(Constants._ATT_ID, Id);
        }
    }

    /**
     * Returns the {@code Id} attribute
     *
     * @return the {@code Id} attribute in {@code ds:Manifest}
     */
    public String getId() {
        return getLocalAttribute(Constants._ATT_ID);
    }

    /**
     * Used to do a <A HREF="http://www.w3.org/TR/xmldsig-core/#def-ValidationReference">reference
     * validation</A> of all enclosed references using the {@link Reference#verify} method.
     *
     * <p>This step loops through all {@link Reference}s and does verify the hash
     * values. If one or more verifications fail, the method returns
     * {@code false}. If <i>all</i> verifications are successful,
     * it returns {@code true}. The results of the individual reference
     * validations are available by using the {@link #getVerificationResult(int)} method
     *
     * @return true if all References verify, false if one or more do not verify.
     * @throws MissingResourceFailureException if a {@link Reference} does not verify
     * (throws a {@link com.sun.org.apache.xml.internal.security.signature.ReferenceNotInitializedException}
     * because of an uninitialized {@link XMLSignatureInput}
     * @see com.sun.org.apache.xml.internal.security.signature.Reference#verify
     * @see com.sun.org.apache.xml.internal.security.signature.SignedInfo#verify()
     * @see com.sun.org.apache.xml.internal.security.signature.MissingResourceFailureException
     * @throws XMLSecurityException
     */
    public boolean verifyReferences()
        throws MissingResourceFailureException, XMLSecurityException {
        return this.verifyReferences(false);
    }

    /**
     * Used to do a <A HREF="http://www.w3.org/TR/xmldsig-core/#def-ValidationReference">reference
     * validation</A> of all enclosed references using the {@link Reference#verify} method.
     *
     * <p>This step loops through all {@link Reference}s and does verify the hash
     * values. If one or more verifications fail, the method returns
     * {@code false}. If <i>all</i> verifications are successful,
     * it returns {@code true}. The results of the individual reference
     * validations are available by using the {@link #getVerificationResult(int)} method
     *
     * @param followManifests
     * @return true if all References verify, false if one or more do not verify.
     * @throws MissingResourceFailureException if a {@link Reference} does not verify
     * (throws a {@link com.sun.org.apache.xml.internal.security.signature.ReferenceNotInitializedException}
     * because of an uninitialized {@link XMLSignatureInput}
     * @see com.sun.org.apache.xml.internal.security.signature.Reference#verify
     * @see com.sun.org.apache.xml.internal.security.signature.SignedInfo#verify(boolean)
     * @see com.sun.org.apache.xml.internal.security.signature.MissingResourceFailureException
     * @throws XMLSecurityException
     */
    public boolean verifyReferences(boolean followManifests)
        throws MissingResourceFailureException, XMLSecurityException {
        if (referencesEl == null) {
            this.referencesEl =
                XMLUtils.selectDsNodes(
                    getFirstChild(), Constants._TAG_REFERENCE
                );
        }
        LOG.debug("verify {} References", referencesEl.length);
        LOG.debug("I am {} requested to follow nested Manifests", followManifests
            ? "" : "not");
        if (referencesEl.length == 0) {
            throw new XMLSecurityException("empty", new Object[]{"References are empty"});
        }
        if (secureValidation && referencesEl.length > referenceCount) {
            Object[] exArgs = { referencesEl.length, referenceCount };

            throw new XMLSecurityException("signature.tooManyReferences", exArgs);
        }

        this.verificationResults = new ArrayList<>(referencesEl.length);
        boolean verify = true;
        for (int i = 0; i < this.referencesEl.length; i++) {
            Reference currentRef =
                new Reference(referencesEl[i], this.baseURI, this, secureValidation);

            this.references.set(i, currentRef);

            // if only one item does not verify, the whole verification fails
            try {
                boolean currentRefVerified = currentRef.verify();

                if (!currentRefVerified) {
                    verify = false;
                }
                LOG.debug("The Reference has Type {}", currentRef.getType());

                List<VerifiedReference> manifestReferences = Collections.emptyList();

                // was verification successful till now and do we want to verify the Manifest?
                if (verify && followManifests && currentRef.typeIsReferenceToManifest()) {
                    LOG.debug("We have to follow a nested Manifest");

                    try {
                        XMLSignatureInput signedManifestNodes =
                            currentRef.dereferenceURIandPerformTransforms(null);
                        Set<Node> nl = signedManifestNodes.getNodeSet();
                        Manifest referencedManifest = null;
                        Iterator<Node> nlIterator = nl.iterator();

                        while (nlIterator.hasNext()) {
                            Node n = nlIterator.next();

                            if (n.getNodeType() == Node.ELEMENT_NODE
                                && ((Element) n).getNamespaceURI().equals(Constants.SignatureSpecNS)
                                && ((Element) n).getLocalName().equals(Constants._TAG_MANIFEST)
                            ) {
                                try {
                                    referencedManifest =
                                        new Manifest(
                                             (Element)n, signedManifestNodes.getSourceURI(), secureValidation
                                        );
                                    break;
                                } catch (XMLSecurityException ex) {
                                    LOG.debug(ex.getMessage(), ex);
                                    // Hm, seems not to be a ds:Manifest
                                }
                            }
                        }

                        if (referencedManifest == null) {
                            // The Reference stated that it points to a ds:Manifest
                            // but we did not find a ds:Manifest in the signed area
                            throw new MissingResourceFailureException(currentRef, "empty",
                                                                      new Object[]{"No Manifest found"});
                        }

                        referencedManifest.perManifestResolvers = this.perManifestResolvers;
                        referencedManifest.resolverProperties = this.resolverProperties;

                        boolean referencedManifestValid =
                            referencedManifest.verifyReferences(followManifests);

                        if (!referencedManifestValid) {
                            verify = false;

                            LOG.warn("The nested Manifest was invalid (bad)");
                        } else {
                            LOG.debug("The nested Manifest was valid (good)");
                        }

                        manifestReferences = referencedManifest.getVerificationResults();
                    } catch (IOException ex) {
                        throw new ReferenceNotInitializedException(ex);
                    } catch (XMLParserException ex) {
                        throw new ReferenceNotInitializedException(ex);
                    }
                }

                verificationResults.add(new VerifiedReference(currentRefVerified, currentRef.getURI(), manifestReferences));
            } catch (ReferenceNotInitializedException ex) {
                Object[] exArgs = { currentRef.getURI() };

                throw new MissingResourceFailureException(
                    ex, currentRef, "signature.Verification.Reference.NoInput", exArgs
                );
            }
        }

        return verify;
    }

    /**
     * After verifying a {@link Manifest} or a {@link SignedInfo} using the
     * {@link Manifest#verifyReferences()} or {@link SignedInfo#verify()} methods,
     * the individual results can be retrieved with this method.
     *
     * @param index an index of into a {@link Manifest} or a {@link SignedInfo}
     * @return the results of reference validation at the specified index
     * @throws XMLSecurityException
     */
    public boolean getVerificationResult(int index) throws XMLSecurityException {
        if (index < 0 || index > this.getLength() - 1) {
            Object[] exArgs = { Integer.toString(index), Integer.toString(this.getLength()) };
            Exception e =
                new IndexOutOfBoundsException(
                    I18n.translate("signature.Verification.IndexOutOfBounds", exArgs)
                );

            throw new XMLSecurityException(e);
        }

        if (this.verificationResults == null) {
            try {
                this.verifyReferences();
            } catch (Exception ex) {
                throw new XMLSecurityException(ex);
            }
        }

        return verificationResults.get(index).isValid();
    }

    /**
     * Get the list of verification result objects
     */
    public List<VerifiedReference> getVerificationResults() {
        if (verificationResults == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(verificationResults);
    }

    /**
     * Adds Resource Resolver for retrieving resources at specified {@code URI} attribute
     * in {@code reference} element
     *
     * @param resolver {@link ResourceResolverSpi} can provide the implementation subclass of
     * {@link ResourceResolverSpi} for retrieving resource.
     */
    public void addResourceResolver(ResourceResolverSpi resolver) {
        if (resolver == null) {
            return;
        }
        if (perManifestResolvers == null) {
            perManifestResolvers = new ArrayList<>();
        }
        this.perManifestResolvers.add(resolver);
    }

    /**
     * Get the Per-Manifest Resolver List
     * @return the per-manifest Resolver List
     */
    public List<ResourceResolverSpi> getPerManifestResolvers() {
        return perManifestResolvers;
    }

    /**
     * Get the resolver property map
     * @return the resolver property map
     */
    public Map<String, String> getResolverProperties() {
        return resolverProperties;
    }

    /**
     * Used to pass parameters like proxy servers etc to the ResourceResolver
     * implementation.
     *
     * @param key the key
     * @param value the value
     */
    public void setResolverProperty(String key, String value) {
        if (resolverProperties == null) {
            resolverProperties = new HashMap<>(10);
        }
        this.resolverProperties.put(key, value);
    }

    /**
     * Returns the value at specified key
     *
     * @param key the key
     * @return the value
     */
    public String getResolverProperty(String key) {
        return this.resolverProperties.get(key);
    }

    /**
     * Method getSignedContentItem
     *
     * @param i
     * @return The signed content of the i reference.
     *
     * @throws XMLSignatureException
     */
    public byte[] getSignedContentItem(int i) throws XMLSignatureException {
        try {
            return this.getReferencedContentAfterTransformsItem(i).getBytes();
        } catch (IOException ex) {
            throw new XMLSignatureException(ex);
        } catch (CanonicalizationException ex) {
            throw new XMLSignatureException(ex);
        } catch (InvalidCanonicalizerException ex) {
            throw new XMLSignatureException(ex);
        } catch (XMLSecurityException ex) {
            throw new XMLSignatureException(ex);
        }
    }

    /**
     * Method getReferencedContentPriorTransformsItem
     *
     * @param i
     * @return The contents before transformation of the reference i.
     * @throws XMLSecurityException
     */
    public XMLSignatureInput getReferencedContentBeforeTransformsItem(int i)
        throws XMLSecurityException {
        return this.item(i).getContentsBeforeTransformation();
    }

    /**
     * Method getReferencedContentAfterTransformsItem
     *
     * @param i
     * @return The contents after transformation of the reference i.
     * @throws XMLSecurityException
     */
    public XMLSignatureInput getReferencedContentAfterTransformsItem(int i)
        throws XMLSecurityException {
        return this.item(i).getContentsAfterTransformation();
    }

    /**
     * Method getSignedContentLength
     *
     * @return The number of references contained in this reference.
     */
    public int getSignedContentLength() {
        return this.getLength();
    }

    /**
     * Method getBaseLocalName
     *
     * {@inheritDoc}
     */
    public String getBaseLocalName() {
        return Constants._TAG_MANIFEST;
    }

    public boolean isSecureValidation() {
        return secureValidation;
    }
}
