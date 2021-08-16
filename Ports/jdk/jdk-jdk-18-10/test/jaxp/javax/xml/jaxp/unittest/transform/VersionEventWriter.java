/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

package transform;

import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartDocument;
import javax.xml.stream.events.XMLEvent;

public class VersionEventWriter implements XMLEventWriter {

    private String version = null;

    private String encoding = null;

    /** Creates a new instance of VersionEventWriter */
    public VersionEventWriter() {
    }

    public void add(XMLEvent event) throws XMLStreamException {
        if (event.getEventType() == XMLStreamConstants.START_DOCUMENT) {
            version = ((StartDocument) event).getVersion();
            encoding = ((StartDocument) event).getCharacterEncodingScheme();
        }
    }

    public void flush() throws XMLStreamException {
    }

    public void close() throws XMLStreamException {
    }

    public void add(XMLEventReader reader) throws XMLStreamException {
    }

    public java.lang.String getPrefix(java.lang.String uri) throws XMLStreamException {
        return null;
    }

    public void setPrefix(java.lang.String prefix, java.lang.String uri) throws XMLStreamException {
    }

    public void setDefaultNamespace(java.lang.String uri) throws XMLStreamException {
    }

    public void setNamespaceContext(NamespaceContext context) throws XMLStreamException {
    }

    public NamespaceContext getNamespaceContext() {
        return null;
    }

    public String getVersion() {
        return version;
    }

    public String getEncoding() {
        return encoding;
    }
}
