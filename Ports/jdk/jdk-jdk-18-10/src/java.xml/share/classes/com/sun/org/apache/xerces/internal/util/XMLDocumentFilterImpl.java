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

package com.sun.org.apache.xerces.internal.util;

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
 * Default implementation of {@link XMLDocumentFilter}
 * that simply passes through events to the next component.
 *
 * <p>
 * Can be used as a base implementation of other more sophisticated
 * {@link XMLDocumentFilter}s.
 *
 * @author
 *     Kohsuke Kawaguchi
 */
public class XMLDocumentFilterImpl implements XMLDocumentFilter {
    private XMLDocumentHandler next;
    private XMLDocumentSource source;


    public void setDocumentHandler(XMLDocumentHandler handler) {
        this.next = handler;
    }

    public XMLDocumentHandler getDocumentHandler() {
        return next;
    }

    public void setDocumentSource(XMLDocumentSource source) {
        this.source = source;
    }

    public XMLDocumentSource getDocumentSource() {
        return source;
    }






    public void characters(XMLString text, Augmentations augs) throws XNIException {
        next.characters(text, augs);
    }

    public void comment(XMLString text, Augmentations augs) throws XNIException {
        next.comment(text, augs);
    }

    public void doctypeDecl(String rootElement, String publicId, String systemId, Augmentations augs)
        throws XNIException {
        next.doctypeDecl(rootElement, publicId, systemId, augs);
    }

    public void emptyElement(QName element, XMLAttributes attributes, Augmentations augs) throws XNIException {
        next.emptyElement(element, attributes, augs);
    }

    public void endCDATA(Augmentations augs) throws XNIException {
        next.endCDATA(augs);
    }

    public void endDocument(Augmentations augs) throws XNIException {
        next.endDocument(augs);
    }

    public void endElement(QName element, Augmentations augs) throws XNIException {
        next.endElement(element, augs);
    }

    public void endGeneralEntity(String name, Augmentations augs) throws XNIException {
        next.endGeneralEntity(name, augs);
    }

    public void ignorableWhitespace(XMLString text, Augmentations augs) throws XNIException {
        next.ignorableWhitespace(text, augs);
    }

    public void processingInstruction(String target, XMLString data, Augmentations augs) throws XNIException {
        next.processingInstruction(target, data, augs);
    }

    public void startCDATA(Augmentations augs) throws XNIException {
        next.startCDATA(augs);
    }

    public void startDocument(
        XMLLocator locator,
        String encoding,
        NamespaceContext namespaceContext,
        Augmentations augs)
        throws XNIException {
        next.startDocument(locator, encoding, namespaceContext, augs);
    }

    public void startElement(QName element, XMLAttributes attributes, Augmentations augs) throws XNIException {
        next.startElement(element, attributes, augs);
    }

    public void startGeneralEntity(String name, XMLResourceIdentifier identifier, String encoding, Augmentations augs)
        throws XNIException {
        next.startGeneralEntity(name, identifier, encoding, augs);
    }

    public void textDecl(String version, String encoding, Augmentations augs) throws XNIException {
        next.textDecl(version, encoding, augs);
    }

    public void xmlDecl(String version, String encoding, String standalone, Augmentations augs) throws XNIException {
        next.xmlDecl(version, encoding, standalone, augs);
    }
}
