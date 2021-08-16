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

package com.sun.org.apache.xalan.internal.xsltc.dom;

import com.sun.org.apache.xalan.internal.xsltc.DOM;
import com.sun.org.apache.xalan.internal.xsltc.Translet;
import com.sun.org.apache.xml.internal.dtm.DTM;
import com.sun.org.apache.xml.internal.dtm.DTMAxisIterator;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Jacek Ambroziak
 * @author Santiago Pericas-Geertsen
 * @author Morten Jorgensen
 * @LastModified: Oct 2017
 */
public abstract class NodeCounter {
    public static final int END = DTM.NULL;

    protected int _node = END;
    protected int _nodeType = DOM.FIRST_TYPE - 1;
    protected double _value = Integer.MIN_VALUE;

    public final DOM          _document;
    public final DTMAxisIterator _iterator;
    public final Translet     _translet;

    protected String _format;
    protected String _lang;
    protected String _letterValue;
    protected String _groupSep;
    protected int    _groupSize;

    private boolean _separFirst = true;
    private boolean _separLast = false;
    private List<String> _separToks = new ArrayList<>();
    private List<String> _formatToks = new ArrayList<>();
    private int _nSepars  = 0;
    private int _nFormats = 0;

    private final static String[] Thousands =
        {"", "m", "mm", "mmm" };
    private final static String[] Hundreds =
    {"", "c", "cc", "ccc", "cd", "d", "dc", "dcc", "dccc", "cm"};
    private final static String[] Tens =
    {"", "x", "xx", "xxx", "xl", "l", "lx", "lxx", "lxxx", "xc"};
    private final static String[] Ones =
    {"", "i", "ii", "iii", "iv", "v", "vi", "vii", "viii", "ix"};

    private StringBuilder _tempBuffer = new StringBuilder();

    /**
     * Indicates if this instance of xsl:number has a from pattern.
     */
    protected boolean _hasFrom;

    protected NodeCounter(Translet translet,
              DOM document, DTMAxisIterator iterator) {
    _translet = translet;
    _document = document;
    _iterator = iterator;
    }

    protected NodeCounter(Translet translet,
              DOM document, DTMAxisIterator iterator, boolean hasFrom) {
        _translet = translet;
        _document = document;
        _iterator = iterator;
        _hasFrom = hasFrom;
    }

    /**
     * Set the start node for this counter. The same <tt>NodeCounter</tt>
     * object can be used multiple times by resetting the starting node.
     */
    abstract public NodeCounter setStartNode(int node);

    /**
     * If the user specified a value attribute, use this instead of
     * counting nodes.
     */
    public NodeCounter setValue(double value) {
    _value = value;
    return this;
    }

    /**
     * Sets formatting fields before calling formatNumbers().
     */
    protected void setFormatting(String format, String lang, String letterValue,
                 String groupSep, String groupSize) {
    _lang = lang;
    _groupSep = groupSep;
    _letterValue = letterValue;
    _groupSize = parseStringToAnInt(groupSize);
    setTokens(format);

 }

    /**
     * Effectively does the same thing as Integer.parseInt(String s) except
     * instead of throwing a NumberFormatException, it returns 0.  This method
     * is used instead of Integer.parseInt() since it does not incur the
     * overhead of throwing an Exception which is expensive.
     *
     * @param s  A String to be parsed into an int.
     * @return  Either an int represented by the incoming String s, or 0 if
     *          the parsing is not successful.
     */
    private int parseStringToAnInt(String s) {
        if (s == null)
            return 0;

        int result = 0;
        boolean negative = false;
        int radix = 10, i = 0, max = s.length();
        int limit, multmin, digit;

        if (max > 0) {
            if (s.charAt(0) == '-') {
                negative = true;
                limit = Integer.MIN_VALUE;
                i++;
            } else {
                limit = -Integer.MAX_VALUE;
            }
            multmin = limit / radix;
            if (i < max) {
                digit = Character.digit(s.charAt(i++), radix);
                if (digit < 0)
                    return 0;
                else
                    result = -digit;
            }
            while (i < max) {
                // Accumulating negatively avoids surprises near MAX_VALUE
                digit = Character.digit(s.charAt(i++), radix);
                if (digit < 0)
                    return 0;
                if (result < multmin)
                    return 0;
                result *= radix;
                if (result < limit + digit)
                    return 0;
                result -= digit;
            }
        } else {
            return 0;
        }
        if (negative) {
            if (i > 1)
                return result;
            else /* Only got "-" */
                return 0;
        } else {
            return -result;
        }
    }

  // format == null assumed here
 private final void setTokens(final String format){
     if( (_format!=null) &&(format.equals(_format)) ){// has already been set
        return;
     }
     _format = format;
     // reset
     final int length = _format.length();
     boolean isFirst = true;
     _separFirst = true;
     _separLast = false;
     _nSepars  = 0;
     _nFormats = 0;
     _separToks.clear() ;
     _formatToks.clear();

         /*
          * Tokenize the format string into alphanumeric and non-alphanumeric
          * tokens as described in M. Kay page 241.
          */
         for (int j = 0, i = 0; i < length;) {
                 char c = format.charAt(i);
                 for (j = i; Character.isLetterOrDigit(c);) {
                     if (++i == length) break;
             c = format.charAt(i);
                 }
                 if (i > j) {
                     if (isFirst) {
                         _separToks.add(".");
                         isFirst = _separFirst = false;
                     }
                     _formatToks.add(format.substring(j, i));
                 }

                 if (i == length) break;

                 c = format.charAt(i);
                 for (j = i; !Character.isLetterOrDigit(c);) {
                     if (++i == length) break;
                     c = format.charAt(i);
                     isFirst = false;
                 }
                 if (i > j) {
                     _separToks.add(format.substring(j, i));
                 }
             }

         _nSepars = _separToks.size();
         _nFormats = _formatToks.size();
         if (_nSepars > _nFormats) _separLast = true;

         if (_separFirst) _nSepars--;
         if (_separLast) _nSepars--;
         if (_nSepars == 0) {
             _separToks.add(1, ".");
             _nSepars++;
         }
         if (_separFirst) _nSepars ++;

 }
    /**
     * Sets formatting fields to their default values.
     */
    public NodeCounter setDefaultFormatting() {
    setFormatting("1", "en", "alphabetic", null, null);
    return this;
    }

    /**
     * Returns the position of <tt>node</tt> according to the level and
     * the from and count patterns.
     */
    abstract public String getCounter();

    /**
     * Returns the position of <tt>node</tt> according to the level and
     * the from and count patterns. This position is converted into a
     * string based on the arguments passed.
     */
    public String getCounter(String format, String lang, String letterValue,
                String groupSep, String groupSize) {
    setFormatting(format, lang, letterValue, groupSep, groupSize);
    return getCounter();
    }

    /**
     * Returns true if <tt>node</tt> matches the count pattern. By
     * default a node matches the count patterns if it is of the
     * same type as the starting node.
     */
    public boolean matchesCount(int node) {
    return _nodeType == _document.getExpandedTypeID(node);
    }

    /**
     * Returns true if <tt>node</tt> matches the from pattern. By default,
     * no node matches the from pattern.
     */
    public boolean matchesFrom(int node) {
    return false;
    }

    /**
     * Format a single value according to the format parameters.
     */
    protected String formatNumbers(int value) {
    return formatNumbers(new int[] { value });
    }

    /**
     * Format a sequence of values according to the format paramaters
     * set by calling setFormatting().
     */
    protected String formatNumbers(int[] values) {
    final int nValues = values.length;

    boolean isEmpty = true;
    for (int i = 0; i < nValues; i++)
        if (values[i] != Integer.MIN_VALUE)
        isEmpty = false;
    if (isEmpty) return("");

    // Format the output string using the values array and the fmt. tokens
    boolean isFirst = true;
    int t = 0, n = 0, s = 1;
  _tempBuffer.setLength(0);
    final StringBuilder buffer = _tempBuffer;

    // Append separation token before first digit/letter/numeral
    if (_separFirst) buffer.append(_separToks.get(0));

    // Append next digit/letter/numeral and separation token
    while (n < nValues) {
        final int value = values[n];
        if (value != Integer.MIN_VALUE) {
        if (!isFirst) buffer.append(_separToks.get(s++));
        formatValue(value, _formatToks.get(t++), buffer);
        if (t == _nFormats) t--;
        if (s >= _nSepars) s--;
        isFirst = false;
        }
        n++;
    }

    // Append separation token after last digit/letter/numeral
    if (_separLast) buffer.append(_separToks.get(_separToks.size() - 1));
    return buffer.toString();
    }

    /**
     * Format a single value based on the appropriate formatting token.
     * This method is based on saxon (Michael Kay) and only implements
     * lang="en".
     */
    private void formatValue(int value, String format, StringBuilder buffer) {
        char c = format.charAt(0);

        if (Character.isDigit(c)) {
            char zero = (char)(c - Character.getNumericValue(c));

            StringBuilder temp = buffer;
            if (_groupSize > 0) {
                temp = new StringBuilder();
            }
            String s = "";
            int n = value;
            while (n > 0) {
                s = (char) ((int) zero + (n % 10)) + s;
                n = n / 10;
            }

            for (int i = 0; i < format.length() - s.length(); i++) {
                temp.append(zero);
            }
            temp.append(s);

            if (_groupSize > 0) {
                for (int i = 0; i < temp.length(); i++) {
                    if (i != 0 && ((temp.length() - i) % _groupSize) == 0) {
                        buffer.append(_groupSep);
                    }
                    buffer.append(temp.charAt(i));
                }
            }
        }
    else if (c == 'i' && !_letterValue.equals("alphabetic")) {
            buffer.append(romanValue(value));
        }
    else if (c == 'I' && !_letterValue.equals("alphabetic")) {
            buffer.append(romanValue(value).toUpperCase());
        }
    else {
        int min = (int) c;
        int max = (int) c;

        // Special case for Greek alphabet
        if (c >= 0x3b1 && c <= 0x3c9) {
        max = 0x3c9;    // omega
        }
        else {
        // General case: search for end of group
        while (Character.isLetterOrDigit((char) (max + 1))) {
            max++;
        }
        }
            buffer.append(alphaValue(value, min, max));
        }
    }

    private String alphaValue(int value, int min, int max) {
        if (value <= 0) {
        return "" + value;
    }

        int range = max - min + 1;
        char last = (char)(((value-1) % range) + min);
        if (value > range) {
            return alphaValue((value-1) / range, min, max) + last;
        }
    else {
            return "" + last;
        }
    }

    private String romanValue(int n) {
        if (n <= 0 || n > 4000) {
        return "" + n;
    }
        return
        Thousands[n / 1000] +
        Hundreds[(n / 100) % 10] +
        Tens[(n/10) % 10] +
        Ones[n % 10];
    }

}
