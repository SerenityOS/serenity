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

package com.sun.org.apache.xml.internal.serializer;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import jdk.xml.internal.SecuritySupport;

/**
 * Provides information about encodings. Depends on the Java runtime
 * to provides writers for the different encodings, but can be used
 * to override encoding names and provide the last printable character
 * for each encoding.
 *
 * @author <a href="mailto:arkin@intalio.com">Assaf Arkin</a>
 * @LastModified: Oct 2017
 */

public final class Encodings extends Object
{

    /**
     * The last printable character for unknown encodings.
     */
    private static final int m_defaultLastPrintable = 0x7F;

    /**
     * Standard filename for properties file with encodings data.
     */
    private static final String ENCODINGS_FILE = "com/sun/org/apache/xml/internal/serializer/Encodings.properties";

    /**
     * Standard filename for properties file with encodings data.
     */
    private static final String ENCODINGS_PROP = "com.sun.org.apache.xalan.internal.serialize.encodings";


    /**
     * Returns a writer for the specified encoding based on
     * an output stream.
     *
     * @param output The output stream
     * @param encoding The encoding
     * @return A suitable writer
     * @throws UnsupportedEncodingException There is no convertor
     *  to support this encoding
     */
    static Writer getWriter(OutputStream output, String encoding)
        throws UnsupportedEncodingException
    {

        final EncodingInfo ei = _encodingInfos.findEncoding(toUpperCaseFast(encoding));
        if (ei != null) {
            try {
                return new BufferedWriter(new OutputStreamWriter(
                        output, ei.javaName));
            } catch (UnsupportedEncodingException usee) {
                // keep trying
            }
        }

        return new BufferedWriter(new OutputStreamWriter(output, encoding));
    }


    /**
     * Returns the last printable character for an unspecified
     * encoding.
     *
     * @return the default size
     */
    public static int getLastPrintable()
    {
        return m_defaultLastPrintable;
    }



    /**
     * Returns the EncodingInfo object for the specified
     * encoding.
     * <p>
     * This is not a public API.
     *
     * @param encoding The encoding
     * @return The object that is used to determine if
     * characters are in the given encoding.
     * @xsl.usage internal
     */
    static EncodingInfo getEncodingInfo(String encoding)
    {
        EncodingInfo ei;

        String normalizedEncoding = toUpperCaseFast(encoding);
        ei = _encodingInfos.findEncoding(normalizedEncoding);
        if (ei == null) {
            // We shouldn't have to do this, but just in case.
            try {
                // This may happen if the caller tries to use
                // an encoding that wasn't registered in the
                // (java name)->(preferred mime name) mapping file.
                // In that case we attempt to load the charset for the
                // given encoding, and if that succeeds - we create a new
                // EncodingInfo instance - assuming the canonical name
                // of the charset can be used as the mime name.
                final Charset c = Charset.forName(encoding);
                final String name = c.name();
                ei = new EncodingInfo(name, name);
                _encodingInfos.putEncoding(normalizedEncoding, ei);
            } catch (IllegalCharsetNameException | UnsupportedCharsetException x) {
                ei = new EncodingInfo(null,null);
            }
        }

        return ei;
    }

    /**
     * Determines if the encoding specified was recognized by the
     * serializer or not.
     *
     * @param encoding The encoding
     * @return boolean - true if the encoding was recognized else false
     */
    public static boolean isRecognizedEncoding(String encoding)
    {
        EncodingInfo ei;

        String normalizedEncoding = toUpperCaseFast(encoding);
        ei = _encodingInfos.findEncoding(normalizedEncoding);
        if (ei != null)
            return true;
        return false;
    }

    /**
     * A fast and cheap way to uppercase a String that is
     * only made of printable ASCII characters.
     * <p>
     * This is not a public API.
     * @param s a String of ASCII characters
     * @return an uppercased version of the input String,
     * possibly the same String.
     * @xsl.usage internal
     */
    static private String toUpperCaseFast(final String s) {

        boolean different = false;
        final int mx = s.length();
                char[] chars = new char[mx];
        for (int i=0; i < mx; i++) {
                char ch = s.charAt(i);
            // is the character a lower case ASCII one?
                if ('a' <= ch && ch <= 'z') {
                // a cheap and fast way to uppercase that is good enough
                        ch = (char) (ch + ('A' - 'a'));
                        different = true; // the uppercased String is different
                }
                chars[i] = ch;
        }

        // A little optimization, don't call String.valueOf() if
        // the uppercased string is the same as the input string.
        final String upper;
        if (different)
                upper = String.valueOf(chars);
        else
                upper = s;

        return upper;
    }

    /** The default encoding, ISO style, ISO style.   */
    static final String DEFAULT_MIME_ENCODING = "UTF-8";

    /**
     * Get the proper mime encoding.  From the XSLT recommendation: "The encoding
     * attribute specifies the preferred encoding to use for outputting the result
     * tree. XSLT processors are required to respect values of UTF-8 and UTF-16.
     * For other values, if the XSLT processor does not support the specified
     * encoding it may signal an error; if it does not signal an error it should
     * use UTF-8 or UTF-16 instead. The XSLT processor must not use an encoding
     * whose name does not match the EncName production of the XML Recommendation
     * [XML]. If no encoding attribute is specified, then the XSLT processor should
     * use either UTF-8 or UTF-16."
     *
     * @param encoding Reference to java-style encoding string, which may be null,
     * in which case a default will be found.
     *
     * @return The ISO-style encoding string, or null if failure.
     */
    static String getMimeEncoding(String encoding)
    {

        if (null == encoding)
        {
            try
            {

                // Get the default system character encoding.  This may be
                // incorrect if they passed in a writer, but right now there
                // seems to be no way to get the encoding from a writer.
                encoding = SecuritySupport.getSystemProperty("file.encoding", "UTF8");

                if (null != encoding)
                {

                    /*
                    * See if the mime type is equal to UTF8.  If you don't
                    * do that, then  convertJava2MimeEncoding will convert
                    * 8859_1 to "ISO-8859-1", which is not what we want,
                    * I think, and I don't think I want to alter the tables
                    * to convert everything to UTF-8.
                    */
                    String jencoding =
                        (encoding.equalsIgnoreCase("Cp1252")
                            || encoding.equalsIgnoreCase("ISO8859_1")
                            || encoding.equalsIgnoreCase("8859_1")
                            || encoding.equalsIgnoreCase("UTF8"))
                            ? DEFAULT_MIME_ENCODING
                            : convertJava2MimeEncoding(encoding);

                    encoding =
                        (null != jencoding) ? jencoding : DEFAULT_MIME_ENCODING;
                }
                else
                {
                    encoding = DEFAULT_MIME_ENCODING;
                }
            }
            catch (SecurityException se)
            {
                encoding = DEFAULT_MIME_ENCODING;
            }
        }
        else
        {
            encoding = convertJava2MimeEncoding(encoding);
        }

        return encoding;
    }

    /**
     * Try the best we can to convert a Java encoding to a XML-style encoding.
     *
     * @param encoding non-null reference to encoding string, java style.
     *
     * @return ISO-style encoding string.
     */
    private static String convertJava2MimeEncoding(String encoding)
    {
        final EncodingInfo enc =
             _encodingInfos.getEncodingFromJavaKey(toUpperCaseFast(encoding));
        if (null != enc)
            return enc.name;
        return encoding;
    }

    /**
     * Try the best we can to convert a Java encoding to a XML-style encoding.
     *
     * @param encoding non-null reference to encoding string, java style.
     *
     * @return ISO-style encoding string.
     */
    public static String convertMime2JavaEncoding(String encoding)
    {
        final EncodingInfo info = _encodingInfos.findEncoding(toUpperCaseFast(encoding));
        return info != null ? info.javaName : encoding;
    }

    // Using an inner static class here prevent initialization races
    // where the hash maps could be used before they were populated.
    //
    private final static class EncodingInfos {
        // These maps are final and not modified after initialization.
        private final Map<String, EncodingInfo> _encodingTableKeyJava = new HashMap<>();
        private final Map<String, EncodingInfo> _encodingTableKeyMime = new HashMap<>();
        // This map will be added to after initialization: make sure it's
        // thread-safe. This map should not be used frequently - only in cases
        // where the mapping requested was not declared in the Encodings.properties
        // file.
        private final Map<String, EncodingInfo> _encodingDynamicTable =
                Collections.synchronizedMap(new HashMap<String, EncodingInfo>());

        private EncodingInfos() {
            loadEncodingInfo();
        }

        // Opens the file/resource containing java charset name -> preferred mime
        // name mapping and returns it as an InputStream.
        private InputStream openEncodingsFileStream() throws MalformedURLException, IOException {
            String urlString = null;
            InputStream is = null;

            try {
                urlString = SecuritySupport.getSystemProperty(ENCODINGS_PROP, "");
            } catch (SecurityException e) {
            }

            if (urlString != null && urlString.length() > 0) {
                URL url = new URL(urlString);
                is = url.openStream();
            }

            if (is == null) {
                is = SecuritySupport.getResourceAsStream(ENCODINGS_FILE);
            }
            return is;
        }

        // Loads the Properties resource containing the mapping:
        //    java charset name -> preferred mime name
        // and returns it.
        private Properties loadProperties() throws MalformedURLException, IOException {
            Properties props = new Properties();
            try (InputStream is = openEncodingsFileStream()) {
                if (is != null) {
                    props.load(is);
                } else {
                    // Seems to be no real need to force failure here, let the
                    // system do its best... The issue is not really very critical,
                    // and the output will be in any case _correct_ though maybe not
                    // always human-friendly... :)
                    // But maybe report/log the resource problem?
                    // Any standard ways to report/log errors (in static context)?
                }
            }
            return props;
        }

        // Parses the mime list associated to a java charset name.
        // The first mime name in the list is supposed to be the preferred
        // mime name.
        private String[] parseMimeTypes(String val) {
            int pos = val.indexOf(' ');
            //int lastPrintable;
            if (pos < 0) {
                // Maybe report/log this problem?
                //  "Last printable character not defined for encoding " +
                //  mimeName + " (" + val + ")" ...
                return new String[] { val };
                //lastPrintable = 0x00FF;
            }
            //lastPrintable =
            //    Integer.decode(val.substring(pos).trim()).intValue();
            StringTokenizer st =
                    new StringTokenizer(val.substring(0, pos), ",");
            String[] values = new String[st.countTokens()];
            for (int i=0; st.hasMoreTokens(); i++) {
                values[i] = st.nextToken();
            }
            return values;
        }

        // This method here attempts to find the canonical charset name for the
        // the given name - which is supposed to be either a java name or a mime
        // name.
        // For that, it attempts to load the charset using the given name, and
        // then returns the charset's canonical name.
        // If the charset could not be loaded from the given name,
        // the method returns null.
        private String findCharsetNameFor(String name) {
            try {
                return Charset.forName(name).name();
            } catch (Exception x) {
                return null;
            }
        }

        // This method here attempts to find the canonical charset name for the
        // the set javaName+mimeNames - which are supposed to all refer to the
        // same charset.
        // For that it attempts to load the charset using the javaName, and if
        // not found, attempts again using each of the mime names in turn.
        // If the charset could be loaded from the javaName, then the javaName
        // itself is returned as charset name. Otherwise, each of the mime names
        // is tried in turn, until a charset can be loaded from one of the names,
        // and the loaded charset's canonical name is returned.
        // If no charset can be loaded from either the javaName or one of the
        // mime names, then null is returned.
        //
        // Note that the returned name is the 'java' name that will be used in
        // instances of EncodingInfo.
        // This is important because EncodingInfo uses that 'java name' later on
        // in calls to String.getBytes(javaName).
        // As it happens, sometimes only one element of the set mime names/javaName
        // is known by Charset: sometimes only one of the mime names is known,
        // sometime only the javaName is known, sometimes all are known.
        //
        // By using this method here, we fix the problem where one of the mime
        // names is known but the javaName is unknown, by associating the charset
        // loaded from one of the mime names with the unrecognized javaName.
        //
        // When none of the mime names or javaName are known - there's not much we can
        // do... It can mean that this encoding is not supported for this
        // OS. If such a charset is ever use it will result in having all characters
        // escaped.
        //
        private String findCharsetNameFor(String javaName, String[] mimes) {
            String cs = findCharsetNameFor(javaName);
            if (cs != null) return javaName;
            for (String m : mimes) {
                cs = findCharsetNameFor(m);
                if (cs != null) break;
            }
            return cs;
        }

        /**
         * Loads a list of all the supported encodings.
         *
         * System property "encodings" formatted using URL syntax may define an
         * external encodings list. Thanks to Sergey Ushakov for the code
         * contribution!
         */
        private void loadEncodingInfo() {
            try {
                // load (java name)->(preferred mime name) mapping.
                final Properties props = loadProperties();

                // create instances of EncodingInfo from the loaded mapping
                Enumeration<Object> keys = props.keys();
                Map<String, EncodingInfo> canonicals = new HashMap<>();
                while (keys.hasMoreElements()) {
                    final String javaName = (String) keys.nextElement();
                    final String[] mimes = parseMimeTypes(props.getProperty(javaName));

                    final String charsetName = findCharsetNameFor(javaName, mimes);
                    if (charsetName != null) {
                        final String kj = toUpperCaseFast(javaName);
                        final String kc = toUpperCaseFast(charsetName);
                        for (int i = 0; i < mimes.length; ++i) {
                            final String mimeName = mimes[i];
                            final String km = toUpperCaseFast(mimeName);
                            EncodingInfo info = new EncodingInfo(mimeName, charsetName);
                            _encodingTableKeyMime.put(km, info);
                            if (!canonicals.containsKey(kc)) {
                                // canonicals will map the charset name to
                                //   the info containing the prefered mime name
                                //   (the preferred mime name is the first mime
                                //   name in the list).
                                canonicals.put(kc, info);
                                _encodingTableKeyJava.put(kc, info);
                            }
                            _encodingTableKeyJava.put(kj, info);
                        }
                    } else {
                        // None of the java or mime names on the line were
                        // recognized => this charset is not supported?
                    }
                }

                // Fix up the _encodingTableKeyJava so that the info mapped to
                // the java name contains the preferred mime name.
                // (a given java name can correspond to several mime name,
                //  but we want the _encodingTableKeyJava to point to the
                //  preferred mime name).
                for (Entry<String, EncodingInfo> e : _encodingTableKeyJava.entrySet()) {
                    e.setValue(canonicals.get(toUpperCaseFast(e.getValue().javaName)));
                }

            } catch (java.net.MalformedURLException mue) {
                throw new com.sun.org.apache.xml.internal.serializer.utils.WrappedRuntimeException(mue);
            } catch (java.io.IOException ioe) {
                throw new com.sun.org.apache.xml.internal.serializer.utils.WrappedRuntimeException(ioe);
            }
        }

        EncodingInfo findEncoding(String normalizedEncoding) {
            EncodingInfo info = _encodingTableKeyJava.get(normalizedEncoding);
            if (info == null) {
                info = _encodingTableKeyMime.get(normalizedEncoding);
            }
            if (info == null) {
                info = _encodingDynamicTable.get(normalizedEncoding);
            }
            return info;
        }

        EncodingInfo getEncodingFromMimeKey(String normalizedMimeName) {
            return _encodingTableKeyMime.get(normalizedMimeName);
        }

        EncodingInfo getEncodingFromJavaKey(String normalizedJavaName) {
            return _encodingTableKeyJava.get(normalizedJavaName);
        }

        void putEncoding(String key, EncodingInfo info) {
            _encodingDynamicTable.put(key, info);
        }
    }

    /**
     * Return true if the character is the high member of a surrogate pair.
     * <p>
     * This is not a public API.
     * @param ch the character to test
     * @xsl.usage internal
     */
    static boolean isHighUTF16Surrogate(char ch) {
        return ('\uD800' <= ch && ch <= '\uDBFF');
    }
    /**
     * Return true if the character is the low member of a surrogate pair.
     * <p>
     * This is not a public API.
     * @param ch the character to test
     * @xsl.usage internal
     */
    static boolean isLowUTF16Surrogate(char ch) {
        return ('\uDC00' <= ch && ch <= '\uDFFF');
    }
    /**
     * Return the unicode code point represented by the high/low surrogate pair.
     * <p>
     * This is not a public API.
     * @param highSurrogate the high char of the high/low pair
     * @param lowSurrogate the low char of the high/low pair
     * @xsl.usage internal
     */
    static int toCodePoint(char highSurrogate, char lowSurrogate) {
        int codePoint =
            ((highSurrogate - 0xd800) << 10)
                + (lowSurrogate - 0xdc00)
                + 0x10000;
        return codePoint;
    }
    /**
     * Return the unicode code point represented by the char.
     * A bit of a dummy method, since all it does is return the char,
     * but as an int value.
     * <p>
     * This is not a public API.
     * @param ch the char.
     * @xsl.usage internal
     */
    static int toCodePoint(char ch) {
        int codePoint = ch;
        return codePoint;
    }

    private final static EncodingInfos _encodingInfos = new EncodingInfos();

}
