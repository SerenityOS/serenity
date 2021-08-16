/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 */
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sun.org.apache.xerces.internal.jaxp.validation;

import com.sun.org.apache.xerces.internal.dom.AttrImpl;
import com.sun.org.apache.xerces.internal.dom.CoreDocumentImpl;
import com.sun.org.apache.xerces.internal.dom.DOMMessageFormatter;
import com.sun.org.apache.xerces.internal.dom.DocumentTypeImpl;
import com.sun.org.apache.xerces.internal.dom.ElementImpl;
import com.sun.org.apache.xerces.internal.dom.ElementNSImpl;
import com.sun.org.apache.xerces.internal.dom.EntityImpl;
import com.sun.org.apache.xerces.internal.dom.NotationImpl;
import com.sun.org.apache.xerces.internal.dom.PSVIAttrNSImpl;
import com.sun.org.apache.xerces.internal.dom.PSVIDocumentImpl;
import com.sun.org.apache.xerces.internal.dom.PSVIElementNSImpl;
import com.sun.org.apache.xerces.internal.impl.Constants;
import com.sun.org.apache.xerces.internal.impl.dv.XSSimpleType;
import com.sun.org.apache.xerces.internal.xni.Augmentations;
import com.sun.org.apache.xerces.internal.xni.NamespaceContext;
import com.sun.org.apache.xerces.internal.xni.QName;
import com.sun.org.apache.xerces.internal.xni.XMLAttributes;
import com.sun.org.apache.xerces.internal.xni.XMLLocator;
import com.sun.org.apache.xerces.internal.xni.XMLResourceIdentifier;
import com.sun.org.apache.xerces.internal.xni.XMLString;
import com.sun.org.apache.xerces.internal.xni.XNIException;
import com.sun.org.apache.xerces.internal.xni.parser.XMLDocumentSource;
import com.sun.org.apache.xerces.internal.xs.AttributePSVI;
import com.sun.org.apache.xerces.internal.xs.ElementPSVI;
import com.sun.org.apache.xerces.internal.xs.XSTypeDefinition;
import java.util.ArrayList;
import java.util.List;
import javax.xml.transform.dom.DOMResult;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.Entity;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.Notation;
import org.w3c.dom.ProcessingInstruction;
import org.w3c.dom.Text;


/**
 * <p>DOM result builder.</p>
 *
 * @author Michael Glavassevich, IBM
 * @LastModified: Oct 2017
 */
final class DOMResultBuilder implements DOMDocumentHandler {

    /** Table for quick check of child insertion. */
    private final static int[] kidOK;

    static {
        kidOK = new int[13];
        kidOK[Node.DOCUMENT_NODE] =
            1 << Node.ELEMENT_NODE | 1 << Node.PROCESSING_INSTRUCTION_NODE |
            1 << Node.COMMENT_NODE | 1 << Node.DOCUMENT_TYPE_NODE;
        kidOK[Node.DOCUMENT_FRAGMENT_NODE] =
        kidOK[Node.ENTITY_NODE] =
        kidOK[Node.ENTITY_REFERENCE_NODE] =
        kidOK[Node.ELEMENT_NODE] =
            1 << Node.ELEMENT_NODE | 1 << Node.PROCESSING_INSTRUCTION_NODE |
            1 << Node.COMMENT_NODE | 1 << Node.TEXT_NODE |
            1 << Node.CDATA_SECTION_NODE | 1 << Node.ENTITY_REFERENCE_NODE ;
        kidOK[Node.ATTRIBUTE_NODE] = 1 << Node.TEXT_NODE | 1 << Node.ENTITY_REFERENCE_NODE;
        kidOK[Node.DOCUMENT_TYPE_NODE] = 0;
        kidOK[Node.PROCESSING_INSTRUCTION_NODE] = 0;
        kidOK[Node.COMMENT_NODE] = 0;
        kidOK[Node.TEXT_NODE] = 0;
        kidOK[Node.CDATA_SECTION_NODE] = 0;
        kidOK[Node.NOTATION_NODE] = 0;
    } // static

    //
    // Data
    //

    private Document fDocument;
    private CoreDocumentImpl fDocumentImpl;
    private boolean fStorePSVI;

    private Node fTarget;
    private Node fNextSibling;

    private Node fCurrentNode;
    private Node fFragmentRoot;

    private final List<Node> fTargetChildren = new ArrayList<>();

    private boolean fIgnoreChars;

    private final QName fAttributeQName = new QName();

    public DOMResultBuilder() {}

    /*
     * DOMDocumentHandler methods
     */

    public void setDOMResult(DOMResult result) {
        fCurrentNode = null;
        fFragmentRoot = null;
        fIgnoreChars = false;
        fTargetChildren.clear();
        if (result != null) {
            fTarget = result.getNode();
            fNextSibling = result.getNextSibling();
            fDocument = (fTarget.getNodeType() == Node.DOCUMENT_NODE) ? (Document) fTarget : fTarget.getOwnerDocument();
            fDocumentImpl = (fDocument instanceof CoreDocumentImpl) ? (CoreDocumentImpl) fDocument : null;
            fStorePSVI = (fDocument instanceof PSVIDocumentImpl);
            return;
        }
        fTarget = null;
        fNextSibling = null;
        fDocument = null;
        fDocumentImpl = null;
        fStorePSVI = false;
    }

    public void doctypeDecl(DocumentType node) throws XNIException {
        /** Create new DocumentType node for the target. */
        if (fDocumentImpl != null) {
            DocumentType docType = fDocumentImpl.createDocumentType(node.getName(), node.getPublicId(), node.getSystemId());
            final String internalSubset = node.getInternalSubset();
            /** Copy internal subset. */
            if (internalSubset != null) {
                ((DocumentTypeImpl) docType).setInternalSubset(internalSubset);
            }
            /** Copy entities. */
            NamedNodeMap oldMap = node.getEntities();
            NamedNodeMap newMap = docType.getEntities();
            int length = oldMap.getLength();
            for (int i = 0; i < length; ++i) {
                Entity oldEntity = (Entity) oldMap.item(i);
                EntityImpl newEntity = (EntityImpl) fDocumentImpl.createEntity(oldEntity.getNodeName());
                newEntity.setPublicId(oldEntity.getPublicId());
                newEntity.setSystemId(oldEntity.getSystemId());
                newEntity.setNotationName(oldEntity.getNotationName());
                newMap.setNamedItem(newEntity);
            }
            /** Copy notations. */
            oldMap = node.getNotations();
            newMap = docType.getNotations();
            length = oldMap.getLength();
            for (int i = 0; i < length; ++i) {
                Notation oldNotation = (Notation) oldMap.item(i);
                NotationImpl newNotation = (NotationImpl) fDocumentImpl.createNotation(oldNotation.getNodeName());
                newNotation.setPublicId(oldNotation.getPublicId());
                newNotation.setSystemId(oldNotation.getSystemId());
                newMap.setNamedItem(newNotation);
            }
            append(docType);
        }
    }

    public void characters(Text node) throws XNIException {
        /** Create new Text node for the target. */
        append(fDocument.createTextNode(node.getNodeValue()));
    }

    public void cdata(CDATASection node) throws XNIException {
        /** Create new CDATASection node for the target. */
        append(fDocument.createCDATASection(node.getNodeValue()));
    }

    public void comment(Comment node) throws XNIException {
        /** Create new Comment node for the target. */
        append(fDocument.createComment(node.getNodeValue()));
    }

    public void processingInstruction(ProcessingInstruction node)
            throws XNIException {
        /** Create new ProcessingInstruction node for the target. */
        append(fDocument.createProcessingInstruction(node.getTarget(), node.getData()));
    }

    public void setIgnoringCharacters(boolean ignore) {
        fIgnoreChars = ignore;
    }

    /*
     * XMLDocumentHandler methods
     */

    public void startDocument(XMLLocator locator, String encoding,
            NamespaceContext namespaceContext, Augmentations augs)
            throws XNIException {}

    public void xmlDecl(String version, String encoding, String standalone,
            Augmentations augs) throws XNIException {}

    public void doctypeDecl(String rootElement, String publicId,
            String systemId, Augmentations augs) throws XNIException {}

    public void comment(XMLString text, Augmentations augs) throws XNIException {}

    public void processingInstruction(String target, XMLString data,
            Augmentations augs) throws XNIException {}

    public void startElement(QName element, XMLAttributes attributes,
            Augmentations augs) throws XNIException {
        Element elem;
        int attrCount = attributes.getLength();
        if (fDocumentImpl == null) {
            elem = fDocument.createElementNS(element.uri, element.rawname);
            for (int i = 0; i < attrCount; ++i) {
                attributes.getName(i, fAttributeQName);
                elem.setAttributeNS(fAttributeQName.uri, fAttributeQName.rawname, attributes.getValue(i));
            }
        }
        // If it's a Xerces DOM store type information for attributes, set idness, etc..
        else {
            elem = fDocumentImpl.createElementNS(element.uri, element.rawname, element.localpart);
            for (int i = 0; i < attrCount; ++i) {
                attributes.getName(i, fAttributeQName);
                AttrImpl attr = (AttrImpl) fDocumentImpl.createAttributeNS(fAttributeQName.uri,
                        fAttributeQName.rawname, fAttributeQName.localpart);
                attr.setValue(attributes.getValue(i));

                // write type information to this attribute
                AttributePSVI attrPSVI = (AttributePSVI) attributes.getAugmentations(i).getItem (Constants.ATTRIBUTE_PSVI);
                if (attrPSVI != null) {
                    if (fStorePSVI) {
                        ((PSVIAttrNSImpl) attr).setPSVI(attrPSVI);
                    }
                    Object type = attrPSVI.getMemberTypeDefinition();
                    if (type == null) {
                        type = attrPSVI.getTypeDefinition();
                        if (type != null) {
                            attr.setType (type);
                            if (((XSSimpleType) type).isIDType()) {
                                ((ElementImpl) elem).setIdAttributeNode (attr, true);
                            }
                        }
                    }
                    else {
                        attr.setType (type);
                        if (((XSSimpleType) type).isIDType()) {
                            ((ElementImpl) elem).setIdAttributeNode (attr, true);
                        }
                    }
                }
                attr.setSpecified(attributes.isSpecified(i));
                elem.setAttributeNode(attr);
            }
        }
        append(elem);
        fCurrentNode = elem;
        if (fFragmentRoot == null) {
            fFragmentRoot = elem;
        }
    }

    public void emptyElement(QName element, XMLAttributes attributes,
            Augmentations augs) throws XNIException {
        startElement(element, attributes, augs);
        endElement(element, augs);
    }

    public void startGeneralEntity(String name,
            XMLResourceIdentifier identifier, String encoding,
            Augmentations augs) throws XNIException {}

    public void textDecl(String version, String encoding, Augmentations augs)
            throws XNIException {}

    public void endGeneralEntity(String name, Augmentations augs)
            throws XNIException {}

    public void characters(XMLString text, Augmentations augs)
            throws XNIException {
        if (!fIgnoreChars) {
            append(fDocument.createTextNode(text.toString()));
        }
    }

    public void ignorableWhitespace(XMLString text, Augmentations augs)
            throws XNIException {
        characters(text, augs);
    }

    public void endElement(QName element, Augmentations augs)
            throws XNIException {
        // write type information to this element
        if (augs != null && fDocumentImpl != null) {
            ElementPSVI elementPSVI = (ElementPSVI)augs.getItem(Constants.ELEMENT_PSVI);
            if (elementPSVI != null) {
                if (fStorePSVI) {
                    ((PSVIElementNSImpl)fCurrentNode).setPSVI(elementPSVI);
                }
                XSTypeDefinition type = elementPSVI.getMemberTypeDefinition();
                if (type == null) {
                    type = elementPSVI.getTypeDefinition();
                }
                ((ElementNSImpl)fCurrentNode).setType(type);
            }
        }

        // adjust current node reference
        if (fCurrentNode == fFragmentRoot) {
            fCurrentNode = null;
            fFragmentRoot = null;
            return;
        }
        fCurrentNode = fCurrentNode.getParentNode();
    }

    public void startCDATA(Augmentations augs) throws XNIException {}

    public void endCDATA(Augmentations augs) throws XNIException {}

    public void endDocument(Augmentations augs) throws XNIException {
        if (fNextSibling == null) {
            for (Node node : fTargetChildren) {
                fTarget.appendChild(node);
            }
        }
        else {
            for (Node node : fTargetChildren) {
                fTarget.insertBefore(node, fNextSibling);
            }
        }
    }

    public void setDocumentSource(XMLDocumentSource source) {}

    public XMLDocumentSource getDocumentSource() {
        return null;
    }

    /*
     * Other methods
     */

    private void append(Node node) throws XNIException {
        if (fCurrentNode != null) {
            fCurrentNode.appendChild(node);
        }
        else {
            /** Check if this node can be attached to the target. */
            if ((kidOK[fTarget.getNodeType()] & (1 << node.getNodeType())) == 0) {
                String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "HIERARCHY_REQUEST_ERR", null);
                throw new XNIException(msg);
            }
            fTargetChildren.add(node);
        }
    }

} // DOMResultBuilder
