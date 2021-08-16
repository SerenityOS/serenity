/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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


import com.sun.org.apache.xerces.internal.xni.XNIException;
import com.sun.org.apache.xerces.internal.xni.XMLResourceIdentifier;
import com.sun.org.apache.xerces.internal.xni.grammars.XMLGrammarDescription;
import com.sun.org.apache.xerces.internal.xni.parser.XMLEntityResolver;
import com.sun.org.apache.xerces.internal.xni.parser.XMLInputSource;

import org.w3c.dom.ls.LSResourceResolver;
import org.w3c.dom.ls.LSInput;

import java.io.InputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;


/**
 * This class wraps DOM entity resolver to XNI entity resolver.
 *
 * @see LSResourceResolver
 *
 * @author Gopal Sharma, SUN MicroSystems Inc.
 * @author Elena Litani, IBM
 * @author Ramesh Mandava, Sun Microsystems
 * @LastModified: Nov 2017
 */
public class DOMEntityResolverWrapper
    implements XMLEntityResolver {

    //
    // Data
    //

    /** XML 1.0 type constant according to DOM L3 LS CR spec "http://www.w3.org/TR/2003/CR-DOM-Level-3-LS-20031107" */
    private static final String XML_TYPE = "http://www.w3.org/TR/REC-xml";

    /** XML Schema constant according to DOM L3 LS CR spec "http://www.w3.org/TR/2003/CR-DOM-Level-3-LS-20031107" */
    private static final String XSD_TYPE = "http://www.w3.org/2001/XMLSchema";

    /** The DOM entity resolver. */
    protected LSResourceResolver fEntityResolver;

    //
    // Constructors
    //

    /** Default constructor. */
    public DOMEntityResolverWrapper() {}

    /** Wraps the specified DOM entity resolver. */
    public DOMEntityResolverWrapper(LSResourceResolver entityResolver) {
        setEntityResolver(entityResolver);
    } // LSResourceResolver

    //
    // Public methods
    //

    /** Sets the DOM entity resolver. */
    public void setEntityResolver(LSResourceResolver entityResolver) {
        fEntityResolver = entityResolver;
    } // setEntityResolver(LSResourceResolver)

    /** Returns the DOM entity resolver. */
    public LSResourceResolver getEntityResolver() {
        return fEntityResolver;
    } // getEntityResolver():LSResourceResolver

    //
    // XMLEntityResolver methods
    //

    /**
     * Resolves an external parsed entity. If the entity cannot be
     * resolved, this method should return null.
     *
     * @param resourceIdentifier        description of the resource to be revsoved
     * @throws XNIException Thrown on general error.
     * @throws IOException  Thrown if resolved entity stream cannot be
     *                      opened or some other i/o error occurs.
     */
    public XMLInputSource resolveEntity(XMLResourceIdentifier resourceIdentifier)
        throws XNIException, IOException {
        // resolve entity using DOM entity resolver
        if (fEntityResolver != null) {
            // For entity resolution the type of the resource would be  XML TYPE
            // DOM L3 LS spec mention only the XML 1.0 recommendation right now
            LSInput inputSource =
                resourceIdentifier == null
                    ? fEntityResolver.resolveResource(
                        null,
                        null,
                        null,
                        null,
                        null)
                    : fEntityResolver.resolveResource(
                        getType(resourceIdentifier),
                        resourceIdentifier.getNamespace(),
                        resourceIdentifier.getPublicId(),
                        resourceIdentifier.getLiteralSystemId(),
                        resourceIdentifier.getBaseSystemId());
            if (inputSource != null) {
                String publicId = inputSource.getPublicId();
                String systemId = inputSource.getSystemId();
                String baseSystemId = inputSource.getBaseURI();
                InputStream byteStream = inputSource.getByteStream();
                Reader charStream = inputSource.getCharacterStream();
                String encoding = inputSource.getEncoding();
                String data = inputSource.getStringData();

                /**
                 * An LSParser looks at inputs specified in LSInput in
                 * the following order: characterStream, byteStream,
                 * stringData, systemId, publicId.
                 */
                XMLInputSource xmlInputSource =
                    new XMLInputSource(publicId, systemId, baseSystemId, true);

                if (charStream != null) {
                    xmlInputSource.setCharacterStream(charStream);
                }
                else if (byteStream != null) {
                    xmlInputSource.setByteStream(byteStream);
                }
                else if (data != null && data.length() != 0) {
                    xmlInputSource.setCharacterStream(new StringReader(data));
                }
                xmlInputSource.setEncoding(encoding);
                return xmlInputSource;
            }
        }

        // unable to resolve entity
        return null;

    } // resolveEntity(String,String,String):XMLInputSource

    /** Determines the type of resource being resolved **/
    private String getType(XMLResourceIdentifier resourceIdentifier) {
        if (resourceIdentifier instanceof XMLGrammarDescription) {
            XMLGrammarDescription desc = (XMLGrammarDescription) resourceIdentifier;
            if (XMLGrammarDescription.XML_SCHEMA.equals(desc.getGrammarType())) {
                return XSD_TYPE;
            }
        }
        return XML_TYPE;
    } // getType(XMLResourceIdentifier):String

} // DOMEntityResolverWrapper
