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
/*
 * Copyright (c) 2005, Oracle and/or its affiliates. All rights reserved.
 */
package com.sun.org.apache.xml.internal.security.signature.reference;

import java.io.InputStream;

/**
 * A representation of a {@code ReferenceData} type containing an OctetStream.
 */
public class ReferenceOctetStreamData implements ReferenceData {
    private InputStream octetStream;
    private String uri;
    private String mimeType;

    /**
     * Creates a new {@code ReferenceOctetStreamData}.
     *
     * @param octetStream the input stream containing the octets
     * @throws NullPointerException if {@code octetStream} is
     *    {@code null}
     */
    public ReferenceOctetStreamData(InputStream octetStream) {
        if (octetStream == null) {
            throw new NullPointerException("octetStream is null");
        }
        this.octetStream = octetStream;
    }

    /**
     * Creates a new {@code ReferenceOctetStreamData}.
     *
     * @param octetStream the input stream containing the octets
     * @param uri the URI String identifying the data object (may be
     *    {@code null})
     * @param mimeType the MIME type associated with the data object (may be
     *    {@code null})
     * @throws NullPointerException if {@code octetStream} is
     *    {@code null}
     */
    public ReferenceOctetStreamData(InputStream octetStream, String uri,
        String mimeType) {
        if (octetStream == null) {
            throw new NullPointerException("octetStream is null");
        }
        this.octetStream = octetStream;
        this.uri = uri;
        this.mimeType = mimeType;
    }

    /**
     * Returns the input stream of this {@code ReferenceOctetStreamData}.
     *
     * @return the input stream of this {@code ReferenceOctetStreamData}.
     */
    public InputStream getOctetStream() {
        return octetStream;
    }

    /**
     * Returns the URI String identifying the data object represented by this
     * {@code ReferenceOctetStreamData}.
     *
     * @return the URI String or {@code null} if not applicable
     */
    public String getURI() {
        return uri;
    }

    /**
     * Returns the MIME type associated with the data object represented by this
     * {@code ReferenceOctetStreamData}.
     *
     * @return the MIME type or {@code null} if not applicable
     */
    public String getMimeType() {
        return mimeType;
    }

}
