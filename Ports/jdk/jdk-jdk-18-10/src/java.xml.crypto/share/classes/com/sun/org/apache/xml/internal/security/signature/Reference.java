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
import java.io.OutputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.sun.org.apache.xml.internal.security.algorithms.Algorithm;
import com.sun.org.apache.xml.internal.security.algorithms.MessageDigestAlgorithm;
import com.sun.org.apache.xml.internal.security.c14n.CanonicalizationException;
import com.sun.org.apache.xml.internal.security.exceptions.XMLSecurityException;
import com.sun.org.apache.xml.internal.security.signature.reference.ReferenceData;
import com.sun.org.apache.xml.internal.security.signature.reference.ReferenceNodeSetData;
import com.sun.org.apache.xml.internal.security.signature.reference.ReferenceOctetStreamData;
import com.sun.org.apache.xml.internal.security.signature.reference.ReferenceSubTreeData;
import com.sun.org.apache.xml.internal.security.transforms.InvalidTransformException;
import com.sun.org.apache.xml.internal.security.transforms.Transform;
import com.sun.org.apache.xml.internal.security.transforms.TransformationException;
import com.sun.org.apache.xml.internal.security.transforms.Transforms;
import com.sun.org.apache.xml.internal.security.transforms.params.InclusiveNamespaces;
import com.sun.org.apache.xml.internal.security.utils.Constants;
import com.sun.org.apache.xml.internal.security.utils.DigesterOutputStream;
import com.sun.org.apache.xml.internal.security.utils.SignatureElementProxy;
import com.sun.org.apache.xml.internal.security.utils.UnsyncBufferedOutputStream;
import com.sun.org.apache.xml.internal.security.utils.XMLUtils;
import com.sun.org.apache.xml.internal.security.utils.resolver.ResourceResolver;
import com.sun.org.apache.xml.internal.security.utils.resolver.ResourceResolverContext;
import com.sun.org.apache.xml.internal.security.utils.resolver.ResourceResolverException;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

/**
 * Handles {@code &lt;ds:Reference&gt;} elements.
 *
 * This includes:
 *
 * Construct a {@code ds:Reference} from an {@link org.w3c.dom.Element}.
 *
 * <p>Create a new reference</p>
 * <pre>
 * Document doc;
 * MessageDigestAlgorithm sha1 = MessageDigestAlgorithm.getInstance("http://#sha1");
 * Reference ref = new Reference(new XMLSignatureInput(new FileInputStream("1.gif"),
 *                               "http://localhost/1.gif",
 *                               (Transforms) null, sha1);
 * Element refElem = ref.toElement(doc);
 * </pre>
 *
 * <p>Verify a reference</p>
 * <pre>
 * Element refElem = doc.getElement("Reference"); // PSEUDO
 * Reference ref = new Reference(refElem);
 * String url = ref.getURI();
 * ref.setData(new XMLSignatureInput(new FileInputStream(url)));
 * if (ref.verify()) {
 *    System.out.println("verified");
 * }
 * </pre>
 *
 * <pre>
 * &lt;element name="Reference" type="ds:ReferenceType"/&gt;
 *  &lt;complexType name="ReferenceType"&gt;
 *    &lt;sequence&gt;
 *      &lt;element ref="ds:Transforms" minOccurs="0"/&gt;
 *      &lt;element ref="ds:DigestMethod"/&gt;
 *      &lt;element ref="ds:DigestValue"/&gt;
 *    &lt;/sequence&gt;
 *    &lt;attribute name="Id" type="ID" use="optional"/&gt;
 *    &lt;attribute name="URI" type="anyURI" use="optional"/&gt;
 *    &lt;attribute name="Type" type="anyURI" use="optional"/&gt;
 *  &lt;/complexType&gt;
 * </pre>
 *
 * @see ObjectContainer
 * @see Manifest
 */
public class Reference extends SignatureElementProxy {

    /** Field OBJECT_URI */
    public static final String OBJECT_URI = Constants.SignatureSpecNS + Constants._TAG_OBJECT;

    /** Field MANIFEST_URI */
    public static final String MANIFEST_URI = Constants.SignatureSpecNS + Constants._TAG_MANIFEST;

    /**
     * The maximum number of transforms per reference, if secure validation is enabled.
     */
    public static final int MAXIMUM_TRANSFORM_COUNT = 5;

    private boolean secureValidation;

    /**
     * Look up useC14N11 system property. If true, an explicit C14N11 transform
     * will be added if necessary when generating the signature. See section
     * 3.1.1 of http://www.w3.org/2007/xmlsec/Drafts/xmldsig-core/ for more info.
     */
    @SuppressWarnings("removal")
    private static boolean useC14N11 =
        AccessController.doPrivileged((PrivilegedAction<Boolean>)
            () -> Boolean.getBoolean("com.sun.org.apache.xml.internal.security.useC14N11"));

    private static final com.sun.org.slf4j.internal.Logger LOG =
        com.sun.org.slf4j.internal.LoggerFactory.getLogger(Reference.class);

    private Manifest manifest;
    private XMLSignatureInput transformsOutput;

    private Transforms transforms;

    private Element digestMethodElem;

    private Element digestValueElement;

    private ReferenceData referenceData;

    private static final Set<String> TRANSFORM_ALGORITHMS;

    static {
        Set<String> algorithms = new HashSet<>();
        algorithms.add(Transforms.TRANSFORM_C14N_EXCL_OMIT_COMMENTS);
        algorithms.add(Transforms.TRANSFORM_C14N_EXCL_WITH_COMMENTS);
        algorithms.add(Transforms.TRANSFORM_C14N_OMIT_COMMENTS);
        algorithms.add(Transforms.TRANSFORM_C14N_WITH_COMMENTS);
        algorithms.add(Transforms.TRANSFORM_C14N11_OMIT_COMMENTS);
        algorithms.add(Transforms.TRANSFORM_C14N11_WITH_COMMENTS);
        TRANSFORM_ALGORITHMS = Collections.unmodifiableSet(algorithms);
    }

    /**
     * Constructor Reference
     *
     * @param doc the {@link Document} in which {@code XMLsignature} is placed
     * @param baseURI the URI of the resource where the XML instance will be stored
     * @param referenceURI URI indicate where is data which will digested
     * @param manifest
     * @param transforms {@link Transforms} applied to data
     * @param messageDigestAlgorithm {@link MessageDigestAlgorithm Digest algorithm} which is
     * applied to the data
     * TODO should we throw XMLSignatureException if MessageDigestAlgoURI is wrong?
     */
    protected Reference(
        Document doc, String baseURI, String referenceURI, Manifest manifest,
        Transforms transforms, String messageDigestAlgorithm
    ) throws XMLSignatureException {
        super(doc);

        addReturnToSelf();

        this.baseURI = baseURI;
        this.manifest = manifest;

        this.setURI(referenceURI);

        // important: The ds:Reference must be added to the associated ds:Manifest
        //            or ds:SignedInfo _before_ the this.resolverResult() is called.
        // this.manifest.appendChild(this.constructionElement);
        // this.manifest.appendChild(this.doc.createTextNode("\n"));

        if (transforms != null) {
            this.transforms = transforms;
            appendSelf(transforms);
            addReturnToSelf();
        }

        // Create DigestMethod Element without actually instantiating a MessageDigest Object
        Algorithm digestAlgorithm = new Algorithm(getDocument(), messageDigestAlgorithm) {
            public String getBaseNamespace() {
                return Constants.SignatureSpecNS;
            }

            public String getBaseLocalName() {
                return Constants._TAG_DIGESTMETHOD;
            }
        };

        digestMethodElem = digestAlgorithm.getElement();

        appendSelf(digestMethodElem);
        addReturnToSelf();

        digestValueElement =
            XMLUtils.createElementInSignatureSpace(getDocument(), Constants._TAG_DIGESTVALUE);

        appendSelf(digestValueElement);
        addReturnToSelf();
    }


    /**
     * Build a {@link Reference} from an {@link Element}
     *
     * @param element {@code Reference} element
     * @param baseURI the URI of the resource where the XML instance was stored
     * @param manifest is the {@link Manifest} of {@link SignedInfo} in which the Reference occurs.
     * We need this because the Manifest has the individual {@link ResourceResolver}s which have
     * been set by the user
     * @throws XMLSecurityException
     */
    protected Reference(Element element, String baseURI, Manifest manifest) throws XMLSecurityException {
        this(element, baseURI, manifest, true);
    }

    /**
     * Build a {@link Reference} from an {@link Element}
     *
     * @param element {@code Reference} element
     * @param baseURI the URI of the resource where the XML instance was stored
     * @param manifest is the {@link Manifest} of {@link SignedInfo} in which the Reference occurs.
     * @param secureValidation whether secure validation is enabled or not
     * We need this because the Manifest has the individual {@link ResourceResolver}s which have
     * been set by the user
     * @throws XMLSecurityException
     */
    protected Reference(Element element, String baseURI, Manifest manifest, boolean secureValidation)
        throws XMLSecurityException {
        super(element, baseURI);
        this.secureValidation = secureValidation;
        this.baseURI = baseURI;
        Element el = XMLUtils.getNextElement(element.getFirstChild());

        if (el != null && Constants._TAG_TRANSFORMS.equals(el.getLocalName())
            && Constants.SignatureSpecNS.equals(el.getNamespaceURI())) {
            transforms = new Transforms(el, this.baseURI);
            transforms.setSecureValidation(secureValidation);
            if (secureValidation && transforms.getLength() > MAXIMUM_TRANSFORM_COUNT) {
                Object[] exArgs = { transforms.getLength(), MAXIMUM_TRANSFORM_COUNT };

                throw new XMLSecurityException("signature.tooManyTransforms", exArgs);
            }
            el = XMLUtils.getNextElement(el.getNextSibling());
        }

        digestMethodElem = el;
        if (digestMethodElem == null ||
            !(Constants.SignatureSpecNS.equals(digestMethodElem.getNamespaceURI())
                && Constants._TAG_DIGESTMETHOD.equals(digestMethodElem.getLocalName()))) {
            throw new XMLSecurityException("signature.Reference.NoDigestMethod");
        }

        digestValueElement = XMLUtils.getNextElement(digestMethodElem.getNextSibling());
        if (digestValueElement == null ||
            !(Constants.SignatureSpecNS.equals(digestValueElement.getNamespaceURI())
                && Constants._TAG_DIGESTVALUE.equals(digestValueElement.getLocalName()))) {
            throw new XMLSecurityException("signature.Reference.NoDigestValue");
        }
        this.manifest = manifest;
    }

    /**
     * Returns {@link MessageDigestAlgorithm}
     *
     *
     * @return {@link MessageDigestAlgorithm}
     *
     * @throws XMLSignatureException
     */
    public MessageDigestAlgorithm getMessageDigestAlgorithm() throws XMLSignatureException {
        if (digestMethodElem == null) {
            return null;
        }

        String uri = digestMethodElem.getAttributeNS(null, Constants._ATT_ALGORITHM);

        if (uri.isEmpty()) {
            return null;
        }

        if (secureValidation && MessageDigestAlgorithm.ALGO_ID_DIGEST_NOT_RECOMMENDED_MD5.equals(uri)) {
            Object[] exArgs = { uri };

            throw new XMLSignatureException("signature.signatureAlgorithm", exArgs);
        }

        return MessageDigestAlgorithm.getInstance(getDocument(), uri);
    }

    /**
     * Sets the {@code URI} of this {@code Reference} element
     *
     * @param uri the {@code URI} of this {@code Reference} element
     */
    public void setURI(String uri) {
        if (uri != null) {
            setLocalAttribute(Constants._ATT_URI, uri);
        }
    }

    /**
     * Returns the {@code URI} of this {@code Reference} element
     *
     * @return URI the {@code URI} of this {@code Reference} element
     */
    public String getURI() {
        return getLocalAttribute(Constants._ATT_URI);
    }

    /**
     * Sets the {@code Id} attribute of this {@code Reference} element
     *
     * @param id the {@code Id} attribute of this {@code Reference} element
     */
    public void setId(String id) {
        if (id != null) {
            setLocalIdAttribute(Constants._ATT_ID, id);
        }
    }

    /**
     * Returns the {@code Id} attribute of this {@code Reference} element
     *
     * @return Id the {@code Id} attribute of this {@code Reference} element
     */
    public String getId() {
        return getLocalAttribute(Constants._ATT_ID);
    }

    /**
     * Sets the {@code type} atttibute of the Reference indicate whether an
     * {@code ds:Object}, {@code ds:SignatureProperty}, or {@code ds:Manifest}
     * element.
     *
     * @param type the {@code type} attribute of the Reference
     */
    public void setType(String type) {
        if (type != null) {
            setLocalAttribute(Constants._ATT_TYPE, type);
        }
    }

    /**
     * Return the {@code type} attribute of the Reference indicate whether an
     * {@code ds:Object}, {@code ds:SignatureProperty}, or {@code ds:Manifest}
     * element
     *
     * @return the {@code type} attribute of the Reference
     */
    public String getType() {
        return getLocalAttribute(Constants._ATT_TYPE);
    }

    /**
     * Method isReferenceToObject
     *
     * This returns true if the {@code Type} attribute of the
     * {@code Reference} element points to a {@code #Object} element
     *
     * @return true if the Reference type indicates that this Reference points to an
     * {@code Object}
     */
    public boolean typeIsReferenceToObject() {
        return Reference.OBJECT_URI.equals(this.getType());
    }

    /**
     * Method isReferenceToManifest
     *
     * This returns true if the {@code Type} attribute of the
     * {@code Reference} element points to a {@code #Manifest} element
     *
     * @return true if the Reference type indicates that this Reference points to a
     * {@link Manifest}
     */
    public boolean typeIsReferenceToManifest() {
        return Reference.MANIFEST_URI.equals(this.getType());
    }

    /**
     * Method setDigestValueElement
     *
     * @param digestValue
     */
    private void setDigestValueElement(byte[] digestValue) {
        Node n = digestValueElement.getFirstChild();
        while (n != null) {
            digestValueElement.removeChild(n);
            n = n.getNextSibling();
        }

        String base64codedValue = XMLUtils.encodeToString(digestValue);
        Text t = createText(base64codedValue);

        digestValueElement.appendChild(t);
    }

    /**
     * Method generateDigestValue
     *
     * @throws ReferenceNotInitializedException
     * @throws XMLSignatureException
     */
    public void generateDigestValue()
        throws XMLSignatureException, ReferenceNotInitializedException {
        this.setDigestValueElement(this.calculateDigest(false));
    }

    /**
     * Returns the XMLSignatureInput which is created by de-referencing the URI attribute.
     * @return the XMLSignatureInput of the source of this reference
     * @throws ReferenceNotInitializedException If the resolver found any
     * problem resolving the reference
     */
    public XMLSignatureInput getContentsBeforeTransformation()
        throws ReferenceNotInitializedException {
        try {
            Attr uriAttr =
                getElement().getAttributeNodeNS(null, Constants._ATT_URI);

            ResourceResolverContext resolverContext =
                new ResourceResolverContext(uriAttr, this.baseURI,
                    secureValidation, this.manifest.getResolverProperties());

            return ResourceResolver.resolve(this.manifest.getPerManifestResolvers(), resolverContext);
        }  catch (ResourceResolverException ex) {
            throw new ReferenceNotInitializedException(ex);
        }
    }

    private XMLSignatureInput getContentsAfterTransformation(
        XMLSignatureInput input, OutputStream os
    ) throws XMLSignatureException {
        try {
            Transforms transforms = this.getTransforms();
            XMLSignatureInput output = null;

            if (transforms != null) {
                output = transforms.performTransforms(input, os);
                this.transformsOutput = output;//new XMLSignatureInput(output.getBytes());

                //this.transformsOutput.setSourceURI(output.getSourceURI());
            } else {
                output = input;
            }

            return output;
        } catch (XMLSecurityException ex) {
            throw new XMLSignatureException(ex);
        }
    }

    /**
     * Returns the XMLSignatureInput which is the result of the Transforms.
     * @return a XMLSignatureInput with all transformations applied.
     * @throws XMLSignatureException
     */
    public XMLSignatureInput getContentsAfterTransformation()
        throws XMLSignatureException {
        XMLSignatureInput input = this.getContentsBeforeTransformation();
        cacheDereferencedElement(input);

        return this.getContentsAfterTransformation(input, null);
    }

    /**
     * This method returns the XMLSignatureInput which represents the node set before
     * some kind of canonicalization is applied for the first time.
     * @return Gets a the node doing everything till the first c14n is needed
     *
     * @throws XMLSignatureException
     */
    public XMLSignatureInput getNodesetBeforeFirstCanonicalization()
        throws XMLSignatureException {
        try {
            XMLSignatureInput input = this.getContentsBeforeTransformation();
            cacheDereferencedElement(input);
            XMLSignatureInput output = input;
            Transforms transforms = this.getTransforms();

            if (transforms != null) {
                for (int i = 0; i < transforms.getLength(); i++) {
                    Transform t = transforms.item(i);
                    String uri = t.getURI();

                    if (TRANSFORM_ALGORITHMS.contains(uri)) {
                        break;
                    }

                    output = t.performTransform(output, null, secureValidation);
                }

                output.setSourceURI(input.getSourceURI());
            }
            return output;
        } catch (IOException | XMLSecurityException ex) {
            throw new XMLSignatureException(ex);
        }
    }

    /**
     * Method getHTMLRepresentation
     * @return The HTML of the transformation
     * @throws XMLSignatureException
     */
    public String getHTMLRepresentation() throws XMLSignatureException {
        try {
            XMLSignatureInput nodes = this.getNodesetBeforeFirstCanonicalization();

            Transforms transforms = this.getTransforms();
            Transform c14nTransform = null;

            if (transforms != null) {
                for (int i = 0; i < transforms.getLength(); i++) {
                    Transform t = transforms.item(i);
                    String uri = t.getURI();

                    if (uri.equals(Transforms.TRANSFORM_C14N_EXCL_OMIT_COMMENTS)
                        || uri.equals(Transforms.TRANSFORM_C14N_EXCL_WITH_COMMENTS)) {
                        c14nTransform = t;
                        break;
                    }
                }
            }

            Set<String> inclusiveNamespaces = new HashSet<>();
            if (c14nTransform != null
                && c14nTransform.length(
                    InclusiveNamespaces.ExclusiveCanonicalizationNamespace,
                    InclusiveNamespaces._TAG_EC_INCLUSIVENAMESPACES) == 1) {

                // there is one InclusiveNamespaces element
                InclusiveNamespaces in =
                    new InclusiveNamespaces(
                        XMLUtils.selectNode(
                            c14nTransform.getElement().getFirstChild(),
                            InclusiveNamespaces.ExclusiveCanonicalizationNamespace,
                            InclusiveNamespaces._TAG_EC_INCLUSIVENAMESPACES,
                            0
                        ), this.getBaseURI());

                inclusiveNamespaces =
                    InclusiveNamespaces.prefixStr2Set(in.getInclusiveNamespaces());
            }

            return nodes.getHTMLRepresentation(inclusiveNamespaces);
        } catch (XMLSecurityException ex) {
            throw new XMLSignatureException(ex);
        }
    }

    /**
     * This method only works after a call to verify.
     * @return the transformed output(i.e. what is going to be digested).
     */
    public XMLSignatureInput getTransformsOutput() {
        return this.transformsOutput;
    }

    /**
     * Get the ReferenceData that corresponds to the cached representation of the dereferenced
     * object before transformation.
     */
    public ReferenceData getReferenceData() {
        return referenceData;
    }

    /**
     * This method returns the {@link XMLSignatureInput} which is referenced by the
     * {@code URI} Attribute.
     * @param os where to write the transformation can be null.
     * @return the element to digest
     *
     * @throws XMLSignatureException
     * @see Manifest#verifyReferences()
     */
    protected XMLSignatureInput dereferenceURIandPerformTransforms(OutputStream os)
        throws XMLSignatureException {
        try {
            XMLSignatureInput input = this.getContentsBeforeTransformation();
            cacheDereferencedElement(input);

            XMLSignatureInput output = this.getContentsAfterTransformation(input, os);
            this.transformsOutput = output;
            return output;
        } catch (XMLSecurityException ex) {
            throw new ReferenceNotInitializedException(ex);
        }
    }

    /**
     * Store the dereferenced Element(s) so that it/they can be retrieved later.
     */
    private void cacheDereferencedElement(XMLSignatureInput input) {
        if (input.isNodeSet()) {
            try {
                final Set<Node> s = input.getNodeSet();
                referenceData = new ReferenceNodeSetData() {
                    public Iterator<Node> iterator() {
                        return new Iterator<Node>() {

                            final Iterator<Node> sIterator = s.iterator();

                            @Override
                            public boolean hasNext() {
                                return sIterator.hasNext();
                            }

                            @Override
                            public Node next() {
                                return sIterator.next();
                            }

                            @Override
                            public void remove() {
                                throw new UnsupportedOperationException();
                            }
                        };
                    }
                };
            } catch (Exception e) {
                // LOG a warning
                LOG.warn("cannot cache dereferenced data: " + e);
            }
        } else if (input.isElement()) {
            referenceData = new ReferenceSubTreeData
                (input.getSubNode(), input.isExcludeComments());
        } else if (input.isOctetStream() || input.isByteArray()) {
            try {
                referenceData = new ReferenceOctetStreamData
                    (input.getOctetStream(), input.getSourceURI(),
                        input.getMIMEType());
            } catch (IOException ioe) {
                // LOG a warning
                LOG.warn("cannot cache dereferenced data: " + ioe);
            }
        }
    }

    /**
     * Method getTransforms
     *
     * @return The transforms that applied this reference.
     * @throws InvalidTransformException
     * @throws TransformationException
     * @throws XMLSecurityException
     * @throws XMLSignatureException
     */
    public Transforms getTransforms()
        throws XMLSignatureException, InvalidTransformException,
        TransformationException, XMLSecurityException {
        return transforms;
    }

    /**
     * Method getReferencedBytes
     *
     * @return the bytes that will be used to generated digest.
     * @throws ReferenceNotInitializedException
     * @throws XMLSignatureException
     */
    public byte[] getReferencedBytes()
        throws ReferenceNotInitializedException, XMLSignatureException {
        try {
            XMLSignatureInput output = this.dereferenceURIandPerformTransforms(null);
            return output.getBytes();
        } catch (IOException | CanonicalizationException ex) {
            throw new ReferenceNotInitializedException(ex);
        }
    }


    /**
     * Method calculateDigest
     *
     * @param validating true if validating the reference
     * @return reference Calculate the digest of this reference.
     * @throws ReferenceNotInitializedException
     * @throws XMLSignatureException
     */
    private byte[] calculateDigest(boolean validating)
        throws ReferenceNotInitializedException, XMLSignatureException {
        XMLSignatureInput input = this.getContentsBeforeTransformation();
        if (input.isPreCalculatedDigest()) {
            return getPreCalculatedDigest(input);
        }

        cacheDereferencedElement(input);

        MessageDigestAlgorithm mda = this.getMessageDigestAlgorithm();
        mda.reset();

        XMLSignatureInput output = null;
        try (DigesterOutputStream diOs = new DigesterOutputStream(mda);
            OutputStream os = new UnsyncBufferedOutputStream(diOs)) {

            output = this.getContentsAfterTransformation(input, os);
            this.transformsOutput = output;

            // if signing and c14n11 property == true explicitly add
            // C14N11 transform if needed
            if (Reference.useC14N11 && !validating && !output.isOutputStreamSet()
                && !output.isOctetStream()) {
                if (transforms == null) {
                    transforms = new Transforms(getDocument());
                    transforms.setSecureValidation(secureValidation);
                    getElement().insertBefore(transforms.getElement(), digestMethodElem);
                }
                transforms.addTransform(Transforms.TRANSFORM_C14N11_OMIT_COMMENTS);
                output.updateOutputStream(os, true);
            } else {
                output.updateOutputStream(os);
            }
            os.flush();

            //this.getReferencedBytes(diOs);
            //mda.update(data);

            return diOs.getDigestValue();
        } catch (XMLSecurityException | IOException ex) {
            throw new ReferenceNotInitializedException(ex);
        } finally { //NOPMD
            try {
                if (output != null && output.getOctetStreamReal() != null) {
                    output.getOctetStreamReal().close();
                }
            } catch (IOException ex) {
                throw new ReferenceNotInitializedException(ex);
            }
        }
    }

    /**
     * Get the pre-calculated digest value from the XMLSignatureInput.
     *
     * @param input XMLSignature
     * @return a pre-calculated digest value.
     * @throws ReferenceNotInitializedException if there is an error decoding digest value
     * in Base64. Properly encoded pre-calculated digest value must be set.
     */
    private byte[] getPreCalculatedDigest(XMLSignatureInput input)
            throws ReferenceNotInitializedException {
        LOG.debug("Verifying element with pre-calculated digest");
        String preCalculatedDigest = input.getPreCalculatedDigest();
        return XMLUtils.decode(preCalculatedDigest);
    }

    /**
     * Returns the digest value.
     *
     * @return the digest value.
     * @throws XMLSecurityException if the Reference does not contain a DigestValue element
     */
    public byte[] getDigestValue() throws XMLSecurityException {
        if (digestValueElement == null) {
            // The required element is not in the XML!
            Object[] exArgs ={ Constants._TAG_DIGESTVALUE, Constants.SignatureSpecNS };
            throw new XMLSecurityException(
                "signature.Verification.NoSignatureElement", exArgs
            );
        }
        String content = XMLUtils.getFullTextChildrenFromNode(digestValueElement);
        return XMLUtils.decode(content);
    }


    /**
     * Tests reference validation is success or false
     *
     * @return true if reference validation is success, otherwise false
     * @throws ReferenceNotInitializedException
     * @throws XMLSecurityException
     */
    public boolean verify()
        throws ReferenceNotInitializedException, XMLSecurityException {
        byte[] elemDig = this.getDigestValue();
        byte[] calcDig = this.calculateDigest(true);
        boolean equal = MessageDigestAlgorithm.isEqual(elemDig, calcDig);

        if (!equal) {
            LOG.warn("Verification failed for URI \"" + this.getURI() + "\"");
            LOG.warn("Expected Digest: " + XMLUtils.encodeToString(elemDig));
            LOG.warn("Actual Digest: " + XMLUtils.encodeToString(calcDig));
        } else {
            LOG.debug("Verification successful for URI \"{}\"", this.getURI());
        }

        return equal;
    }

    /**
     * Method getBaseLocalName
     * {@inheritDoc}
     */
    public String getBaseLocalName() {
        return Constants._TAG_REFERENCE;
    }
}
