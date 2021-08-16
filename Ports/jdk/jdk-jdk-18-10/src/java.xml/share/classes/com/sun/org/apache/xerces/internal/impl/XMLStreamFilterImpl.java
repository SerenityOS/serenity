/*
 * Copyright (c) 2005, 2006, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package com.sun.org.apache.xerces.internal.impl;

import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.StreamFilter;
import javax.xml.stream.XMLStreamException;
import javax.xml.namespace.QName;
import javax.xml.stream.events.XMLEvent;


/**
 *
 * @author Joe Wang:
 * This is a rewrite of the original class. The focus is on removing caching, and make the filtered
 * stream reader more compatible with those in other implementations. Note however, that this version
 * will not solve all the issues related to the undefined condition in the spec. The priority is
 * to pass the TCK. Issues arising due to the requirement, that is, (1) should it initiate at BEGIN_DOCUMENT
 * or an accepted event; (2) should hasNext() advance the underlining stream in order to find an acceptable
 * event, would have to wait until 1.1 of StAX in which the filtered stream reader would be defined more clearly.
 */

public class XMLStreamFilterImpl implements javax.xml.stream.XMLStreamReader {

    private StreamFilter fStreamFilter = null;
    private XMLStreamReader fStreamReader = null;
    private int fCurrentEvent;
    private boolean fEventAccepted = false;

    /**the very issue around a long discussion. but since we must pass the TCK, we have to allow
     * hasNext() to advance the underlining stream in order to find the next acceptable event
     */
    private boolean fStreamAdvancedByHasNext = false;

    /**
     * Creates a new instance of XMLStreamFilterImpl, advancing the reader to
     * the next event accepted by the filter, if not already positioned on an
     * accepted event.
     *
     * @param reader
     *            the reader to filter
     * @param filter
     *            the filter to apply to the reader
     * @throws XMLStreamException
     *             when an {@code XMLStreamException} is thrown when
     *             advancing the reader to an accepted event.
     **/
    public XMLStreamFilterImpl(XMLStreamReader reader,StreamFilter filter) throws XMLStreamException {
        fStreamReader = reader;
        this.fStreamFilter = filter;

        //this is debatable to initiate at an acceptable event,
        //but it's neccessary in order to pass the TCK and yet avoid skipping element
        if (fStreamFilter.accept(fStreamReader)) {
            fEventAccepted = true;
        } else {
            findNextEvent();
        }
    }

    /**
     *
     * @param sf
     */
    protected void setStreamFilter(StreamFilter sf){
        this.fStreamFilter = sf;
    }

    /**
     *
     * @return
     * @throws XMLStreamException
     */
    public int next() throws XMLStreamException {
        if (fStreamAdvancedByHasNext && fEventAccepted) {
            fStreamAdvancedByHasNext = false;
            return fCurrentEvent;
        }
        int event = findNextEvent();
        if (event != -1) {
            return event;
        }

        throw new IllegalStateException("The stream reader has reached the end of the document, or there are no more "+
                                    " items to return");
    }
    /**
     *
     * @throws XMLStreamException
     * @return
     */
    public int nextTag() throws XMLStreamException {
        if (fStreamAdvancedByHasNext && fEventAccepted &&
                (fCurrentEvent == XMLEvent.START_ELEMENT || fCurrentEvent == XMLEvent.START_ELEMENT)) {
            fStreamAdvancedByHasNext = false;
            return fCurrentEvent;
        }

        int event = findNextTag();
        if (event != -1) {
            return event;
        }
        throw new IllegalStateException("The stream reader has reached the end of the document, or there are no more "+
                                    " items to return");
    }

    /**
     *
     * @throws XMLStreamException
     * @return
     */
    public boolean hasNext() throws XMLStreamException {
        if (fStreamReader.hasNext()) {
            if (!fEventAccepted) {
                if ((fCurrentEvent = findNextEvent()) == -1) {
                    return false;
                } else {
                    fStreamAdvancedByHasNext = true;
                }
            }
            return true;
        }
        return false;
    }

    private int findNextEvent() throws XMLStreamException {
        fStreamAdvancedByHasNext = false;
        while(fStreamReader.hasNext()){
            fCurrentEvent = fStreamReader.next();
            if(fStreamFilter.accept(fStreamReader)){
                fEventAccepted = true;
                return fCurrentEvent;
            }
        }
        //although it seems that IllegalStateException should be thrown when next() is called
        //on a stream that has no more items, we have to assume END_DOCUMENT is always accepted
        //in order to pass the TCK
        if (fCurrentEvent == XMLEvent.END_DOCUMENT)
            return fCurrentEvent;
        else
            return -1;
    }
    private int findNextTag() throws XMLStreamException {
        fStreamAdvancedByHasNext = false;
        while(fStreamReader.hasNext()){
            fCurrentEvent = fStreamReader.nextTag();
            if(fStreamFilter.accept(fStreamReader)){
                fEventAccepted = true;
                return fCurrentEvent;
            }
        }
        if (fCurrentEvent == XMLEvent.END_DOCUMENT)
            return fCurrentEvent;
        else
            return -1;
    }
    /**
     *
     * @throws XMLStreamException
     */
    public void close() throws XMLStreamException {
        fStreamReader.close();
    }

    /**
     *
     * @return
     */
    public int getAttributeCount() {
        return fStreamReader.getAttributeCount();
    }

    /**
     *
     * @param index
     * @return
     */
    public QName getAttributeName(int index) {
        return fStreamReader.getAttributeName(index);
    }

    /**
     *
     * @param index
     * @return
     */
    public String getAttributeNamespace(int index) {
        return fStreamReader.getAttributeNamespace(index);
    }

    /**
     *
     * @param index
     * @return
     */
    public String getAttributePrefix(int index) {
        return fStreamReader.getAttributePrefix(index);
    }

    /**
     *
     * @param index
     * @return
     */
    public String getAttributeType(int index) {
        return fStreamReader.getAttributeType(index);
    }

    /**
     *
     * @param index
     * @return
     */
    public String getAttributeValue(int index) {
        return fStreamReader.getAttributeValue(index);
    }

    /**
     *
     * @param namespaceURI
     * @param localName
     * @return
     */
    public String getAttributeValue(String namespaceURI, String localName) {
        return fStreamReader.getAttributeValue(namespaceURI,localName);
    }

    /**
     *
     * @return
     */
    public String getCharacterEncodingScheme() {
        return fStreamReader.getCharacterEncodingScheme();
    }

    /**
     *
     * @throws XMLStreamException
     * @return
     */
    public String getElementText() throws XMLStreamException {
        return fStreamReader.getElementText();
    }

    /**
     *
     * @return
     */
    public String getEncoding() {
        return fStreamReader.getEncoding();
    }

    /**
     *
     * @return
     */
    public int getEventType() {
        return fStreamReader.getEventType();
    }

    /**
     *
     * @return
     */
    public String getLocalName() {
        return fStreamReader.getLocalName();
    }

    /**
     *
     * @return
     */
    public javax.xml.stream.Location getLocation() {
        return fStreamReader.getLocation();
    }

    /**
     *
     * @return
     */
    public javax.xml.namespace.QName getName() {
        return fStreamReader.getName();
    }

    /**
     *
     * @return
     */
    public javax.xml.namespace.NamespaceContext getNamespaceContext() {
        return fStreamReader.getNamespaceContext();
    }

    /**
     *
     * @return
     */
    public int getNamespaceCount() {
        return fStreamReader.getNamespaceCount();
    }

    /**
     *
     * @param index
     * @return
     */
    public String getNamespacePrefix(int index) {
        return fStreamReader.getNamespacePrefix(index);
    }

    /**
     *
     * @return
     */
    public String getNamespaceURI() {
        return fStreamReader.getNamespaceURI();
    }

    /**
     *
     * @param index
     * @return
     */
    public String getNamespaceURI(int index) {
        return fStreamReader.getNamespaceURI(index);
    }

    /**
     *
     * @param prefix
     * @return
     */
    public String getNamespaceURI(String prefix) {
        return fStreamReader.getNamespaceURI(prefix);
    }

    /**
     *
     * @return
     */
    public String getPIData() {
        return fStreamReader.getPIData();
    }

    /**
     *
     * @return
     */
    public String getPITarget() {
        return fStreamReader.getPITarget();
    }

    /**
     *
     * @return
     */
    public String getPrefix() {
        return fStreamReader.getPrefix();
    }

    /**
     *
     * @param name
     * @throws IllegalArgumentException
     * @return
     */
    public Object getProperty(java.lang.String name) throws java.lang.IllegalArgumentException {
        return fStreamReader.getProperty(name);
    }

    /**
     *
     * @return
     */
    public String getText() {
        return fStreamReader.getText();
    }

    /**
     *
     * @return
     */
    public char[] getTextCharacters() {
        return fStreamReader.getTextCharacters();
    }

    /**
     *
     * @param sourceStart
     * @param target
     * @param targetStart
     * @param length
     * @throws XMLStreamException
     * @return
     */
    public int getTextCharacters(int sourceStart, char[] target, int targetStart, int length) throws XMLStreamException {
        return fStreamReader.getTextCharacters(sourceStart, target,targetStart,length);
    }

    /**
     *
     * @return
     */
    public int getTextLength() {
        return fStreamReader.getTextLength();
    }

    /**
     *
     * @return
     */
    public int getTextStart() {
        return fStreamReader.getTextStart();
    }

    /**
     *
     * @return
     */
    public String getVersion() {
        return fStreamReader.getVersion();
    }

    /**
     *
     * @return
     */
    public boolean hasName() {
        return fStreamReader.hasName();
    }

    /**
     *
     * @return
     */
    public boolean hasText() {
        return fStreamReader.hasText();
    }

    /**
     *
     * @return
     * @param index
     */
    public boolean isAttributeSpecified(int index) {
        return fStreamReader.isAttributeSpecified(index);
    }

    /**
     *
     * @return
     */
    public boolean isCharacters() {
        return fStreamReader.isCharacters();
    }

    /**
     *
     * @return
     */
    public boolean isEndElement() {
        return fStreamReader.isEndElement();
    }

    /**
     *
     * @return
     */
    public boolean isStandalone() {
        return fStreamReader.isStandalone();
    }

    /**
     *
     * @return
     */
    public boolean isStartElement() {
        return fStreamReader.isStartElement();
    }

    /**
     *
     * @return
     */
    public boolean isWhiteSpace() {
        return fStreamReader.isWhiteSpace();
    }


    /**
     *
     * @param type
     * @param namespaceURI
     * @param localName
     * @throws XMLStreamException
     */
    public void require(int type, String namespaceURI, String localName) throws XMLStreamException {
        fStreamReader.require(type,namespaceURI,localName);
    }

    /**
     *
     * @return
     */
    public boolean standaloneSet() {
        return fStreamReader.standaloneSet();
    }

    /**
     *
     * @param index
     * @return
     */
    public String getAttributeLocalName(int index){
        return fStreamReader.getAttributeLocalName(index);
    }
}
