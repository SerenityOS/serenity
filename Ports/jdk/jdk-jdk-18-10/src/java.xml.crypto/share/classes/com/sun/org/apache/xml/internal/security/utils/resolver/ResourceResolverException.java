/*
 * reserved comment block
 * DO NOT REMOVE OR ALTER!
 */
/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.sun.org.apache.xml.internal.security.utils.resolver;

import com.sun.org.apache.xml.internal.security.exceptions.XMLSecurityException;

/**
 * This Exception is thrown if something related to the
 * {@link com.sun.org.apache.xml.internal.security.utils.resolver.ResourceResolver} goes wrong.
 *
 */
public class ResourceResolverException extends XMLSecurityException {

    private static final long serialVersionUID = 1L;

    private String uri;

    private String baseURI;

    /**
     * Constructor ResourceResolverException
     *
     * @param msgID
     * @param uri
     * @param baseURI
     */
    public ResourceResolverException(String msgID, String uri, String baseURI) {
        super(msgID);

        this.uri = uri;
        this.baseURI = baseURI;
    }

    /**
     * Constructor ResourceResolverException
     *
     * @param msgID
     * @param exArgs
     * @param uri
     * @param baseURI
     */
    public ResourceResolverException(String msgID, Object[] exArgs, String uri,
                                     String baseURI) {
        super(msgID, exArgs);

        this.uri = uri;
        this.baseURI = baseURI;
    }

    /**
     * Constructor ResourceResolverException
     *
     * @param originalException
     * @param uri
     * @param baseURI
     * @param msgID
     */
    public ResourceResolverException(Exception originalException,
                                     String uri, String baseURI, String msgID) {
        super(originalException, msgID);

        this.uri = uri;
        this.baseURI = baseURI;
    }

    @Deprecated
    public ResourceResolverException(String msgID, Exception originalException,
                                     String uri, String baseURI) {
        this(originalException, uri, baseURI, msgID);
    }

    /**
     * Constructor ResourceResolverException
     *
     * @param originalException
     * @param uri
     * @param baseURI
     * @param msgID
     * @param exArgs
     */
    public ResourceResolverException(Exception originalException, String uri,
                                     String baseURI, String msgID, Object[] exArgs) {
        super(originalException, msgID, exArgs);

        this.uri = uri;
        this.baseURI = baseURI;
    }

    @Deprecated
    public ResourceResolverException(String msgID, Object[] exArgs,
                                     Exception originalException, String uri,
                                     String baseURI) {
        this(originalException, uri, baseURI, msgID, exArgs);
    }

    /**
     *
     * @param uri
     */
    public void setURI(String uri) {
        this.uri = uri;
    }

    /**
     *
     * @return the uri
     */
    public String getURI() {
        return this.uri;
    }

    /**
     *
     * @param baseURI
     */
    public void setbaseURI(String baseURI) {
        this.baseURI = baseURI;
    }

    /**
     *
     * @return the baseURI
     */
    public String getbaseURI() {
        return this.baseURI;
    }

}
