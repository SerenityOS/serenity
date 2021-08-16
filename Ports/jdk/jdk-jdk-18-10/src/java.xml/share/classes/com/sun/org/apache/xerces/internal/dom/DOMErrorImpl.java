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

package com.sun.org.apache.xerces.internal.dom;

import org.w3c.dom.DOMError;
import org.w3c.dom.DOMLocator;
import com.sun.org.apache.xerces.internal.xni.parser.XMLParseException;


/**
 * <code>DOMErrorImpl</code> is an implementation that describes an error.
 * <strong>Note:</strong> The error object that describes the error
 * might be reused by Xerces implementation, across multiple calls to the
 * handleEvent method on DOMErrorHandler interface.
 *
 *
 * <p>See also the <a href='http://www.w3.org/TR/2001/WD-DOM-Level-3-Core-20010913'>Document Object Model (DOM) Level 3 Core Specification</a>.
 *
 * @xerces.internal
 *
 * @author Gopal Sharma, SUN Microsystems Inc.
 * @author Elena Litani, IBM
 *
 */

// REVISIT: the implementation of ErrorReporter.
//          we probably should not pass XMLParseException
//

public class DOMErrorImpl implements DOMError {

    //
    // Data
    //

    public short fSeverity = DOMError.SEVERITY_WARNING;
    public String fMessage = null;
    public DOMLocatorImpl fLocator = new DOMLocatorImpl();
    public Exception fException = null;
    public String fType;
    public Object fRelatedData;



    //
    // Constructors
    //

    /** Default constructor. */
    public DOMErrorImpl () {
    }

    /** Exctracts information from XMLParserException) */
    public DOMErrorImpl (short severity, XMLParseException exception) {
        fSeverity = severity;
        fException = exception;
        fLocator = createDOMLocator (exception);
    }

    /**
     * The severity of the error, either <code>SEVERITY_WARNING</code>,
     * <code>SEVERITY_ERROR</code>, or <code>SEVERITY_FATAL_ERROR</code>.
     */

    public short getSeverity() {
        return fSeverity;
    }

    /**
     * An implementation specific string describing the error that occured.
     */

    public String getMessage() {
        return fMessage;
    }

    /**
     * The location of the error.
     */

    public DOMLocator getLocation() {
        return fLocator;
    }

    // method to get the DOMLocator Object
    private DOMLocatorImpl createDOMLocator(XMLParseException exception) {
        // assuming DOMLocator wants the *expanded*, not the literal, URI of the doc... - neilg
        return new DOMLocatorImpl(exception.getLineNumber(),
                                  exception.getColumnNumber(),
                                  exception.getCharacterOffset(),
                                  exception.getExpandedSystemId());
    } // createDOMLocator()


    /**
     * The related platform dependent exception if any.exception is a reserved
     * word, we need to rename it.Change to "relatedException". (F2F 26 Sep
     * 2001)
     */
    public Object getRelatedException(){
        return fException;
    }

    public void reset(){
        fSeverity = DOMError.SEVERITY_WARNING;
        fException = null;
    }

    public String getType(){
        return fType;
    }

    public Object getRelatedData(){
        return fRelatedData;
    }


}// class DOMErrorImpl
