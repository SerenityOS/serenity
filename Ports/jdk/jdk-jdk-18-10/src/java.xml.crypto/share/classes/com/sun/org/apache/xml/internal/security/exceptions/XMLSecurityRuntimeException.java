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

import java.text.MessageFormat;

import com.sun.org.apache.xml.internal.security.utils.Constants;
import com.sun.org.apache.xml.internal.security.utils.I18n;

/**
 * The mother of all runtime Exceptions in this bundle. It allows exceptions to have
 * their messages translated to the different locales.
 *
 * The {@code xmlsecurity_en.properties} file contains this line:
 * <pre>
 * xml.WrongElement = Can't create a {0} from a {1} element
 * </pre>
 *
 * Usage in the Java source is:
 * <pre>
 * {
 *    Object[] exArgs = { Constants._TAG_TRANSFORMS, "BadElement" };
 *
 *    throw new XMLSecurityException("xml.WrongElement", exArgs);
 * }
 * </pre>
 *
 * Additionally, if another Exception has been caught, we can supply it, too
 * <pre>
 * try {
 *    ...
 * } catch (Exception oldEx) {
 *    Object[] exArgs = { Constants._TAG_TRANSFORMS, "BadElement" };
 *
 *    throw new XMLSecurityException("xml.WrongElement", exArgs, oldEx);
 * }
 * </pre>
 *
 *
 */
public class XMLSecurityRuntimeException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** Field msgID */
    protected String msgID;

    /**
     * Constructor XMLSecurityRuntimeException
     *
     */
    public XMLSecurityRuntimeException() {
        super("Missing message string");

        this.msgID = null;
    }

    /**
     * Constructor XMLSecurityRuntimeException
     *
     * @param msgID
     */
    public XMLSecurityRuntimeException(String msgID) {
        super(I18n.getExceptionMessage(msgID));

        this.msgID = msgID;
    }

    /**
     * Constructor XMLSecurityRuntimeException
     *
     * @param msgID
     * @param exArgs
     */
    public XMLSecurityRuntimeException(String msgID, Object[] exArgs) {
        super(MessageFormat.format(I18n.getExceptionMessage(msgID), exArgs));

        this.msgID = msgID;
    }

    /**
     * Constructor XMLSecurityRuntimeException
     *
     * @param originalException
     */
    public XMLSecurityRuntimeException(Exception originalException) {
        super("Missing message ID to locate message string in resource bundle \""
              + Constants.exceptionMessagesResourceBundleBase
              + "\". Original Exception was a "
              + originalException.getClass().getName() + " and message "
              + originalException.getMessage(), originalException);
    }

    /**
     * Constructor XMLSecurityRuntimeException
     *
     * @param msgID
     * @param originalException
     */
    public XMLSecurityRuntimeException(String msgID, Exception originalException) {
        super(I18n.getExceptionMessage(msgID, originalException), originalException);

        this.msgID = msgID;
    }

    /**
     * Constructor XMLSecurityRuntimeException
     *
     * @param msgID
     * @param exArgs
     * @param originalException
     */
    public XMLSecurityRuntimeException(String msgID, Object[] exArgs, Exception originalException) {
        super(MessageFormat.format(I18n.getExceptionMessage(msgID), exArgs), originalException);

        this.msgID = msgID;
    }

    /**
     * Method getMsgID
     *
     * @return the messageId
     */
    public String getMsgID() {
        if (msgID == null) {
            return "Missing message ID";
        }
        return msgID;
    }

    /** {@inheritDoc} */
    public String toString() {
        String s = this.getClass().getName();
        String message = super.getLocalizedMessage();

        if (message != null) {
            message = s + ": " + message;
        } else {
            message = s;
        }

        if (this.getCause() != null) {
            message = message + "\nOriginal Exception was " + this.getCause().toString();
        }

        return message;
    }

    /**
     * Method getOriginalException
     *
     * @return the original exception
     */
    public Exception getOriginalException() {
        if (this.getCause() instanceof Exception) {
            return (Exception)this.getCause();
        }
        return null;
    }

}
