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

package com.sun.org.apache.xerces.internal.impl.xs.traversers;

import com.sun.org.apache.xerces.internal.impl.xpath.XPathException;
import com.sun.org.apache.xerces.internal.impl.xs.SchemaSymbols;
import com.sun.org.apache.xerces.internal.impl.xs.identity.Field;
import com.sun.org.apache.xerces.internal.impl.xs.identity.IdentityConstraint;
import com.sun.org.apache.xerces.internal.impl.xs.identity.Selector;
import com.sun.org.apache.xerces.internal.util.DOMUtil;
import com.sun.org.apache.xerces.internal.util.XMLChar;
import org.w3c.dom.Element;

/**
 * This class contains code that all three IdentityConstraint
 * traversers (the XSDUniqueTraverser, XSDKeyTraverser and
 * XSDKeyrefTraverser) rely upon.
 *
 * @xerces.internal
 *
 */
class XSDAbstractIDConstraintTraverser extends XSDAbstractTraverser {

    public XSDAbstractIDConstraintTraverser (XSDHandler handler,
            XSAttributeChecker gAttrCheck) {
        super(handler, gAttrCheck);
    }

    boolean traverseIdentityConstraint(IdentityConstraint ic,
            Element icElem, XSDocumentInfo schemaDoc, Object [] icElemAttrs) {

        // General Attribute Checking will have been done on icElem by caller

        // check for <annotation> and get selector
        Element sElem = DOMUtil.getFirstChildElement(icElem);
        if(sElem == null) {
            reportSchemaError("s4s-elt-must-match.2",
                    new Object[]{"identity constraint", "(annotation?, selector, field+)"},
                    icElem);
            return false;
        }

        // General Attribute Checking on sElem
        // first child could be an annotation
        if (DOMUtil.getLocalName(sElem).equals(SchemaSymbols.ELT_ANNOTATION)) {
            ic.addAnnotation(traverseAnnotationDecl(sElem, icElemAttrs, false, schemaDoc));
            sElem = DOMUtil.getNextSiblingElement(sElem);
            // if no more children report an error
            if(sElem == null) {
                reportSchemaError("s4s-elt-must-match.2", new Object[]{"identity constraint", "(annotation?, selector, field+)"}, icElem);
                return false;
            }
        }
        else {
            String text = DOMUtil.getSyntheticAnnotation(icElem);
            if (text != null) {
                ic.addAnnotation(traverseSyntheticAnnotation(icElem, text, icElemAttrs, false, schemaDoc));
            }
        }

        // must be <selector>
        if(!DOMUtil.getLocalName(sElem).equals(SchemaSymbols.ELT_SELECTOR)) {
            reportSchemaError("s4s-elt-must-match.1", new Object[]{"identity constraint", "(annotation?, selector, field+)", SchemaSymbols.ELT_SELECTOR}, sElem);
            return false;
        }
        Object [] attrValues = fAttrChecker.checkAttributes(sElem, false, schemaDoc);

        // make sure <selector>'s content is fine:
        Element selChild = DOMUtil.getFirstChildElement(sElem);

        if (selChild !=null) {
            // traverse annotation if any
            if (DOMUtil.getLocalName(selChild).equals(SchemaSymbols.ELT_ANNOTATION)) {
                ic.addAnnotation(traverseAnnotationDecl(selChild, attrValues, false, schemaDoc));
                selChild = DOMUtil.getNextSiblingElement(selChild);
            }
            else {
                reportSchemaError("s4s-elt-must-match.1", new Object[]{SchemaSymbols.ELT_SELECTOR, "(annotation?)", DOMUtil.getLocalName(selChild)}, selChild);
            }
            if (selChild != null) {
                reportSchemaError("s4s-elt-must-match.1", new Object [] {SchemaSymbols.ELT_SELECTOR, "(annotation?)", DOMUtil.getLocalName(selChild)}, selChild);
            }
        }
        else {
            String text = DOMUtil.getSyntheticAnnotation(sElem);
            if (text != null) {
                ic.addAnnotation(traverseSyntheticAnnotation(icElem, text, attrValues, false, schemaDoc));
            }
        }

        String sText = ((String)attrValues[XSAttributeChecker.ATTIDX_XPATH]);
        if(sText == null) {
            reportSchemaError("s4s-att-must-appear", new Object [] {SchemaSymbols.ELT_SELECTOR, SchemaSymbols.ATT_XPATH}, sElem);
            return false;
        }
        sText = XMLChar.trim(sText);

        Selector.XPath sXpath = null;
        try {
            sXpath = new Selector.XPath(sText, fSymbolTable,
                    schemaDoc.fNamespaceSupport);
            Selector selector = new Selector(sXpath, ic);
            ic.setSelector(selector);
        }
        catch (XPathException e) {
            reportSchemaError(e.getKey(), new Object[]{sText}, sElem);
            // put back attr values...
            fAttrChecker.returnAttrArray(attrValues, schemaDoc);
            return false;
        }

        // put back attr values...
        fAttrChecker.returnAttrArray(attrValues, schemaDoc);

        // get fields
        Element fElem = DOMUtil.getNextSiblingElement(sElem);
        if(fElem == null) {
            reportSchemaError("s4s-elt-must-match.2", new Object[]{"identity constraint", "(annotation?, selector, field+)"}, sElem);
            return false;
        }
        while (fElem != null) {
            if(!DOMUtil.getLocalName(fElem).equals(SchemaSymbols.ELT_FIELD)) {
                reportSchemaError("s4s-elt-must-match.1", new Object[]{"identity constraint", "(annotation?, selector, field+)", SchemaSymbols.ELT_FIELD}, fElem);
                fElem = DOMUtil.getNextSiblingElement(fElem);
                continue;
            }

            // General Attribute Checking
            attrValues = fAttrChecker.checkAttributes(fElem, false, schemaDoc);

            // and make sure <field>'s content is fine:
            Element fieldChild = DOMUtil.getFirstChildElement(fElem);
            if (fieldChild != null) {
                // traverse annotation
                if (DOMUtil.getLocalName(fieldChild).equals(SchemaSymbols.ELT_ANNOTATION)) {
                    ic.addAnnotation(traverseAnnotationDecl(fieldChild, attrValues, false, schemaDoc));
                    fieldChild = DOMUtil.getNextSiblingElement(fieldChild);
                }
            }
            if (fieldChild != null) {
                reportSchemaError("s4s-elt-must-match.1", new Object [] {SchemaSymbols.ELT_FIELD, "(annotation?)", DOMUtil.getLocalName(fieldChild)}, fieldChild);
            }
            else {
                String text = DOMUtil.getSyntheticAnnotation(fElem);
                if (text != null) {
                    ic.addAnnotation(traverseSyntheticAnnotation(icElem, text, attrValues, false, schemaDoc));
                }
            }
            String fText = ((String)attrValues[XSAttributeChecker.ATTIDX_XPATH]);
            if (fText == null) {
                reportSchemaError("s4s-att-must-appear", new Object [] {SchemaSymbols.ELT_FIELD, SchemaSymbols.ATT_XPATH}, fElem);
                fAttrChecker.returnAttrArray(attrValues, schemaDoc);
                return false;
            }
            fText = XMLChar.trim(fText);
            try {
                Field.XPath fXpath = new Field.XPath(fText, fSymbolTable,
                        schemaDoc.fNamespaceSupport);
                Field field = new Field(fXpath, ic);
                ic.addField(field);
            }
            catch (XPathException e) {
                reportSchemaError(e.getKey(), new Object[]{fText}, fElem);
                // put back attr values...
                fAttrChecker.returnAttrArray(attrValues, schemaDoc);
                return false;
            }
            fElem = DOMUtil.getNextSiblingElement(fElem);
            // put back attr values...
            fAttrChecker.returnAttrArray(attrValues, schemaDoc);
        }

        return ic.getFieldCount() > 0;
    } // traverseIdentityConstraint(IdentityConstraint,Element, XSDocumentInfo)
} // XSDAbstractIDConstraintTraverser
