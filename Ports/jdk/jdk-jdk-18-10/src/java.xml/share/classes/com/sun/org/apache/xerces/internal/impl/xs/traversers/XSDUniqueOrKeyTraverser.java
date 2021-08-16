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

package com.sun.org.apache.xerces.internal.impl.xs.traversers;

import com.sun.org.apache.xerces.internal.impl.xs.SchemaGrammar;
import com.sun.org.apache.xerces.internal.impl.xs.SchemaSymbols;
import com.sun.org.apache.xerces.internal.impl.xs.XSElementDecl;
import com.sun.org.apache.xerces.internal.impl.xs.identity.IdentityConstraint;
import com.sun.org.apache.xerces.internal.impl.xs.identity.UniqueOrKey;
import com.sun.org.apache.xerces.internal.util.DOMUtil;
import org.w3c.dom.Element;

/**
 * This class contains code that is used to traverse both <key>s and
 * <unique>s.
 *
 * @xerces.internal
 *
 * @author Neil Graham, IBM
 * @LastModified: Nov 2017
 */
class XSDUniqueOrKeyTraverser extends XSDAbstractIDConstraintTraverser {

    public XSDUniqueOrKeyTraverser (XSDHandler handler,
                                  XSAttributeChecker gAttrCheck) {
        super(handler, gAttrCheck);
    }


    void traverse(Element uElem, XSElementDecl element,
            XSDocumentInfo schemaDoc, SchemaGrammar grammar) {

        // General Attribute Checking
        Object[] attrValues = fAttrChecker.checkAttributes(uElem, false, schemaDoc);

        // create identity constraint
        String uName = (String)attrValues[XSAttributeChecker.ATTIDX_NAME];

        if(uName == null){
            reportSchemaError("s4s-att-must-appear", new Object [] {DOMUtil.getLocalName(uElem) , SchemaSymbols.ATT_NAME }, uElem);
            //return this array back to pool
            fAttrChecker.returnAttrArray(attrValues, schemaDoc);
            return;
        }

        UniqueOrKey uniqueOrKey;
        if(DOMUtil.getLocalName(uElem).equals(SchemaSymbols.ELT_UNIQUE)) {
            uniqueOrKey = new UniqueOrKey(schemaDoc.fTargetNamespace, uName, element.fName, IdentityConstraint.IC_UNIQUE);
        } else {
            uniqueOrKey = new UniqueOrKey(schemaDoc.fTargetNamespace, uName, element.fName, IdentityConstraint.IC_KEY);
        }
        // it's XSDElementTraverser's job to ensure that there's no
        // duplication (or if there is that restriction is involved
        // and there's identity).

        // If errors occurred in traversing the identity constraint, then don't
        // add it to the schema, to avoid errors when processing the instance.
        if (traverseIdentityConstraint(uniqueOrKey, uElem, schemaDoc, attrValues)) {
            // and stuff this in the grammar
            if (grammar.getIDConstraintDecl(uniqueOrKey.getIdentityConstraintName()) == null) {
                grammar.addIDConstraintDecl(element, uniqueOrKey);
            }

            final String loc = fSchemaHandler.schemaDocument2SystemId(schemaDoc);
            final IdentityConstraint idc = grammar.getIDConstraintDecl(uniqueOrKey.getIdentityConstraintName(), loc);
            if (idc == null) {
                grammar.addIDConstraintDecl(element, uniqueOrKey, loc);
            }

            // handle duplicates
            if (fSchemaHandler.fTolerateDuplicates) {
                if (idc != null) {
                    if (idc instanceof UniqueOrKey) {
                        uniqueOrKey = (UniqueOrKey)idc;
                    }
                }
                fSchemaHandler.addIDConstraintDecl(uniqueOrKey);
            }
        }

        // and fix up attributeChecker
        fAttrChecker.returnAttrArray(attrValues, schemaDoc);
    } // traverse(Element,XSDElementDecl,XSDocumentInfo, SchemaGrammar)
} // XSDUniqueOrKeyTraverser
