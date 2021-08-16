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

import javax.xml.stream.events.NotationDeclaration;
import javax.xml.stream.events.XMLEvent;
import com.sun.xml.internal.stream.dtd.nonvalidating.XMLNotationDecl;

/**
 * Implementation of NotationDeclaration event.
 *
 * @author k venugopal
 */
public class NotationDeclarationImpl extends DummyEvent implements NotationDeclaration {

    String fName = null;
    String fPublicId = null;
    String fSystemId = null;

    /** Creates a new instance of NotationDeclarationImpl */
    public NotationDeclarationImpl() {
        setEventType(XMLEvent.NOTATION_DECLARATION);
    }

    public NotationDeclarationImpl(String name,String publicId,String systemId){
        this.fName = name;
        this.fPublicId = publicId;
        this.fSystemId = systemId;
        setEventType(XMLEvent.NOTATION_DECLARATION);
    }

    public NotationDeclarationImpl(XMLNotationDecl notation){
        this.fName = notation.name;
        this.fPublicId = notation.publicId;
        this.fSystemId = notation.systemId;
        setEventType(XMLEvent.NOTATION_DECLARATION);
    }

    public String getName() {
        return fName;
    }

    public String getPublicId() {
        return fPublicId;
    }

    public String getSystemId() {
        return fSystemId;
    }

    void setPublicId(String publicId){
        this.fPublicId = publicId;
    }

    void setSystemId(String systemId){
        this.fSystemId = systemId;
    }

    void setName(String name){
        this.fName = name;
    }

    protected void writeAsEncodedUnicodeEx(java.io.Writer writer)
    throws java.io.IOException
    {
        writer.write("<!NOTATION ");
        writer.write(getName());
        if (fPublicId != null) {
            writer.write(" PUBLIC \"");
            writer.write(fPublicId);
            writer.write("\"");
        } else if (fSystemId != null) {
            writer.write(" SYSTEM");
            writer.write(" \"");
            writer.write(fSystemId);
            writer.write("\"");
        }
        writer.write('>');
    }
}
