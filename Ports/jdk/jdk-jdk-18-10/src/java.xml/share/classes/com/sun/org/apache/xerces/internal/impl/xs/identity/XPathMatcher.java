/*
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.org.apache.xerces.internal.impl.Constants;
import com.sun.org.apache.xerces.internal.impl.xpath.XPath;
import com.sun.org.apache.xerces.internal.util.IntStack;
import com.sun.org.apache.xerces.internal.xni.QName;
import com.sun.org.apache.xerces.internal.xni.XMLAttributes;
import com.sun.org.apache.xerces.internal.xs.AttributePSVI;
import com.sun.org.apache.xerces.internal.xs.ShortList;
import com.sun.org.apache.xerces.internal.xs.XSTypeDefinition;
import org.xml.sax.SAXException;


/**
 * XPath matcher.
 *
 * @xerces.internal
 *
 * @author Andy Clark, IBM
 * @LastModified: July 2019
 *
 */
public class XPathMatcher {

    //
    // Constants
    //

    // debugging

    /** Compile to true to debug everything. */
    protected static final boolean DEBUG_ALL = false;

    /** Compile to true to debug method callbacks. */
    protected static final boolean DEBUG_METHODS = false || DEBUG_ALL;

    /** Compile to true to debug important method callbacks. */
    protected static final boolean DEBUG_METHODS2 = false || DEBUG_METHODS || DEBUG_ALL;

    /** Compile to true to debug the <em>really</em> important methods. */
    protected static final boolean DEBUG_METHODS3 = false || DEBUG_METHODS || DEBUG_ALL;

    /** Compile to true to debug match. */
    protected static final boolean DEBUG_MATCH = false || DEBUG_ALL;

    /** Compile to true to debug step index stack. */
    protected static final boolean DEBUG_STACK = false || DEBUG_ALL;

    /** Don't touch this value unless you add more debug constants. */
    protected static final boolean DEBUG_ANY = DEBUG_METHODS ||
                                               DEBUG_METHODS2 ||
                                               DEBUG_METHODS3 ||
                                               DEBUG_MATCH ||
                                               DEBUG_STACK;

    // constants describing whether a match was made,
    // and if so how.
    // matched any way
    protected static final int MATCHED = 1;
    // matched on the attribute axis
    protected static final int MATCHED_ATTRIBUTE = 3;
    // matched on the descendant-or-self axixs
    protected static final int MATCHED_DESCENDANT = 5;
    // matched some previous (ancestor) node on the descendant-or-self-axis, but not this node
    protected static final int MATCHED_DESCENDANT_PREVIOUS = 13;

    //
    // Data
    //

    /** XPath location path. */
    private final XPath.LocationPath[] fLocationPaths;

    /** True if XPath has been matched. */
    private final int[] fMatched;

    /** The matching string. */
    protected Object fMatchedString;

    /** Integer stack of step indexes. */
    private final IntStack[] fStepIndexes;

    /** Current step. */
    private final int[] fCurrentStep;

    /**
     * No match depth. The value of this field will be zero while
     * matching is successful for the given xpath expression.
     */
    private final int [] fNoMatchDepth;

    final QName fQName = new QName();


    //
    // Constructors
    //

    /**
     * Constructs an XPath matcher that implements a document fragment
     * handler.
     *
     * @param xpath   The xpath.
     */
    public XPathMatcher(XPath xpath) {
        fLocationPaths = xpath.getLocationPaths();
        fStepIndexes = new IntStack[fLocationPaths.length];
        for(int i=0; i<fStepIndexes.length; i++) fStepIndexes[i] = new IntStack();
        fCurrentStep = new int[fLocationPaths.length];
        fNoMatchDepth = new int[fLocationPaths.length];
        fMatched = new int[fLocationPaths.length];
    } // <init>(XPath)

    //
    // Public methods
    //

    /**
     * Returns value of first member of fMatched that
     * is nonzero.
     */
    public boolean isMatched() {
        // xpath has been matched if any one of the members of the union have matched.
        for (int i=0; i < fLocationPaths.length; i++)
            if (((fMatched[i] & MATCHED) == MATCHED)
                    && ((fMatched[i] & MATCHED_DESCENDANT_PREVIOUS) != MATCHED_DESCENDANT_PREVIOUS)
                    && ((fNoMatchDepth[i] == 0)
                    || ((fMatched[i] & MATCHED_DESCENDANT) == MATCHED_DESCENDANT)))
                return true;

        return false;
    } // isMatched():int

    //
    // Protected methods
    //

    // a place-holder method; to be overridden by subclasses
    // that care about matching element content.
    protected void handleContent(XSTypeDefinition type, boolean nillable,
            Object value, short valueType, ShortList itemValueType) {
    }

    /**
     * This method is called when the XPath handler matches the
     * XPath expression. Subclasses can override this method to
     * provide default handling upon a match.
     */
    protected void matched(Object actualValue, short valueType,
            ShortList itemValueType, boolean isNil) {
        if (DEBUG_METHODS3) {
            System.out.println(toString()+"#matched(\""+actualValue+"\")");
        }
    } // matched(String content, XSSimpleType val)

    //
    // ~XMLDocumentFragmentHandler methods
    //

    /**
     * The start of the document fragment.
     */
    public void startDocumentFragment(){
        if (DEBUG_METHODS) {
            System.out.println(toString()+"#startDocumentFragment("+
                               ")");
        }

        // reset state
        fMatchedString = null;
        for(int i = 0; i < fLocationPaths.length; i++) {
            fStepIndexes[i].clear();
            fCurrentStep[i] = 0;
            fNoMatchDepth[i] = 0;
            fMatched[i] = 0;
        }


    } // startDocumentFragment()

    /**
     * The start of an element. If the document specifies the start element
     * by using an empty tag, then the startElement method will immediately
     * be followed by the endElement method, with no intervening methods.
     *
     * @param element    The name of the element.
     * @param attributes The element attributes.
     *
     * @throws SAXException Thrown by handler to signal an error.
     */
    public void startElement(QName element, XMLAttributes attributes) {
        if (DEBUG_METHODS2) {
            System.out.println(toString()+"#startElement("+
                               "element={"+element+"},"+
                               "attributes=..."+attributes+
                               ")");
        }

        for (int i = 0; i < fLocationPaths.length; i++) {
            // push context
            int startStep = fCurrentStep[i];
            fStepIndexes[i].push(startStep);

            // try next xpath, if not matching
            if ((fMatched[i] & MATCHED_DESCENDANT) == MATCHED || fNoMatchDepth[i] > 0) {
                fNoMatchDepth[i]++;
                continue;
            }
            if((fMatched[i] & MATCHED_DESCENDANT) == MATCHED_DESCENDANT) {
                fMatched[i] = MATCHED_DESCENDANT_PREVIOUS;
            }

            if (DEBUG_STACK) {
                System.out.println(toString()+": "+fStepIndexes[i]);
            }

            // consume self::node() steps
            XPath.Step[] steps = fLocationPaths[i].steps;
            while (fCurrentStep[i] < steps.length &&
                    steps[fCurrentStep[i]].axis.type == XPath.Axis.SELF) {
                if (DEBUG_MATCH) {
                    XPath.Step step = steps[fCurrentStep[i]];
                    System.out.println(toString()+" [SELF] MATCHED!");
                }
                fCurrentStep[i]++;
            }
            if (fCurrentStep[i] == steps.length) {
                if (DEBUG_MATCH) {
                    System.out.println(toString()+" XPath MATCHED!");
                }
                fMatched[i] = MATCHED;
                continue;
            }

            // now if the current step is a descendant step, we let the next
            // step do its thing; if it fails, we reset ourselves
            // to look at this step for next time we're called.
            // so first consume all descendants:
            int descendantStep = fCurrentStep[i];
            while(fCurrentStep[i] < steps.length &&
                    steps[fCurrentStep[i]].axis.type == XPath.Axis.DESCENDANT) {
                if (DEBUG_MATCH) {
                    XPath.Step step = steps[fCurrentStep[i]];
                    System.out.println(toString()+" [DESCENDANT] MATCHED!");
                }
                fCurrentStep[i]++;
            }
            boolean sawDescendant = fCurrentStep[i] > descendantStep;
            if (fCurrentStep[i] == steps.length) {
                if (DEBUG_MATCH) {
                    System.out.println(toString()+" XPath DIDN'T MATCH!");
                }
                fNoMatchDepth[i]++;
                if (DEBUG_MATCH) {
                    System.out.println(toString()+" [CHILD] after NO MATCH");
                }
                continue;
            }

            // match child::... step, if haven't consumed any self::node()
            if ((fCurrentStep[i] == startStep || fCurrentStep[i] > descendantStep) &&
                steps[fCurrentStep[i]].axis.type == XPath.Axis.CHILD) {
                XPath.Step step = steps[fCurrentStep[i]];
                XPath.NodeTest nodeTest = step.nodeTest;
                if (DEBUG_MATCH) {
                    System.out.println(toString()+" [CHILD] before");
                }
                if (!matches(nodeTest, element)) {
                    if (fCurrentStep[i] > descendantStep) {
                        fCurrentStep[i] = descendantStep;
                        continue;
                    }
                    fNoMatchDepth[i]++;
                    if (DEBUG_MATCH) {
                        System.out.println(toString()+" [CHILD] after NO MATCH");
                    }
                    continue;
                }
                fCurrentStep[i]++;
                if (DEBUG_MATCH) {
                    System.out.println(toString()+" [CHILD] after MATCHED!");
                }
            }
            if (fCurrentStep[i] == steps.length) {
                if (sawDescendant) {
                    fCurrentStep[i] = descendantStep;
                    fMatched[i] = MATCHED_DESCENDANT;
                }
                else {
                    fMatched[i] = MATCHED;
                }
                continue;
            }

            // match attribute::... step
            if (fCurrentStep[i] < steps.length &&
                steps[fCurrentStep[i]].axis.type == XPath.Axis.ATTRIBUTE) {
                if (DEBUG_MATCH) {
                    System.out.println(toString()+" [ATTRIBUTE] before");
                }
                int attrCount = attributes.getLength();
                if (attrCount > 0) {
                    XPath.NodeTest nodeTest = steps[fCurrentStep[i]].nodeTest;

                    for (int aIndex = 0; aIndex < attrCount; aIndex++) {
                        attributes.getName(aIndex, fQName);
                        if (matches(nodeTest, fQName)) {
                            fCurrentStep[i]++;
                            if (fCurrentStep[i] == steps.length) {
                                fMatched[i] = MATCHED_ATTRIBUTE;
                                int j=0;
                                for(; j<i && ((fMatched[j] & MATCHED) != MATCHED); j++);
                                if(j==i) {
                                    AttributePSVI attrPSVI = (AttributePSVI)attributes.
                                            getAugmentations(aIndex).getItem(Constants.ATTRIBUTE_PSVI);
                                    fMatchedString = attrPSVI.getSchemaValue().getActualValue();
                                    matched(fMatchedString, attrPSVI.getSchemaValue().getActualValueType(),
                                            attrPSVI.getSchemaValue().getListValueTypes(), false);
                                }
                            }
                            break;
                        }
                    }
                }
                if ((fMatched[i] & MATCHED) != MATCHED) {
                    if(fCurrentStep[i] > descendantStep) {
                        fCurrentStep[i] = descendantStep;
                        continue;
                    }
                    fNoMatchDepth[i]++;
                    if (DEBUG_MATCH) {
                        System.out.println(toString()+" [ATTRIBUTE] after");
                    }
                    continue;
                }
                if (DEBUG_MATCH) {
                    System.out.println(toString()+" [ATTRIBUTE] after MATCHED!");
                }
            }
        }

    }
    // startElement(QName,XMLAttrList,int)

    /**
       * @param element
       *        name of the element.
       * @param type
       *        content type of this element. IOW, the XML schema type
       *        of the <tt>value</tt>. Note that this may not be the type declared
       *        in the element declaration, but it is "the actual type". For example,
       *        if the XML is &lt;foo xsi:type="xs:string">aaa&lt;/foo>, this
       *        parameter will be "xs:string".
       * @param nillable - nillable
       *        true if the element declaration is nillable.
       * @param value - actual value
       *        the typed value of the content of this element.
       */
    public void endElement(QName element, XSTypeDefinition type, boolean nillable,
            Object value, short valueType, ShortList itemValueType) {
        if (DEBUG_METHODS2) {
            System.out.println(toString()+"#endElement("+
                               "element={"+element+"},"+
                               ")");
        }
        for (int i = 0; i < fLocationPaths.length; i++) {
            // go back a step
            fCurrentStep[i] = fStepIndexes[i].pop();

            // don't do anything, if not matching
            if (fNoMatchDepth[i] > 0) {
                fNoMatchDepth[i]--;
            }

            // signal match, if appropriate
            else {
                int j = 0;
                for(; j < i && ((fMatched[j] & MATCHED) != MATCHED); j++);
                if ((j < i) || (fMatched[j] == 0)) {
                    continue;
                }
                if ((fMatched[j] & MATCHED_ATTRIBUTE) == MATCHED_ATTRIBUTE) {
                    fMatched[i] = 0;
                    continue;
                }
                // only certain kinds of matchers actually
                // match element content.  This permits
                // them a way to override this to do nothing
                // and hopefully save a few operations.
                handleContent(type, nillable, value, valueType, itemValueType);
                fMatched[i] = 0;
            }

            if (DEBUG_STACK) {
                System.out.println(toString()+": "+fStepIndexes[i]);
            }
        }

    } // endElement(QName)

    //
    // Object methods
    //

    /** Returns a string representation of this object. */
    public String toString() {
        /***
        return fLocationPath.toString();
        /***/
        StringBuffer str = new StringBuffer();
        String s = super.toString();
        int index2 = s.lastIndexOf('.');
        if (index2 != -1) {
            s = s.substring(index2 + 1);
        }
        str.append(s);
        for(int i =0;i<fLocationPaths.length; i++) {
            str.append('[');
            XPath.Step[] steps = fLocationPaths[i].steps;
            for (int j = 0; j < steps.length; j++) {
                if (j == fCurrentStep[i]) {
                    str.append('^');
                }
                str.append(steps[j].toString());
                if (j < steps.length - 1) {
                    str.append('/');
                }
            }
            if (fCurrentStep[i] == steps.length) {
                str.append('^');
            }
            str.append(']');
            str.append(',');
        }
        return str.toString();
    } // toString():String

    //
    // Private methods
    //

    /** Normalizes text. */
    private String normalize(String s) {
        StringBuffer str = new StringBuffer();
        int length = s.length();
        for (int i = 0; i < length; i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\n': {
                    str.append("\\n");
                    break;
                }
                default: {
                    str.append(c);
                }
            }
        }
        return str.toString();
    } // normalize(String):String

    /** Returns true if the given QName matches the node test. **/
    private static boolean matches(XPath.NodeTest nodeTest, QName value) {
        if (nodeTest.type == XPath.NodeTest.QNAME) {
            return nodeTest.name.equals(value);
        }
        if (nodeTest.type == XPath.NodeTest.NAMESPACE) {
            return nodeTest.name.uri == value.uri;
        }
        // XPath.NodeTest.WILDCARD
        return true;
    } // matches(XPath.NodeTest,QName):boolean

    //
    // MAIN
    //

    // NOTE: The main of this class is here for debugging purposes.
    //       However, javac (JDK 1.1.8) has an internal compiler
    //       error when compiling. Jikes has no problem, though.
    //
    //       If you want to use this main, use Jikes to compile but
    //       *never* check in this code to CVS without commenting it
    //       out. -Ac

    /** Main program. */
    /***
    public static void main(String[] argv) throws XNIException {

        if (DEBUG_ANY) {
            for (int i = 0; i < argv.length; i++) {
                final String expr = argv[i];
                final XPath xpath = new XPath(expr, symbols, null);
                final XPathMatcher matcher = new XPathMatcher(xpath, true);
                com.sun.org.apache.xerces.internal.parsers.SAXParser parser =
                    new com.sun.org.apache.xerces.internal.parsers.SAXParser(symbols) {
                    public void startDocument() throws XNIException {
                        matcher.startDocumentFragment(symbols, null);
                    }
                    public void startElement(QName element, XMLAttrList attributes, int handle) throws XNIException {
                        matcher.startElement(element, attributes, handle);
                    }
                    public void characters(char[] ch, int offset, int length) throws XNIException {
                        matcher.characters(ch, offset, length);
                    }
                    public void endElement(QName element) throws XNIException {
                        matcher.endElement(element);
                    }
                    public void endDocument() throws XNIException {
                        matcher.endDocumentFragment();
                    }
                };
                System.out.println("#### argv["+i+"]: \""+expr+"\" -> \""+xpath.toString()+'"');
                final String uri = argv[++i];
                System.out.println("#### argv["+i+"]: "+uri);
                parser.parse(uri);
            }
        }

    } // main(String[])
    /***/

} // class XPathMatcher
