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

package com.sun.org.apache.xerces.internal.impl.xs.util;

import com.sun.org.apache.xerces.internal.impl.Constants;
import com.sun.org.apache.xerces.internal.impl.xs.SchemaGrammar;
import com.sun.org.apache.xerces.internal.impl.xs.XSModelImpl;
import com.sun.org.apache.xerces.internal.util.XMLGrammarPoolImpl;
import com.sun.org.apache.xerces.internal.xni.grammars.Grammar;
import com.sun.org.apache.xerces.internal.xni.grammars.XMLGrammarDescription;
import com.sun.org.apache.xerces.internal.xs.XSModel;
import java.util.ArrayList;
import java.util.List;


/**
 * Add a method that return an <code>XSModel</code> that represents components in
 * the schema grammars in this pool implementation.
 *
 * @xerces.internal
 *
 * @LastModified: Nov 2017
 */
public class XSGrammarPool extends XMLGrammarPoolImpl {

    /**
     * Return an <code>XSModel</code> that represents components in
     * the schema grammars in this pool implementation.
     *
     * @return  an <code>XSModel</code> representing this schema grammar
     */
    public XSModel toXSModel() {
        return toXSModel(Constants.SCHEMA_VERSION_1_0);
    }

    public XSModel toXSModel(short schemaVersion) {
        List<Grammar> list = new ArrayList<>();
        for (int i = 0; i < fGrammars.length; i++) {
            for (Entry entry = fGrammars[i] ; entry != null ; entry = entry.next) {
                if (entry.desc.getGrammarType().equals(XMLGrammarDescription.XML_SCHEMA)) {
                    list.add(entry.grammar);
                }
            }
        }
        int size = list.size();
        if (size == 0) {
            return toXSModel(new SchemaGrammar[0], schemaVersion);
        }
        SchemaGrammar[] gs = list.toArray(new SchemaGrammar[size]);
        return toXSModel(gs, schemaVersion);
    }

    protected XSModel toXSModel(SchemaGrammar[] grammars, short schemaVersion) {
        return new XSModelImpl(grammars, schemaVersion);
    }

} // class XSGrammarPool
