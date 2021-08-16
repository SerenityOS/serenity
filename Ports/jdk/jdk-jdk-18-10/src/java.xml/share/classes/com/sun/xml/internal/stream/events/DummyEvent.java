/*
 * Copyright (c) 2005, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.xml.internal.stream.events ;

import java.io.IOException;
import java.io.Writer;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;

/** DummyEvent is an abstract class. It provides functionality for most of the
 * function of XMLEvent.
 *
 * @author Neeraj Bajaj Sun Microsystems,Inc.
 * @author K.Venugopal Sun Microsystems,Inc.
 *
 */

public abstract class DummyEvent implements XMLEvent {
    // Make sure that getLocation() never returns null. Instead, return this dummy location
    // that indicates "nowhere" as effectively as possible.
    private static DummyLocation nowhere = new DummyLocation();

    /* Event type this event corresponds to */
    private int fEventType;
    protected Location fLocation = (Location) nowhere;

    public DummyEvent() {
    }

    public DummyEvent(int i) {
        fEventType = i;
    }

    public int getEventType() {
        return fEventType;
    }

    protected void setEventType(int eventType){
        fEventType = eventType;
    }


    public boolean isStartElement() {
        return fEventType == XMLEvent.START_ELEMENT;
    }

    public boolean isEndElement() {
        return fEventType == XMLEvent.END_ELEMENT;
    }

    public boolean isEntityReference() {
        return fEventType == XMLEvent.ENTITY_REFERENCE;
    }

    public boolean isProcessingInstruction() {
        return fEventType == XMLEvent.PROCESSING_INSTRUCTION;
    }

    public boolean isCharacterData() {
        return fEventType == XMLEvent.CHARACTERS;
    }

    public boolean isStartDocument() {
        return fEventType == XMLEvent.START_DOCUMENT;
    }

    public boolean isEndDocument() {
        return fEventType == XMLEvent.END_DOCUMENT;
    }

    public Location getLocation(){
        return fLocation;
    }

    void setLocation(Location loc){
        if (loc == null) {
            fLocation = nowhere;
        } else {
            fLocation = loc;
        }
    }

    /** Returns this event as Characters, may result in
     * a class cast exception if this event is not Characters.
     */
    public Characters asCharacters() {
        return (Characters)this;
    }

    /** Returns this event as an end  element event, may result in
     * a class cast exception if this event is not a end element.
     */
    public EndElement asEndElement() {
        return (EndElement)this;
    }

    /** Returns this event as a start element event, may result in
     * a class cast exception if this event is not a start element.
     */
    public StartElement asStartElement() {
        return (StartElement)this;
    }

    /** This method is provided for implementations to provide
     * optional type information about the associated event.
     * It is optional and will return null if no information
     * is available.
     */
    public QName getSchemaType() {
        //Base class will take care of providing extra information about this event.
        return null;
    }

    /** A utility function to check if this event is an Attribute.
     * @see Attribute
     */
    public boolean isAttribute() {
        return fEventType == XMLEvent.ATTRIBUTE;
    }

    /** A utility function to check if this event is Characters.
     * @see Characters
     */
    public boolean isCharacters() {
        return fEventType == XMLEvent.CHARACTERS;
    }

    /** A utility function to check if this event is a Namespace.
     * @see Namespace
     */
    public boolean isNamespace() {
        return fEventType == XMLEvent.NAMESPACE;
    }

    /** This method will write the XMLEvent as per the XML 1.0 specification as Unicode characters.
     * No indentation or whitespace should be outputted.
     *
     * Any user defined event type SHALL have this method
     * called when being written to on an output stream.
     * Built in Event types MUST implement this method,
     * but implementations MAY choose not call these methods
     * for optimizations reasons when writing out built in
     * Events to an output stream.
     * The output generated MUST be equivalent in terms of the
     * infoset expressed.
     *
     * @param writer The writer that will output the data
     * @throws XMLStreamException if there is a fatal error writing the event
     */
    public void writeAsEncodedUnicode(Writer writer) throws XMLStreamException {
        try {
            writeAsEncodedUnicodeEx(writer);
        } catch (IOException e) {
            throw new XMLStreamException(e);
        }
    }
    /** Helper method in order to expose IOException.
     * @param writer The writer that will output the data
     * @throws XMLStreamException if there is a fatal error writing the event
     * @throws IOException if there is an IO error
     */
    protected abstract void writeAsEncodedUnicodeEx(Writer writer)
        throws IOException, XMLStreamException;

    /** Helper method to escape < > & for characters event and
     *  quotes, lt and amps for Entity
     */
    protected void charEncode(Writer writer, String data)
        throws IOException
    {
        if (data == null || data == "") return;
        int i = 0, start = 0;
        int len = data.length();

        loop:
        for (; i < len; ++i) {
            switch (data.charAt(i)) {
            case '<':
                writer.write(data, start, i - start);
                writer.write("&lt;");
                start = i + 1;
                break;

            case '&':
                writer.write(data, start, i - start);
                writer.write("&amp;");
                start = i + 1;
                break;

            case '>':
                writer.write(data, start, i - start);
                writer.write("&gt;");
                start = i + 1;
                break;
            case '"':
                writer.write(data, start, i - start);
                writer.write("&quot;");
                start = i + 1;
                break;
            }
        }
        // Write any pending data
        writer.write(data, start, len - start);
    }

    static class DummyLocation implements Location {
        public DummyLocation() {
        }

        public int getCharacterOffset() {
            return -1;
        }

        public int getColumnNumber() {
            return -1;
        }

        public int getLineNumber() {
            return -1;
        }

        public String getPublicId() {
            return null;
        }

        public String getSystemId() {
            return null;
        }
    }
}
