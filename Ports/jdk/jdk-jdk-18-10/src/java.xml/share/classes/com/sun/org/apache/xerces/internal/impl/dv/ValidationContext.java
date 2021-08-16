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

package com.sun.org.apache.xerces.internal.impl.dv;

import java.util.Locale;

/**
 * ValidationContext has all the information required for the
 * validation of: id, idref, entity, notation, qname
 *
 * @xerces.internal
 *
 * @author Sandy Gao, IBM
 */
public interface ValidationContext {
    // whether to validate against facets
    public boolean needFacetChecking();

    // whether to do extra id/idref/entity checking
    public boolean needExtraChecking();

    // whether we need to normalize the value that is passed!
    public boolean needToNormalize();

    // are namespaces relevant in this context?
    public boolean useNamespaces();

    // entity
    public boolean isEntityDeclared (String name);
    public boolean isEntityUnparsed (String name);

    // id
    public boolean isIdDeclared (String name);
    public void    addId(String name);

    // idref
    public void addIdRef(String name);

    // get symbol from symbol table
    public String getSymbol (String symbol);

    // qname
    public String getURI(String prefix);

    // Locale
    public Locale getLocale();

}
