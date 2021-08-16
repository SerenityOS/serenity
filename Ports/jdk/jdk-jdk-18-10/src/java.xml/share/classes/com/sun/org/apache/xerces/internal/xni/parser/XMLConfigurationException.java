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

package com.sun.org.apache.xerces.internal.xni.parser;

import com.sun.org.apache.xerces.internal.util.Status;
import com.sun.org.apache.xerces.internal.xni.XNIException;

/**
 * An XNI parser configuration exception. This exception class extends
 * <code>XNIException</code> in order to differentiate between general
 * parsing errors and configuration errors.
 *
 * @author Andy Clark, IBM
 *
 */
public class XMLConfigurationException
    extends XNIException {

    /** Serialization version. */
    static final long serialVersionUID = -5437427404547669188L;

    //
    // Data
    //

    /** Exception type. */
    protected Status fType;

    /** Identifier. */
    protected String fIdentifier;

    //
    // Constructors
    //

    /**
     * Constructs a configuration exception with the specified type
     * and feature/property identifier.
     *
     * @param type       The type of the exception.
     * @param identifier The feature or property identifier.
     */
    public XMLConfigurationException(Status type, String identifier) {
        super(identifier);
        fType = type;
        fIdentifier = identifier;
    } // <init>(short,String)

    /**
     * Constructs a configuration exception with the specified type,
     * feature/property identifier, and error message
     *
     * @param type       The type of the exception.
     * @param identifier The feature or property identifier.
     * @param message    The error message.
     */
    public XMLConfigurationException(Status type, String identifier,
                                     String message) {
        super(message);
        fType = type;
        fIdentifier = identifier;
    } // <init>(short,String,String)

    //
    // Public methods
    //

    /**
     * Returns the exception type.
     */
    public Status getType() {
        return fType;
    } // getType():short

    /** Returns the feature or property identifier. */
    public String getIdentifier() {
        return fIdentifier;
    } // getIdentifier():String

} // class XMLConfigurationException
