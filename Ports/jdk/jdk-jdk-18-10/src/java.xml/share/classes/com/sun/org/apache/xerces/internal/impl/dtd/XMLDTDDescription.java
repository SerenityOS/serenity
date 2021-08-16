/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.org.apache.xerces.internal.impl.dtd;

import com.sun.org.apache.xerces.internal.util.XMLResourceIdentifierImpl;
import com.sun.org.apache.xerces.internal.xni.XMLResourceIdentifier;
import com.sun.org.apache.xerces.internal.xni.grammars.XMLGrammarDescription;
import com.sun.org.apache.xerces.internal.xni.parser.XMLInputSource;
import java.util.List;

/**
 * All information specific to DTD grammars.
 *
 * @xerces.internal
 *
 * @author Neil Graham, IBM
 * @LastModified: Oct 2017
 */
public class XMLDTDDescription extends XMLResourceIdentifierImpl
        implements com.sun.org.apache.xerces.internal.xni.grammars.XMLDTDDescription {

    // Data

    // pieces of information needed to make this usable as a Grammar key
    // if we know the root of this grammar, here's its name:
    protected String fRootName = null;

    // if we don't know the root name, this stores all elements that
    // could serve; fPossibleRoots and fRootName cannot both be non-null
    protected List<String> fPossibleRoots = null;

    // Constructors:
    public XMLDTDDescription(XMLResourceIdentifier id, String rootName) {
        this.setValues(id.getPublicId(), id.getLiteralSystemId(),
                id.getBaseSystemId(), id.getExpandedSystemId());
        this.fRootName = rootName;
        this.fPossibleRoots = null;
    } // init(XMLResourceIdentifier, String)

    public XMLDTDDescription(String publicId, String literalId,
                String baseId, String expandedId, String rootName) {
        this.setValues(publicId, literalId, baseId, expandedId);
        this.fRootName = rootName;
        this.fPossibleRoots = null;
    } // init(String, String, String, String, String)

    public XMLDTDDescription(XMLInputSource source) {
        this.setValues(source.getPublicId(), null,
                source.getBaseSystemId(), source.getSystemId());
        this.fRootName = null;
        this.fPossibleRoots = null;
    } // init(XMLInputSource)

    // XMLGrammarDescription methods

    public String getGrammarType () {
        return XMLGrammarDescription.XML_DTD;
    } // getGrammarType():  String

    /**
     * @return the root name of this DTD or null if root name is unknown
     */
    public String getRootName() {
        return fRootName;
    } // getRootName():  String

    /** Set the root name **/
    public void setRootName(String rootName) {
        fRootName = rootName;
        fPossibleRoots = null;
    }

    /** Set possible roots **/
    public void setPossibleRoots(List<String> possibleRoots) {
        fPossibleRoots = possibleRoots;
    }

    /**
     * Compares this grammar with the given grammar. Currently, we compare
     * as follows:
     * - if grammar type not equal return false immediately
     * - try and find a common root name:
     *    - if both have roots, use them
     *    - else if one has a root, examine other's possible root's for a match;
     *    - else try all combinations
     *  - test fExpandedSystemId and fPublicId as above
     *
     * @param desc The description of the grammar to be compared with
     * @return     True if they are equal, else false
     */
    public boolean equals(Object desc) {
        if (!(desc instanceof XMLGrammarDescription)) return false;
        if (!getGrammarType().equals(((XMLGrammarDescription)desc).getGrammarType())) {
            return false;
        }
        // assume it's a DTDDescription
        XMLDTDDescription dtdDesc = (XMLDTDDescription)desc;
        if (fRootName != null) {
            if ((dtdDesc.fRootName) != null && !dtdDesc.fRootName.equals(fRootName)) {
                return false;
            }
            else if (dtdDesc.fPossibleRoots != null && !dtdDesc.fPossibleRoots.contains(fRootName)) {
                return false;
            }
        }
        else if (fPossibleRoots != null) {
            if (dtdDesc.fRootName != null) {
                if (!fPossibleRoots.contains(dtdDesc.fRootName)) {
                    return false;
                }
            }
            else if (dtdDesc.fPossibleRoots == null) {
                return false;
            }
            else {
                boolean found = false;
                for (String root : fPossibleRoots) {
                    found = dtdDesc.fPossibleRoots.contains(root);
                    if (found) break;
                }
                if (!found) return false;
            }
        }
        // if we got this far we've got a root match... try other two fields,
        // since so many different DTD's have roots in common:
        if (fExpandedSystemId != null) {
            if (!fExpandedSystemId.equals(dtdDesc.fExpandedSystemId)) {
                return false;
            }
        }
        else if (dtdDesc.fExpandedSystemId != null) {
            return false;
        }
        if (fPublicId != null) {
            if (!fPublicId.equals(dtdDesc.fPublicId)) {
                return false;
            }
        }
        else if (dtdDesc.fPublicId != null) {
            return false;
        }
        return true;
    }

    /**
     * Returns the hash code of this grammar
     * Because our .equals method is so complex, we just return a very
     * simple hash that might avoid calls to the equals method a bit...
     * @return The hash code
     */
    public int hashCode() {
        if (fExpandedSystemId != null) {
            return fExpandedSystemId.hashCode();
        }
        if (fPublicId != null) {
            return fPublicId.hashCode();
        }
        // give up; hope .equals can handle it:
        return 0;
    }
} // class XMLDTDDescription
