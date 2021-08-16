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

import javax.xml.namespace.QName;
/**
 *
 *@author Neeraj Bajaj, Sun Microsystems
 *
 */
public class NamedEvent extends DummyEvent {

    private QName name;

    public NamedEvent() {
    }


    public NamedEvent(QName qname) {
        this.name = qname;
    }


    public NamedEvent(String prefix, String uri, String localpart) {
        this.name = new QName(uri, localpart, prefix);
    }

    public String getPrefix() {
        return this.name.getPrefix();
    }


    public QName getName() {
        return name;
    }

    public void setName(QName qname) {
        this.name = qname;
    }

    public String nameAsString() {
        if("".equals(name.getNamespaceURI()))
            return name.getLocalPart();
        if(name.getPrefix() != null)
            return "['" + name.getNamespaceURI() + "']:" + getPrefix() + ":" + name.getLocalPart();
        else
            return "['" + name.getNamespaceURI() + "']:" + name.getLocalPart();
    }

    public String getNamespace(){
        return name.getNamespaceURI();
    }

    protected void writeAsEncodedUnicodeEx(java.io.Writer writer)
    throws java.io.IOException
    {
        writer.write(nameAsString());
    }

}
