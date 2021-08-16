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

package com.sun.org.apache.xerces.internal.util;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * {@link ErrorHandler} that throws all errors and fatal errors.
 *
 * @author
 *     Kohsuke Kawaguchi
 */
public class DraconianErrorHandler implements ErrorHandler {
    /**
     * Use this singleton instance.
     */
    public static final ErrorHandler theInstance = new DraconianErrorHandler();

    private DraconianErrorHandler() {}

    public void error(SAXParseException e) throws SAXException {
        throw e;
    }
    public void fatalError(SAXParseException e) throws SAXException {
        throw e;
    }
    public void warning(SAXParseException e) throws SAXException {
        ; // noop
    }
}
