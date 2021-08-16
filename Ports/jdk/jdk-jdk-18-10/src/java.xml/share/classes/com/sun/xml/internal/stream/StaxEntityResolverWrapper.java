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

package com.sun.xml.internal.stream;

import java.io.InputStream;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLResolver;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import com.sun.org.apache.xerces.internal.xni.XMLResourceIdentifier;
import com.sun.org.apache.xerces.internal.xni.XNIException;
import com.sun.org.apache.xerces.internal.xni.parser.XMLInputSource;
import javax.xml.catalog.CatalogException;

/**
 *
 * @author  Neeraj Bajaj
 */
public class StaxEntityResolverWrapper {

    XMLResolver fStaxResolver ;

    /** Creates a new instance of StaxEntityResolverWrapper */
    public StaxEntityResolverWrapper(XMLResolver resolver) {
        fStaxResolver = resolver ;
    }

    public void setStaxEntityResolver(XMLResolver resolver ){
        fStaxResolver = resolver ;
    }

    public XMLResolver getStaxEntityResolver(){
        return fStaxResolver ;
    }

    public StaxXMLInputSource resolveEntity(XMLResourceIdentifier resourceIdentifier)
    throws XNIException, java.io.IOException {
        Object object = null ;
        try {
            object = fStaxResolver.resolveEntity(resourceIdentifier.getPublicId(), resourceIdentifier.getLiteralSystemId(),
            resourceIdentifier.getBaseSystemId(), null);
            return getStaxInputSource(object) ;
        } catch(XMLStreamException | CatalogException streamException){
            throw new XNIException(streamException) ;
        }
    }

    StaxXMLInputSource getStaxInputSource(Object object){
        if(object == null) return null ;

        if(object  instanceof java.io.InputStream){
            return new StaxXMLInputSource(new XMLInputSource(null, null, null, (InputStream)object, null), true);
        }
        else if(object instanceof XMLStreamReader){
            return new StaxXMLInputSource((XMLStreamReader)object, true) ;
        }else if(object instanceof XMLEventReader){
            return new StaxXMLInputSource((XMLEventReader)object, true) ;
        }

        return null ;
    }
}//class StaxEntityResolverWrapper
