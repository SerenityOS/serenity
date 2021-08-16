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
package com.sun.org.apache.xml.internal.security.c14n.implementations;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import com.sun.org.apache.xml.internal.security.c14n.CanonicalizationException;
import com.sun.org.apache.xml.internal.security.c14n.Canonicalizer;
import com.sun.org.apache.xml.internal.security.parser.XMLParserException;
import com.sun.org.apache.xml.internal.security.signature.XMLSignatureInput;
import org.w3c.dom.Attr;
import org.w3c.dom.Comment;
import org.w3c.dom.DOMException;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.ProcessingInstruction;

/**
 * Serializes the physical representation of the subtree. All the attributes
 * present in the subtree are emitted. The attributes are sorted within an element,
 * with the namespace declarations appearing before the regular attributes.
 * This algorithm is not a true canonicalization since equivalent subtrees
 * may produce different output. It is therefore unsuitable for digital signatures.
 * This same property makes it ideal for XML Encryption Syntax and Processing,
 * because the decrypted XML content will share the same physical representation
 * as the original XML content that was encrypted.
 */
public class CanonicalizerPhysical extends CanonicalizerBase {

    /**
     * Constructor Canonicalizer20010315
     */
    public CanonicalizerPhysical() {
        super(true);
    }

    /**
     * Always throws a CanonicalizationException.
     *
     * @param xpathNodeSet
     * @param inclusiveNamespaces
     * @param writer OutputStream to write the canonicalization result
     * @throws CanonicalizationException always
     */
    public void engineCanonicalizeXPathNodeSet(Set<Node> xpathNodeSet, String inclusiveNamespaces, OutputStream writer)
        throws CanonicalizationException {

        /** $todo$ well, should we throw UnsupportedOperationException ? */
        throw new CanonicalizationException("c14n.Canonicalizer.UnsupportedOperation");
    }

    /**
     * Always throws a CanonicalizationException.
     *
     * @param rootNode
     * @param inclusiveNamespaces
     * @param writer OutputStream to write the canonicalization result
     * @throws CanonicalizationException
     */
    public void engineCanonicalizeSubTree(Node rootNode, String inclusiveNamespaces, OutputStream writer)
        throws CanonicalizationException {

        /** $todo$ well, should we throw UnsupportedOperationException ? */
        throw new CanonicalizationException("c14n.Canonicalizer.UnsupportedOperation");
    }

    /**
     * Always throws a CanonicalizationException.
     *
     * @param rootNode
     * @param inclusiveNamespaces
     * @param writer OutputStream to write the canonicalization result
     * @throws CanonicalizationException
     */
    public void engineCanonicalizeSubTree(
            Node rootNode, String inclusiveNamespaces, boolean propagateDefaultNamespace, OutputStream writer)
            throws CanonicalizationException {

        /** $todo$ well, should we throw UnsupportedOperationException ? */
        throw new CanonicalizationException("c14n.Canonicalizer.UnsupportedOperation");
    }

    /**
     * Output the Attr[]s for the given element.
     * <br>
     * The code of this method is a copy of
     * {@link #outputAttributes(Element, NameSpaceSymbTable, Map)},
     * whereas it takes into account that subtree-c14n is -- well -- subtree-based.
     * So if the element in question isRoot of c14n, it's parent is not in the
     * node set, as well as all other ancestors.
     *
     * @param element
     * @param ns
     * @param cache
     * @param writer OutputStream to write the canonicalization result
     * @throws CanonicalizationException, DOMException, IOException
     */
    @Override
    protected void outputAttributesSubtree(Element element, NameSpaceSymbTable ns,
                                           Map<String, byte[]> cache, OutputStream writer)
        throws CanonicalizationException, DOMException, IOException {
        if (element.hasAttributes()) {
            // result will contain all the attrs declared directly on that element
            SortedSet<Attr> result = new TreeSet<>(COMPARE);

            NamedNodeMap attrs = element.getAttributes();
            int attrsLength = attrs.getLength();

            for (int i = 0; i < attrsLength; i++) {
                Attr attribute = (Attr) attrs.item(i);
                result.add(attribute);
            }

            //we output all Attrs which are available
            for (Attr attr : result) {
                outputAttrToWriter(attr.getNodeName(), attr.getNodeValue(), writer, cache);
            }
        }
    }

    @Override
    protected void outputAttributes(Element element, NameSpaceSymbTable ns,
                                    Map<String, byte[]> cache, OutputStream writer)
        throws CanonicalizationException, DOMException, IOException {

        /** $todo$ well, should we throw UnsupportedOperationException ? */
        throw new CanonicalizationException("c14n.Canonicalizer.UnsupportedOperation");
    }

    @Override
    protected void circumventBugIfNeeded(XMLSignatureInput input)
        throws XMLParserException, IOException {
        // nothing to do
    }

    @Override
    protected void handleParent(Element e, NameSpaceSymbTable ns) {
        // nothing to do
    }

    /** {@inheritDoc} */
    public final String engineGetURI() {
        return Canonicalizer.ALGO_ID_C14N_PHYSICAL;
    }

    @Override
    protected void outputPItoWriter(ProcessingInstruction currentPI,
                                    OutputStream writer, int position) throws IOException {
        // Processing Instructions before or after the document element are not treated specially
        super.outputPItoWriter(currentPI, writer, NODE_NOT_BEFORE_OR_AFTER_DOCUMENT_ELEMENT);
    }

    @Override
    protected void outputCommentToWriter(Comment currentComment,
                                         OutputStream writer, int position) throws IOException {
        // Comments before or after the document element are not treated specially
        super.outputCommentToWriter(currentComment, writer, NODE_NOT_BEFORE_OR_AFTER_DOCUMENT_ELEMENT);
    }

}
