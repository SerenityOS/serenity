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

import com.sun.org.apache.xerces.internal.utils.XMLLimitAnalyzer;
import java.io.IOException;
import com.sun.org.apache.xerces.internal.xni.XNIException;

/**
 * This interface defines a generic DTD scanner. This interface
 * allows a scanner to be used interchangably in existing parser
 * configurations.
 * <p>
 * If the parser configuration uses a DTD scanner that implements
 * this interface, components should be able to query the scanner
 * instance from the component manager using the following property
 * identifier:
 * <blockquote>
 *  "http://apache.org/xml/properties/internal/dtd-scanner"
 * </blockquote>
 *
 * @author Andy Clark, IBM
 *
 */
public interface XMLDTDScanner
    extends XMLDTDSource, XMLDTDContentModelSource {

    //
    // XMLDTDScanner methods
    //

    /**
     * Sets the input source.
     *
     * @param inputSource The input source or null.
     *
     * @throws IOException Thrown on i/o error.
     */
    public void setInputSource(XMLInputSource inputSource) throws IOException;

    /**
     * Scans the internal subset of the document.
     *
     * @param complete True if the scanner should scan the document
     *                 completely, pushing all events to the registered
     *                 document handler. A value of false indicates that
     *                 that the scanner should only scan the next portion
     *                 of the document and return. A scanner instance is
     *                 permitted to completely scan a document if it does
     *                 not support this "pull" scanning model.
     * @param standalone True if the document was specified as standalone.
     *                   This value is important for verifying certain
     *                   well-formedness constraints.
     * @param hasExternalSubset True if the document has an external DTD.
     *                          This allows the scanner to properly notify
     *                          the handler of the end of the DTD in the
     *                          absence of an external subset.
     *
     * @return True if there is more to scan, false otherwise.
     */
    public boolean scanDTDInternalSubset(boolean complete, boolean standalone,
                                         boolean hasExternalSubset)
        throws IOException, XNIException;

    /**
     * Scans the external subset of the document.
     *
     * @param complete True if the scanner should scan the document
     *                 completely, pushing all events to the registered
     *                 document handler. A value of false indicates that
     *                 that the scanner should only scan the next portion
     *                 of the document and return. A scanner instance is
     *                 permitted to completely scan a document if it does
     *                 not support this "pull" scanning model.
     *
     * @return True if there is more to scan, false otherwise.
     */
    public boolean scanDTDExternalSubset(boolean complete)
        throws IOException, XNIException;

    /**
     * Skip the DTD if javax.xml.stream.supportDTD is false.
     * @param supportDTD The value of the property javax.xml.stream.supportDTD.
     * @return true if DTD is skipped, false otherwise.
     * @throws java.io.IOException if i/o error occurs
     */
    public boolean skipDTD(boolean supportDTD)
        throws IOException;

    public void setLimitAnalyzer(XMLLimitAnalyzer limitAnalyzer);
} // interface XMLDTDScanner
