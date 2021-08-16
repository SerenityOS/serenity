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

package com.sun.org.apache.xerces.internal.xni.grammars;

import com.sun.org.apache.xerces.internal.xni.XMLResourceIdentifier;

/**
 * <p> This interface describes basic attributes of XML grammars--their
 * physical location and their type. </p>
 *
 * @author Neil Graham, IBM
 */
public interface XMLGrammarDescription extends XMLResourceIdentifier {

    // initial set of grammar constants that some configurations will recognize;user
    // components which create and/or recognize other types of grammars may
    // certainly use their own constants in place of these (so long as
    // their Grammar objects implement this interface).

    /**
     * The grammar type constant for XML Schema grammars. When getGrammarType()
     * method returns this constant, the object should be an instance of
     * the XMLSchemaDescription interface.
     */
    public static final String XML_SCHEMA = "http://www.w3.org/2001/XMLSchema";

    /**
     * The grammar type constant for DTD grammars. When getGrammarType()
     * method returns this constant, the object should be an instance of
     * the XMLDTDDescription interface.
     */
    public static final String XML_DTD = "http://www.w3.org/TR/REC-xml";

    /**
     * Return the type of this grammar.
     *
     * @return  the type of this grammar
     */
    public String getGrammarType();

} // interface XMLGrammarDescription
