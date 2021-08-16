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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.sun.org.apache.xml.internal.security.c14n.CanonicalizationException;
import com.sun.org.apache.xml.internal.security.c14n.implementations.Canonicalizer11_OmitComments;
import com.sun.org.apache.xml.internal.security.c14n.implementations.Canonicalizer20010315OmitComments;
import com.sun.org.apache.xml.internal.security.c14n.implementations.CanonicalizerBase;
import com.sun.org.apache.xml.internal.security.exceptions.XMLSecurityRuntimeException;
import com.sun.org.apache.xml.internal.security.parser.XMLParserException;
import com.sun.org.apache.xml.internal.security.utils.JavaUtils;
import com.sun.org.apache.xml.internal.security.utils.XMLUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * Class XMLSignatureInput
 *
 * $todo$ check whether an XMLSignatureInput can be _both_, octet stream _and_ node set?
 */
public class XMLSignatureInput {
    /*
     * The XMLSignature Input can be either:
     *   A byteArray like with/or without InputStream.
     *   Or a nodeSet like defined either:
     *       * as a collection of nodes
     *       * or as subnode excluding or not comments and excluding or
     *         not other nodes.
     */

    /**
     * Some InputStreams do not support the {@link java.io.InputStream#reset}
     * method, so we read it in completely and work on our Proxy.
     */
    private InputStream inputOctetStreamProxy;
    /**
     * The original NodeSet for this XMLSignatureInput
     */
    private Set<Node> inputNodeSet;
    /**
     * The original Element
     */
    private Node subNode;
    /**
     * Exclude Node *for enveloped transformations*
     */
    private Node excludeNode;
    /**
     *
     */
    private boolean excludeComments = false;

    private boolean isNodeSet = false;
    /**
     * A cached bytes
     */
    private byte[] bytes;
    private boolean secureValidation;

    /**
     * Some Transforms may require explicit MIME type, charset (IANA registered
     * "character set"), or other such information concerning the data they are
     * receiving from an earlier Transform or the source data, although no
     * Transform algorithm specified in this document needs such explicit
     * information. Such data characteristics are provided as parameters to the
     * Transform algorithm and should be described in the specification for the
     * algorithm.
     */
    private String mimeType;

    /**
     * Field sourceURI
     */
    private String sourceURI;

    /**
     * Node Filter list.
     */
    private List<NodeFilter> nodeFilters = new ArrayList<>();

    private boolean needsToBeExpanded = false;
    private OutputStream outputStream;

    /**
     * Pre-calculated digest value of the object in base64.
     */
    private String preCalculatedDigest;

    /**
     * Construct a XMLSignatureInput from an octet array.
     * <p>
     * This is a comfort method, which internally converts the byte[] array into
     * an InputStream
     * <p>NOTE: no defensive copy</p>
     * @param inputOctets an octet array which including XML document or node
     */
    public XMLSignatureInput(byte[] inputOctets) {
        // NO defensive copy
        this.bytes = inputOctets;
    }

    /**
     * Constructs a {@code XMLSignatureInput} from an octet stream. The
     * stream is directly read.
     *
     * @param inputOctetStream
     */
    public XMLSignatureInput(InputStream inputOctetStream)  {
        this.inputOctetStreamProxy = inputOctetStream;
    }

    /**
     * Construct a XMLSignatureInput from a subtree rooted by rootNode. This
     * method included the node and <I>all</I> his descendants in the output.
     *
     * @param rootNode
     */
    public XMLSignatureInput(Node rootNode) {
        this.subNode = rootNode;
    }

    /**
     * Constructor XMLSignatureInput
     *
     * @param inputNodeSet
     */
    public XMLSignatureInput(Set<Node> inputNodeSet) {
        this.inputNodeSet = inputNodeSet;
    }

    /**
     * Construct a {@code XMLSignatureInput} from a known digest value in Base64.
     * This makes it possible to compare the element digest with the provided digest value.
     * @param preCalculatedDigest digest value in base64.
     */
    public XMLSignatureInput(String preCalculatedDigest) {
        this.preCalculatedDigest = preCalculatedDigest;
    }

    /**
     * Check if the structure needs to be expanded.
     * @return true if so.
     */
    public boolean isNeedsToBeExpanded() {
        return needsToBeExpanded;
    }

    /**
     * Set if the structure needs to be expanded.
     * @param needsToBeExpanded true if so.
     */
    public void setNeedsToBeExpanded(boolean needsToBeExpanded) {
        this.needsToBeExpanded = needsToBeExpanded;
    }

    /**
     * Returns the node set from input which was specified as the parameter of
     * {@link XMLSignatureInput} constructor
     *
     * @return the node set
     * @throws XMLParserException
     * @throws IOException
     */
    public Set<Node> getNodeSet() throws XMLParserException, IOException {
        return getNodeSet(false);
    }

    /**
     * Get the Input NodeSet.
     * @return the Input NodeSet.
     */
    public Set<Node> getInputNodeSet() {
        return inputNodeSet;
    }

    /**
     * Returns the node set from input which was specified as the parameter of
     * {@link XMLSignatureInput} constructor
     * @param circumvent
     *
     * @return the node set
     * @throws XMLParserException
     * @throws IOException
     */
    public Set<Node> getNodeSet(boolean circumvent) throws XMLParserException, IOException {
        if (inputNodeSet != null) {
            return inputNodeSet;
        }
        if (inputOctetStreamProxy == null && subNode != null) {
            if (circumvent) {
                XMLUtils.circumventBug2650(XMLUtils.getOwnerDocument(subNode));
            }
            inputNodeSet = new LinkedHashSet<>();
            XMLUtils.getSet(subNode, inputNodeSet, excludeNode, excludeComments);
            return inputNodeSet;
        } else if (isOctetStream()) {
            convertToNodes();
            Set<Node> result = new LinkedHashSet<>();
            XMLUtils.getSet(subNode, result, null, false);
            return result;
        }

        throw new RuntimeException("getNodeSet() called but no input data present");
    }

    /**
     * Returns the Octet stream(byte Stream) from input which was specified as
     * the parameter of {@link XMLSignatureInput} constructor
     *
     * @return the Octet stream(byte Stream) from input which was specified as
     * the parameter of {@link XMLSignatureInput} constructor
     * @throws IOException
     */
    public InputStream getOctetStream() throws IOException  {
        if (inputOctetStreamProxy != null) {
            return inputOctetStreamProxy;
        }

        if (bytes != null) {
            inputOctetStreamProxy = new ByteArrayInputStream(bytes);
            return inputOctetStreamProxy;
        }

        return null;
    }

    /**
     * @return real octet stream
     */
    public InputStream getOctetStreamReal() {
        return inputOctetStreamProxy;
    }

    /**
     * Returns the byte array from input which was specified as the parameter of
     * {@link XMLSignatureInput} constructor
     *
     * @return the byte[] from input which was specified as the parameter of
     * {@link XMLSignatureInput} constructor
     *
     * @throws CanonicalizationException
     * @throws IOException
     */
    public byte[] getBytes() throws IOException, CanonicalizationException {
        byte[] inputBytes = getBytesFromInputStream();
        if (inputBytes != null) {
            return inputBytes;
        }
        if (isOctetStream() || isElement() || isNodeSet()) {
            Canonicalizer20010315OmitComments c14nizer = new Canonicalizer20010315OmitComments();
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                c14nizer.engineCanonicalize(this, baos, secureValidation);
                bytes = baos.toByteArray();
            }
        }
        return bytes;
    }

    /**
     * Determines if the object has been set up with a Node set
     *
     * @return true if the object has been set up with a Node set
     */
    public boolean isNodeSet() {
        return inputOctetStreamProxy == null && inputNodeSet != null || isNodeSet;
    }

    /**
     * Determines if the object has been set up with an Element
     *
     * @return true if the object has been set up with an Element
     */
    public boolean isElement() {
        return inputOctetStreamProxy == null && subNode != null
            && inputNodeSet == null && !isNodeSet;
    }

    /**
     * Determines if the object has been set up with an octet stream
     *
     * @return true if the object has been set up with an octet stream
     */
    public boolean isOctetStream() {
        return (inputOctetStreamProxy != null || bytes != null)
          && inputNodeSet == null && subNode == null;
    }

    /**
     * Determines if {@link #setOutputStream} has been called with a
     * non-null OutputStream.
     *
     * @return true if {@link #setOutputStream} has been called with a
     * non-null OutputStream
     */
    public boolean isOutputStreamSet() {
        return outputStream != null;
    }

    /**
     * Determines if the object has been set up with a ByteArray
     *
     * @return true if the object has been set up with an octet stream
     */
    public boolean isByteArray() {
        return bytes != null && this.inputNodeSet == null && subNode == null;
    }

    /**
     * Determines if the object has been set up with a pre-calculated digest.
     * @return true if the object has been set up with a pre-calculated digest.
     */
    public boolean isPreCalculatedDigest() {
        return preCalculatedDigest != null;
    }

    /**
     * Is the object correctly set up?
     *
     * @return true if the object has been set up correctly
     */
    public boolean isInitialized() {
        return isOctetStream() || isNodeSet();
    }

    /**
     * Returns mimeType
     *
     * @return mimeType
     */
    public String getMIMEType() {
        return mimeType;
    }

    /**
     * Sets mimeType
     *
     * @param mimeType
     */
    public void setMIMEType(String mimeType) {
        this.mimeType = mimeType;
    }

    /**
     * Return SourceURI
     *
     * @return SourceURI
     */
    public String getSourceURI() {
        return sourceURI;
    }

    /**
     * Sets SourceURI
     *
     * @param sourceURI
     */
    public void setSourceURI(String sourceURI) {
        this.sourceURI = sourceURI;
    }

    /**
     * Method toString
     * {@inheritDoc}
     */
    public String toString() {
        if (isNodeSet()) {
            return "XMLSignatureInput/NodeSet/" + inputNodeSet.size()
                   + " nodes/" + getSourceURI();
        }
        if (isElement()) {
            return "XMLSignatureInput/Element/" + subNode
                + " exclude "+ excludeNode + " comments:"
                + excludeComments +"/" + getSourceURI();
        }
        try {
            byte[] bytes = getBytes();
            return "XMLSignatureInput/OctetStream/"
                   + (bytes != null ? bytes.length : 0)
                   + " octets/" + getSourceURI();
        } catch (IOException | CanonicalizationException ex) {
            return "XMLSignatureInput/OctetStream//" + getSourceURI();
        }
    }

    /**
     * Method getHTMLRepresentation
     *
     * @throws XMLSignatureException
     * @return The HTML representation for this XMLSignature
     */
    public String getHTMLRepresentation() throws XMLSignatureException {
        XMLSignatureInputDebugger db = new XMLSignatureInputDebugger(this);
        return db.getHTMLRepresentation();
    }

    /**
     * Method getHTMLRepresentation
     *
     * @param inclusiveNamespaces
     * @throws XMLSignatureException
     * @return The HTML representation for this XMLSignature
     */
    public String getHTMLRepresentation(Set<String> inclusiveNamespaces)
       throws XMLSignatureException {
        XMLSignatureInputDebugger db =
            new XMLSignatureInputDebugger(this, inclusiveNamespaces);
        return db.getHTMLRepresentation();
    }

    /**
     * Gets the exclude node of this XMLSignatureInput
     * @return Returns the excludeNode.
     */
    public Node getExcludeNode() {
        return excludeNode;
    }

    /**
     * Sets the exclude node of this XMLSignatureInput
     * @param excludeNode The excludeNode to set.
     */
    public void setExcludeNode(Node excludeNode) {
        this.excludeNode = excludeNode;
    }

    /**
     * Gets the node of this XMLSignatureInput
     * @return The excludeNode set.
     */
    public Node getSubNode() {
        return subNode;
    }

    /**
     * @return Returns the excludeComments.
     */
    public boolean isExcludeComments() {
        return excludeComments;
    }

    /**
     * @param excludeComments The excludeComments to set.
     */
    public void setExcludeComments(boolean excludeComments) {
        this.excludeComments = excludeComments;
    }

    /**
     * @param diOs
     * @throws IOException
     * @throws CanonicalizationException
     */
    public void updateOutputStream(OutputStream diOs)
        throws CanonicalizationException, IOException {
        updateOutputStream(diOs, false);
    }

    public void updateOutputStream(OutputStream diOs, boolean c14n11)
        throws CanonicalizationException, IOException {
        if (diOs == outputStream) {
            return;
        }
        if (bytes != null) {
            diOs.write(bytes);
        } else if (inputOctetStreamProxy == null) {
            CanonicalizerBase c14nizer = null;
            if (c14n11) {
                c14nizer = new Canonicalizer11_OmitComments();
            } else {
                c14nizer = new Canonicalizer20010315OmitComments();
            }
            c14nizer.engineCanonicalize(this, diOs, secureValidation);
        } else {
            byte[] buffer = new byte[4 * 1024];
            int bytesread = 0;
            try {
                while ((bytesread = inputOctetStreamProxy.read(buffer)) != -1) {
                    diOs.write(buffer, 0, bytesread);
                }
            } catch (IOException ex) {
                inputOctetStreamProxy.close();
                throw ex;
            }
        }
    }

    /**
     * @param os
     */
    public void setOutputStream(OutputStream os) {
        outputStream = os;
    }

    private byte[] getBytesFromInputStream() throws IOException {
        if (bytes != null) {
            return bytes;
        }
        if (inputOctetStreamProxy == null) {
            return null;
        }
        try {
            bytes = JavaUtils.getBytesFromStream(inputOctetStreamProxy);
        } finally {
            inputOctetStreamProxy.close();
        }
        return bytes;
    }

    /**
     * @param filter
     */
    public void addNodeFilter(NodeFilter filter) {
        if (isOctetStream()) {
            try {
                convertToNodes();
            } catch (Exception e) {
                throw new XMLSecurityRuntimeException(
                    "signature.XMLSignatureInput.nodesetReference", e
                );
            }
        }
        nodeFilters.add(filter);
    }

    /**
     * @return the node filters
     */
    public List<NodeFilter> getNodeFilters() {
        return nodeFilters;
    }

    /**
     * @param b
     */
    public void setNodeSet(boolean b) {
        isNodeSet = b;
    }

    private void convertToNodes() throws XMLParserException, IOException {
        // select all nodes, also the comments.
        try {
            Document doc = XMLUtils.read(this.getOctetStream(), secureValidation);
            this.subNode = doc;
        } finally {
            if (this.inputOctetStreamProxy != null) {
                this.inputOctetStreamProxy.close();
            }
            this.inputOctetStreamProxy = null;
            this.bytes = null;
        }
    }

    public boolean isSecureValidation() {
        return secureValidation;
    }

    public void setSecureValidation(boolean secureValidation) {
        this.secureValidation = secureValidation;
    }

    public String getPreCalculatedDigest() {
        return preCalculatedDigest;
    }
}
