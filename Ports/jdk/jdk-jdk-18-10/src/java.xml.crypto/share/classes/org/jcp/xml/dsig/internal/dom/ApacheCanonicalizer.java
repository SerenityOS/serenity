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
 * Copyright (c) 2005, Oracle and/or its affiliates. All rights reserved.
 */
package org.jcp.xml.dsig.internal.dom;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.security.spec.AlgorithmParameterSpec;
import java.security.InvalidAlgorithmParameterException;
import java.util.Set;

import javax.xml.crypto.*;
import javax.xml.crypto.dom.DOMCryptoContext;
import javax.xml.crypto.dsig.TransformException;
import javax.xml.crypto.dsig.TransformService;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;

import com.sun.org.apache.xml.internal.security.c14n.Canonicalizer;
import com.sun.org.apache.xml.internal.security.c14n.InvalidCanonicalizerException;
import com.sun.org.apache.xml.internal.security.signature.XMLSignatureInput;
import com.sun.org.apache.xml.internal.security.transforms.Transform;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public abstract class ApacheCanonicalizer extends TransformService {

    static {
        com.sun.org.apache.xml.internal.security.Init.init();
    }

    private static final com.sun.org.slf4j.internal.Logger LOG =
        com.sun.org.slf4j.internal.LoggerFactory.getLogger(ApacheCanonicalizer.class);
    protected Canonicalizer canonicalizer;
    private Transform apacheTransform;
    protected String inclusiveNamespaces;
    protected C14NMethodParameterSpec params;
    protected Document ownerDoc;
    protected Element transformElem;

    public final AlgorithmParameterSpec getParameterSpec()
    {
        return params;
    }

    public void init(XMLStructure parent, XMLCryptoContext context)
        throws InvalidAlgorithmParameterException
    {
        if (context != null && !(context instanceof DOMCryptoContext)) {
            throw new ClassCastException
                ("context must be of type DOMCryptoContext");
        }
        if (parent == null) {
            throw new NullPointerException();
        }
        if (!(parent instanceof javax.xml.crypto.dom.DOMStructure)) {
            throw new ClassCastException("parent must be of type DOMStructure");
        }
        transformElem = (Element)
            ((javax.xml.crypto.dom.DOMStructure)parent).getNode();
        ownerDoc = DOMUtils.getOwnerDocument(transformElem);
    }

    public void marshalParams(XMLStructure parent, XMLCryptoContext context)
        throws MarshalException
    {
        if (context != null && !(context instanceof DOMCryptoContext)) {
            throw new ClassCastException
                ("context must be of type DOMCryptoContext");
        }
        if (parent == null) {
            throw new NullPointerException();
        }
        if (!(parent instanceof javax.xml.crypto.dom.DOMStructure)) {
            throw new ClassCastException("parent must be of type DOMStructure");
        }
        transformElem = (Element)
            ((javax.xml.crypto.dom.DOMStructure)parent).getNode();
        ownerDoc = DOMUtils.getOwnerDocument(transformElem);
    }

    public Data canonicalize(Data data, XMLCryptoContext xc)
        throws TransformException
    {
        return canonicalize(data, xc, null);
    }

    public Data canonicalize(Data data, XMLCryptoContext xc, OutputStream os)
        throws TransformException
    {
        if (canonicalizer == null) {
            try {
                canonicalizer = Canonicalizer.getInstance(getAlgorithm());
                LOG.debug("Created canonicalizer for algorithm: {}", getAlgorithm());
            } catch (InvalidCanonicalizerException ice) {
                throw new TransformException
                    ("Couldn't find Canonicalizer for: " + getAlgorithm() +
                     ": " + ice.getMessage(), ice);
            }
        }

        boolean isByteArrayOutputStream = os == null;
        OutputStream writer = isByteArrayOutputStream ? new ByteArrayOutputStream() : os;
        try {
            boolean secVal = Utils.secureValidation(xc);
            Set<Node> nodeSet = null;
            if (data instanceof ApacheData) {
                XMLSignatureInput in =
                    ((ApacheData)data).getXMLSignatureInput();
                if (in.isElement()) {
                    if (inclusiveNamespaces != null) {
                        canonicalizer.canonicalizeSubtree(in.getSubNode(), inclusiveNamespaces, writer);
                        return new OctetStreamData(new ByteArrayInputStream(getC14nBytes(writer, isByteArrayOutputStream)));
                    } else {
                        canonicalizer.canonicalizeSubtree(in.getSubNode(), writer);
                        return new OctetStreamData(new ByteArrayInputStream(getC14nBytes(writer, isByteArrayOutputStream)));
                    }
                } else if (in.isNodeSet()) {
                    nodeSet = in.getNodeSet();
                } else {
                    canonicalizer.canonicalize(Utils.readBytesFromStream(in.getOctetStream()), writer, secVal);
                    return new OctetStreamData(new ByteArrayInputStream(getC14nBytes(writer, isByteArrayOutputStream)));
                }
            } else if (data instanceof DOMSubTreeData) {
                DOMSubTreeData subTree = (DOMSubTreeData)data;
                if (inclusiveNamespaces != null) {
                    canonicalizer.canonicalizeSubtree(subTree.getRoot(), inclusiveNamespaces, writer);
                    return new OctetStreamData(new ByteArrayInputStream(getC14nBytes(writer, isByteArrayOutputStream)));
                } else {
                    canonicalizer.canonicalizeSubtree(subTree.getRoot(), writer);
                    return new OctetStreamData(new ByteArrayInputStream(getC14nBytes(writer, isByteArrayOutputStream)));
                }
            } else if (data instanceof NodeSetData) {
                NodeSetData<?> nsd = (NodeSetData<?>)data;
                // convert Iterator to Set
                nodeSet = Utils.toNodeSet(nsd.iterator());
                LOG.debug("Canonicalizing {} nodes", nodeSet.size());
            } else {
                canonicalizer.canonicalize(Utils.readBytesFromStream(((OctetStreamData)data).getOctetStream()), writer, secVal);
                return new OctetStreamData(new ByteArrayInputStream(getC14nBytes(writer, isByteArrayOutputStream)));
            }

            if (inclusiveNamespaces != null) {
                canonicalizer.canonicalizeXPathNodeSet(nodeSet, inclusiveNamespaces, writer);
                return new OctetStreamData(new ByteArrayInputStream(getC14nBytes(writer, isByteArrayOutputStream)));
            } else {
                canonicalizer.canonicalizeXPathNodeSet(nodeSet, writer);
                return new OctetStreamData(new ByteArrayInputStream(getC14nBytes(writer, isByteArrayOutputStream)));
            }
        } catch (Exception e) {
            throw new TransformException(e);
        }
    }

    private byte[] getC14nBytes(OutputStream outputStream, boolean isByteArrayOutputStream) {    // NOPMD - preserving previous behavior here
        if (isByteArrayOutputStream) {
            return ((ByteArrayOutputStream)outputStream).toByteArray();
        }
        return null;
    }

    public Data transform(Data data, XMLCryptoContext xc, OutputStream os)
        throws TransformException
    {
        if (data == null) {
            throw new NullPointerException("data must not be null");
        }
        if (os == null) {
            throw new NullPointerException("output stream must not be null");
        }

        if (ownerDoc == null) {
            throw new TransformException("transform must be marshalled");
        }

        if (apacheTransform == null) {
            try {
                apacheTransform =
                    new Transform(ownerDoc, getAlgorithm(), transformElem.getChildNodes());
                apacheTransform.setElement(transformElem, xc.getBaseURI());
                LOG.debug("Created transform for algorithm: {}", getAlgorithm());
            } catch (Exception ex) {
                throw new TransformException
                    ("Couldn't find Transform for: " + getAlgorithm(), ex);
            }
        }

        XMLSignatureInput in;
        if (data instanceof ApacheData) {
            LOG.debug("ApacheData = true");
            in = ((ApacheData)data).getXMLSignatureInput();
        } else if (data instanceof NodeSetData) {
            LOG.debug("isNodeSet() = true");
            if (data instanceof DOMSubTreeData) {
                DOMSubTreeData subTree = (DOMSubTreeData)data;
                in = new XMLSignatureInput(subTree.getRoot());
                in.setExcludeComments(subTree.excludeComments());
            } else {
                @SuppressWarnings("unchecked")
                Set<Node> nodeSet =
                    Utils.toNodeSet(((NodeSetData)data).iterator());
                in = new XMLSignatureInput(nodeSet);
            }
        } else {
            LOG.debug("isNodeSet() = false");
            try {
                in = new XMLSignatureInput
                    (((OctetStreamData)data).getOctetStream());
            } catch (Exception ex) {
                throw new TransformException(ex);
            }
        }

        boolean secVal = Utils.secureValidation(xc);
        in.setSecureValidation(secVal);

        try {
            in = apacheTransform.performTransform(in, os, secVal);
            if (!in.isNodeSet() && !in.isElement()) {
                return null;
            }
            if (in.isOctetStream()) {
                return new ApacheOctetStreamData(in);
            } else {
                return new ApacheNodeSetData(in);
            }
        } catch (Exception ex) {
            throw new TransformException(ex);
        }
    }

    public final boolean isFeatureSupported(String feature) {
        if (feature == null) {
            throw new NullPointerException();
        } else {
            return false;
        }
    }
}
