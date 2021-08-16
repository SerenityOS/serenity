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

package com.sun.org.apache.xerces.internal.impl.xs.identity;

import com.sun.org.apache.xerces.internal.impl.xpath.XPathException;
import com.sun.org.apache.xerces.internal.util.SymbolTable;
import com.sun.org.apache.xerces.internal.util.XMLChar;
import com.sun.org.apache.xerces.internal.xni.NamespaceContext;
import com.sun.org.apache.xerces.internal.xni.QName;
import com.sun.org.apache.xerces.internal.xni.XMLAttributes;
import com.sun.org.apache.xerces.internal.xs.ShortList;
import com.sun.org.apache.xerces.internal.xs.XSTypeDefinition;

/**
 * Schema identity constraint selector.
 *
 * @xerces.internal
 *
 * @author Andy Clark, IBM
 */
public class Selector {

    //
    // Data
    //

    /** XPath. */
    protected final Selector.XPath fXPath;

    /** Identity constraint. */
    protected final IdentityConstraint fIdentityConstraint;

    // the Identity constraint we're the matcher for.  Only
    // used for selectors!
    protected IdentityConstraint fIDConstraint;

    //
    // Constructors
    //

    /** Constructs a selector. */
    public Selector(Selector.XPath xpath,
                    IdentityConstraint identityConstraint) {
        fXPath = xpath;
        fIdentityConstraint = identityConstraint;
    } // <init>(Selector.XPath,IdentityConstraint)

    //
    // Public methods
    //

    /** Returns the selector XPath. */
    public com.sun.org.apache.xerces.internal.impl.xpath.XPath getXPath() {
        return fXPath;
    } // getXPath():com.sun.org.apache.xerces.internal.v1.schema.identity.XPath

    /** Returns the identity constraint. */
    public IdentityConstraint getIDConstraint() {
        return fIdentityConstraint;
    } // getIDConstraint():IdentityConstraint

    // factory method

    /** Creates a selector matcher.
     * @param activator     The activator for this selector's fields.
     * @param initialDepth  The depth in the document at which this matcher began its life;
     *                          used in correctly handling recursive elements.
     */
    public XPathMatcher createMatcher(FieldActivator activator, int initialDepth) {
        return new Selector.Matcher(fXPath, activator, initialDepth);
    } // createMatcher(FieldActivator):XPathMatcher

    //
    // Object methods
    //

    /** Returns a string representation of this object. */
    public String toString() {
        return fXPath.toString();
    } // toString():String

    //
    // Classes
    //

    /**
     * Schema identity constraint selector XPath expression.
     *
     * @author Andy Clark, IBM
     */
    public static class XPath
    extends com.sun.org.apache.xerces.internal.impl.xpath.XPath {

        //
        // Constructors
        //

        /** Constructs a selector XPath expression. */
        public XPath(String xpath, SymbolTable symbolTable,
                     NamespaceContext context) throws XPathException {
            super(normalize(xpath), symbolTable, context);
            // verify that an attribute is not selected
            for (int i=0;i<fLocationPaths.length;i++) {
                com.sun.org.apache.xerces.internal.impl.xpath.XPath.Axis axis =
                fLocationPaths[i].steps[fLocationPaths[i].steps.length-1].axis;
                if (axis.type == XPath.Axis.ATTRIBUTE) {
                    throw new XPathException("c-selector-xpath");
                }
            }

        } // <init>(String,SymbolTable,NamespacesScope)

        private static String normalize(String xpath) {
            // NOTE: We have to prefix the selector XPath with "./" in
            //       order to handle selectors such as "." that select
            //       the element container because the fields could be
            //       relative to that element. -Ac
            //       Unless xpath starts with a descendant node -Achille Fokoue
            //      ... or a '.' or a '/' - NG
            //  And we also need to prefix exprs to the right of | with ./ - NG
            StringBuffer modifiedXPath = new StringBuffer(xpath.length()+5);
            int unionIndex = -1;
            do {
                if(!(XMLChar.trim(xpath).startsWith("/") || XMLChar.trim(xpath).startsWith("."))) {
                    modifiedXPath.append("./");
                }
                unionIndex = xpath.indexOf('|');
                if(unionIndex == -1) {
                    modifiedXPath.append(xpath);
                    break;
                }
                modifiedXPath.append(xpath.substring(0,unionIndex+1));
                xpath = xpath.substring(unionIndex+1, xpath.length());
            } while(true);
            return modifiedXPath.toString();
        }

    } // class Selector.XPath

    /**
     * Selector matcher.
     *
     * @author Andy Clark, IBM
     */
    public class Matcher
    extends XPathMatcher {

        //
        // Data
        //

        /** Field activator. */
        protected final FieldActivator fFieldActivator;

        /** Initial depth in the document at which this matcher was created. */
        protected final int fInitialDepth;

        /** Element depth. */
        protected int fElementDepth;

        /** Depth at match. */
        protected int fMatchedDepth;

        //
        // Constructors
        //

        /** Constructs a selector matcher. */
        public Matcher(Selector.XPath xpath, FieldActivator activator,
                int initialDepth) {
            super(xpath);
            fFieldActivator = activator;
            fInitialDepth = initialDepth;
        } // <init>(Selector.XPath,FieldActivator)

        //
        // XMLDocumentFragmentHandler methods
        //

        public void startDocumentFragment(){
            super.startDocumentFragment();
            fElementDepth = 0;
            fMatchedDepth = -1;
        } // startDocumentFragment()

        /**
         * The start of an element. If the document specifies the start element
         * by using an empty tag, then the startElement method will immediately
         * be followed by the endElement method, with no intervening methods.
         *
         * @param element    The name of the element.
         * @param attributes The element attributes.
         *
         */
        public void startElement(QName element, XMLAttributes attributes) {
            super.startElement(element, attributes);
            fElementDepth++;
            // activate the fields, if selector is matched
            //int matched = isMatched();

            if (isMatched()) {
/*            (fMatchedDepth == -1 && ((matched & MATCHED) == MATCHED)) ||
                    ((matched & MATCHED_DESCENDANT) == MATCHED_DESCENDANT)) { */
                fMatchedDepth = fElementDepth;
                fFieldActivator.startValueScopeFor(fIdentityConstraint, fInitialDepth);
                int count = fIdentityConstraint.getFieldCount();
                for (int i = 0; i < count; i++) {
                    Field field = fIdentityConstraint.getFieldAt(i);
                    XPathMatcher matcher = fFieldActivator.activateField(field, fInitialDepth);
                    matcher.startElement(element, attributes);
                }
            }

        } // startElement(QName,XMLAttrList,int)

        public void endElement(QName element, XSTypeDefinition type, boolean nillable, Object actualValue, short valueType, ShortList itemValueType) {
            super.endElement(element, type, nillable, actualValue, valueType, itemValueType);
            if (fElementDepth-- == fMatchedDepth) {
                fMatchedDepth = -1;
                fFieldActivator.endValueScopeFor(fIdentityConstraint, fInitialDepth);
            }
        }

        /** Returns the identity constraint. */
        public IdentityConstraint getIdentityConstraint() {
            return fIdentityConstraint;
        } // getIdentityConstraint():IdentityConstraint

        /** get the initial depth at which this selector matched. */
        public int getInitialDepth() {
            return fInitialDepth;
        } // getInitialDepth():  int


    } // class Matcher

} // class Selector
