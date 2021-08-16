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

/**
 * A generic grammar for use in validating XML documents. The Grammar
 * object stores the validation information in a compiled form. Specific
 * subclasses extend this class and "populate" the grammar by compiling
 * the specific syntax (DTD, Schema, etc) into the data structures used
 * by this object.
 * <p>
 * <strong>Note:</strong> The Grammar object is not useful as a generic
 * grammar access or query object. In other words, you cannot round-trip
 * specific grammar syntaxes with the compiled grammar information in
 * the Grammar object. You <em>can</em> create equivalent validation
 * rules in your choice of grammar syntax but there is no guarantee that
 * the input and output will be the same.
 *
 * <p> Right now, this class is largely a shell; eventually,
 * it will be enriched by having more expressive methods added. </p>
 * will be moved from dtd.Grammar here.
 *
 * @author Jeffrey Rodriguez, IBM
 * @author Eric Ye, IBM
 * @author Andy Clark, IBM
 * @author Neil Graham, IBM
 *
 */

public interface Grammar {

    /**
     * get the <code>XMLGrammarDescription</code> associated with this
     * object
     */
    public XMLGrammarDescription getGrammarDescription ();
} // interface Grammar
