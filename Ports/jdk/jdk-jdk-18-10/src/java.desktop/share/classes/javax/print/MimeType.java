/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package javax.print;

import java.io.Serial;
import java.io.Serializable;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Vector;

/**
 * Class {@code MimeType} encapsulates a Multipurpose Internet Mail Extensions
 * (MIME) media type as defined in
 * <a href="http://www.ietf.org/rfc/rfc2045.txt">RFC 2045</a> and
 * <a href="http://www.ietf.org/rfc/rfc2046.txt">RFC 2046</a>. A MIME type
 * object is part of a {@link DocFlavor DocFlavor} object and specifies the
 * format of the print data.
 * <p>
 * Class {@code MimeType} is similar to the like-named class in package
 * {@link java.awt.datatransfer java.awt.datatransfer}. Class
 * {@link java.awt.datatransfer.MimeType} is not used in the Jini Print Service
 * API for two reasons:
 * <ol type=1>
 *   <li>Since not all Java profiles include the AWT, the Jini Print Service
 *   should not depend on an AWT class.
 *   <li>The implementation of class {@code java.awt.datatransfer.MimeType} does
 *   not guarantee that equivalent MIME types will have the same serialized
 *   representation. Thus, since the Jini Lookup Service (JLUS) matches service
 *   attributes based on equality of serialized representations, JLUS searches
 *   involving MIME types encapsulated in class
 *   {@code java.awt.datatransfer.MimeType} may incorrectly fail to match.
 * </ol>
 * Class MimeType's serialized representation is based on the following
 * canonical form of a MIME type string. Thus, two MIME types that are not
 * identical but that are equivalent (that have the same canonical form) will be
 * considered equal by the JLUS's matching algorithm.
 * <ul>
 *   <li>The media type, media subtype, and parameters are retained, but all
 *   comments and whitespace characters are discarded.
 *   <li>The media type, media subtype, and parameter names are converted to
 *   lowercase.
 *   <li>The parameter values retain their original case, except a charset
 *   parameter value for a text media type is converted to lowercase.
 *   <li>Quote characters surrounding parameter values are removed.
 *   <li>Quoting backslash characters inside parameter values are removed.
 *   <li>The parameters are arranged in ascending order of parameter name.
 * </ul>
 *
 * @author Alan Kaminsky
 */
class MimeType implements Serializable, Cloneable {

    /**
     * Use serialVersionUID from JDK 1.4 for interoperability.
     */
    @Serial
    private static final long serialVersionUID = -2785720609362367683L;

    /**
     * Array of strings that hold pieces of this MIME type's canonical form. If
     * the MIME type has <i>n</i> parameters, <i>n</i> &gt;= 0, then the
     * strings in the array are:
     * <br>Index 0 -- Media type.
     * <br>Index 1 -- Media subtype.
     * <br>Index 2<i>i</i>+2 -- Name of parameter <i>i</i>,
     * <i>i</i>=0,1,...,<i>n</i>-1.
     * <br>Index 2<i>i</i>+3 -- Value of parameter <i>i</i>,
     * <i>i</i>=0,1,...,<i>n</i>-1.
     * <br>Parameters are arranged in ascending order of parameter name.
     * @serial
     */
    private String[] myPieces;

    /**
     * String value for this MIME type. Computed when needed and cached.
     */
    private transient String myStringValue = null;

    /**
     * Parameter map entry set. Computed when needed and cached.
     */
    private transient ParameterMapEntrySet myEntrySet = null;

    /**
     * Parameter map. Computed when needed and cached.
     */
    private transient ParameterMap myParameterMap = null;

    /**
     * Parameter map entry.
     */
    private class ParameterMapEntry implements Map.Entry<String, String> {

        /**
         * The index of the entry.
         */
        private int myIndex;

        /**
         * Constructs a new parameter map entry.
         *
         * @param  theIndex the index of the entry
         */
        public ParameterMapEntry(int theIndex) {
            myIndex = theIndex;
        }
        public String getKey(){
            return myPieces[myIndex];
        }
        public String getValue(){
            return myPieces[myIndex+1];
        }
        public String setValue (String value) {
            throw new UnsupportedOperationException();
        }
        public boolean equals(Object o) {
            return (o != null &&
                    o instanceof Map.Entry &&
                    getKey().equals (((Map.Entry) o).getKey()) &&
                    getValue().equals(((Map.Entry) o).getValue()));
        }
        public int hashCode() {
            return getKey().hashCode() ^ getValue().hashCode();
        }
    }

    /**
     * Parameter map entry set iterator.
     */
    private class ParameterMapEntrySetIterator implements Iterator<Map.Entry<String, String>> {

        /**
         * The current index of the iterator.
         */
        private int myIndex = 2;
        public boolean hasNext() {
            return myIndex < myPieces.length;
        }
        public Map.Entry<String, String> next() {
            if (hasNext()) {
                ParameterMapEntry result = new ParameterMapEntry (myIndex);
                myIndex += 2;
                return result;
            } else {
                throw new NoSuchElementException();
            }
        }
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Parameter map entry set.
     */
    private class ParameterMapEntrySet extends AbstractSet<Map.Entry<String, String>> {
        public Iterator<Map.Entry<String, String>> iterator() {
            return new ParameterMapEntrySetIterator();
        }
        public int size() {
            return (myPieces.length - 2) / 2;
        }
    }

    /**
     * Parameter map.
     */
    private class ParameterMap extends AbstractMap<String, String> {
        public Set<Map.Entry<String, String>> entrySet() {
            if (myEntrySet == null) {
                myEntrySet = new ParameterMapEntrySet();
            }
            return myEntrySet;
        }
    }

    /**
     * Construct a new MIME type object from the given string. The given string
     * is converted into canonical form and stored internally.
     *
     * @param  s MIME media type string
     * @throws NullPointerException if {@code s} is {@code null}
     * @throws IllegalArgumentException if {@code s} does not obey the syntax
     *         for a MIME media type string
     */
    public MimeType(String s) {
        parse (s);
    }

    /**
     * Returns this MIME type object's MIME type string based on the canonical
     * form. Each parameter value is enclosed in quotes.
     *
     * @return the mime type
     */
    public String getMimeType() {
        return getStringValue();
    }

    /**
     * Returns this MIME type object's media type.
     *
     * @return the media type
     */
    public String getMediaType() {
        return myPieces[0];
    }

    /**
     * Returns this MIME type object's media subtype.
     *
     * @return the media subtype
     */
    public String getMediaSubtype() {
        return myPieces[1];
    }

    /**
     * Returns an unmodifiable map view of the parameters in this MIME type
     * object. Each entry in the parameter map view consists of a parameter name
     * {@code String} (key) mapping to a parameter value {@code String}. If this
     * MIME type object has no parameters, an empty map is returned.
     *
     * @return parameter map for this MIME type object
     */
    public Map<String, String> getParameterMap() {
        if (myParameterMap == null) {
            myParameterMap = new ParameterMap();
        }
        return myParameterMap;
    }

    /**
     * Converts this MIME type object to a string.
     *
     * @return MIME type string based on the canonical form. Each parameter
     *         value is enclosed in quotes.
     */
    public String toString() {
        return getStringValue();
    }

    /**
     * Returns a hash code for this MIME type object.
     */
    public int hashCode() {
        return getStringValue().hashCode();
    }

    /**
     * Determine if this MIME type object is equal to the given object. The two
     * are equal if the given object is not {@code null}, is an instance of
     * class {@code javax.print.data.MimeType}, and has the same canonical form
     * as this MIME type object (that is, has the same type, subtype, and
     * parameters). Thus, if two MIME type objects are the same except for
     * comments, they are considered equal. However, "text/plain" and
     * "text/plain; charset=us-ascii" are not considered equal, even though they
     * represent the same media type (because the default character set for
     * plain text is US-ASCII).
     *
     * @param  obj {@code object} to test
     * @return {@code true} if this MIME type object equals {@code obj},
     *         {@code false} otherwise
     */
    public boolean equals (Object obj) {
        return(obj != null &&
               obj instanceof MimeType &&
               getStringValue().equals(((MimeType) obj).getStringValue()));
    }

    /**
     * Returns this MIME type's string value in canonical form.
     *
     * @return the MIME type's string value in canonical form
     */
    private String getStringValue() {
        if (myStringValue == null) {
            StringBuilder result = new StringBuilder();
            result.append (myPieces[0]);
            result.append ('/');
            result.append (myPieces[1]);
            int n = myPieces.length;
            for (int i = 2; i < n; i += 2) {
                result.append(';');
                result.append(' ');
                result.append(myPieces[i]);
                result.append('=');
                result.append(addQuotes (myPieces[i+1]));
            }
            myStringValue = result.toString();
        }
        return myStringValue;
    }

    // Hidden classes, constants, and operations for parsing a MIME media type
    // string.

    // Lexeme types.
    private static final int TOKEN_LEXEME         = 0;
    private static final int QUOTED_STRING_LEXEME = 1;
    private static final int TSPECIAL_LEXEME      = 2;
    private static final int EOF_LEXEME           = 3;
    private static final int ILLEGAL_LEXEME       = 4;

    /**
     *Class for a lexical analyzer.
     */
    private static class LexicalAnalyzer {
        protected String mySource;
        protected int mySourceLength;
        protected int myCurrentIndex;
        protected int myLexemeType;
        protected int myLexemeBeginIndex;
        protected int myLexemeEndIndex;

        public LexicalAnalyzer(String theSource) {
            mySource = theSource;
            mySourceLength = theSource.length();
            myCurrentIndex = 0;
            nextLexeme();
        }

        public int getLexemeType() {
            return myLexemeType;
        }

        public String getLexeme() {
            return(myLexemeBeginIndex >= mySourceLength ?
                   null :
                   mySource.substring(myLexemeBeginIndex, myLexemeEndIndex));
        }

        public char getLexemeFirstCharacter() {
            return(myLexemeBeginIndex >= mySourceLength ?
                   '\u0000' :
                   mySource.charAt(myLexemeBeginIndex));
        }

        public void nextLexeme() {
            int state = 0;
            int commentLevel = 0;
            char c;
            while (state >= 0) {
                switch (state) {
                    // Looking for a token, quoted string, or tspecial
                case 0:
                    if (myCurrentIndex >= mySourceLength) {
                        myLexemeType = EOF_LEXEME;
                        myLexemeBeginIndex = mySourceLength;
                        myLexemeEndIndex = mySourceLength;
                        state = -1;
                    } else if (Character.isWhitespace
                               (c = mySource.charAt (myCurrentIndex ++))) {
                        state = 0;
                    } else if (c == '\"') {
                        myLexemeType = QUOTED_STRING_LEXEME;
                        myLexemeBeginIndex = myCurrentIndex;
                        state = 1;
                    } else if (c == '(') {
                        ++ commentLevel;
                        state = 3;
                    } else if (c == '/'  || c == ';' || c == '=' ||
                               c == ')'  || c == '<' || c == '>' ||
                               c == '@'  || c == ',' || c == ':' ||
                               c == '\\' || c == '[' || c == ']' ||
                               c == '?') {
                        myLexemeType = TSPECIAL_LEXEME;
                        myLexemeBeginIndex = myCurrentIndex - 1;
                        myLexemeEndIndex = myCurrentIndex;
                        state = -1;
                    } else {
                        myLexemeType = TOKEN_LEXEME;
                        myLexemeBeginIndex = myCurrentIndex - 1;
                        state = 5;
                    }
                    break;
                    // In a quoted string
                case 1:
                    if (myCurrentIndex >= mySourceLength) {
                        myLexemeType = ILLEGAL_LEXEME;
                        myLexemeBeginIndex = mySourceLength;
                        myLexemeEndIndex = mySourceLength;
                        state = -1;
                    } else if ((c = mySource.charAt (myCurrentIndex ++)) == '\"') {
                        myLexemeEndIndex = myCurrentIndex - 1;
                        state = -1;
                    } else if (c == '\\') {
                        state = 2;
                    } else {
                        state = 1;
                    }
                    break;
                    // In a quoted string, backslash seen
                case 2:
                    if (myCurrentIndex >= mySourceLength) {
                        myLexemeType = ILLEGAL_LEXEME;
                        myLexemeBeginIndex = mySourceLength;
                        myLexemeEndIndex = mySourceLength;
                        state = -1;
                    } else {
                        ++ myCurrentIndex;
                        state = 1;
                    } break;
                    // In a comment
                case 3: if (myCurrentIndex >= mySourceLength) {
                    myLexemeType = ILLEGAL_LEXEME;
                    myLexemeBeginIndex = mySourceLength;
                    myLexemeEndIndex = mySourceLength;
                    state = -1;
                } else if ((c = mySource.charAt (myCurrentIndex ++)) == '(') {
                    ++ commentLevel;
                    state = 3;
                } else if (c == ')') {
                    -- commentLevel;
                    state = commentLevel == 0 ? 0 : 3;
                } else if (c == '\\') {
                    state = 4;
                } else { state = 3;
                }
                break;
                // In a comment, backslash seen
                case 4:
                    if (myCurrentIndex >= mySourceLength) {
                        myLexemeType = ILLEGAL_LEXEME;
                        myLexemeBeginIndex = mySourceLength;
                        myLexemeEndIndex = mySourceLength;
                        state = -1;
                    } else {
                        ++ myCurrentIndex;
                        state = 3;
                    }
                    break;
                    // In a token
                case 5:
                    if (myCurrentIndex >= mySourceLength) {
                        myLexemeEndIndex = myCurrentIndex;
                        state = -1;
                    } else if (Character.isWhitespace
                               (c = mySource.charAt (myCurrentIndex ++))) {
                        myLexemeEndIndex = myCurrentIndex - 1;
                        state = -1;
                    } else if (c == '\"' || c == '(' || c == '/' ||
                               c == ';'  || c == '=' || c == ')' ||
                               c == '<' || c == '>'  || c == '@' ||
                               c == ',' || c == ':' || c == '\\' ||
                               c == '[' || c == ']' || c == '?') {
                        -- myCurrentIndex;
                        myLexemeEndIndex = myCurrentIndex;
                        state = -1;
                    } else {
                        state = 5;
                    }
                    break;
                }
            }
        }
    }

    /**
     * Returns a lowercase version of the given string. The lowercase version is
     * constructed by applying {@code Character.toLowerCase()} to each character
     * of the given string, which maps characters to lowercase using the rules
     * of Unicode. This mapping is the same regardless of locale, whereas the
     * mapping of {@code String.toLowerCase()} may be different depending on the
     * default locale.
     *
     * @param  s the string
     * @return the lowercase version of the string
     */
    private static String toUnicodeLowerCase(String s) {
        int n = s.length();
        char[] result = new char [n];
        for (int i = 0; i < n; ++ i) {
            result[i] = Character.toLowerCase (s.charAt (i));
        }
        return new String (result);
    }

    /**
     * Returns a version of the given string with backslashes removed.
     *
     * @param  s the string
     * @return the string with backslashes removed
     */
    private static String removeBackslashes(String s) {
        int n = s.length();
        char[] result = new char [n];
        int i;
        int j = 0;
        char c;
        for (i = 0; i < n; ++ i) {
            c = s.charAt (i);
            if (c == '\\') {
                c = s.charAt (++ i);
            }
            result[j++] = c;
        }
        return new String (result, 0, j);
    }

    /**
     * Returns a version of the string surrounded by quotes and with interior
     * quotes preceded by a backslash.
     *
     * @param  s the string
     * @return the string surrounded by quotes and with interior quotes preceded
     *         by a backslash
     */
    private static String addQuotes(String s) {
        int n = s.length();
        int i;
        char c;
        StringBuilder result = new StringBuilder (n+2);
        result.append ('\"');
        for (i = 0; i < n; ++ i) {
            c = s.charAt (i);
            if (c == '\"') {
                result.append ('\\');
            }
            result.append (c);
        }
        result.append ('\"');
        return result.toString();
    }

    /**
     * Parses the given string into canonical pieces and stores the pieces in
     * {@link #myPieces myPieces}.
     * <p>
     * Special rules applied:
     * <ul>
     *   <li>If the media type is text, the value of a charset parameter is
     *   converted to lowercase.
     * </ul>
     *
     * @param  s MIME media type string
     * @throws NullPointerException if {@code s} is {@code null}
     * @throws IllegalArgumentException if {@code s} does not obey the syntax
     *         for a MIME media type string
     */
    private void parse(String s) {
        // Initialize.
        if (s == null) {
            throw new NullPointerException();
        }
        LexicalAnalyzer theLexer = new LexicalAnalyzer (s);
        int theLexemeType;
        Vector<String> thePieces = new Vector<>();
        boolean mediaTypeIsText = false;
        boolean parameterNameIsCharset = false;

        // Parse media type.
        if (theLexer.getLexemeType() == TOKEN_LEXEME) {
            String mt = toUnicodeLowerCase (theLexer.getLexeme());
            thePieces.add (mt);
            theLexer.nextLexeme();
            mediaTypeIsText = mt.equals ("text");
        } else {
            throw new IllegalArgumentException();
        }
        // Parse slash.
        if (theLexer.getLexemeType() == TSPECIAL_LEXEME &&
              theLexer.getLexemeFirstCharacter() == '/') {
            theLexer.nextLexeme();
        } else {
            throw new IllegalArgumentException();
        }
        if (theLexer.getLexemeType() == TOKEN_LEXEME) {
            thePieces.add (toUnicodeLowerCase (theLexer.getLexeme()));
            theLexer.nextLexeme();
        } else {
            throw new IllegalArgumentException();
        }
        // Parse zero or more parameters.
        while (theLexer.getLexemeType() == TSPECIAL_LEXEME &&
               theLexer.getLexemeFirstCharacter() == ';') {
            // Parse semicolon.
            theLexer.nextLexeme();

            // Parse parameter name.
            if (theLexer.getLexemeType() == TOKEN_LEXEME) {
                String pn = toUnicodeLowerCase (theLexer.getLexeme());
                thePieces.add (pn);
                theLexer.nextLexeme();
                parameterNameIsCharset = pn.equals ("charset");
            } else {
                throw new IllegalArgumentException();
            }

            // Parse equals.
            if (theLexer.getLexemeType() == TSPECIAL_LEXEME &&
                theLexer.getLexemeFirstCharacter() == '=') {
                theLexer.nextLexeme();
            } else {
                throw new IllegalArgumentException();
            }

            // Parse parameter value.
            if (theLexer.getLexemeType() == TOKEN_LEXEME) {
                String pv = theLexer.getLexeme();
                thePieces.add(mediaTypeIsText && parameterNameIsCharset ?
                              toUnicodeLowerCase (pv) :
                              pv);
                theLexer.nextLexeme();
            } else if (theLexer.getLexemeType() == QUOTED_STRING_LEXEME) {
                String pv = removeBackslashes (theLexer.getLexeme());
                thePieces.add(mediaTypeIsText && parameterNameIsCharset ?
                              toUnicodeLowerCase (pv) :
                              pv);
                theLexer.nextLexeme();
            } else {
                throw new IllegalArgumentException();
            }
        }

        // Make sure we've consumed everything.
        if (theLexer.getLexemeType() != EOF_LEXEME) {
            throw new IllegalArgumentException();
        }

        // Save the pieces. Parameters are not in ascending order yet.
        int n = thePieces.size();
        myPieces = thePieces.toArray (new String [n]);

        // Sort the parameters into ascending order using an insertion sort.
        int i, j;
        String temp;
        for (i = 4; i < n; i += 2) {
            j = 2;
            while (j < i && myPieces[j].compareTo (myPieces[i]) <= 0) {
                j += 2;
            }
            while (j < i) {
                temp = myPieces[j];
                myPieces[j] = myPieces[i];
                myPieces[i] = temp;
                temp = myPieces[j+1];
                myPieces[j+1] = myPieces[i+1];
                myPieces[i+1] = temp;
                j += 2;
            }
        }
    }
}
