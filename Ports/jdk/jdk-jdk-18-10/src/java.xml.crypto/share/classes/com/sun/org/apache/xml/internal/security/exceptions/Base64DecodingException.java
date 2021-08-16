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
package com.sun.org.apache.xml.internal.security.exceptions;

/**
 * This Exception is thrown if decoding of Base64 data fails.
 *
 */
public class Base64DecodingException extends XMLSecurityException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructor Base64DecodingException
     *
     */
    public Base64DecodingException() {
        super();
    }

    /**
     * Constructor Base64DecodingException
     *
     * @param msgID
     */
    public Base64DecodingException(String msgID) {
        super(msgID);
    }

    /**
     * Constructor Base64DecodingException
     *
     * @param msgID
     * @param exArgs
     */
    public Base64DecodingException(String msgID, Object[] exArgs) {
        super(msgID, exArgs);
    }

    /**
     * Constructor Base64DecodingException
     *
     * @param originalException
     * @param msgID
     */
    public Base64DecodingException(Exception originalException, String msgID) {
        super(originalException, msgID);
    }

    @Deprecated
    public Base64DecodingException(String msgID, Exception originalException) {
        this(originalException, msgID);
    }

    /**
     * Constructor Base64DecodingException
     *
     * @param originalException
     * @param msgID
     * @param exArgs
     */
    public Base64DecodingException(Exception originalException, String msgID, Object[] exArgs) {
        super(originalException, msgID, exArgs);
    }

    @Deprecated
    public Base64DecodingException(String msgID, Object[] exArgs, Exception originalException) {
        this(originalException, msgID, exArgs);
    }

}
