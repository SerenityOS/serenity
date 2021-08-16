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
package com.sun.org.apache.xml.internal.security.transforms;

import com.sun.org.apache.xml.internal.security.exceptions.XMLSecurityException;

/**
 *
 */
public class InvalidTransformException extends XMLSecurityException {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /**
     * Constructor InvalidTransformException
     *
     */
    public InvalidTransformException() {
        super();
    }

    /**
     * Constructor InvalidTransformException
     *
     * @param msgId
     */
    public InvalidTransformException(String msgId) {
        super(msgId);
    }

    /**
     * Constructor InvalidTransformException
     *
     * @param msgId
     * @param exArgs
     */
    public InvalidTransformException(String msgId, Object[] exArgs) {
        super(msgId, exArgs);
    }

    /**
     * Constructor InvalidTransformException
     *
     * @param msgId
     * @param originalException
     */
    public InvalidTransformException(Exception originalException, String msgId) {
        super(originalException, msgId);
    }

    @Deprecated
    public InvalidTransformException(String msgID, Exception originalException) {
        this(originalException, msgID);
    }

    /**
     * Constructor InvalidTransformException
     *
     * @param msgId
     * @param exArgs
     * @param originalException
     */
    public InvalidTransformException(Exception originalException, String msgId, Object[] exArgs) {
        super(originalException, msgId, exArgs);
    }

    @Deprecated
    public InvalidTransformException(String msgID, Object[] exArgs, Exception originalException) {
        this(originalException, msgID, exArgs);
    }
}
