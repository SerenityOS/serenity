/*
 * reserved comment block
 * DO NOT REMOVE OR ALTER!
 */
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sun.org.apache.xml.internal.serializer.dom3;

import org.w3c.dom.DOMError;
import org.w3c.dom.DOMLocator;

/**
 * Implementation of the DOM Level 3 DOMError interface.
 *
 * <p>See also the <a href='http://www.w3.org/TR/2004/REC-DOM-Level-3-Core-20040407/core.html#ERROR-Interfaces-DOMError'>DOMError Interface definition from Document Object Model (DOM) Level 3 Core Specification</a>.
 *
 * @xsl.usage internal
 */

final class DOMErrorImpl implements DOMError {

    /** private data members */

    // The DOMError Severity
    private short fSeverity = DOMError.SEVERITY_WARNING;

    // The Error message
    private String fMessage = null;

    //  A String indicating which related data is expected in relatedData.
    private String fType;

    // The platform related exception
    private Exception fException = null;

    //
    private Object fRelatedData;

    // The location of the exception
    private DOMLocatorImpl fLocation = new DOMLocatorImpl();


    //
    // Constructors
    //

    /**
     * Default constructor.
     */
    DOMErrorImpl () {
    }

    /**
     * @param severity
     * @param message
     * @param type
     */
    DOMErrorImpl(short severity, String message, String type) {
        fSeverity = severity;
        fMessage = message;
        fType = type;
    }

    /**
     * @param severity
     * @param message
     * @param type
     * @param exception
     */
    DOMErrorImpl(short severity, String message, String type,
            Exception exception) {
        fSeverity = severity;
        fMessage = message;
        fType = type;
        fException = exception;
    }

    /**
     * @param severity
     * @param message
     * @param type
     * @param exception
     * @param relatedData
     * @param location
     */
    DOMErrorImpl(short severity, String message, String type,
            Exception exception, Object relatedData, DOMLocatorImpl location) {
        fSeverity = severity;
        fMessage = message;
        fType = type;
        fException = exception;
        fRelatedData = relatedData;
        fLocation = location;
    }


    /**
     * The severity of the error, either <code>SEVERITY_WARNING</code>,
     * <code>SEVERITY_ERROR</code>, or <code>SEVERITY_FATAL_ERROR</code>.
     *
     * @return A short containing the DOMError severity
     */
    public short getSeverity() {
        return fSeverity;
    }

    /**
     * The DOMError message string.
     *
     * @return String
     */
    public String getMessage() {
        return fMessage;
    }

    /**
     * The location of the DOMError.
     *
     * @return A DOMLocator object containing the DOMError location.
     */
    public DOMLocator getLocation() {
        return fLocation;
    }

    /**
     * The related platform dependent exception if any.
     *
     * @return A java.lang.Exception
     */
    public Object getRelatedException(){
        return fException;
    }

    /**
     * Returns a String indicating which related data is expected in relatedData.
     *
     * @return A String
     */
    public String getType(){
        return fType;
    }

    /**
     * The related DOMError.type dependent data if any.
     *
     * @return java.lang.Object
     */
    public Object getRelatedData(){
        return fRelatedData;
    }

    public void reset(){
        fSeverity = DOMError.SEVERITY_WARNING;
        fException = null;
        fMessage = null;
        fType = null;
        fRelatedData = null;
        fLocation = null;
    }

}// class DOMErrorImpl
