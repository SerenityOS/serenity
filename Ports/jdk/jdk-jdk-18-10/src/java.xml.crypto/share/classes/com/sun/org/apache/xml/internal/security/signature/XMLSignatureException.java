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
package com.sun.org.apache.xml.internal.security.signature;

import com.sun.org.apache.xml.internal.security.exceptions.XMLSecurityException;

/**
 * All XML Signature related exceptions inherit herefrom.
 *
 * @see MissingResourceFailureException InvalidDigestValueException InvalidSignatureValueException
 */
public class XMLSignatureException extends XMLSecurityException {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /**
     * Constructor XMLSignatureException
     *
     */
    public XMLSignatureException() {
        super();
    }

    public XMLSignatureException(Exception ex) {
        super(ex);
    }

    /**
     * Constructor XMLSignatureException
     *
     * @param msgID
     */
    public XMLSignatureException(String msgID) {
        super(msgID);
    }

    /**
     * Constructor XMLSignatureException
     *
     * @param msgID
     * @param exArgs
     */
    public XMLSignatureException(String msgID, Object[] exArgs) {
        super(msgID, exArgs);
    }

    /**
     * Constructor XMLSignatureException
     *
     * @param originalException
     * @param msgID
     */
    public XMLSignatureException(Exception originalException, String msgID) {
        super(originalException, msgID);
    }

    @Deprecated
    public XMLSignatureException(String msgID, Exception originalException) {
        this(originalException, msgID);
    }

    /**
     * Constructor XMLSignatureException
     *
     * @param originalException
     * @param msgID
     * @param exArgs
     */
    public XMLSignatureException(Exception originalException, String msgID, Object[] exArgs) {
        super(originalException, msgID, exArgs);
    }

    @Deprecated
    public XMLSignatureException(String msgID, Object[] exArgs, Exception originalException) {
        this(originalException, msgID, exArgs);
    }
}
