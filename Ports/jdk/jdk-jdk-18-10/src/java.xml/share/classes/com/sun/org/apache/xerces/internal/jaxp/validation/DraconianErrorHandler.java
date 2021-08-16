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

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * {@link ErrorHandler} that throws all errors and fatal errors.
 *
 * @author Kohsuke Kawaguchi
 */
final class DraconianErrorHandler implements ErrorHandler {

    /**
     * Singleton instance.
     */
    private static final DraconianErrorHandler ERROR_HANDLER_INSTANCE
        = new DraconianErrorHandler();

    private DraconianErrorHandler() {}

    /** Returns the one and only instance of this error handler. */
    public static DraconianErrorHandler getInstance() {
        return ERROR_HANDLER_INSTANCE;
    }

    /** Warning: Ignore. */
    public void warning(SAXParseException e) throws SAXException {
        // noop
    }

    /** Error: Throws back SAXParseException. */
    public void error(SAXParseException e) throws SAXException {
        throw e;
    }

    /** Fatal Error: Throws back SAXParseException. */
    public void fatalError(SAXParseException e) throws SAXException {
        throw e;
    }

} // DraconianErrorHandler
