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

package com.sun.org.apache.xerces.internal.impl.dtd;

import com.sun.org.apache.xerces.internal.xni.parser.XMLDocumentFilter;

/**
 * Defines a DTD Validator filter to allow
 * components to query the DTD validator.
 *
 * @xerces.internal
 *
 * @author Elena Litani, IBM
 *
 */
public interface XMLDTDValidatorFilter
    extends XMLDocumentFilter {

    /**
     * Returns true if the validator has a DTD grammar
     *
     * @return true if the validator has a DTD grammar
     */
    public boolean hasGrammar();

    /**
     * Return true if validator must validate the document
     *
     * @return true if validator must validate the document
     */
    public boolean validate();


} // interface XMLDTDValidatorFilter
