/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 */
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sun.org.apache.xerces.internal.util;

import java.io.InputStream;
import java.io.IOException;
import java.io.Reader;

import com.sun.org.apache.xerces.internal.xni.XNIException;
import com.sun.org.apache.xerces.internal.xni.XMLResourceIdentifier;
import com.sun.org.apache.xerces.internal.xni.parser.XMLEntityResolver;
import com.sun.org.apache.xerces.internal.xni.parser.XMLInputSource;
import javax.xml.catalog.CatalogException;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * This class wraps a SAX entity resolver in an XNI entity resolver.
 *
 * @see EntityResolver
 *
 * @author Andy Clark, IBM
 *
 */
public class EntityResolverWrapper
    implements XMLEntityResolver {

    //
    // Data
    //

    /** The SAX entity resolver. */
    protected EntityResolver fEntityResolver;

    //
    // Constructors
    //

    /** Default constructor. */
    public EntityResolverWrapper() {}

    /** Wraps the specified SAX entity resolver. */
    public EntityResolverWrapper(EntityResolver entityResolver) {
        setEntityResolver(entityResolver);
    } // <init>(EntityResolver)

    //
    // Public methods
    //

    /** Sets the SAX entity resolver. */
    public void setEntityResolver(EntityResolver entityResolver) {
        fEntityResolver = entityResolver;
    } // setEntityResolver(EntityResolver)

    /** Returns the SAX entity resolver. */
    public EntityResolver getEntityResolver() {
        return fEntityResolver;
    } // getEntityResolver():EntityResolver

    //
    // XMLEntityResolver methods
    //

    /**
     * Resolves an external parsed entity. If the entity cannot be
     * resolved, this method should return null.
     *
     * @param resourceIdentifier        contains the physical co-ordinates of the resource to be resolved
     *
     * @throws XNIException Thrown on general error.
     * @throws IOException  Thrown if resolved entity stream cannot be
     *                      opened or some other i/o error occurs.
     */
    public XMLInputSource resolveEntity(XMLResourceIdentifier resourceIdentifier)
        throws XNIException, IOException {

        // When both pubId and sysId are null, the user's entity resolver
        // can do nothing about it. We'd better not bother calling it.
        // This happens when the resourceIdentifier is a GrammarDescription,
        // which describes a schema grammar of some namespace, but without
        // any schema location hint. -Sg
        String pubId = resourceIdentifier.getPublicId();
        String sysId = resourceIdentifier.getExpandedSystemId();
        if (pubId == null && sysId == null)
            return null;

        // resolve entity using SAX entity resolver
        if (fEntityResolver != null && resourceIdentifier != null) {
            try {
                InputSource inputSource = fEntityResolver.resolveEntity(pubId, sysId);
                if (inputSource != null) {
                    String publicId = inputSource.getPublicId();
                    String systemId = inputSource.getSystemId();
                    String baseSystemId = resourceIdentifier.getBaseSystemId();
                    InputStream byteStream = inputSource.getByteStream();
                    Reader charStream = inputSource.getCharacterStream();
                    String encoding = inputSource.getEncoding();
                    XMLInputSource xmlInputSource =
                        new XMLInputSource(publicId, systemId, baseSystemId, true);
                    xmlInputSource.setByteStream(byteStream);
                    xmlInputSource.setCharacterStream(charStream);
                    xmlInputSource.setEncoding(encoding);
                    return xmlInputSource;
                }
            }

            // error resolving entity
            catch (SAXException e) {
                Exception ex = e.getException();
                if (ex == null) {
                    ex = e;
                }
                throw new XNIException(ex);
            }

            catch (CatalogException e) {
                throw new XNIException(e);
            }
        }

        // unable to resolve entity
        return null;

    } // resolveEntity(String,String,String):XMLInputSource
}
