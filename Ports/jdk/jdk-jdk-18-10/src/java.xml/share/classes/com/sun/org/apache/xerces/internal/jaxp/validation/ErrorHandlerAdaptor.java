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

package com.sun.org.apache.xerces.internal.jaxp.validation;

import com.sun.org.apache.xerces.internal.xni.parser.XMLErrorHandler;
import com.sun.org.apache.xerces.internal.xni.parser.XMLParseException;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;

/**
 * Receives errors through Xerces {@link XMLErrorHandler}
 * and pass them down to SAX {@link ErrorHandler}.
 *
 * @author
 *     Kohsuke Kawaguchi
 */
public abstract class ErrorHandlerAdaptor implements XMLErrorHandler
{
    /** set to true if there was any error. */
    private boolean hadError = false;

    /**
     * returns if there was an error since the last invocation of
     * the resetError method.
     */
    public boolean hadError() { return hadError; }
    /** resets the error flag. */
    public void reset() { hadError = false; }

    /**
     * Implemented by the derived class to return the actual
     * {@link ErrorHandler} to which errors are sent.
     *
     * @return always return non-null valid object.
     */
    protected abstract ErrorHandler getErrorHandler();

    public void fatalError( String domain, String key, XMLParseException e ) {
        try {
            hadError = true;
            getErrorHandler().fatalError( Util.toSAXParseException(e) );
        } catch( SAXException se ) {
            throw new WrappedSAXException(se);
        }
    }

    public void error( String domain, String key, XMLParseException e ) {
        try {
            hadError = true;
            getErrorHandler().error( Util.toSAXParseException(e) );
        } catch( SAXException se ) {
            throw new WrappedSAXException(se);
        }
    }

    public void warning( String domain, String key, XMLParseException e ) {
        try {
            getErrorHandler().warning( Util.toSAXParseException(e) );
        } catch( SAXException se ) {
            throw new WrappedSAXException(se);
        }
    }

}
