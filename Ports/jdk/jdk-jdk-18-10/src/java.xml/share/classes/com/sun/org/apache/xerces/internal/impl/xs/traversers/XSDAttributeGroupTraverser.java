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

import com.sun.org.apache.xerces.internal.impl.xs.SchemaGrammar;
import com.sun.org.apache.xerces.internal.impl.xs.SchemaSymbols;
import com.sun.org.apache.xerces.internal.impl.xs.XSAnnotationImpl;
import com.sun.org.apache.xerces.internal.impl.xs.XSAttributeGroupDecl;
import com.sun.org.apache.xerces.internal.impl.xs.util.XSObjectListImpl;
import com.sun.org.apache.xerces.internal.util.DOMUtil;
import com.sun.org.apache.xerces.internal.util.XMLSymbols;
import com.sun.org.apache.xerces.internal.xni.QName;
import com.sun.org.apache.xerces.internal.xs.XSObjectList;
import org.w3c.dom.Element;

/**
 * The attribute group definition schema component traverser.
 *
 * <attributeGroup
 *   id = ID
 *   name = NCName
 *   ref = QName
 *   {any attributes with non-schema namespace . . .}>
 *   Content: (annotation?, ((attribute | attributeGroup)*, anyAttribute?))
 * </attributeGroup>
 *
 * @xerces.internal
 *
 * @author Rahul Srivastava, Sun Microsystems Inc.
 * @author Sandy Gao, IBM
 *
 */
class XSDAttributeGroupTraverser extends XSDAbstractTraverser {

    XSDAttributeGroupTraverser (XSDHandler handler,
            XSAttributeChecker gAttrCheck) {

        super(handler, gAttrCheck);
    }


    XSAttributeGroupDecl traverseLocal(Element elmNode,
            XSDocumentInfo schemaDoc,
            SchemaGrammar grammar) {

        // General Attribute Checking for elmNode declared locally
        Object[] attrValues = fAttrChecker.checkAttributes(elmNode, false, schemaDoc);

        // get attribute
        QName   refAttr = (QName)   attrValues[XSAttributeChecker.ATTIDX_REF];

        XSAttributeGroupDecl attrGrp = null;

        // ref should be here.
        if (refAttr == null) {
            reportSchemaError("s4s-att-must-appear", new Object[]{"attributeGroup (local)", "ref"}, elmNode);
            fAttrChecker.returnAttrArray(attrValues, schemaDoc);
            return null;
        }

        // get global decl
        attrGrp = (XSAttributeGroupDecl)fSchemaHandler.getGlobalDecl(schemaDoc, XSDHandler.ATTRIBUTEGROUP_TYPE, refAttr, elmNode);

        // no children are allowed here except annotation, which is optional.
        Element child = DOMUtil.getFirstChildElement(elmNode);
        if (child != null) {
            String childName = DOMUtil.getLocalName(child);
            if (childName.equals(SchemaSymbols.ELT_ANNOTATION)) {
                traverseAnnotationDecl(child, attrValues, false, schemaDoc);
                child = DOMUtil.getNextSiblingElement(child);
            } else {
                String text = DOMUtil.getSyntheticAnnotation(child);
                if (text != null) {
                    traverseSyntheticAnnotation(child, text, attrValues, false, schemaDoc);
                }
            }

            if (child != null) {
                Object[] args = new Object [] {refAttr.rawname, "(annotation?)", DOMUtil.getLocalName(child)};
                reportSchemaError("s4s-elt-must-match.1", args, child);
            }
        } // if

        fAttrChecker.returnAttrArray(attrValues, schemaDoc);
        return attrGrp;

    } // traverseLocal

    XSAttributeGroupDecl traverseGlobal(Element elmNode,
            XSDocumentInfo schemaDoc,
            SchemaGrammar grammar) {

        XSAttributeGroupDecl attrGrp = new XSAttributeGroupDecl();

        // General Attribute Checking for elmNode declared globally
        Object[] attrValues = fAttrChecker.checkAttributes(elmNode, true, schemaDoc);

        String  nameAttr   = (String) attrValues[XSAttributeChecker.ATTIDX_NAME];

        // global declaration must have a name
        if (nameAttr == null) {
            reportSchemaError("s4s-att-must-appear", new Object[]{"attributeGroup (global)", "name"}, elmNode);
            nameAttr = NO_NAME;
        }

        attrGrp.fName = nameAttr;
        attrGrp.fTargetNamespace = schemaDoc.fTargetNamespace;

        // check the content
        Element child = DOMUtil.getFirstChildElement(elmNode);
        XSAnnotationImpl annotation = null;

        if (child!=null && DOMUtil.getLocalName(child).equals(SchemaSymbols.ELT_ANNOTATION)) {
            annotation = traverseAnnotationDecl(child, attrValues, false, schemaDoc);
            child = DOMUtil.getNextSiblingElement(child);
        }
        else {
            String text = DOMUtil.getSyntheticAnnotation(elmNode);
            if (text != null) {
                annotation = traverseSyntheticAnnotation(elmNode, text, attrValues, false, schemaDoc);
            }
        }

        // Traverse the attribute and attribute group elements and fill in the
        // attributeGroup structure

        Element nextNode = traverseAttrsAndAttrGrps(child, attrGrp, schemaDoc, grammar, null);
        if (nextNode!=null) {
            // An invalid element was found...
            Object[] args = new Object [] {nameAttr, "(annotation?, ((attribute | attributeGroup)*, anyAttribute?))", DOMUtil.getLocalName(nextNode)};
            reportSchemaError("s4s-elt-must-match.1", args, nextNode);
        }

        if (nameAttr.equals(NO_NAME)) {
            // if a global group doesn't have a name, then don't add it.
            fAttrChecker.returnAttrArray(attrValues, schemaDoc);
            return null;
        }

        // Remove prohibited attributes from the set
        attrGrp.removeProhibitedAttrs();

        // check for restricted redefine:
        XSAttributeGroupDecl redefinedAttrGrp = (XSAttributeGroupDecl)fSchemaHandler.getGrpOrAttrGrpRedefinedByRestriction(
                XSDHandler.ATTRIBUTEGROUP_TYPE,
                new QName(XMLSymbols.EMPTY_STRING, nameAttr, nameAttr, schemaDoc.fTargetNamespace),
                schemaDoc, elmNode);
        if(redefinedAttrGrp != null) {
            Object[] errArgs = attrGrp.validRestrictionOf(nameAttr, redefinedAttrGrp);
            if (errArgs != null) {
                reportSchemaError((String)errArgs[errArgs.length-1], errArgs, child);
                reportSchemaError("src-redefine.7.2.2", new Object [] {nameAttr, errArgs[errArgs.length-1]}, child);
            }
        }

        XSObjectList annotations;
        if (annotation != null) {
            annotations = new XSObjectListImpl();
            ((XSObjectListImpl)annotations).addXSObject (annotation);
        } else {
            annotations = XSObjectListImpl.EMPTY_LIST;
        }

        attrGrp.fAnnotations = annotations;

        // make an entry in global declarations.
        if (grammar.getGlobalAttributeGroupDecl(attrGrp.fName) == null) {
            grammar.addGlobalAttributeGroupDecl(attrGrp);
        }

        // also add it to extended map
        final String loc = fSchemaHandler.schemaDocument2SystemId(schemaDoc);
        final XSAttributeGroupDecl attrGrp2 = grammar.getGlobalAttributeGroupDecl(attrGrp.fName, loc);
        if (attrGrp2 == null) {
            grammar.addGlobalAttributeGroupDecl(attrGrp, loc);
        }

        // handle duplicates
        if (fSchemaHandler.fTolerateDuplicates) {
            if (attrGrp2 != null) {
                attrGrp = attrGrp2;
            }
            fSchemaHandler.addGlobalAttributeGroupDecl(attrGrp);
        }

        fAttrChecker.returnAttrArray(attrValues, schemaDoc);
        return attrGrp;

    } // traverseGlobal

} // XSDAttributeGroupTraverser
