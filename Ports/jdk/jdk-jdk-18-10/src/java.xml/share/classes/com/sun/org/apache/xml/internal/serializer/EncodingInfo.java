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

package com.sun.org.apache.xml.internal.serializer;

import java.io.UnsupportedEncodingException;

/**
 * Holds information about a given encoding, which is the Java name for the
 * encoding, the equivalent ISO name.
 * <p>
 * An object of this type has two useful methods
 * <pre>
 * isInEncoding(char ch);
 * </pre>
 * which can be called if the character is not the high one in
 * a surrogate pair and:
 * <pre>
 * isInEncoding(char high, char low);
 * </pre>
 * which can be called if the two characters from a high/low surrogate pair.
 * <p>
 * An EncodingInfo object is a node in a binary search tree. Such a node
 * will answer if a character is in the encoding, and do so for a given
 * range of unicode values (<code>m_first</code> to
 * <code>m_last</code>). It will handle a certain range of values
 * explicitly (<code>m_explFirst</code> to <code>m_explLast</code>).
 * If the unicode point is before that explicit range, that is it
 * is in the range <code>m_first <= value < m_explFirst</code>, then it will delegate to another EncodingInfo object for The root
 * of such a tree, m_before.  Likewise for values in the range
 * <code>m_explLast < value <= m_last</code>, but delgating to <code>m_after</code>
 * <p>
 * Actually figuring out if a code point is in the encoding is expensive. So the
 * purpose of this tree is to cache such determinations, and not to build the
 * entire tree of information at the start, but only build up as much of the
 * tree as is used during the transformation.
 * <p>
 * This Class is not a public API, and should only be used internally within
 * the serializer.
 *
 * @xsl.usage internal
 */
public final class EncodingInfo extends Object
{

    /**
     * The ISO encoding name.
     */
    final String name;

    /**
     * The name used by the Java convertor.
     */
    final String javaName;

    /**
     * A helper object that we can ask if a
     * single char, or a surrogate UTF-16 pair
     * of chars that form a single character,
     * is in this encoding.
     */
    private InEncoding m_encoding;

    /**
     * This is not a public API. It returns true if the
     * char in question is in the encoding.
     * @param ch the char in question.
     * @xsl.usage internal
     */
    public boolean isInEncoding(char ch) {
        if (m_encoding == null) {
            m_encoding = new EncodingImpl();

            // One could put alternate logic in here to
            // instantiate another object that implements the
            // InEncoding interface. For example if the JRE is 1.4 or up
            // we could have an object that uses JRE 1.4 methods
        }
        return m_encoding.isInEncoding(ch);
    }

    /**
     * This is not a public API. It returns true if the
     * character formed by the high/low pair is in the encoding.
     * @param high a char that the a high char of a high/low surrogate pair.
     * @param low a char that is the low char of a high/low surrogate pair.
     * @xsl.usage internal
     */
    public boolean isInEncoding(char high, char low) {
        if (m_encoding == null) {
            m_encoding = new EncodingImpl();

            // One could put alternate logic in here to
            // instantiate another object that implements the
            // InEncoding interface. For example if the JRE is 1.4 or up
            // we could have an object that uses JRE 1.4 methods
        }
        return m_encoding.isInEncoding(high, low);
    }

    /**
     * Create an EncodingInfo object based on the ISO name and Java name.
     * If both parameters are null any character will be considered to
     * be in the encoding. This is useful for when the serializer is in
     * temporary output state, and has no assciated encoding.
     *
     * @param name reference to the ISO name.
     * @param javaName reference to the Java encoding name.
     */
    public EncodingInfo(String name, String javaName)
    {

        this.name = name;
        this.javaName = javaName;
    }



    /**
     * A simple interface to isolate the implementation.
     * We could also use some new JRE 1.4 methods in another implementation
     * provided we use reflection with them.
     * <p>
     * This interface is not a public API,
     * and should only be used internally within the serializer.
     * @xsl.usage internal
     */
    private interface InEncoding {
        /**
         * Returns true if the char is in the encoding
         */
        public boolean isInEncoding(char ch);
        /**
         * Returns true if the high/low surrogate pair forms
         * a character that is in the encoding.
         */
        public boolean isInEncoding(char high, char low);
    }

    /**
     * This class implements the
     */
    private class EncodingImpl implements InEncoding {



        public boolean isInEncoding(char ch1) {
            final boolean ret;
            int codePoint = Encodings.toCodePoint(ch1);
            if (codePoint < m_explFirst) {
                // The unicode value is before the range
                // that we explictly manage, so we delegate the answer.

                // If we don't have an m_before object to delegate to, make one.
                if (m_before == null)
                    m_before =
                        new EncodingImpl(
                            m_encoding,
                            m_first,
                            m_explFirst - 1,
                            codePoint);
                ret = m_before.isInEncoding(ch1);
            } else if (m_explLast < codePoint) {
                // The unicode value is after the range
                // that we explictly manage, so we delegate the answer.

                // If we don't have an m_after object to delegate to, make one.
                if (m_after == null)
                    m_after =
                        new EncodingImpl(
                            m_encoding,
                            m_explLast + 1,
                            m_last,
                            codePoint);
                ret = m_after.isInEncoding(ch1);
            } else {
                // The unicode value is in the range we explitly handle
                final int idx = codePoint - m_explFirst;

                // If we already know the answer, just return it.
                if (m_alreadyKnown[idx])
                    ret = m_isInEncoding[idx];
                else {
                    // We don't know the answer, so find out,
                    // which may be expensive, then cache the answer
                    ret = inEncoding(ch1, m_encoding);
                    m_alreadyKnown[idx] = true;
                    m_isInEncoding[idx] = ret;
                }
            }
            return ret;
        }

        public boolean isInEncoding(char high, char low) {
            final boolean ret;
            int codePoint = Encodings.toCodePoint(high,low);
            if (codePoint < m_explFirst) {
                // The unicode value is before the range
                // that we explictly manage, so we delegate the answer.

                // If we don't have an m_before object to delegate to, make one.
                if (m_before == null)
                    m_before =
                        new EncodingImpl(
                            m_encoding,
                            m_first,
                            m_explFirst - 1,
                            codePoint);
                ret = m_before.isInEncoding(high,low);
            } else if (m_explLast < codePoint) {
                // The unicode value is after the range
                // that we explictly manage, so we delegate the answer.

                // If we don't have an m_after object to delegate to, make one.
                if (m_after == null)
                    m_after =
                        new EncodingImpl(
                            m_encoding,
                            m_explLast + 1,
                            m_last,
                            codePoint);
                ret = m_after.isInEncoding(high,low);
            } else {
                // The unicode value is in the range we explitly handle
                final int idx = codePoint - m_explFirst;

                // If we already know the answer, just return it.
                if (m_alreadyKnown[idx])
                    ret = m_isInEncoding[idx];
                else {
                    // We don't know the answer, so find out,
                    // which may be expensive, then cache the answer
                    ret = inEncoding(high, low, m_encoding);
                    m_alreadyKnown[idx] = true;
                    m_isInEncoding[idx] = ret;
                }
            }
            return ret;
        }

        /**
         * The encoding.
         */
        final private String m_encoding;
        /**
         * m_first through m_last is the range of unicode
         * values that this object will return an answer on.
         * It may delegate to a similar object with a different
         * range
         */
        final private int m_first;

        /**
         * m_explFirst through m_explLast is the range of unicode
         * value that this object handles explicitly and does not
         * delegate to a similar object.
         */
        final private int m_explFirst;
        final private int m_explLast;
        final private int m_last;

        /**
         * The object, of the same type as this one,
         * that handles unicode values in a range before
         * the range explictly handled by this object, and
         * to which this object may delegate.
         */
        private InEncoding m_before;
        /**
         * The object, of the same type as this one,
         * that handles unicode values in a range after
         * the range explictly handled by this object, and
         * to which this object may delegate.
         */
        private InEncoding m_after;

        /**
         * The number of unicode values explicitly handled
         * by a single EncodingInfo object. This value is
         * tuneable, but is set to 128 because that covers the
         * entire low range of ASCII type chars within a single
         * object.
         */
        private static final int RANGE = 128;

        /**
         * A flag to record if we already know the answer
         * for the given unicode value.
         */
        final private boolean m_alreadyKnown[] = new boolean[RANGE];
        /**
         * A table holding the answer on whether the given unicode
         * value is in the encoding.
         */
        final private boolean m_isInEncoding[] = new boolean[RANGE];

        private EncodingImpl() {
            // This object will answer whether any unicode value
            // is in the encoding, it handles values 0 through Integer.MAX_VALUE
            this(javaName, 0, Integer.MAX_VALUE, (char) 0);
        }

        private EncodingImpl(String encoding, int first, int last, int codePoint) {
            // Set the range of unicode values that this object manages
            // either explicitly or implicitly.
            m_first = first;
            m_last = last;

            // Set the range of unicode values that this object
            // explicitly manages. Align the explicitly managed values
            // to RANGE so multiple EncodingImpl objects dont manage the same
            // values.
            m_explFirst = codePoint / RANGE * RANGE;
            m_explLast = m_explFirst + (RANGE-1);

            m_encoding = encoding;

            if (javaName != null)
            {
                // Some optimization.
                if (0 <= m_explFirst && m_explFirst <= 127) {
                    // This particular EncodingImpl explicitly handles
                    // characters in the low range.
                    if ("UTF8".equals(javaName)
                        || "UTF-16".equals(javaName)
                        || "ASCII".equals(javaName)
                        || "US-ASCII".equals(javaName)
                        || "Unicode".equals(javaName)
                        || "UNICODE".equals(javaName)
                        || javaName.startsWith("ISO8859")) {

                        // Not only does this EncodingImpl object explicitly
                        // handle chracters in the low range, it is
                        // also one that we know something about, without
                        // needing to call inEncoding(char ch, String encoding)
                        // for this low range
                        //
                        // By initializing the table ahead of time
                        // for these low values, we prevent the expensive
                        // inEncoding(char ch, String encoding)
                        // from being called, at least for these common
                        // encodings.
                        for (int unicode = 1; unicode < 127; unicode++) {
                            final int idx = unicode - m_explFirst;
                            if (0 <= idx && idx < RANGE) {
                                m_alreadyKnown[idx] = true;
                                m_isInEncoding[idx] = true;
                            }
                        }
                    }
                }

                /* A little bit more than optimization.
                 *
                 * We will say that any character is in the encoding if
                 * we don't have an encoding.
                 * This is meaningful when the serializer is being used
                 * in temporary output state, where we are not writing to
                 * the final output tree.  It is when writing to the
                 * final output tree that we need to worry about the output
                 * encoding
                 */
                if (javaName == null) {
                    for (int idx = 0; idx < m_alreadyKnown.length; idx++) {
                        m_alreadyKnown[idx] = true;
                        m_isInEncoding[idx] = true;
                    }
                }
            }
        }
    }

    /**
     * This is heart of the code that determines if a given character
     * is in the given encoding. This method is probably expensive,
     * and the answer should be cached.
     * <p>
     * This method is not a public API,
     * and should only be used internally within the serializer.
     * @param ch the char in question, that is not a high char of
     * a high/low surrogate pair.
     * @param encoding the Java name of the enocding.
     *
     * @xsl.usage internal
     *
     */
    private static boolean inEncoding(char ch, String encoding) {
        boolean isInEncoding;
        try {
            char cArray[] = new char[1];
            cArray[0] = ch;
            // Construct a String from the char
            String s = new String(cArray);
            // Encode the String into a sequence of bytes
            // using the given, named charset.
            byte[] bArray = s.getBytes(encoding);
            isInEncoding = inEncoding(ch, bArray);

        } catch (Exception e) {
            isInEncoding = false;

            // If for some reason the encoding is null, e.g.
            // for a temporary result tree, we should just
            // say that every character is in the encoding.
            if (encoding == null)
                isInEncoding = true;
        }
        return isInEncoding;
    }

    /**
     * This is heart of the code that determines if a given high/low
     * surrogate pair forms a character that is in the given encoding.
     * This method is probably expensive, and the answer should be cached.
     * <p>
     * This method is not a public API,
     * and should only be used internally within the serializer.
     * @param high the high char of
     * a high/low surrogate pair.
     * @param low the low char of a high/low surrogate pair.
     * @param encoding the Java name of the encoding.
     *
     * @xsl.usage internal
     *
     */
    private static boolean inEncoding(char high, char low, String encoding) {
        boolean isInEncoding;
        try {
            char cArray[] = new char[2];
            cArray[0] = high;
            cArray[1] = low;
            // Construct a String from the char
            String s = new String(cArray);
            // Encode the String into a sequence of bytes
            // using the given, named charset.
            byte[] bArray = s.getBytes(encoding);
            isInEncoding = inEncoding(high,bArray);
        } catch (Exception e) {
            isInEncoding = false;
        }

        return isInEncoding;
    }

    /**
     * This method is the core of determining if character
     * is in the encoding. The method is not foolproof, because
     * s.getBytes(encoding) has specified behavior only if the
     * characters are in the specified encoding. However this
     * method tries it's best.
     * @param ch the char that was converted using getBytes, or
     * the first char of a high/low pair that was converted.
     * @param data the bytes written out by the call to s.getBytes(encoding);
     * @return true if the character is in the encoding.
     */
    private static boolean inEncoding(char ch, byte[] data) {
        final boolean isInEncoding;
        // If the string written out as data is not in the encoding,
        // the output is not specified according to the documentation
        // on the String.getBytes(encoding) method,
        // but we do our best here.
        if (data==null || data.length == 0) {
            isInEncoding = false;
        }
        else {
            if (data[0] == 0)
                isInEncoding = false;
            else if (data[0] == '?' && ch != '?')
                isInEncoding = false;
            /*
             * else if (isJapanese) {
             *   // isJapanese is really
             *   //   (    "EUC-JP".equals(javaName)
             *   //    ||  "EUC_JP".equals(javaName)
             *  //     ||  "SJIS".equals(javaName)   )
             *
             *   // Work around some bugs in JRE for Japanese
             *   if(data[0] == 0x21)
             *     isInEncoding = false;
             *   else if (ch == 0xA5)
             *     isInEncoding = false;
             *   else
             *     isInEncoding = true;
             * }
             */

            else {
                // We don't know for sure, but it looks like it is in the encoding
                isInEncoding = true;
            }
        }
        return isInEncoding;
    }

}
