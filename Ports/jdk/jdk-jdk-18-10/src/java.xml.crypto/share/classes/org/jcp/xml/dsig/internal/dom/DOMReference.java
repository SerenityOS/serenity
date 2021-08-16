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
/*
 * Copyright (c) 2005, 2021, Oracle and/or its affiliates. All rights reserved.
 */
/*
 * ===========================================================================
 *
 * (C) Copyright IBM Corp. 2003 All Rights Reserved.
 *
 * ===========================================================================
 */
package org.jcp.xml.dsig.internal.dom;

import javax.xml.crypto.*;
import javax.xml.crypto.dsig.*;
import javax.xml.crypto.dom.DOMCryptoContext;
import javax.xml.crypto.dom.DOMURIReference;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.*;
import java.util.*;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.sun.org.apache.xml.internal.security.utils.XMLUtils;

import org.jcp.xml.dsig.internal.DigesterOutputStream;
import com.sun.org.apache.xml.internal.security.signature.XMLSignatureInput;
import com.sun.org.apache.xml.internal.security.utils.UnsyncBufferedOutputStream;

/**
 * DOM-based implementation of Reference.
 *
 */
public final class DOMReference extends DOMStructure
    implements Reference, DOMURIReference {

   /**
    * The maximum number of transforms per reference, if secure validation is enabled.
    */
   public static final int MAXIMUM_TRANSFORM_COUNT = 5;

   /**
    * Look up useC14N11 system property. If true, an explicit C14N11 transform
    * will be added if necessary when generating the signature. See section
    * 3.1.1 of http://www.w3.org/2007/xmlsec/Drafts/xmldsig-core/ for more info.
    *
    * If true, overrides the same property if set in the XMLSignContext.
    */
    @SuppressWarnings("removal")
    private static boolean useC14N11 =
        AccessController.doPrivileged((PrivilegedAction<Boolean>)
            () -> Boolean.getBoolean("com.sun.org.apache.xml.internal.security.useC14N11"));

    private static final com.sun.org.slf4j.internal.Logger LOG =
        com.sun.org.slf4j.internal.LoggerFactory.getLogger(DOMReference.class);

    private final DigestMethod digestMethod;
    private final String id;
    private final List<Transform> transforms;
    private List<Transform> allTransforms;
    private final Data appliedTransformData;
    private Attr here;
    private final String uri;
    private final String type;
    private byte[] digestValue;
    private byte[] calcDigestValue;
    private Element refElem;
    private boolean digested = false;
    private boolean validated = false;
    private boolean validationStatus;
    private Data derefData;
    private InputStream dis;
    private MessageDigest md;
    private Provider provider;

    /**
     * Creates a {@code Reference} from the specified parameters.
     *
     * @param uri the URI (may be null)
     * @param type the type (may be null)
     * @param dm the digest method
     * @param transforms a list of {@link Transform}s. The list
     *    is defensively copied to protect against subsequent modification.
     *    May be {@code null} or empty.
     * @param id the reference ID (may be {@code null})
     * @throws NullPointerException if {@code dm} is {@code null}
     * @throws ClassCastException if any of the {@code transforms} are
     *    not of type {@code Transform}
     */
    public DOMReference(String uri, String type, DigestMethod dm,
                        List<? extends Transform> transforms, String id,
                        Provider provider)
    {
        this(uri, type, dm, null, null, transforms, id, null, provider);
    }

    public DOMReference(String uri, String type, DigestMethod dm,
                        List<? extends Transform> appliedTransforms,
                        Data result, List<? extends Transform> transforms,
                        String id, Provider provider)
    {
        this(uri, type, dm, appliedTransforms,
             result, transforms, id, null, provider);
    }

    public DOMReference(String uri, String type, DigestMethod dm,
                        List<? extends Transform> appliedTransforms,
                        Data result, List<? extends Transform> transforms,
                        String id, byte[] digestValue, Provider provider)
    {
        if (dm == null) {
            throw new NullPointerException("DigestMethod must be non-null");
        }
        if (appliedTransforms == null) {
            this.allTransforms = new ArrayList<>();
        } else {
            this.allTransforms = new ArrayList<>(appliedTransforms);
            for (int i = 0, size = this.allTransforms.size(); i < size; i++) {
                if (!(this.allTransforms.get(i) instanceof Transform)) {
                    throw new ClassCastException
                        ("appliedTransforms["+i+"] is not a valid type");
                }
            }
        }
        if (transforms == null) {
            this.transforms = Collections.emptyList();
        } else {
            this.transforms = new ArrayList<>(transforms);
            for (int i = 0, size = this.transforms.size(); i < size; i++) {
                if (!(this.transforms.get(i) instanceof Transform)) {
                    throw new ClassCastException
                        ("transforms["+i+"] is not a valid type");
                }
            }
            this.allTransforms.addAll(this.transforms);
        }
        this.digestMethod = dm;
        this.uri = uri;
        if (uri != null && !uri.isEmpty()) {
            try {
                new URI(uri);
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException(e.getMessage());
            }
        }
        this.type = type;
        this.id = id;
        if (digestValue != null) {
            this.digestValue = digestValue.clone();
            this.digested = true;
        }
        this.appliedTransformData = result;
        this.provider = provider;
    }

    /**
     * Creates a {@code DOMReference} from an element.
     *
     * @param refElem a Reference element
     */
    public DOMReference(Element refElem, XMLCryptoContext context,
                        Provider provider)
        throws MarshalException
    {
        boolean secVal = Utils.secureValidation(context);

        // unmarshal Transforms, if specified
        Element nextSibling = DOMUtils.getFirstChildElement(refElem);
        List<Transform> newTransforms = new ArrayList<>(MAXIMUM_TRANSFORM_COUNT);
        if ("Transforms".equals(nextSibling.getLocalName())
            && XMLSignature.XMLNS.equals(nextSibling.getNamespaceURI())) {
            Element transformElem = DOMUtils.getFirstChildElement(nextSibling,
                                                                  "Transform",
                                                                  XMLSignature.XMLNS);
            newTransforms.add(new DOMTransform(transformElem, context, provider));
            transformElem = DOMUtils.getNextSiblingElement(transformElem);
            while (transformElem != null) {
                String localName = transformElem.getLocalName();
                String namespace = transformElem.getNamespaceURI();
                if (!"Transform".equals(localName) || !XMLSignature.XMLNS.equals(namespace)) {
                    throw new MarshalException(
                        "Invalid element name: " + localName +
                        ", expected Transform");
                }
                newTransforms.add
                    (new DOMTransform(transformElem, context, provider));
                if (secVal && Policy.restrictNumTransforms(newTransforms.size())) {
                    String error = "A maximum of " + Policy.maxTransforms()
                        + " transforms per Reference are allowed when"
                        + " secure validation is enabled";
                    throw new MarshalException(error);
                }
                transformElem = DOMUtils.getNextSiblingElement(transformElem);
            }
            nextSibling = DOMUtils.getNextSiblingElement(nextSibling);
        }
        if (!"DigestMethod".equals(nextSibling.getLocalName())
            && XMLSignature.XMLNS.equals(nextSibling.getNamespaceURI())) {
            throw new MarshalException("Invalid element name: " +
                                       nextSibling.getLocalName() +
                                       ", expected DigestMethod");
        }

        // unmarshal DigestMethod
        Element dmElem = nextSibling;
        this.digestMethod = DOMDigestMethod.unmarshal(dmElem);
        String digestMethodAlgorithm = this.digestMethod.getAlgorithm();
        if (secVal && Policy.restrictAlg(digestMethodAlgorithm)) {
            throw new MarshalException(
                "It is forbidden to use algorithm " + digestMethodAlgorithm +
                " when secure validation is enabled"
            );
        }

        // unmarshal DigestValue
        Element dvElem = DOMUtils.getNextSiblingElement(dmElem, "DigestValue", XMLSignature.XMLNS);
        String content = XMLUtils.getFullTextChildrenFromNode(dvElem);
        this.digestValue = XMLUtils.decode(content);

        // check for extra elements
        if (DOMUtils.getNextSiblingElement(dvElem) != null) {
            throw new MarshalException(
                "Unexpected element after DigestValue element");
        }

        // unmarshal attributes
        this.uri = DOMUtils.getAttributeValue(refElem, "URI");

        Attr attr = refElem.getAttributeNodeNS(null, "Id");
        if (attr != null) {
            this.id = attr.getValue();
            refElem.setIdAttributeNode(attr, true);
        } else {
            this.id = null;
        }

        this.type = DOMUtils.getAttributeValue(refElem, "Type");
        this.here = refElem.getAttributeNodeNS(null, "URI");
        this.refElem = refElem;
        this.transforms = newTransforms;
        this.allTransforms = transforms;
        this.appliedTransformData = null;
        this.provider = provider;
    }

    public DigestMethod getDigestMethod() {
        return digestMethod;
    }

    public String getId() {
        return id;
    }

    public String getURI() {
        return uri;
    }

    public String getType() {
        return type;
    }

    public List<Transform> getTransforms() {
        return Collections.unmodifiableList(allTransforms);
    }

    public byte[] getDigestValue() {
        return digestValue == null ? null : digestValue.clone();
    }

    public byte[] getCalculatedDigestValue() {
        return calcDigestValue == null ? null
                                        : calcDigestValue.clone();
    }

    @Override
    public void marshal(Node parent, String dsPrefix, DOMCryptoContext context)
        throws MarshalException
    {
        LOG.debug("Marshalling Reference");
        Document ownerDoc = DOMUtils.getOwnerDocument(parent);

        refElem = DOMUtils.createElement(ownerDoc, "Reference",
                                         XMLSignature.XMLNS, dsPrefix);

        // set attributes
        DOMUtils.setAttributeID(refElem, "Id", id);
        DOMUtils.setAttribute(refElem, "URI", uri);
        DOMUtils.setAttribute(refElem, "Type", type);

        // create and append Transforms element
        if (!allTransforms.isEmpty()) {
            Element transformsElem = DOMUtils.createElement(ownerDoc,
                                                            "Transforms",
                                                            XMLSignature.XMLNS,
                                                            dsPrefix);
            refElem.appendChild(transformsElem);
            for (Transform transform : allTransforms) {
                ((DOMStructure)transform).marshal(transformsElem,
                                                  dsPrefix, context);
            }
        }

        // create and append DigestMethod element
        ((DOMDigestMethod)digestMethod).marshal(refElem, dsPrefix, context);

        // create and append DigestValue element
        LOG.debug("Adding digestValueElem");
        Element digestValueElem = DOMUtils.createElement(ownerDoc,
                                                         "DigestValue",
                                                         XMLSignature.XMLNS,
                                                         dsPrefix);
        if (digestValue != null) {
            digestValueElem.appendChild
                (ownerDoc.createTextNode(XMLUtils.encodeToString(digestValue)));
        }
        refElem.appendChild(digestValueElem);

        parent.appendChild(refElem);
        here = refElem.getAttributeNodeNS(null, "URI");
    }

    public void digest(XMLSignContext signContext)
        throws XMLSignatureException
    {
        Data data = null;
        if (appliedTransformData == null) {
            data = dereference(signContext);
        } else {
            data = appliedTransformData;
        }
        digestValue = transform(data, signContext);

        // insert digestValue into DigestValue element
        String encodedDV = XMLUtils.encodeToString(digestValue);
        LOG.debug("Reference object uri = {}", uri);
        Element digestElem = DOMUtils.getLastChildElement(refElem);
        if (digestElem == null) {
            throw new XMLSignatureException("DigestValue element expected");
        }
        DOMUtils.removeAllChildren(digestElem);
        digestElem.appendChild
            (refElem.getOwnerDocument().createTextNode(encodedDV));

        digested = true;
        LOG.debug("Reference digesting completed");
    }

    public boolean validate(XMLValidateContext validateContext)
        throws XMLSignatureException
    {
        if (validateContext == null) {
            throw new NullPointerException("validateContext cannot be null");
        }
        if (validated) {
            return validationStatus;
        }
        Data data = dereference(validateContext);
        calcDigestValue = transform(data, validateContext);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Expected digest: " + XMLUtils.encodeToString(digestValue));
            LOG.debug("Actual digest: " + XMLUtils.encodeToString(calcDigestValue));
        }

        validationStatus = Arrays.equals(digestValue, calcDigestValue);
        validated = true;
        return validationStatus;
    }

    public Data getDereferencedData() {
        return derefData;
    }

    public InputStream getDigestInputStream() {
        return dis;
    }

    private Data dereference(XMLCryptoContext context)
        throws XMLSignatureException
    {
        Data data = null;

        // use user-specified URIDereferencer if specified; otherwise use deflt
        URIDereferencer deref = context.getURIDereferencer();
        if (deref == null) {
            deref = DOMURIDereferencer.INSTANCE;
        }
        try {
            data = deref.dereference(this, context);
            LOG.debug("URIDereferencer class name: {}", deref.getClass().getName());
            LOG.debug("Data class name: {}", data.getClass().getName());
        } catch (URIReferenceException ure) {
            throw new XMLSignatureException(ure);
        }

        return data;
    }

    private byte[] transform(Data dereferencedData,
                             XMLCryptoContext context)
        throws XMLSignatureException
    {
        if (md == null) {
            try {
                md = MessageDigest.getInstance
                    (((DOMDigestMethod)digestMethod).getMessageDigestAlgorithm());
            } catch (NoSuchAlgorithmException nsae) {
                throw new XMLSignatureException(nsae);
            }
        }
        md.reset();
        DigesterOutputStream dos;
        Boolean cache = (Boolean)
            context.getProperty("javax.xml.crypto.dsig.cacheReference");
        if (cache != null && cache) {
            this.derefData = copyDerefData(dereferencedData);
            dos = new DigesterOutputStream(md, true);
        } else {
            dos = new DigesterOutputStream(md);
        }
        Data data = dereferencedData;
        XMLSignatureInput xi = null;
        try (OutputStream os = new UnsyncBufferedOutputStream(dos)) {
            for (int i = 0, size = transforms.size(); i < size; i++) {
                DOMTransform transform = (DOMTransform)transforms.get(i);
                if (i < size - 1) {
                    data = transform.transform(data, context);
                } else {
                    data = transform.transform(data, context, os);
                }
            }

            if (data != null) {
                // explicitly use C14N 1.1 when generating signature
                // first check system property, then context property
                boolean c14n11 = useC14N11;
                String c14nalg = CanonicalizationMethod.INCLUSIVE;
                if (context instanceof XMLSignContext) {
                    if (!c14n11) {
                        Boolean prop = (Boolean)context.getProperty
                            ("com.sun.org.apache.xml.internal.security.useC14N11");
                        c14n11 = prop != null && prop;
                        if (c14n11) {
                            c14nalg = "http://www.w3.org/2006/12/xml-c14n11";
                        }
                    } else {
                        c14nalg = "http://www.w3.org/2006/12/xml-c14n11";
                    }
                }
                if (data instanceof ApacheData) {
                    xi = ((ApacheData)data).getXMLSignatureInput();
                } else if (data instanceof OctetStreamData) {
                    xi = new XMLSignatureInput
                        (((OctetStreamData)data).getOctetStream());
                } else if (data instanceof NodeSetData) {
                    TransformService spi = null;
                    if (provider == null) {
                        spi = TransformService.getInstance(c14nalg, "DOM");
                    } else {
                        try {
                            spi = TransformService.getInstance(c14nalg, "DOM", provider);
                        } catch (NoSuchAlgorithmException nsae) {
                            spi = TransformService.getInstance(c14nalg, "DOM");
                        }
                    }
                    data = spi.transform(data, context);
                    xi = new XMLSignatureInput
                        (((OctetStreamData)data).getOctetStream());
                } else {
                    throw new XMLSignatureException("unrecognized Data type");
                }

                boolean secVal = Utils.secureValidation(context);
                try {
                    xi.setSecureValidation(secVal);
                    if (context instanceof XMLSignContext && c14n11
                            && !xi.isOctetStream() && !xi.isOutputStreamSet()) {
                        TransformService spi = null;
                        if (provider == null) {
                            spi = TransformService.getInstance(c14nalg, "DOM");
                        } else {
                            try {
                                spi = TransformService.getInstance(c14nalg, "DOM", provider);
                            } catch (NoSuchAlgorithmException nsae) {
                                spi = TransformService.getInstance(c14nalg, "DOM");
                            }
                        }

                        DOMTransform t = new DOMTransform(spi);
                        Element transformsElem = null;
                        String dsPrefix = DOMUtils.getSignaturePrefix(context);
                        if (allTransforms.isEmpty()) {
                            transformsElem = DOMUtils.createElement(
                                    refElem.getOwnerDocument(),
                                    "Transforms", XMLSignature.XMLNS, dsPrefix);
                            refElem.insertBefore(transformsElem,
                                    DOMUtils.getFirstChildElement(refElem));
                        } else {
                            transformsElem = DOMUtils.getFirstChildElement(refElem);
                        }
                        t.marshal(transformsElem, dsPrefix,
                                (DOMCryptoContext) context);
                        allTransforms.add(t);
                        xi.updateOutputStream(os, true);
                    } else {
                        xi.updateOutputStream(os);
                    }
                } finally {
                    if(xi.getOctetStreamReal() != null) {
                        xi.getOctetStreamReal().close();
                    }
                }
            } else {
                LOG.warn("The input bytes to the digest operation are null. " +
                   "This may be due to a problem with the Reference URI " +
                   "or its Transforms.");
            }
            os.flush();
            if (cache != null && cache) {
                this.dis = dos.getInputStream();
            }
            return dos.getDigestValue();
        } catch (NoSuchAlgorithmException | TransformException | MarshalException
                | IOException | com.sun.org.apache.xml.internal.security.c14n.CanonicalizationException e) {
            throw new XMLSignatureException(e);
        } finally { //NOPMD
            if (xi != null && xi.getOctetStreamReal() != null) {
                try {
                    xi.getOctetStreamReal().close();
                } catch (IOException e) {
                    throw new XMLSignatureException(e);
                }
            }
            if (dos != null) {
                try {
                    dos.close();
                } catch (IOException e) {
                    throw new XMLSignatureException(e);
                }
            }
        }
    }

    public Node getHere() {
        return here;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof Reference)) {
            return false;
        }
        Reference oref = (Reference)o;

        boolean idsEqual = id == null ? oref.getId() == null
                                       : id.equals(oref.getId());
        boolean urisEqual = uri == null ? oref.getURI() == null
                                         : uri.equals(oref.getURI());
        boolean typesEqual = type == null ? oref.getType() == null
                                           : type.equals(oref.getType());
        boolean digestValuesEqual =
            Arrays.equals(digestValue, oref.getDigestValue());

        return digestMethod.equals(oref.getDigestMethod()) && idsEqual &&
            urisEqual && typesEqual &&
            allTransforms.equals(oref.getTransforms()) && digestValuesEqual;
    }

    @Override
    public int hashCode() {
        int result = 17;
        if (id != null) {
            result = 31 * result + id.hashCode();
        }
        if (uri != null) {
            result = 31 * result + uri.hashCode();
        }
        if (type != null) {
            result = 31 * result + type.hashCode();
        }
        if (digestValue != null) {
            result = 31 * result + Arrays.hashCode(digestValue);
        }
        result = 31 * result + digestMethod.hashCode();
        result = 31 * result + allTransforms.hashCode();

        return result;
    }

    boolean isDigested() {
        return digested;
    }

    private static Data copyDerefData(Data dereferencedData) {
        if (dereferencedData instanceof ApacheData) {
            // need to make a copy of the Data
            ApacheData ad = (ApacheData)dereferencedData;
            XMLSignatureInput xsi = ad.getXMLSignatureInput();
            if (xsi.isNodeSet()) {
                try {
                    final Set<Node> s = xsi.getNodeSet();
                    return new NodeSetData<Node>() {
                        public Iterator<Node> iterator() { return s.iterator(); }
                    };
                } catch (Exception e) {
                    // LOG a warning
                    LOG.warn("cannot cache dereferenced data: " + e);
                    return null;
                }
            } else if (xsi.isElement()) {
                return new DOMSubTreeData
                    (xsi.getSubNode(), xsi.isExcludeComments());
            } else if (xsi.isOctetStream() || xsi.isByteArray()) {
                try {
                    return new OctetStreamData
                        (xsi.getOctetStream(), xsi.getSourceURI(),
                         xsi.getMIMEType());
                } catch (IOException ioe) {
                    // LOG a warning
                    LOG.warn("cannot cache dereferenced data: " + ioe);
                    return null;
                }
            }
        }
        return dereferencedData;
    }
}
