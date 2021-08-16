/*
 * Copyright (c) 2007, 2017, Oracle and/or its affiliates. All rights reserved.
 */
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * $Id: BasisLibrary.java,v 1.6 2006/06/20 21:51:58 spericas Exp $
 */

package com.sun.org.apache.xalan.internal.xsltc.runtime;

import com.sun.org.apache.xalan.internal.xsltc.DOM;
import com.sun.org.apache.xalan.internal.xsltc.Translet;
import com.sun.org.apache.xalan.internal.xsltc.dom.AbsoluteIterator;
import com.sun.org.apache.xalan.internal.xsltc.dom.ArrayNodeListIterator;
import com.sun.org.apache.xalan.internal.xsltc.dom.DOMAdapter;
import com.sun.org.apache.xalan.internal.xsltc.dom.MultiDOM;
import com.sun.org.apache.xalan.internal.xsltc.dom.SingletonIterator;
import com.sun.org.apache.xalan.internal.xsltc.dom.StepIterator;
import com.sun.org.apache.xml.internal.dtm.Axis;
import com.sun.org.apache.xml.internal.dtm.DTM;
import com.sun.org.apache.xml.internal.dtm.DTMAxisIterator;
import com.sun.org.apache.xml.internal.dtm.DTMManager;
import com.sun.org.apache.xml.internal.dtm.ref.DTMDefaultBase;
import com.sun.org.apache.xml.internal.dtm.ref.DTMNodeProxy;
import com.sun.org.apache.xml.internal.serializer.NamespaceMappings;
import com.sun.org.apache.xml.internal.serializer.SerializationHandler;
import com.sun.org.apache.xml.internal.utils.XML11Char;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.FieldPosition;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicInteger;
import javax.xml.transform.dom.DOMSource;
import jdk.xml.internal.SecuritySupport;
import org.w3c.dom.Attr;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Standard XSLT functions. All standard functions expect the current node
 * and the DOM as their last two arguments.
 *
 * @LastModified: Sep 2017
 */
public final class BasisLibrary {

    private final static String EMPTYSTRING = "";

    /**
     * Re-use a single instance of StringBuffer (per thread) in the basis library.
     * StringBuilder is better, however, DecimalFormat only accept StringBuffer
     */
    private static final ThreadLocal<StringBuilder> threadLocalStringBuilder =
        new ThreadLocal<StringBuilder> () {
            @Override protected StringBuilder initialValue() {
                return new StringBuilder();
            }
    };

    /**
     * ThreadLocal for StringBuffer used
     */
    private static final ThreadLocal<StringBuffer> threadLocalStringBuffer =
        new ThreadLocal<StringBuffer> () {
            @Override protected StringBuffer initialValue() {
                return new StringBuffer();
            }
    };

    /**
     * Standard function count(node-set)
     */
    public static int countF(DTMAxisIterator iterator) {
        return(iterator.getLast());
    }

    /**
     * Standard function position()
     * @deprecated This method exists only for backwards compatibility with old
     *             translets.  New code should not reference it.
     */
    @Deprecated
    public static int positionF(DTMAxisIterator iterator) {
        return iterator.isReverse()
                     ? iterator.getLast() - iterator.getPosition() + 1
                     : iterator.getPosition();
    }

    /**
     * XSLT Standard function sum(node-set).
     * stringToDouble is inlined
     */
    public static double sumF(DTMAxisIterator iterator, DOM dom) {
        try {
            double result = 0.0;
            int node;
            while ((node = iterator.next()) != DTMAxisIterator.END) {
                result += Double.parseDouble(dom.getStringValueX(node));
            }
            return result;
        }
        catch (NumberFormatException e) {
            return Double.NaN;
        }
    }

    /**
     * XSLT Standard function string()
     */
    public static String stringF(int node, DOM dom) {
        return dom.getStringValueX(node);
    }

    /**
     * XSLT Standard function string(value)
     */
    public static String stringF(Object obj, DOM dom) {
        if (obj instanceof DTMAxisIterator) {
            return dom.getStringValueX(((DTMAxisIterator)obj).reset().next());
        }
        else if (obj instanceof Node) {
            return dom.getStringValueX(((Node)obj).node);
        }
        else if (obj instanceof DOM) {
            return ((DOM)obj).getStringValue();
        }
        else {
            return obj.toString();
        }
    }

    /**
     * XSLT Standard function string(value)
     */
    public static String stringF(Object obj, int node, DOM dom) {
        if (obj instanceof DTMAxisIterator) {
            return dom.getStringValueX(((DTMAxisIterator)obj).reset().next());
        }
        else if (obj instanceof Node) {
            return dom.getStringValueX(((Node)obj).node);
        }
        else if (obj instanceof DOM) {
            // When the first argument is a DOM we want the whole
            // DOM and not just a single node - that would not make sense.
            //return ((DOM)obj).getStringValueX(node);
            return ((DOM)obj).getStringValue();
        }
        else if (obj instanceof Double) {
            Double d = (Double)obj;
            final String result = d.toString();
            final int length = result.length();
            if ((result.charAt(length-2)=='.') &&
                (result.charAt(length-1) == '0'))
                return result.substring(0, length-2);
            else
                return result;
        }
        else {
            return obj != null ? obj.toString() : "";
        }
    }

    /**
     * XSLT Standard function number()
     */
    public static double numberF(int node, DOM dom) {
        return stringToReal(dom.getStringValueX(node));
    }

    /**
     * XSLT Standard function number(value)
     */
    public static double numberF(Object obj, DOM dom) {
        if (obj instanceof Double) {
            return ((Double) obj).doubleValue();
        }
        else if (obj instanceof Integer) {
            return ((Integer) obj).doubleValue();
        }
        else if (obj instanceof Boolean) {
            return  ((Boolean) obj).booleanValue() ? 1.0 : 0.0;
        }
        else if (obj instanceof String) {
            return stringToReal((String) obj);
        }
        else if (obj instanceof DTMAxisIterator) {
            DTMAxisIterator iter = (DTMAxisIterator) obj;
            return stringToReal(dom.getStringValueX(iter.reset().next()));
        }
        else if (obj instanceof Node) {
            return stringToReal(dom.getStringValueX(((Node) obj).node));
        }
        else if (obj instanceof DOM) {
            return stringToReal(((DOM) obj).getStringValue());
        }
        else {
            final String className = obj.getClass().getName();
            runTimeError(INVALID_ARGUMENT_ERR, className, "number()");
            return 0.0;
        }
    }

    /**
     * XSLT Standard function round()
     */
    public static double roundF(double d) {
            return (d<-0.5 || d>0.0)?Math.floor(d+0.5):((d==0.0)?
                        d:(Double.isNaN(d)?Double.NaN:-0.0));
    }

    /**
     * XSLT Standard function boolean()
     */
    public static boolean booleanF(Object obj) {
        if (obj instanceof Double) {
            final double temp = ((Double) obj).doubleValue();
            return temp != 0.0 && !Double.isNaN(temp);
        }
        else if (obj instanceof Integer) {
            return ((Integer) obj).doubleValue() != 0;
        }
        else if (obj instanceof Boolean) {
            return  ((Boolean) obj).booleanValue();
        }
        else if (obj instanceof String) {
            return !((String) obj).equals(EMPTYSTRING);
        }
        else if (obj instanceof DTMAxisIterator) {
            DTMAxisIterator iter = (DTMAxisIterator) obj;
            return iter.reset().next() != DTMAxisIterator.END;
        }
        else if (obj instanceof Node) {
            return true;
        }
        else if (obj instanceof DOM) {
            String temp = ((DOM) obj).getStringValue();
            return !temp.equals(EMPTYSTRING);
        }
        else {
            final String className = obj.getClass().getName();
            runTimeError(INVALID_ARGUMENT_ERR, className, "boolean()");
        }
        return false;
    }

    /**
     * XSLT Standard function substring(). Must take a double because of
     * conversions resulting into NaNs and rounding.
     */
    public static String substringF(String value, double start) {
        if (Double.isNaN(start))
            return(EMPTYSTRING);

        final int strlen = getStringLength(value);
        int istart = (int)Math.round(start) - 1;

        if (istart > strlen)
            return(EMPTYSTRING);
        if (istart < 1)
            istart = 0;
        try {
            istart = value.offsetByCodePoints(0, istart);
            return value.substring(istart);
        } catch (IndexOutOfBoundsException e) {
            runTimeError(RUN_TIME_INTERNAL_ERR, "substring()");
            return null;
        }
    }

    /**
     * XSLT Standard function substring(). Must take a double because of
     * conversions resulting into NaNs and rounding.
     */
    public static String substringF(String value, double start, double length) {
        if (Double.isInfinite(start) ||
            Double.isNaN(start) ||
            Double.isNaN(length) ||
            length < 0)
            return(EMPTYSTRING);

        int istart = (int)Math.round(start) - 1;
        int ilength = (int)Math.round(length);
        final int isum;
        if (Double.isInfinite(length))
            isum = Integer.MAX_VALUE;
        else
            isum = istart + ilength;

        final int strlen = getStringLength(value);
        if (isum < 0 || istart > strlen)
                return(EMPTYSTRING);

        if (istart < 0) {
            ilength += istart;
            istart = 0;
        }

        try {
            istart = value.offsetByCodePoints(0, istart);
            if (isum > strlen) {
                return value.substring(istart);
            } else {
                int offset = value.offsetByCodePoints(istart, ilength);
                return value.substring(istart, offset);
            }
        } catch (IndexOutOfBoundsException e) {
            runTimeError(RUN_TIME_INTERNAL_ERR, "substring()");
            return null;
        }
    }

    /**
     * XSLT Standard function substring-after().
     */
    public static String substring_afterF(String value, String substring) {
        final int index = value.indexOf(substring);
        if (index >= 0)
            return value.substring(index + substring.length());
        else
            return EMPTYSTRING;
    }

    /**
     * XSLT Standard function substring-before().
     */
    public static String substring_beforeF(String value, String substring) {
        final int index = value.indexOf(substring);
        if (index >= 0)
            return value.substring(0, index);
        else
            return EMPTYSTRING;
    }

    /**
     * XSLT Standard function translate().
     */
    public static String translateF(String value, String from, String to) {
        final int tol = to.length();
        final int froml = from.length();
        final int valuel = value.length();

        final StringBuilder result = threadLocalStringBuilder.get();
    result.setLength(0);
        for (int j, i = 0; i < valuel; i++) {
            final char ch = value.charAt(i);
            for (j = 0; j < froml; j++) {
                if (ch == from.charAt(j)) {
                    if (j < tol)
                        result.append(to.charAt(j));
                    break;
                }
            }
            if (j == froml)
                result.append(ch);
        }
        return result.toString();
    }

    /**
     * XSLT Standard function normalize-space().
     */
    public static String normalize_spaceF(int node, DOM dom) {
        return normalize_spaceF(dom.getStringValueX(node));
    }

    /**
     * XSLT Standard function normalize-space(string).
     */
    public static String normalize_spaceF(String value) {
        int i = 0, n = value.length();
        StringBuilder result = threadLocalStringBuilder.get();
    result.setLength(0);

        while (i < n && isWhiteSpace(value.charAt(i)))
            i++;

        while (true) {
            while (i < n && !isWhiteSpace(value.charAt(i))) {
                result.append(value.charAt(i++));
            }
            if (i == n)
                break;
            while (i < n && isWhiteSpace(value.charAt(i))) {
                i++;
            }
            if (i < n)
                result.append(' ');
        }
        return result.toString();
    }

    /**
     * XSLT Standard function generate-id().
     */
    public static String generate_idF(int node) {
        if (node > 0)
            // Only generate ID if node exists
            return "N" + node;
        else
            // Otherwise return an empty string
            return EMPTYSTRING;
    }

    /**
     * utility function for calls to local-name().
     */
    public static String getLocalName(String value) {
        int idx = value.lastIndexOf(':');
        if (idx >= 0) value = value.substring(idx + 1);
        idx = value.lastIndexOf('@');
        if (idx >= 0) value = value.substring(idx + 1);
        return(value);
    }

    /**
     * External functions that cannot be resolved are replaced with a call
     * to this method. This method will generate a runtime errors. A good
     * stylesheet checks whether the function exists using conditional
     * constructs, and never really tries to call it if it doesn't exist.
     * But simple stylesheets may result in a call to this method.
     * The compiler should generate a warning if it encounters a call to
     * an unresolved external function.
     */
    public static void unresolved_externalF(String name) {
        runTimeError(EXTERNAL_FUNC_ERR, name);
    }

    /**
     * Utility function to throw a runtime error on the use of an extension
     * function when the secure processing feature is set to true.
     */
    public static void unallowed_extension_functionF(String name) {
        runTimeError(UNALLOWED_EXTENSION_FUNCTION_ERR, name);
    }

    /**
     * Utility function to throw a runtime error on the use of an extension
     * element when the secure processing feature is set to true.
     */
    public static void unallowed_extension_elementF(String name) {
        runTimeError(UNALLOWED_EXTENSION_ELEMENT_ERR, name);
    }

    /**
     * Utility function to throw a runtime error for an unsupported element.
     *
     * This is only used in forward-compatibility mode, when the control flow
     * cannot be determined. In 1.0 mode, the error message is emitted at
     * compile time.
     */
    public static void unsupported_ElementF(String qname, boolean isExtension) {
        if (isExtension)
            runTimeError(UNSUPPORTED_EXT_ERR, qname);
        else
            runTimeError(UNSUPPORTED_XSL_ERR, qname);
    }

    /**
     * XSLT Standard function namespace-uri(node-set).
     */
    public static String namespace_uriF(DTMAxisIterator iter, DOM dom) {
        return namespace_uriF(iter.next(), dom);
    }

    /**
     * XSLT Standard function system-property(name)
     */
    public static String system_propertyF(String name) {
        if (name.equals("xsl:version"))
            return("1.0");
        if (name.equals("xsl:vendor"))
            return("Apache Software Foundation (Xalan XSLTC)");
        if (name.equals("xsl:vendor-url"))
            return("http://xml.apache.org/xalan-j");

        runTimeError(INVALID_ARGUMENT_ERR, name, "system-property()");
        return(EMPTYSTRING);
    }

    /**
     * XSLT Standard function namespace-uri().
     */
    public static String namespace_uriF(int node, DOM dom) {
        final String value = dom.getNodeName(node);
        final int colon = value.lastIndexOf(':');
        if (colon >= 0)
            return value.substring(0, colon);
        else
            return EMPTYSTRING;
    }

    /**
     * Implements the object-type() extension function.
     *
     * @see <a href="http://www.exslt.org/">EXSLT</a>
     */
    public static String objectTypeF(Object obj)
    {
      if (obj instanceof String)
        return "string";
      else if (obj instanceof Boolean)
        return "boolean";
      else if (obj instanceof Number)
        return "number";
      else if (obj instanceof DOM)
        return "RTF";
      else if (obj instanceof DTMAxisIterator)
        return "node-set";
      else
        return "unknown";
    }

    /**
     * Implements the nodeset() extension function.
     */
    public static DTMAxisIterator nodesetF(Object obj) {
        if (obj instanceof DOM) {
           //final DOMAdapter adapter = (DOMAdapter) obj;
           final DOM dom = (DOM)obj;
           return new SingletonIterator(dom.getDocument(), true);
        }
        else if (obj instanceof DTMAxisIterator) {
           return (DTMAxisIterator) obj;
        }
        else {
            final String className = obj.getClass().getName();
            runTimeError(DATA_CONVERSION_ERR, "node-set", className);
            return null;
        }
    }

    //-- Begin utility functions

    private static boolean isWhiteSpace(char ch) {
        return ch == ' ' || ch == '\t' || ch == '\n' || ch == '\r';
    }

    private static boolean compareStrings(String lstring, String rstring,
                                          int op, DOM dom) {
        switch (op) {
    case Operators.EQ:
            return lstring.equals(rstring);

    case Operators.NE:
            return !lstring.equals(rstring);

    case Operators.GT:
            return numberF(lstring, dom) > numberF(rstring, dom);

    case Operators.LT:
            return numberF(lstring, dom) < numberF(rstring, dom);

    case Operators.GE:
            return numberF(lstring, dom) >= numberF(rstring, dom);

    case Operators.LE:
            return numberF(lstring, dom) <= numberF(rstring, dom);

        default:
            runTimeError(RUN_TIME_INTERNAL_ERR, "compare()");
            return false;
        }
    }

    /**
     * Utility function: node-set/node-set compare.
     */
    public static boolean compare(DTMAxisIterator left, DTMAxisIterator right,
                                  int op, DOM dom) {
        int lnode;
        left.reset();

        while ((lnode = left.next()) != DTMAxisIterator.END) {
            final String lvalue = dom.getStringValueX(lnode);

            int rnode;
            right.reset();
            while ((rnode = right.next()) != DTMAxisIterator.END) {
                // String value must be the same if both nodes are the same
                if (lnode == rnode) {
                    if (op == Operators.EQ) {
                        return true;
                    } else if (op == Operators.NE) {
                        continue;
                    }
                }
                if (compareStrings(lvalue, dom.getStringValueX(rnode), op,
                                   dom)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean compare(int node, DTMAxisIterator iterator,
                                  int op, DOM dom) {
        //iterator.reset();

        int rnode;
        String value;

        switch(op) {
    case Operators.EQ:
            rnode = iterator.next();
            if (rnode != DTMAxisIterator.END) {
                value = dom.getStringValueX(node);
                do {
                    if (node == rnode
                          || value.equals(dom.getStringValueX(rnode))) {
                       return true;
                    }
                } while ((rnode = iterator.next()) != DTMAxisIterator.END);
            }
            break;
    case Operators.NE:
            rnode = iterator.next();
            if (rnode != DTMAxisIterator.END) {
                value = dom.getStringValueX(node);
                do {
                    if (node != rnode
                          && !value.equals(dom.getStringValueX(rnode))) {
                        return true;
                    }
                } while ((rnode = iterator.next()) != DTMAxisIterator.END);
            }
            break;
    case Operators.LT:
            // Assume we're comparing document order here
            while ((rnode = iterator.next()) != DTMAxisIterator.END) {
                if (rnode > node) return true;
            }
            break;
    case Operators.GT:
            // Assume we're comparing document order here
            while ((rnode = iterator.next()) != DTMAxisIterator.END) {
                if (rnode < node) return true;
            }
            break;
        }
        return(false);
    }

    /**
     * Utility function: node-set/number compare.
     */
    public static boolean compare(DTMAxisIterator left, final double rnumber,
                                  final int op, DOM dom) {
        int node;
        //left.reset();

        switch (op) {
    case Operators.EQ:
            while ((node = left.next()) != DTMAxisIterator.END) {
                if (numberF(dom.getStringValueX(node), dom) == rnumber)
                    return true;
            }
            break;

    case Operators.NE:
            while ((node = left.next()) != DTMAxisIterator.END) {
                if (numberF(dom.getStringValueX(node), dom) != rnumber)
                    return true;
            }
            break;

    case Operators.GT:
            while ((node = left.next()) != DTMAxisIterator.END) {
                if (numberF(dom.getStringValueX(node), dom) > rnumber)
                    return true;
            }
            break;

    case Operators.LT:
            while ((node = left.next()) != DTMAxisIterator.END) {
                if (numberF(dom.getStringValueX(node), dom) < rnumber)
                    return true;
            }
            break;

    case Operators.GE:
            while ((node = left.next()) != DTMAxisIterator.END) {
                if (numberF(dom.getStringValueX(node), dom) >= rnumber)
                    return true;
            }
            break;

    case Operators.LE:
            while ((node = left.next()) != DTMAxisIterator.END) {
                if (numberF(dom.getStringValueX(node), dom) <= rnumber)
                    return true;
            }
            break;

        default:
            runTimeError(RUN_TIME_INTERNAL_ERR, "compare()");
        }

        return false;
    }

    /**
     * Utility function: node-set/string comparison.
     */
    public static boolean compare(DTMAxisIterator left, final String rstring,
                                  int op, DOM dom) {
        int node;
        //left.reset();
        while ((node = left.next()) != DTMAxisIterator.END) {
            if (compareStrings(dom.getStringValueX(node), rstring, op, dom)) {
                return true;
            }
        }
        return false;
    }


    public static boolean compare(Object left, Object right,
                                  int op, DOM dom)
    {
        boolean result = false;
        boolean hasSimpleArgs = hasSimpleType(left) && hasSimpleType(right);

    if (op != Operators.EQ && op != Operators.NE) {
            // If node-boolean comparison -> convert node to boolean
            if (left instanceof Node || right instanceof Node) {
                if (left instanceof Boolean) {
                    right = booleanF(right);
                    hasSimpleArgs = true;
                }
                if (right instanceof Boolean) {
                    left = booleanF(left);
                    hasSimpleArgs = true;
                }
            }

            if (hasSimpleArgs) {
                switch (op) {
        case Operators.GT:
                    return numberF(left, dom) > numberF(right, dom);

        case Operators.LT:
                    return numberF(left, dom) < numberF(right, dom);

        case Operators.GE:
                    return numberF(left, dom) >= numberF(right, dom);

        case Operators.LE:
                    return numberF(left, dom) <= numberF(right, dom);

        default:
                    runTimeError(RUN_TIME_INTERNAL_ERR, "compare()");
                }
            }
            // falls through
        }

        if (hasSimpleArgs) {
            if (left instanceof Boolean || right instanceof Boolean) {
                result = booleanF(left) == booleanF(right);
            }
            else if (left instanceof Double || right instanceof Double ||
                     left instanceof Integer || right instanceof Integer) {
                result = numberF(left, dom) == numberF(right, dom);
            }
            else { // compare them as strings
                result = stringF(left, dom).equals(stringF(right, dom));
            }

            if (op == Operators.NE) {
                result = !result;
            }
        }
        else {
            if (left instanceof Node) {
                left = new SingletonIterator(((Node)left).node);
            }
            if (right instanceof Node) {
                right = new SingletonIterator(((Node)right).node);
            }

            if (hasSimpleType(left) ||
                left instanceof DOM && right instanceof DTMAxisIterator) {
                // swap operands and operator
                final Object temp = right; right = left; left = temp;
                op = Operators.swapOp(op);
            }

            if (left instanceof DOM) {
                if (right instanceof Boolean) {
                    result = ((Boolean)right).booleanValue();
                    return result == (op == Operators.EQ);
                }

                final String sleft = ((DOM)left).getStringValue();

                if (right instanceof Number) {
                    result = ((Number)right).doubleValue() ==
                        stringToReal(sleft);
                }
                else if (right instanceof String) {
                    result = sleft.equals((String)right);
                }
                else if (right instanceof DOM) {
                    result = sleft.equals(((DOM)right).getStringValue());
                }

                if (op == Operators.NE) {
                    result = !result;
                }
                return result;
            }

            // Next, node-set/t for t in {real, string, node-set, result-tree}

            DTMAxisIterator iter = ((DTMAxisIterator)left).reset();

            if (right instanceof DTMAxisIterator) {
                result = compare(iter, (DTMAxisIterator)right, op, dom);
            }
            else if (right instanceof String) {
                result = compare(iter, (String)right, op, dom);
            }
            else if (right instanceof Number) {
                final double temp = ((Number)right).doubleValue();
                result = compare(iter, temp, op, dom);
            }
            else if (right instanceof Boolean) {
                boolean temp = ((Boolean)right).booleanValue();
                result = (iter.reset().next() != DTMAxisIterator.END) == temp;
            }
            else if (right instanceof DOM) {
                result = compare(iter, ((DOM)right).getStringValue(),
                                 op, dom);
            }
            else if (right == null) {
                return(false);
            }
            else {
                final String className = right.getClass().getName();
                runTimeError(INVALID_ARGUMENT_ERR, className, "compare()");
            }
        }
        return result;
    }

    /**
     * Utility function: used to test context node's language
     */
    public static boolean testLanguage(String testLang, DOM dom, int node) {
        // language for context node (if any)
        String nodeLang = dom.getLanguage(node);
        if (nodeLang == null)
            return(false);
        else
            nodeLang = nodeLang.toLowerCase();

        // compare context node's language agains test language
        testLang = testLang.toLowerCase();
        if (testLang.length() == 2) {
            return(nodeLang.startsWith(testLang));
        }
        else {
            return(nodeLang.equals(testLang));
        }
    }

    private static boolean hasSimpleType(Object obj) {
        return obj instanceof Boolean || obj instanceof Double ||
            obj instanceof Integer || obj instanceof String ||
            obj instanceof Node || obj instanceof DOM;
    }

    /**
     * Utility function: used in StringType to convert a string to a real.
     */
    public static double stringToReal(String s) {
        try {
            return Double.valueOf(s).doubleValue();
        }
        catch (NumberFormatException e) {
            return Double.NaN;
        }
    }

    /**
     * Utility function: used in StringType to convert a string to an int.
     */
    public static int stringToInt(String s) {
        try {
            return Integer.parseInt(s);
        }
        catch (NumberFormatException e) {
            return(-1); // ???
        }
    }

    private static final int DOUBLE_FRACTION_DIGITS = 340;
    private static final double lowerBounds = 0.001;
    private static final double upperBounds = 10000000;
    private static DecimalFormat defaultFormatter, xpathFormatter;
    private static String defaultPattern = "";

    static {
        NumberFormat f = NumberFormat.getInstance(Locale.getDefault());
        defaultFormatter = (f instanceof DecimalFormat) ?
            (DecimalFormat) f : new DecimalFormat();
        // Set max fraction digits so that truncation does not occur. Setting
        // the max to Integer.MAX_VALUE may cause problems with some JDK's.
        defaultFormatter.setMaximumFractionDigits(DOUBLE_FRACTION_DIGITS);
        defaultFormatter.setMinimumFractionDigits(0);
        defaultFormatter.setMinimumIntegerDigits(1);
        defaultFormatter.setGroupingUsed(false);

        // This formatter is used to convert numbers according to the XPath
        // 1.0 syntax which ignores locales (http://www.w3.org/TR/xpath#NT-Number)
        xpathFormatter = new DecimalFormat("",
            new DecimalFormatSymbols(Locale.US));
        xpathFormatter.setMaximumFractionDigits(DOUBLE_FRACTION_DIGITS);
        xpathFormatter.setMinimumFractionDigits(0);
        xpathFormatter.setMinimumIntegerDigits(1);
        xpathFormatter.setGroupingUsed(false);
    }

    /**
     * Utility function: used in RealType to convert a real to a string.
     * Removes the decimal if null. Uses a specialized formatter object
     * for very large and very small numbers that ignores locales, thus
     * using always using "." as a decimal separator.
     */
    public static String realToString(double d) {
        final double m = Math.abs(d);
        if ((m >= lowerBounds) && (m < upperBounds)) {
            final String result = Double.toString(d);
            final int length = result.length();
            // Remove leading zeros.
            if ((result.charAt(length-2) == '.') &&
                (result.charAt(length-1) == '0'))
                return result.substring(0, length-2);
            else
                return result;
        }
        else {
            if (Double.isNaN(d) || Double.isInfinite(d))
                return(Double.toString(d));

            //Convert -0.0 to +0.0 other values remains the same
            d = d + 0.0;

            // Use the XPath formatter to ignore locales
            StringBuffer result = threadLocalStringBuffer.get();
            result.setLength(0);
            xpathFormatter.format(d, result, _fieldPosition);
            return result.toString();
        }
    }

    /**
     * Utility function: used in RealType to convert a real to an integer
     */
    public static int realToInt(double d) {
        return (int)d;
    }

    /**
     * Utility function: used to format/adjust  a double to a string. The
     * DecimalFormat object comes from the 'formatSymbols' map in
     * AbstractTranslet.
     */
    private static FieldPosition _fieldPosition = new FieldPosition(0);

    public static String formatNumber(double number, String pattern,
                                      DecimalFormat formatter) {
        // bugzilla fix 12813
        if (formatter == null) {
            formatter = defaultFormatter;
        }
        try {
            StringBuffer result = threadLocalStringBuffer.get();
        result.setLength(0);
            if (pattern != defaultPattern) {
                formatter.applyLocalizedPattern(pattern);
            }
        formatter.format(number, result, _fieldPosition);
            return result.toString();
        }
        catch (IllegalArgumentException e) {
            runTimeError(FORMAT_NUMBER_ERR, Double.toString(number), pattern);
            return(EMPTYSTRING);
        }
    }

    /**
     * Utility function: used to convert references to node-sets. If the
     * obj is an instanceof Node then create a singleton iterator.
     */
    public static DTMAxisIterator referenceToNodeSet(Object obj) {
        // Convert var/param -> node
        if (obj instanceof Node) {
            return(new SingletonIterator(((Node)obj).node));
        }
        // Convert var/param -> node-set
        else if (obj instanceof DTMAxisIterator) {
            return(((DTMAxisIterator)obj).cloneIterator().reset());
        }
        else {
            final String className = obj.getClass().getName();
            runTimeError(DATA_CONVERSION_ERR, className, "node-set");
            return null;
        }
    }

    /**
     * Utility function: used to convert reference to org.w3c.dom.NodeList.
     */
    public static NodeList referenceToNodeList(Object obj, DOM dom) {
        if (obj instanceof Node || obj instanceof DTMAxisIterator) {
            DTMAxisIterator iter = referenceToNodeSet(obj);
            return dom.makeNodeList(iter);
        }
        else if (obj instanceof DOM) {
          dom = (DOM)obj;
          return dom.makeNodeList(DTMDefaultBase.ROOTNODE);
        }
        else {
            final String className = obj.getClass().getName();
            runTimeError(DATA_CONVERSION_ERR, className,
                "org.w3c.dom.NodeList");
            return null;
        }
    }

    /**
     * Utility function: used to convert reference to org.w3c.dom.Node.
     */
    public static org.w3c.dom.Node referenceToNode(Object obj, DOM dom) {
        if (obj instanceof Node || obj instanceof DTMAxisIterator) {
            DTMAxisIterator iter = referenceToNodeSet(obj);
            return dom.makeNode(iter);
        }
        else if (obj instanceof DOM) {
          dom = (DOM)obj;
          DTMAxisIterator iter = dom.getChildren(DTMDefaultBase.ROOTNODE);
          return dom.makeNode(iter);
        }
        else {
            final String className = obj.getClass().getName();
            runTimeError(DATA_CONVERSION_ERR, className, "org.w3c.dom.Node");
            return null;
        }
    }

    /**
     * Utility function: used to convert reference to long.
     */
    public static long referenceToLong(Object obj) {
        if (obj instanceof Number) {
            return ((Number) obj).longValue();    // handles Integer and Double
        }
        else {
            final String className = obj.getClass().getName();
            runTimeError(DATA_CONVERSION_ERR, className, Long.TYPE);
            return 0;
        }
    }

    /**
     * Utility function: used to convert reference to double.
     */
    public static double referenceToDouble(Object obj) {
        if (obj instanceof Number) {
            return ((Number) obj).doubleValue();   // handles Integer and Double
        }
        else {
            final String className = obj.getClass().getName();
            runTimeError(DATA_CONVERSION_ERR, className, Double.TYPE);
            return 0;
        }
    }

    /**
     * Utility function: used to convert reference to boolean.
     */
    public static boolean referenceToBoolean(Object obj) {
        if (obj instanceof Boolean) {
            return ((Boolean) obj).booleanValue();
        }
        else {
            final String className = obj.getClass().getName();
            runTimeError(DATA_CONVERSION_ERR, className, Boolean.TYPE);
            return false;
        }
    }

    /**
     * Utility function: used to convert reference to String.
     */
    public static String referenceToString(Object obj, DOM dom) {
        if (obj instanceof String) {
            return (String) obj;
        }
        else if (obj instanceof DTMAxisIterator) {
            return dom.getStringValueX(((DTMAxisIterator)obj).reset().next());
        }
        else if (obj instanceof Node) {
            return dom.getStringValueX(((Node)obj).node);
        }
        else if (obj instanceof DOM) {
            return ((DOM) obj).getStringValue();
        }
        else {
            final String className = obj.getClass().getName();
            runTimeError(DATA_CONVERSION_ERR, className, String.class);
            return null;
        }
    }

    /**
     * Utility function used to convert a w3c Node into an internal DOM iterator.
     */
    public static DTMAxisIterator node2Iterator(org.w3c.dom.Node node,
        Translet translet, DOM dom)
    {
        final org.w3c.dom.Node inNode = node;
        // Create a dummy NodeList which only contains the given node to make
        // use of the nodeList2Iterator() interface.
        org.w3c.dom.NodeList nodelist = new org.w3c.dom.NodeList() {
            public int getLength() {
                return 1;
            }

            public org.w3c.dom.Node item(int index) {
                if (index == 0)
                    return inNode;
                else
                    return null;
            }
        };

        return nodeList2Iterator(nodelist, translet, dom);
    }

    /**
     * In a perfect world, this would be the implementation for
     * nodeList2Iterator. In reality, though, this causes a
     * ClassCastException in getDTMHandleFromNode because SAXImpl is
     * not an instance of DOM2DTM. So we use the more lengthy
     * implementation below until this issue has been addressed.
     *
     * @see org.apache.xml.dtm.ref.DTMManagerDefault#getDTMHandleFromNode
     */
    private static DTMAxisIterator nodeList2IteratorUsingHandleFromNode(
                                        org.w3c.dom.NodeList nodeList,
                                        Translet translet, DOM dom)
    {
        final int n = nodeList.getLength();
        final int[] dtmHandles = new int[n];
        DTMManager dtmManager = null;
        if (dom instanceof MultiDOM)
            dtmManager = ((MultiDOM) dom).getDTMManager();
        for (int i = 0; i < n; ++i) {
            org.w3c.dom.Node node = nodeList.item(i);
            int handle;
            if (dtmManager != null) {
                handle = dtmManager.getDTMHandleFromNode(node);
            }
            else if (node instanceof DTMNodeProxy
                     && ((DTMNodeProxy) node).getDTM() == dom) {
                handle = ((DTMNodeProxy) node).getDTMNodeNumber();
            }
            else {
                runTimeError(RUN_TIME_INTERNAL_ERR, "need MultiDOM");
                return null;
            }
            dtmHandles[i] = handle;
            System.out.println("Node " + i + " has handle 0x" +
                               Integer.toString(handle, 16));
        }
        return new ArrayNodeListIterator(dtmHandles);
    }

    /**
     * Utility function used to convert a w3c NodeList into a internal
     * DOM iterator.
     */
    public static DTMAxisIterator nodeList2Iterator(
                                        org.w3c.dom.NodeList nodeList,
                                        Translet translet, DOM dom)
    {
        // First pass: build w3c DOM for all nodes not proxied from our DOM.
        //
        // Notice: this looses some (esp. parent) context for these nodes,
        // so some way to wrap the original nodes inside a DTMAxisIterator
        // might be preferable in the long run.
        int n = 0; // allow for change in list length, just in case.
        Document doc = null;
        DTMManager dtmManager = null;
        int[] proxyNodes = new int[nodeList.getLength()];
        if (dom instanceof MultiDOM)
            dtmManager = ((MultiDOM) dom).getDTMManager();
        for (int i = 0; i < nodeList.getLength(); ++i) {
            org.w3c.dom.Node node = nodeList.item(i);
            if (node instanceof DTMNodeProxy) {
                DTMNodeProxy proxy = (DTMNodeProxy)node;
                DTM nodeDTM = proxy.getDTM();
                int handle = proxy.getDTMNodeNumber();
                boolean isOurDOM = (nodeDTM == dom);
                if (!isOurDOM && dtmManager != null) {
                    try {
                        isOurDOM = (nodeDTM == dtmManager.getDTM(handle));
                    }
                    catch (ArrayIndexOutOfBoundsException e) {
                        // invalid node handle, so definitely not our doc
                    }
                }
                if (isOurDOM) {
                    proxyNodes[i] = handle;
                    ++n;
                    continue;
                }
            }
            proxyNodes[i] = DTM.NULL;
            int nodeType = node.getNodeType();
            if (doc == null) {
                if (dom instanceof MultiDOM == false) {
                    runTimeError(RUN_TIME_INTERNAL_ERR, "need MultiDOM");
                    return null;
                }
                try {
                    AbstractTranslet at = (AbstractTranslet) translet;
                    doc = at.newDocument("", "__top__");
                }
                catch (javax.xml.parsers.ParserConfigurationException e) {
                    runTimeError(RUN_TIME_INTERNAL_ERR, e.getMessage());
                    return null;
                }
            }
            // Use one dummy element as container for each node of the
            // list. That way, it is easier to detect resp. avoid
            // funny things which change the number of nodes,
            // e.g. auto-concatenation of text nodes.
            Element mid;
            switch (nodeType) {
                case org.w3c.dom.Node.ELEMENT_NODE:
                case org.w3c.dom.Node.TEXT_NODE:
                case org.w3c.dom.Node.CDATA_SECTION_NODE:
                case org.w3c.dom.Node.COMMENT_NODE:
                case org.w3c.dom.Node.ENTITY_REFERENCE_NODE:
                case org.w3c.dom.Node.PROCESSING_INSTRUCTION_NODE:
                    mid = doc.createElementNS(null, "__dummy__");
                    mid.appendChild(doc.importNode(node, true));
                    doc.getDocumentElement().appendChild(mid);
                    ++n;
                    break;
                case org.w3c.dom.Node.ATTRIBUTE_NODE:
                    // The mid element also serves as a container for
                    // attributes, avoiding problems with conflicting
                    // attributes or node order.
                    mid = doc.createElementNS(null, "__dummy__");
                    mid.setAttributeNodeNS((Attr)doc.importNode(node, true));
                    doc.getDocumentElement().appendChild(mid);
                    ++n;
                    break;
                default:
                    // Better play it safe for all types we aren't sure we know
                    // how to deal with.
                    runTimeError(RUN_TIME_INTERNAL_ERR,
                                 "Don't know how to convert node type "
                                 + nodeType);
            }
        }

        // w3cDOM -> DTM -> DOMImpl
        DTMAxisIterator iter = null, childIter = null, attrIter = null;
        if (doc != null) {
            final MultiDOM multiDOM = (MultiDOM) dom;
            DOM idom = (DOM)dtmManager.getDTM(new DOMSource(doc), false,
                                              null, true, false);
            // Create DOMAdapter and register with MultiDOM
            DOMAdapter domAdapter = new DOMAdapter(idom,
                translet.getNamesArray(),
                translet.getUrisArray(),
                translet.getTypesArray(),
                translet.getNamespaceArray());
            multiDOM.addDOMAdapter(domAdapter);

            DTMAxisIterator iter1 = idom.getAxisIterator(Axis.CHILD);
            DTMAxisIterator iter2 = idom.getAxisIterator(Axis.CHILD);
            iter = new AbsoluteIterator(
                new StepIterator(iter1, iter2));

            iter.setStartNode(DTMDefaultBase.ROOTNODE);

            childIter = idom.getAxisIterator(Axis.CHILD);
            attrIter = idom.getAxisIterator(Axis.ATTRIBUTE);
        }

        // Second pass: find DTM handles for every node in the list.
        int[] dtmHandles = new int[n];
        n = 0;
        for (int i = 0; i < nodeList.getLength(); ++i) {
            if (proxyNodes[i] != DTM.NULL) {
                dtmHandles[n++] = proxyNodes[i];
                continue;
            }
            org.w3c.dom.Node node = nodeList.item(i);
            DTMAxisIterator iter3 = null;
            int nodeType = node.getNodeType();
            switch (nodeType) {
                case org.w3c.dom.Node.ELEMENT_NODE:
                case org.w3c.dom.Node.TEXT_NODE:
                case org.w3c.dom.Node.CDATA_SECTION_NODE:
                case org.w3c.dom.Node.COMMENT_NODE:
                case org.w3c.dom.Node.ENTITY_REFERENCE_NODE:
                case org.w3c.dom.Node.PROCESSING_INSTRUCTION_NODE:
                    iter3 = childIter;
                    break;
                case org.w3c.dom.Node.ATTRIBUTE_NODE:
                    iter3 = attrIter;
                    break;
                default:
                    // Should not happen, as first run should have got all these
                    throw new InternalRuntimeError("Mismatched cases");
            }
            if (iter3 != null) {
                iter3.setStartNode(iter.next());
                dtmHandles[n] = iter3.next();
                // For now, play it self and perform extra checks:
                if (dtmHandles[n] == DTMAxisIterator.END)
                    throw new InternalRuntimeError("Expected element missing at " + i);
                if (iter3.next() != DTMAxisIterator.END)
                    throw new InternalRuntimeError("Too many elements at " + i);
                ++n;
            }
        }
        if (n != dtmHandles.length)
            throw new InternalRuntimeError("Nodes lost in second pass");

        return new ArrayNodeListIterator(dtmHandles);
    }

    /**
     * Utility function used to convert references to DOMs.
     */
    public static DOM referenceToResultTree(Object obj) {
        try {
            return ((DOM) obj);
        }
        catch (IllegalArgumentException e) {
            final String className = obj.getClass().getName();
            runTimeError(DATA_CONVERSION_ERR, "reference", className);
            return null;
        }
    }

    /**
     * Utility function: used with nth position filters to convert a sequence
     * of nodes to just one single node (the one at position n).
     */
    public static DTMAxisIterator getSingleNode(DTMAxisIterator iterator) {
        int node = iterator.next();
        return(new SingletonIterator(node));
    }

    /**
     * Utility function: used in xsl:copy.
     */
    private static char[] _characterArray = new char[32];

    public static void copy(Object obj,
                            SerializationHandler handler,
                            int node,
                            DOM dom) {
        try {
            if (obj instanceof DTMAxisIterator)
      {
                DTMAxisIterator iter = (DTMAxisIterator) obj;
                dom.copy(iter.reset(), handler);
            }
            else if (obj instanceof Node) {
                dom.copy(((Node) obj).node, handler);
            }
            else if (obj instanceof DOM) {
                //((DOM)obj).copy(((com.sun.org.apache.xml.internal.dtm.ref.DTMDefaultBase)((DOMAdapter)obj).getDOMImpl()).getDocument(), handler);
                DOM newDom = (DOM)obj;
                newDom.copy(newDom.getDocument(), handler);
            }
            else {
                String string = obj.toString();         // or call stringF()
                final int length = string.length();
                if (length > _characterArray.length)
                    _characterArray = new char[length];
                string.getChars(0, length, _characterArray, 0);
                handler.characters(_characterArray, 0, length);
            }
        }
        catch (SAXException e) {
            runTimeError(RUN_TIME_COPY_ERR);
        }
    }

    /**
     * Utility function to check if xsl:attribute has a valid qname
     * This method should only be invoked if the name attribute is an AVT
     */
    public static void checkAttribQName(String name) {
        final int firstOccur = name.indexOf(':');
        final int lastOccur = name.lastIndexOf(':');
        final String localName = name.substring(lastOccur + 1);

        if (firstOccur > 0) {
            final String newPrefix = name.substring(0, firstOccur);

            if (firstOccur != lastOccur) {
               final String oriPrefix = name.substring(firstOccur+1, lastOccur);
                if (!XML11Char.isXML11ValidNCName(oriPrefix)) {
                    // even though the orignal prefix is ignored, it should still get checked for valid NCName
                    runTimeError(INVALID_QNAME_ERR,oriPrefix+":"+localName);
                }
            }

            // prefix must be a valid NCName
            if (!XML11Char.isXML11ValidNCName(newPrefix)) {
                runTimeError(INVALID_QNAME_ERR,newPrefix+":"+localName);
            }
        }

        // local name must be a valid NCName and must not be XMLNS
        if ((!XML11Char.isXML11ValidNCName(localName))||(localName.equals(Constants.XMLNS_PREFIX))) {
            runTimeError(INVALID_QNAME_ERR,localName);
        }
    }

    /**
     * Utility function to check if a name is a valid ncname
     * This method should only be invoked if the attribute value is an AVT
     */
    public static void checkNCName(String name) {
        if (!XML11Char.isXML11ValidNCName(name)) {
            runTimeError(INVALID_NCNAME_ERR,name);
        }
    }

    /**
     * Utility function to check if a name is a valid qname
     * This method should only be invoked if the attribute value is an AVT
     */
    public static void checkQName(String name) {
        if (!XML11Char.isXML11ValidQName(name)) {
            runTimeError(INVALID_QNAME_ERR,name);
        }
    }

    /**
     * Utility function for the implementation of xsl:element.
     */
    public static String startXslElement(String qname, String namespace,
        SerializationHandler handler, DOM dom, int node)
    {
        try {
            // Get prefix from qname
            String prefix;
            final int index = qname.indexOf(':');

            if (index > 0) {
                prefix = qname.substring(0, index);

                // Handle case when prefix is not known at compile time
                if (namespace == null || namespace.length() == 0) {
                    try {
                        // not sure if this line of code ever works
                        namespace = dom.lookupNamespace(node, prefix);
                    }
                    catch(RuntimeException e) {
                        handler.flushPending();  // need to flush or else can't get namespacemappings
                        NamespaceMappings nm = handler.getNamespaceMappings();
                        namespace = nm.lookupNamespace(prefix);
                        if (namespace == null) {
                            runTimeError(NAMESPACE_PREFIX_ERR,prefix);
                        }
                    }
                }

                handler.startElement(namespace, qname.substring(index+1),
                                         qname);
                handler.namespaceAfterStartElement(prefix, namespace);
            }
            else {
                // Need to generate a prefix?
                if (namespace != null && namespace.length() > 0) {
                    prefix = generatePrefix();
                    qname = prefix + ':' + qname;
                    handler.startElement(namespace, qname, qname);
                    handler.namespaceAfterStartElement(prefix, namespace);
                }
                else {
                    handler.startElement(null, null, qname);
                }
            }
        }
        catch (SAXException e) {
            throw new RuntimeException(e.getMessage());
        }

        return qname;
    }

    /**
     * This function is used in the execution of xsl:element
     */
    public static String getPrefix(String qname) {
        final int index = qname.indexOf(':');
        return (index > 0) ? qname.substring(0, index) : null;
    }

    /**
     * These functions are used in the execution of xsl:element to generate
     * and reset namespace prefix index local to current transformation process
     */
    public static String generatePrefix() {
        return ("ns" + threadLocalPrefixIndex.get().getAndIncrement());
    }

    public static void resetPrefixIndex() {
        threadLocalPrefixIndex.get().set(0);
    }

    private static final ThreadLocal<AtomicInteger> threadLocalPrefixIndex =
        new ThreadLocal<AtomicInteger>() {
            @Override
            protected AtomicInteger initialValue() {
                return new AtomicInteger();
            }
        };

    public static final String RUN_TIME_INTERNAL_ERR =
                                           "RUN_TIME_INTERNAL_ERR";
    public static final String RUN_TIME_COPY_ERR =
                                           "RUN_TIME_COPY_ERR";
    public static final String DATA_CONVERSION_ERR =
                                           "DATA_CONVERSION_ERR";
    public static final String EXTERNAL_FUNC_ERR =
                                           "EXTERNAL_FUNC_ERR";
    public static final String EQUALITY_EXPR_ERR =
                                           "EQUALITY_EXPR_ERR";
    public static final String INVALID_ARGUMENT_ERR =
                                           "INVALID_ARGUMENT_ERR";
    public static final String FORMAT_NUMBER_ERR =
                                           "FORMAT_NUMBER_ERR";
    public static final String ITERATOR_CLONE_ERR =
                                           "ITERATOR_CLONE_ERR";
    public static final String AXIS_SUPPORT_ERR =
                                           "AXIS_SUPPORT_ERR";
    public static final String TYPED_AXIS_SUPPORT_ERR =
                                           "TYPED_AXIS_SUPPORT_ERR";
    public static final String STRAY_ATTRIBUTE_ERR =
                                           "STRAY_ATTRIBUTE_ERR";
    public static final String STRAY_NAMESPACE_ERR =
                                           "STRAY_NAMESPACE_ERR";
    public static final String NAMESPACE_PREFIX_ERR =
                                           "NAMESPACE_PREFIX_ERR";
    public static final String DOM_ADAPTER_INIT_ERR =
                                           "DOM_ADAPTER_INIT_ERR";
    public static final String PARSER_DTD_SUPPORT_ERR =
                                           "PARSER_DTD_SUPPORT_ERR";
    public static final String NAMESPACES_SUPPORT_ERR =
                                           "NAMESPACES_SUPPORT_ERR";
    public static final String CANT_RESOLVE_RELATIVE_URI_ERR =
                                           "CANT_RESOLVE_RELATIVE_URI_ERR";
    public static final String UNSUPPORTED_XSL_ERR =
                                           "UNSUPPORTED_XSL_ERR";
    public static final String UNSUPPORTED_EXT_ERR =
                                           "UNSUPPORTED_EXT_ERR";
    public static final String UNKNOWN_TRANSLET_VERSION_ERR =
                                           "UNKNOWN_TRANSLET_VERSION_ERR";
    public static final String INVALID_QNAME_ERR = "INVALID_QNAME_ERR";
    public static final String INVALID_NCNAME_ERR = "INVALID_NCNAME_ERR";
    public static final String UNALLOWED_EXTENSION_FUNCTION_ERR = "UNALLOWED_EXTENSION_FUNCTION_ERR";
    public static final String UNALLOWED_EXTENSION_ELEMENT_ERR = "UNALLOWED_EXTENSION_ELEMENT_ERR";

    // All error messages are localized and are stored in resource bundles.
    private static ResourceBundle m_bundle;

    public final static String ERROR_MESSAGES_KEY = "error-messages";

    static {
        String resource = "com.sun.org.apache.xalan.internal.xsltc.runtime.ErrorMessages";
        m_bundle = SecuritySupport.getResourceBundle(resource);
    }

    /**
     * Print a run-time error message.
     */
    public static void runTimeError(String code) {
        throw new RuntimeException(m_bundle.getString(code));
    }

    public static void runTimeError(String code, Object[] args) {
        final String message = MessageFormat.format(m_bundle.getString(code),
                                                    args);
        throw new RuntimeException(message);
    }

    public static void runTimeError(String code, Object arg0) {
        runTimeError(code, new Object[]{ arg0 } );
    }

    public static void runTimeError(String code, Object arg0, Object arg1) {
        runTimeError(code, new Object[]{ arg0, arg1 } );
    }

    public static void consoleOutput(String msg) {
        System.out.println(msg);
    }

    /**
     * Replace a certain character in a string with a new substring.
     */
    public static String replace(String base, char ch, String str) {
        return (base.indexOf(ch) < 0) ? base :
            replace(base, String.valueOf(ch), new String[] { str });
    }

    public static String replace(String base, String delim, String[] str) {
        final int len = base.length();
        final StringBuilder result = threadLocalStringBuilder.get();
        result.setLength(0);

        for (int i = 0; i < len; i++) {
            final char ch = base.charAt(i);
            final int k = delim.indexOf(ch);

            if (k >= 0) {
                result.append(str[k]);
            }
            else {
                result.append(ch);
            }
        }
        return result.toString();
    }


    /**
     * Utility method to allow setting parameters of the form
     * {namespaceuri}localName
     * which get mapped to an instance variable in the class
     * Hence  a parameter of the form "{http://foo.bar}xyz"
     * will be replaced with the corresponding values
     * by the BasisLibrary's utility method mapQNametoJavaName
     * and thus get mapped to legal java variable names
     */
    public static String mapQNameToJavaName (String base ) {
       return replace(base, ".-:/{}?#%*",
                      new String[] { "$dot$", "$dash$" ,"$colon$", "$slash$",
                                     "","$colon$","$ques$","$hash$","$per$",
                                     "$aster$"});

    }

    /**
     *  Utility method to calculate string-length as a number of code points,
     *  to avoid possible errors with string that contains
     *  complementary characters
     */
    public static int getStringLength(String str) {
        return str.codePointCount(0,str.length());
    }

    //-- End utility functions
}
