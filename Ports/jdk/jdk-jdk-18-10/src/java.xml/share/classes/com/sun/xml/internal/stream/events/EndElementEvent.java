/*
 * Copyright (c) 2005, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.xml.internal.stream.events;

import java.util.List;
import java.util.ArrayList;

import javax.xml.namespace.QName;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.Namespace;
import java.util.Iterator;
import javax.xml.stream.events.XMLEvent;
import com.sun.xml.internal.stream.util.ReadOnlyIterator;

/**
 * Implementation of EndElement event.
 *
 * @author Neeraj Bajaj Sun Microsystems,Inc.
 * @author K.Venugopal Sun Microsystems,Inc.
 */
public class EndElementEvent extends DummyEvent
        implements EndElement {

    List<Namespace> fNamespaces = null;
    QName fQName;

    public EndElementEvent() {
        init();
    }

    protected final void init() {
        setEventType(XMLEvent.END_ELEMENT);
        fNamespaces = new ArrayList<>();
    }

    public EndElementEvent(String prefix, String uri, String localpart) {
        this(new QName(uri, localpart, prefix));
    }

    public EndElementEvent(QName qname) {
        this.fQName = qname;
        init();
    }

    @Override
    public QName getName() {
        return fQName;
    }

    public void setName(QName qname) {
        this.fQName = qname;
    }

    @Override
    protected void writeAsEncodedUnicodeEx(java.io.Writer writer)
            throws java.io.IOException {
        writer.write("</");
        String prefix = fQName.getPrefix();
        if (prefix != null && prefix.length() > 0) {
            writer.write(prefix);
            writer.write(':');
        }
        writer.write(fQName.getLocalPart());
        writer.write('>');
    }

    /**
     * Returns an Iterator of namespaces that have gone out of scope. Returns an
     * empty iterator if no namespaces have gone out of scope.
     *
     * @return an Iterator over Namespace interfaces, or an empty iterator
     */
    @Override
    public Iterator<Namespace> getNamespaces() {
        if (fNamespaces != null) {
            fNamespaces.iterator();
        }
        return new ReadOnlyIterator<>();
    }

    void addNamespace(Namespace ns) {
        if (ns != null) {
            fNamespaces.add(ns);
        }
    }

    public String toString() {
        String s = "</" + nameAsString();
        s = s + ">";
        return s;
    }

    public String nameAsString() {
        if ("".equals(fQName.getNamespaceURI())) {
            return fQName.getLocalPart();
        }

        if (fQName.getPrefix() != null) {
            return "['" + fQName.getNamespaceURI() + "']:" + fQName.getPrefix() + ":" + fQName.getLocalPart();
        } else {
            return "['" + fQName.getNamespaceURI() + "']:" + fQName.getLocalPart();
        }
    }

}
