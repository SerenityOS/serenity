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

package com.sun.xml.internal.stream.events;

import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.XMLEvent;
import javax.xml.namespace.QName;

import javax.xml.XMLConstants;
/**
 *
 * @author  Neeraj Bajaj,K Venugopal
 */
public class NamespaceImpl extends AttributeImpl implements Namespace{

    public NamespaceImpl( ) {
        init();
    }

    /** Creates a new instance of NamespaceImpl */
    public NamespaceImpl(String namespaceURI) {
        super(XMLConstants.XMLNS_ATTRIBUTE,XMLConstants.XMLNS_ATTRIBUTE_NS_URI,XMLConstants.DEFAULT_NS_PREFIX,namespaceURI,null);
        init();
    }

    public NamespaceImpl(String prefix, String namespaceURI){
        super(XMLConstants.XMLNS_ATTRIBUTE,XMLConstants.XMLNS_ATTRIBUTE_NS_URI,prefix,namespaceURI,null);
        init();
    }

    public boolean isDefaultNamespaceDeclaration() {
        QName name = this.getName();

        if(name != null && (name.getLocalPart().equals(XMLConstants.DEFAULT_NS_PREFIX)))
            return true;
        return false;
    }

    void setPrefix(String prefix){
        if(prefix == null)
            setName(new QName(XMLConstants.XMLNS_ATTRIBUTE_NS_URI,XMLConstants.DEFAULT_NS_PREFIX,XMLConstants.XMLNS_ATTRIBUTE));
        else// new QName(uri, localpart, prefix)
            setName(new QName(XMLConstants.XMLNS_ATTRIBUTE_NS_URI,prefix,XMLConstants.XMLNS_ATTRIBUTE));
    }

    public String getPrefix() {
        //for a namespace declaration xmlns:prefix="uri" to get the prefix we have to get the
        //local name if this declaration is stored as QName.
        QName name = this.getName();
        if(name != null)
            return name.getLocalPart();
        return null;
    }

    public String getNamespaceURI() {
        //we are treating namespace declaration as attribute -- so URI is stored as value
        //xmlns:prefix="Value"
        return this.getValue();
    }

    void setNamespaceURI(String uri) {
        //we are treating namespace declaration as attribute -- so URI is stored as value
        //xmlns:prefix="Value"
        this.setValue(uri);
    }

    protected void init(){
        setEventType(XMLEvent.NAMESPACE);
    }

    public int getEventType(){
        return XMLEvent.NAMESPACE;
    }

    public boolean isNamespace(){
        return true;
    }
}
