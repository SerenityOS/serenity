/*
 * reserved comment block
 * DO NOT REMOVE OR ALTER!
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

package com.sun.org.apache.xerces.internal.jaxp;

import com.sun.org.apache.xerces.internal.xni.Augmentations;
import com.sun.org.apache.xerces.internal.xni.NamespaceContext;
import com.sun.org.apache.xerces.internal.xni.QName;
import com.sun.org.apache.xerces.internal.xni.XMLAttributes;
import com.sun.org.apache.xerces.internal.xni.XMLDocumentHandler;
import com.sun.org.apache.xerces.internal.xni.XMLLocator;
import com.sun.org.apache.xerces.internal.xni.XMLResourceIdentifier;
import com.sun.org.apache.xerces.internal.xni.XMLString;
import com.sun.org.apache.xerces.internal.xni.XNIException;
import com.sun.org.apache.xerces.internal.xni.parser.XMLDocumentFilter;
import com.sun.org.apache.xerces.internal.xni.parser.XMLDocumentSource;

/**
 * <p>XMLDocumentHandler which forks the pipeline to two other components.</p>
 *
 * @author Kohsuke Kawaguchi
 */
class TeeXMLDocumentFilterImpl implements XMLDocumentFilter {

    /**
     * The next component in the pipeline who receives the event.
     * This component receives events after the "side" handler
     * receives them.
     */
    private XMLDocumentHandler next;

    /**
     * The component who intercepts events.
     */
    private XMLDocumentHandler side;

    /**
     * The source of the event.
     */
    private XMLDocumentSource source;

    public XMLDocumentHandler getSide() {
        return side;
    }

    public void setSide(XMLDocumentHandler side) {
        this.side = side;
    }

    public XMLDocumentSource getDocumentSource() {
        return source;
    }

    public void setDocumentSource(XMLDocumentSource source) {
        this.source = source;
    }

    public XMLDocumentHandler getDocumentHandler() {
        return next;
    }

    public void setDocumentHandler(XMLDocumentHandler handler) {
        next = handler;
    }

    //
    //
    //  XMLDocumentHandler implementation
    //
    //

    public void characters(XMLString text, Augmentations augs) throws XNIException {
        side.characters(text, augs);
        next.characters(text, augs);
    }

    public void comment(XMLString text, Augmentations augs) throws XNIException {
        side.comment(text, augs);
        next.comment(text, augs);
    }

    public void doctypeDecl(String rootElement, String publicId, String systemId, Augmentations augs)
        throws XNIException {
        side.doctypeDecl(rootElement, publicId, systemId, augs);
        next.doctypeDecl(rootElement, publicId, systemId, augs);
    }

    public void emptyElement(QName element, XMLAttributes attributes, Augmentations augs) throws XNIException {
        side.emptyElement(element, attributes, augs);
        next.emptyElement(element, attributes, augs);
    }

    public void endCDATA(Augmentations augs) throws XNIException {
        side.endCDATA(augs);
        next.endCDATA(augs);
    }

    public void endDocument(Augmentations augs) throws XNIException {
        side.endDocument(augs);
        next.endDocument(augs);
    }

    public void endElement(QName element, Augmentations augs) throws XNIException {
        side.endElement(element, augs);
        next.endElement(element, augs);
    }

    public void endGeneralEntity(String name, Augmentations augs) throws XNIException {
        side.endGeneralEntity(name, augs);
        next.endGeneralEntity(name, augs);
    }

    public void ignorableWhitespace(XMLString text, Augmentations augs) throws XNIException {
        side.ignorableWhitespace(text, augs);
        next.ignorableWhitespace(text, augs);
    }

    public void processingInstruction(String target, XMLString data, Augmentations augs) throws XNIException {
        side.processingInstruction(target, data, augs);
        next.processingInstruction(target, data, augs);
    }

    public void startCDATA(Augmentations augs) throws XNIException {
        side.startCDATA(augs);
        next.startCDATA(augs);
    }

    public void startDocument(
            XMLLocator locator,
            String encoding,
            NamespaceContext namespaceContext,
            Augmentations augs)
        throws XNIException {
        side.startDocument(locator, encoding, namespaceContext, augs);
        next.startDocument(locator, encoding, namespaceContext, augs);
    }

    public void startElement(QName element, XMLAttributes attributes, Augmentations augs) throws XNIException {
        side.startElement(element, attributes, augs);
        next.startElement(element, attributes, augs);
    }

    public void startGeneralEntity(String name, XMLResourceIdentifier identifier, String encoding, Augmentations augs)
        throws XNIException {
        side.startGeneralEntity(name, identifier, encoding, augs);
        next.startGeneralEntity(name, identifier, encoding, augs);
    }

    public void textDecl(String version, String encoding, Augmentations augs) throws XNIException {
        side.textDecl(version, encoding, augs);
        next.textDecl(version, encoding, augs);
    }

    public void xmlDecl(String version, String encoding, String standalone, Augmentations augs) throws XNIException {
        side.xmlDecl(version, encoding, standalone, augs);
        next.xmlDecl(version, encoding, standalone, augs);
    }

}
