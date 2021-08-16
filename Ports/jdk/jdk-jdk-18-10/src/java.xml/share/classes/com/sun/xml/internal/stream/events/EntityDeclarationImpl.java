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

import javax.xml.stream.events.EntityDeclaration;
import javax.xml.stream.events.XMLEvent;
import com.sun.org.apache.xerces.internal.xni.XMLResourceIdentifier;

/**
 *
 * This class store all the information for a particular EntityDeclaration. EntityDeclaration interface
 * has various get* functiosn to retirve information about a particular EntityDeclaration.
 *
 * @author  Neeraj Bajaj, Sun Microsystems.
 */
public class EntityDeclarationImpl extends DummyEvent implements EntityDeclaration {

    private XMLResourceIdentifier fXMLResourceIdentifier ;
    private String fEntityName;
    private String fReplacementText;
    private String fNotationName;

    /** Creates a new instance of EntityDeclarationImpl */
    public EntityDeclarationImpl() {
        init();
    }

    public EntityDeclarationImpl(String entityName , String replacementText){
        this(entityName,replacementText,null);

    }

    public EntityDeclarationImpl(String entityName, String replacementText, XMLResourceIdentifier resourceIdentifier){
        init();
        fEntityName = entityName;
        fReplacementText = replacementText;
        fXMLResourceIdentifier = resourceIdentifier;
    }

    public void setEntityName(String entityName){
        fEntityName = entityName;
    }

    public String getEntityName(){
        return fEntityName;
    }

    public void setEntityReplacementText(String replacementText){
        fReplacementText = replacementText;
    }

    public void setXMLResourceIdentifier(XMLResourceIdentifier resourceIdentifier){
        fXMLResourceIdentifier = resourceIdentifier ;
    }

    public XMLResourceIdentifier getXMLResourceIdentifier(){
        return fXMLResourceIdentifier;
    }

    public String getSystemId(){
        if(fXMLResourceIdentifier != null)
            return fXMLResourceIdentifier.getLiteralSystemId();
        return null;
    }

    public String getPublicId(){
        if(fXMLResourceIdentifier != null)
            return fXMLResourceIdentifier.getPublicId();

        return null;
    }

    public String getBaseURI() {
        if(fXMLResourceIdentifier != null)
            return fXMLResourceIdentifier.getBaseSystemId();
        return null;
    }

    public String getName(){
        return fEntityName;
    }

    public String getNotationName() {
        return fNotationName;
    }

    public void setNotationName(String notationName){
        fNotationName = notationName;
    }

    public String getReplacementText() {
        return fReplacementText;
    }

    protected void init(){
        setEventType(XMLEvent.ENTITY_DECLARATION);
    }

    protected void writeAsEncodedUnicodeEx(java.io.Writer writer)
    throws java.io.IOException
    {
        writer.write("<!ENTITY ");
        writer.write(fEntityName);
        if (fReplacementText != null) {
            //internal entity
            //escape quotes, lt and amps
            writer.write(" \"");
            charEncode(writer, fReplacementText);
        } else {
            //external entity
            String pubId = getPublicId();
            if (pubId != null) {
                writer.write(" PUBLIC \"");
                writer.write(pubId);
            } else {
                writer.write(" SYSTEM \"");
                writer.write(getSystemId());
            }
        }
        writer.write("\"");
        if (fNotationName != null) {
            writer.write(" NDATA ");
            writer.write(fNotationName);
        }
        writer.write(">");
    }
}
