/*
 * Copyright (c) 2005, 2018, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.xml.internal.stream;

import com.sun.xml.internal.stream.events.XMLEventAllocatorImpl;
import java.util.NoSuchElementException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.EntityReference;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.util.XMLEventAllocator;

/**
 * @author  @author  Neeraj Bajaj Sun Microsystems
 *
 */

public class XMLEventReaderImpl implements javax.xml.stream.XMLEventReader{

    protected XMLStreamReader fXMLReader ;
    protected XMLEventAllocator fXMLEventAllocator;

    //only constructor will do because we delegate everything to underlying XMLStreamReader
    public XMLEventReaderImpl(XMLStreamReader reader) throws  XMLStreamException {
        fXMLReader = reader ;
        fXMLEventAllocator = (XMLEventAllocator)reader.getProperty(XMLInputFactory.ALLOCATOR);
        if(fXMLEventAllocator == null){
            fXMLEventAllocator = new XMLEventAllocatorImpl();
        }
        fPeekedEvent = fXMLEventAllocator.allocate(fXMLReader);
    }


    public boolean hasNext() {
        //if we have the peeked event return 'true'
        if(fPeekedEvent != null)return true;
        //this is strange XMLStreamReader throws XMLStreamException
        //XMLEventReader doesn't throw XMLStreamException
        boolean next = false ;
        try{
            next = fXMLReader.hasNext();
        }catch(XMLStreamException ex){
            return false;
        }
        return next ;
    }


    public XMLEvent nextEvent() throws XMLStreamException {
        //if application peeked return the peeked event
        if(fPeekedEvent != null){
            fLastEvent = fPeekedEvent ;
            fPeekedEvent = null;
            return fLastEvent ;
        }
        else if(fXMLReader.hasNext()){
            //advance the reader to next state.
            fXMLReader.next();
            return fLastEvent = fXMLEventAllocator.allocate(fXMLReader);
        }
        else{
            fLastEvent = null;
            throw new NoSuchElementException();
        }
    }

    public void remove(){
        //remove of the event is not supported.
        throw new java.lang.UnsupportedOperationException();
    }


    public void close() throws XMLStreamException {
        fXMLReader.close();
    }

    /** Reads the content of a text-only element. Precondition:
     * the current event is START_ELEMENT. Postcondition:
     * The current event is the corresponding END_ELEMENT.
     * @throws XMLStreamException if the current event is not a START_ELEMENT
     * or if a non text element is encountered
     */
    public String getElementText() throws XMLStreamException {
        //we have to keep reference to the 'last event' of the stream to be able
        //to make this check - is there another way ? - nb.
        if(fLastEvent.getEventType() != XMLEvent.START_ELEMENT){
            throw new XMLStreamException(
            "parser must be on START_ELEMENT to read next text", fLastEvent.getLocation());
        }

        // STag content ETag
        //[43]   content   ::=   CharData? ((element | Reference | CDSect | PI | Comment) CharData?)*

        //<foo>....some long text say in KB and underlying parser reports multiple character
        // but getElementText() events....</foo>

        String data = null;
        //having a peeked event makes things really worse -- we have to test the first event
        if(fPeekedEvent != null){
            XMLEvent event = fPeekedEvent ;
            fPeekedEvent = null;
            int type = event.getEventType();

            if(  type == XMLEvent.CHARACTERS || type == XMLEvent.SPACE ||
            type == XMLEvent.CDATA){
                data = event.asCharacters().getData();
            }
            else if(type == XMLEvent.ENTITY_REFERENCE){
                data = ((EntityReference)event).getDeclaration().getReplacementText();
            }
            else if(type == XMLEvent.COMMENT || type == XMLEvent.PROCESSING_INSTRUCTION){
                //ignore
            } else if(type == XMLEvent.START_ELEMENT) {
                throw new XMLStreamException(
                "elementGetText() function expects text only elment but START_ELEMENT was encountered.", event.getLocation());
            }else if(type == XMLEvent.END_ELEMENT){
                return "";
            }

            //create the string buffer and add initial data
            StringBuffer buffer = new StringBuffer();
            if(data != null && data.length() > 0 ) {
                buffer.append(data);
            }
            //get the next event -- we should stop at END_ELEMENT but it can be any thing
            //things are worse when implementing this function in XMLEventReader because
            //there isn't any function called getText() which can get values for
            //space, cdata, characters and entity reference
            //nextEvent() would also set the last event.
            event = nextEvent();
            while ((type = event.getEventType()) != XMLEvent.END_ELEMENT) {
                if (type == XMLEvent.CHARACTERS || type == XMLEvent.SPACE ||
                    type == XMLEvent.CDATA){
                    data = event.asCharacters().getData();
                }
                else if(type == XMLEvent.ENTITY_REFERENCE){
                    data = ((EntityReference)event).getDeclaration().getReplacementText();
                }
                else if(type == XMLEvent.COMMENT || type == XMLEvent.PROCESSING_INSTRUCTION){
                    //ignore
                    data = null;
                } else if(type == XMLEvent.END_DOCUMENT) {
                    throw new XMLStreamException("unexpected end of document when reading element text content");
                } else if(type == XMLEvent.START_ELEMENT) {
                    throw new XMLStreamException(
                    "elementGetText() function expects text only elment but START_ELEMENT was encountered.", event.getLocation());
                } else {
                    throw new XMLStreamException(
                    "Unexpected event type "+ type, event.getLocation());
                }
                //add the data to the buffer
                if(data != null && data.length() > 0 ) {
                    buffer.append(data);
                }
                event = nextEvent();
            }
            return buffer.toString();
        }//if (fPeekedEvent != null)

        //if there was no peeked, delegate everything to fXMLReader
        //update the last event before returning the text
        data = fXMLReader.getElementText();
        fLastEvent = fXMLEventAllocator.allocate(fXMLReader);
        return data;
    }

    /** Get the value of a feature/property from the underlying implementation
     * @param name The name of the property
     * @return The value of the property
     * @throws IllegalArgumentException if the property is not supported
     */
    public Object getProperty(java.lang.String name) throws java.lang.IllegalArgumentException {
        return fXMLReader.getProperty(name) ;
    }

    /** Skips any insignificant space events until a START_ELEMENT or
     * END_ELEMENT is reached. If anything other than space characters are
     * encountered, an exception is thrown. This method should
     * be used when processing element-only content because
     * the parser is not able to recognize ignorable whitespace if
     * the DTD is missing or not interpreted.
     * @throws XMLStreamException if anything other than space characters are encountered
     */
    public XMLEvent nextTag() throws XMLStreamException {
        //its really a pain if there is peeked event before calling nextTag()
        if(fPeekedEvent != null){
            //check the peeked event first.
            XMLEvent event = fPeekedEvent;
            fPeekedEvent = null ;
            int eventType = event.getEventType();
            //if peeked event is whitespace move to the next event
            //if peeked event is PI or COMMENT move to the next event
            if( (event.isCharacters() && event.asCharacters().isWhiteSpace())
            || eventType == XMLStreamConstants.PROCESSING_INSTRUCTION
            || eventType == XMLStreamConstants.COMMENT
            || eventType == XMLStreamConstants.START_DOCUMENT){
                event = nextEvent();
                eventType = event.getEventType();
            }

            //we have to have the while loop because there can be many PI or comment event in sucession
            while((event.isCharacters() && event.asCharacters().isWhiteSpace())
            || eventType == XMLStreamConstants.PROCESSING_INSTRUCTION
            || eventType == XMLStreamConstants.COMMENT){

                event = nextEvent();
                eventType = event.getEventType();
            }

            if (eventType != XMLStreamConstants.START_ELEMENT && eventType != XMLStreamConstants.END_ELEMENT) {
                throw new XMLStreamException("expected start or end tag", event.getLocation());
            }
            return event;
        }

        //if there is no peeked event -- delegate the work of getting next event to fXMLReader
        fXMLReader.nextTag();
        return (fLastEvent = fXMLEventAllocator.allocate(fXMLReader));
    }

    public Object next() {
        Object object = null;
        try{
            object = nextEvent();
        }catch(XMLStreamException streamException){
            fLastEvent = null ;
            //don't swallow the cause
            NoSuchElementException e = new NoSuchElementException(streamException.getMessage());
            e.initCause(streamException.getCause());
            throw e;

        }
        return object;
    }

    public XMLEvent peek() throws XMLStreamException{
        //if someone call peek() two times we should just return the peeked event
        //this is reset if we call next() or nextEvent()
        if(fPeekedEvent != null) return fPeekedEvent;

        if(hasNext()){
            //revisit: we can implement peek() by calling underlying reader to advance
            // the stream and returning the event without the knowledge of the user
            // that the stream was advanced but the point is we are advancing the stream
            //here. -- nb.

            // Is there any application that relies on this behavior ?
            //Can it be an application knows that there is particularly very large 'comment' section
            //or character data which it doesn't want to read or to be returned as event
            //But as of now we are creating every event but it can be optimized not to create
            // the event.
            fXMLReader.next();
            fPeekedEvent = fXMLEventAllocator.allocate(fXMLReader);
            return fPeekedEvent;
        }else{
            return null;
        }
    }//peek()

    private XMLEvent fPeekedEvent;
    private XMLEvent fLastEvent;

}//XMLEventReaderImpl
