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
import org.w3c.dom.DOMErrorHandler;

/**
 * This is the default implementation of the ErrorHandler interface and is
 * used if one is not provided.  The default implementation simply reports
 * DOMErrors to System.err.
 *
 * @xsl.usage internal
 */
final class DOMErrorHandlerImpl implements DOMErrorHandler {

    /**
     * Default Constructor
     */
    DOMErrorHandlerImpl() {
    }

    /**
     * Implementation of DOMErrorHandler.handleError that
     * adds copy of error to list for later retrieval.
     *
     */
    public boolean handleError(DOMError error) {
        boolean fail = true;
        String severity = null;
        if (error.getSeverity() == DOMError.SEVERITY_WARNING) {
            fail = false;
            severity = "[Warning]";
        } else if (error.getSeverity() == DOMError.SEVERITY_ERROR) {
            severity = "[Error]";
        } else if (error.getSeverity() == DOMError.SEVERITY_FATAL_ERROR) {
            severity = "[Fatal Error]";
        }

        System.err.println(severity + ": " + error.getMessage() + "\t");
        System.err.println("Type : " + error.getType() + "\t" + "Related Data: "
                + error.getRelatedData() + "\t" + "Related Exception: "
                + error.getRelatedException() );

        return fail;
    }
}
