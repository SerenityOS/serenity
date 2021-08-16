/*
 * reserved comment block
 * DO NOT REMOVE OR ALTER!
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

package com.sun.org.apache.xerces.internal.impl.io;

import java.io.CharConversionException;
import java.util.Locale;
import com.sun.org.apache.xerces.internal.util.MessageFormatter;

/**
 * <p>Signals that a malformed byte sequence was detected
 * by a <code>java.io.Reader</code> that decodes bytes
 * of a given encoding into characters.</p>
 *
 * @xerces.internal
 *
 * @author Michael Glavassevich, IBM
 *
 */
public class MalformedByteSequenceException extends CharConversionException {

    /** Serialization version. */
    static final long serialVersionUID = 8436382245048328739L;

    //
    // Data
    //

    /** message formatter **/
    private MessageFormatter fFormatter;

    /** locale for error message **/
    private Locale fLocale;

    /** error domain **/
    private String fDomain;

    /** key for the error message **/
    private String fKey;

    /** replacement arguements for the error message **/
    private Object[] fArguments;

    /** message text for this message, initially null **/
    private String fMessage;

    //
    // Constructors
    //

    /**
     * Constructs a MalformedByteSequenceException with the given
     * parameters which may be passed to an error reporter to
     * generate a localized string for this exception.
     *
     * @param formatter The MessageFormatter used for building the
     *                  message text for this exception.
     * @param locale    The Locale for which messages are to be reported.
     * @param domain    The error domain.
     * @param key       The key of the error message.
     * @param arguments The replacement arguments for the error message,
     *                  if needed.
     */
    public MalformedByteSequenceException(MessageFormatter formatter,
        Locale locale, String domain, String key, Object[] arguments) {
        fFormatter = formatter;
        fLocale = locale;
        fDomain = domain;
        fKey = key;
        fArguments = arguments;
    } // <init>(MessageFormatter, Locale, String, String, Object[])

    //
    // Public methods
    //

    /**
     * <p>Returns the error domain of the error message.</p>
     *
     * @return the error domain
     */
    public String getDomain () {
        return fDomain;
    } // getDomain

    /**
     * <p>Returns the key of the error message.</p>
     *
     * @return the error key of the error message
     */
    public String getKey () {
        return fKey;
    } // getKey()

    /**
     * <p>Returns the replacement arguments for the error
     * message or <code>null</code> if none exist.</p>
     *
     * @return the replacement arguments for the error message
     * or <code>null</code> if none exist
     */
    public Object[] getArguments () {
        return fArguments;
    } // getArguments();

    /**
     * <p>Returns the localized message for this exception.</p>
     *
     * @return the localized message for this exception.
     */
    public String getMessage() {
        if (fMessage == null) {
            fMessage = fFormatter.formatMessage(fLocale, fKey, fArguments);
            // The references to the message formatter and locale
            // aren't needed anymore so null them.
            fFormatter = null;
            fLocale = null;
        }
        return fMessage;
     } // getMessage()

} // MalformedByteSequenceException
