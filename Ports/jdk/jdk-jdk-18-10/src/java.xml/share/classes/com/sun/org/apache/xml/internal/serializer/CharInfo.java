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

import com.sun.org.apache.xml.internal.serializer.utils.MsgKey;
import com.sun.org.apache.xml.internal.serializer.utils.SystemIDResolver;
import com.sun.org.apache.xml.internal.serializer.utils.Utils;
import com.sun.org.apache.xml.internal.serializer.utils.WrappedRuntimeException;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import javax.xml.transform.TransformerException;
import jdk.xml.internal.SecuritySupport;

/**
 * This class provides services that tell if a character should have
 * special treatement, such as entity reference substitution or normalization
 * of a newline character.  It also provides character to entity reference
 * lookup.
 *
 * DEVELOPERS: See Known Issue in the constructor.
 *
 * @xsl.usage internal
 * @LastModified: Oct 2017
 */
final class CharInfo
{
    /** Given a character, lookup a String to output (e.g. a decorated entity reference). */
    private Map<CharKey, String> m_charToString = new HashMap<>();

    /**
     * The name of the HTML entities file.
     * If specified, the file will be resource loaded with the default class loader.
     */
    public static final String HTML_ENTITIES_RESOURCE =
                "com.sun.org.apache.xml.internal.serializer.HTMLEntities";

    /**
     * The name of the XML entities file.
     * If specified, the file will be resource loaded with the default class loader.
     */
    public static final String XML_ENTITIES_RESOURCE =
                "com.sun.org.apache.xml.internal.serializer.XMLEntities";

    /** The horizontal tab character, which the parser should always normalize. */
    public static final char S_HORIZONAL_TAB = 0x09;

    /** The linefeed character, which the parser should always normalize. */
    public static final char S_LINEFEED = 0x0A;

    /** The carriage return character, which the parser should always normalize. */
    public static final char S_CARRIAGERETURN = 0x0D;

    /** This flag is an optimization for HTML entities. It false if entities
     * other than quot (34), amp (38), lt (60) and gt (62) are defined
     * in the range 0 to 127.
     * @xsl.usage internal
     */
    final boolean onlyQuotAmpLtGt;

    /** Copy the first 0,1 ... ASCII_MAX values into an array */
    private static final int ASCII_MAX = 128;

    /** Array of values is faster access than a set of bits
     * to quickly check ASCII characters in attribute values.
     */
    private boolean[] isSpecialAttrASCII = new boolean[ASCII_MAX];

    /** Array of values is faster access than a set of bits
     * to quickly check ASCII characters in text nodes.
     */
    private boolean[] isSpecialTextASCII = new boolean[ASCII_MAX];

    private boolean[] isCleanTextASCII = new boolean[ASCII_MAX];

    /** An array of bits to record if the character is in the set.
     * Although information in this array is complete, the
     * isSpecialAttrASCII array is used first because access to its values
     * is common and faster.
     */
    private int array_of_bits[] = createEmptySetOfIntegers(65535);


    // 5 for 32 bit words,  6 for 64 bit words ...
    /*
     * This constant is used to shift an integer to quickly
     * calculate which element its bit is stored in.
     * 5 for 32 bit words (int) ,  6 for 64 bit words (long)
     */
    private static final int SHIFT_PER_WORD = 5;

    /*
     * A mask to get the low order bits which are used to
     * calculate the value of the bit within a given word,
     * that will represent the presence of the integer in the
     * set.
     *
     * 0x1F for 32 bit words (int),
     * or 0x3F for 64 bit words (long)
     */
    private static final int LOW_ORDER_BITMASK = 0x1f;

    /*
     * This is used for optimizing the lookup of bits representing
     * the integers in the set. It is the index of the first element
     * in the array array_of_bits[] that is not used.
     */
    private int firstWordNotUsed;


    /**
     * Constructor that reads in a resource file that describes the mapping of
     * characters to entity references.
     * This constructor is private, just to force the use
     * of the getCharInfo(entitiesResource) factory
     *
     * Resource files must be encoded in UTF-8 and can either be properties
     * files with a .properties extension assumed.  Alternatively, they can
     * have the following form, with no particular extension assumed:
     *
     * <pre>
     * # First char # is a comment
     * Entity numericValue
     * quot 34
     * amp 38
     * </pre>
     *
     * @param entitiesResource Name of properties or resource file that should
     * be loaded, which describes that mapping of characters to entity
     * references.
     */
    private CharInfo(String entitiesResource, String method)
    {
        this(entitiesResource, method, false);
    }

    private CharInfo(String entitiesResource, String method, boolean internal)
    {
        ResourceBundle entities = null;
        boolean noExtraEntities = true;

        // Make various attempts to interpret the parameter as a properties
        // file or resource file, as follows:
        //
        //   1) attempt to load .properties file using ResourceBundle
        //   2) try using the class loader to find the specified file a resource
        //      file
        //   3) try treating the resource a URI

        try {
            if (internal) {
                // Load entity property files by using PropertyResourceBundle,
                // cause of security issure for applets
                entities = PropertyResourceBundle.getBundle(entitiesResource);
            } else {
                ClassLoader cl = SecuritySupport.getContextClassLoader();
                if (cl != null) {
                    entities = PropertyResourceBundle.getBundle(entitiesResource,
                            Locale.getDefault(), cl);
                }
            }
        } catch (Exception e) {}

        if (entities != null) {
            Enumeration<String> keys = entities.getKeys();
            while (keys.hasMoreElements()){
                String name = keys.nextElement();
                String value = entities.getString(name);
                int code = Integer.parseInt(value);
                defineEntity(name, (char) code);
                if (extraEntity(code))
                    noExtraEntities = false;
            }
            set(S_LINEFEED);
            set(S_CARRIAGERETURN);
        } else {
            InputStream is = null;
            String err = null;

            // Load user specified resource file by using URL loading, it
            // requires a valid URI as parameter
            try {
                if (internal) {
                    is = CharInfo.class.getResourceAsStream(entitiesResource);
                } else {
                    ClassLoader cl = SecuritySupport.getContextClassLoader();
                    if (cl != null) {
                        try {
                            is = cl.getResourceAsStream(entitiesResource);
                        } catch (Exception e) {
                            err = e.getMessage();
                        }
                    }

                    if (is == null) {
                        try {
                            URL url = new URL(entitiesResource);
                            is = url.openStream();
                        } catch (Exception e) {
                            err = e.getMessage();
                        }
                    }
                }

                if (is == null) {
                    throw new RuntimeException(
                        Utils.messages.createMessage(
                            MsgKey.ER_RESOURCE_COULD_NOT_FIND,
                            new Object[] {entitiesResource, err}));
                }

                // Fix Bugzilla#4000: force reading in UTF-8
                //  This creates the de facto standard that Xalan's resource
                //  files must be encoded in UTF-8. This should work in all
                // JVMs.
                //
                // %REVIEW% KNOWN ISSUE: IT FAILS IN MICROSOFT VJ++, which
                // didn't implement the UTF-8 encoding. Theoretically, we should
                // simply let it fail in that case, since the JVM is obviously
                // broken if it doesn't support such a basic standard.  But
                // since there are still some users attempting to use VJ++ for
                // development, we have dropped in a fallback which makes a
                // second attempt using the platform's default encoding. In VJ++
                // this is apparently ASCII, which is subset of UTF-8... and
                // since the strings we'll be reading here are also primarily
                // limited to the 7-bit ASCII range (at least, in English
                // versions of Xalan), this should work well enough to keep us
                // on the air until we're ready to officially decommit from
                // VJ++.

                BufferedReader reader;
                try {
                    reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    reader = new BufferedReader(new InputStreamReader(is));
                }

                String line = reader.readLine();

                while (line != null) {
                    if (line.length() == 0 || line.charAt(0) == '#') {
                        line = reader.readLine();

                        continue;
                    }

                    int index = line.indexOf(' ');

                    if (index > 1) {
                        String name = line.substring(0, index);

                        ++index;

                        if (index < line.length()) {
                            String value = line.substring(index);
                            index = value.indexOf(' ');

                            if (index > 0) {
                                value = value.substring(0, index);
                            }

                            int code = Integer.parseInt(value);

                            defineEntity(name, (char) code);
                            if (extraEntity(code))
                                noExtraEntities = false;
                        }
                    }

                    line = reader.readLine();
                }

                is.close();
                set(S_LINEFEED);
                set(S_CARRIAGERETURN);
            } catch (Exception e) {
                throw new RuntimeException(
                    Utils.messages.createMessage(
                        MsgKey.ER_RESOURCE_COULD_NOT_LOAD,
                        new Object[] { entitiesResource,
                                       e.toString(),
                                       entitiesResource,
                                       e.toString()}));
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (Exception except) {}
                }
            }
        }

        /* initialize the array isCleanTextASCII[] with a cache of values
         * for use by ToStream.character(char[], int , int)
         * and the array isSpecialTextASCII[] with the opposite values
         * (all in the name of performance!)
         */
        for (int ch = 0; ch <ASCII_MAX; ch++)
        if((((0x20 <= ch || (0x0A == ch || 0x0D == ch || 0x09 == ch)))
             && (!get(ch))) || ('"' == ch))
        {
            isCleanTextASCII[ch] = true;
            isSpecialTextASCII[ch] = false;
        }
        else {
            isCleanTextASCII[ch] = false;
            isSpecialTextASCII[ch] = true;
        }



        onlyQuotAmpLtGt = noExtraEntities;

        // initialize the array with a cache of the BitSet values
        for (int i=0; i<ASCII_MAX; i++)
            isSpecialAttrASCII[i] = get(i);

        /* Now that we've used get(ch) just above to initialize the
         * two arrays we will change by adding a tab to the set of
         * special chars for XML (but not HTML!).
         * We do this because a tab is always a
         * special character in an XML attribute,
         * but only a special character in XML text
         * if it has an entity defined for it.
         * This is the reason for this delay.
         */
        if (Method.XML.equals(method))
        {
            isSpecialAttrASCII[S_HORIZONAL_TAB] = true;
        }
    }

    /**
     * Defines a new character reference. The reference's name and value are
     * supplied. Nothing happens if the character reference is already defined.
     * <p>Unlike internal entities, character references are a string to single
     * character mapping. They are used to map non-ASCII characters both on
     * parsing and printing, primarily for HTML documents. '&lt;amp;' is an
     * example of a character reference.</p>
     *
     * @param name The entity's name
     * @param value The entity's value
     */
    private void defineEntity(String name, char value)
    {
        StringBuilder sb = new StringBuilder("&");
        sb.append(name);
        sb.append(';');
        String entityString = sb.toString();

        defineChar2StringMapping(entityString, value);
    }

    /**
     * Map a character to a String. For example given
     * the character '>' this method would return the fully decorated
     * entity name "&lt;".
     * Strings for entity references are loaded from a properties file,
     * but additional mappings defined through calls to defineChar2String()
     * are possible. Such entity reference mappings could be over-ridden.
     *
     * This is reusing a stored key object, in an effort to avoid
     * heap activity. Unfortunately, that introduces a threading risk.
     * Simplest fix for now is to make it a synchronized method, or to give
     * up the reuse; I see very little performance difference between them.
     * Long-term solution would be to replace the hashtable with a sparse array
     * keyed directly from the character's integer value; see DTM's
     * string pool for a related solution.
     *
     * @param value The character that should be resolved to
     * a String, e.g. resolve '>' to  "&lt;".
     *
     * @return The String that the character is mapped to, or null if not found.
     * @xsl.usage internal
     */
    String getOutputStringForChar(char value)
    {
        CharKey charKey = new CharKey();
        charKey.setChar(value);
        return m_charToString.get(charKey);
    }

    /**
     * Tell if the character argument that is from
     * an attribute value should have special treatment.
     *
     * @param value the value of a character that is in an attribute value
     * @return true if the character should have any special treatment,
     * such as when writing out attribute values,
     * or entity references.
     * @xsl.usage internal
     */
    final boolean isSpecialAttrChar(int value)
    {
        // for performance try the values in the boolean array first,
        // this is faster access than the BitSet for common ASCII values

        if (value < ASCII_MAX)
            return isSpecialAttrASCII[value];

        // rather than java.util.BitSet, our private
        // implementation is faster (and less general).
        return get(value);
    }

    /**
     * Tell if the character argument that is from a
     * text node should have special treatment.
     *
     * @param value the value of a character that is in a text node
     * @return true if the character should have any special treatment,
     * such as when writing out attribute values,
     * or entity references.
     * @xsl.usage internal
     */
    final boolean isSpecialTextChar(int value)
    {
        // for performance try the values in the boolean array first,
        // this is faster access than the BitSet for common ASCII values

        if (value < ASCII_MAX)
            return isSpecialTextASCII[value];

        // rather than java.util.BitSet, our private
        // implementation is faster (and less general).
        return get(value);
    }

    /**
     * This method is used to determine if an ASCII character in
     * a text node (not an attribute value) is "clean".
     * @param value the character to check (0 to 127).
     * @return true if the character can go to the writer as-is
     * @xsl.usage internal
     */
    final boolean isTextASCIIClean(int value)
    {
        return isCleanTextASCII[value];
    }


    /**
     * Read an internal resource file that describes the mapping of
     * characters to entity references; Construct a CharInfo object.
     *
     * @param entitiesFileName Name of entities resource file that should
     * be loaded, which describes the mapping of characters to entity references.
     * @param method the output method type, which should be one of "xml", "html", and "text".
     * @return an instance of CharInfo
     *
     * @xsl.usage internal
     */
    static CharInfo getCharInfoInternal(String entitiesFileName, String method)
    {
        CharInfo charInfo = m_getCharInfoCache.get(entitiesFileName);
        if (charInfo != null) {
            return charInfo;
        }

        charInfo = new CharInfo(entitiesFileName, method, true);
        m_getCharInfoCache.put(entitiesFileName, charInfo);
        return charInfo;
    }

    /**
     * Constructs a CharInfo object using the following process to try reading
     * the entitiesFileName parameter:
     *
     *   1) attempt to load it as a ResourceBundle
     *   2) try using the class loader to find the specified file
     *   3) try opening it as an URI
     *
     * In case of 2 and 3, the resource file must be encoded in UTF-8 and have the
     * following format:
     * <pre>
     * # First char # is a comment
     * Entity numericValue
     * quot 34
     * amp 38
     * </pre>
     *
     * @param entitiesFileName Name of entities resource file that should
     * be loaded, which describes the mapping of characters to entity references.
     * @param method the output method type, which should be one of "xml", "html", and "text".
     * @return an instance of CharInfo
     */
    static CharInfo getCharInfo(String entitiesFileName, String method)
    {
        try {
            return new CharInfo(entitiesFileName, method, false);
        } catch (Exception e) {}

        String absoluteEntitiesFileName;

        if (entitiesFileName.indexOf(':') < 0) {
            absoluteEntitiesFileName =
                SystemIDResolver.getAbsoluteURIFromRelative(entitiesFileName);
        } else {
            try {
                absoluteEntitiesFileName =
                    SystemIDResolver.getAbsoluteURI(entitiesFileName, null);
            } catch (TransformerException te) {
                throw new WrappedRuntimeException(te);
            }
        }

        return new CharInfo(absoluteEntitiesFileName, method, false);
    }

    /** Table of user-specified char infos. */
    private static Map<String, CharInfo> m_getCharInfoCache = new HashMap<>();

    /**
     * Returns the array element holding the bit value for the
     * given integer
     * @param i the integer that might be in the set of integers
     *
     */
    private static int arrayIndex(int i) {
        return (i >> SHIFT_PER_WORD);
    }

    /**
     * For a given integer in the set it returns the single bit
     * value used within a given word that represents whether
     * the integer is in the set or not.
     */
    private static int bit(int i) {
        int ret = (1 << (i & LOW_ORDER_BITMASK));
        return ret;
    }

    /**
     * Creates a new empty set of integers (characters)
     * @param max the maximum integer to be in the set.
     */
    private int[] createEmptySetOfIntegers(int max) {
        firstWordNotUsed = 0; // an optimization

        int[] arr = new int[arrayIndex(max - 1) + 1];
            return arr;

    }

    /**
     * Adds the integer (character) to the set of integers.
     * @param i the integer to add to the set, valid values are
     * 0, 1, 2 ... up to the maximum that was specified at
     * the creation of the set.
     */
    private final void set(int i) {
        setASCIIdirty(i);

        int j = (i >> SHIFT_PER_WORD); // this word is used
        int k = j + 1;

        if(firstWordNotUsed < k) // for optimization purposes.
            firstWordNotUsed = k;

        array_of_bits[j] |= (1 << (i & LOW_ORDER_BITMASK));
    }


    /**
     * Return true if the integer (character)is in the set of integers.
     *
     * This implementation uses an array of integers with 32 bits per
     * integer.  If a bit is set to 1 the corresponding integer is
     * in the set of integers.
     *
     * @param i an integer that is tested to see if it is the
     * set of integers, or not.
     */
    private final boolean get(int i) {

        boolean in_the_set = false;
        int j = (i >> SHIFT_PER_WORD); // wordIndex(i)
        // an optimization here, ... a quick test to see
        // if this integer is beyond any of the words in use
        if(j < firstWordNotUsed)
            in_the_set = (array_of_bits[j] &
                          (1 << (i & LOW_ORDER_BITMASK))
            ) != 0;  // 0L for 64 bit words
        return in_the_set;
    }

    // record if there are any entities other than
    // quot, amp, lt, gt  (probably user defined)
    /**
     * @return true if the entity
     * @param code The value of the character that has an entity defined
     * for it.
     */
    private boolean extraEntity(int entityValue)
    {
        boolean extra = false;
        if (entityValue < 128)
        {
            switch (entityValue)
            {
                case 34 : // quot
                case 38 : // amp
                case 60 : // lt
                case 62 : // gt
                    break;
                default : // other entity in range 0 to 127
                    extra = true;
            }
        }
        return extra;
    }

    /**
     * If the character is a printable ASCII character then
     * mark it as not clean and needing replacement with
     * a String on output.
     * @param ch
     */
    private void setASCIIdirty(int j)
    {
        if (0 <= j && j < ASCII_MAX)
        {
            isCleanTextASCII[j] = false;
            isSpecialTextASCII[j] = true;
        }
    }

    /**
     * If the character is a printable ASCII character then
     * mark it as and not needing replacement with
     * a String on output.
     * @param ch
     */
    private void setASCIIclean(int j)
    {
        if (0 <= j && j < ASCII_MAX)
        {
            isCleanTextASCII[j] = true;
            isSpecialTextASCII[j] = false;
        }
    }

    private void defineChar2StringMapping(String outputString, char inputChar)
    {
        CharKey character = new CharKey(inputChar);
        m_charToString.put(character, outputString);
        set(inputChar);
    }

    /**
     * Simple class for fast lookup of char values, when used with
     * hashtables.  You can set the char, then use it as a key.
     *
     * This class is a copy of the one in com.sun.org.apache.xml.internal.utils.
     * It exists to cut the serializers dependancy on that package.
     *
     * @xsl.usage internal
     */
    private static class CharKey extends Object
    {

      /** String value          */
      private char m_char;

      /**
       * Constructor CharKey
       *
       * @param key char value of this object.
       */
      public CharKey(char key)
      {
        m_char = key;
      }

      /**
       * Default constructor for a CharKey.
       *
       * @param key char value of this object.
       */
      public CharKey()
      {
      }

      /**
       * Get the hash value of the character.
       *
       * @return hash value of the character.
       */
      public final void setChar(char c)
      {
        m_char = c;
      }



      /**
       * Get the hash value of the character.
       *
       * @return hash value of the character.
       */
      public final int hashCode()
      {
        return (int)m_char;
      }

      /**
       * Override of equals() for this object
       *
       * @param obj to compare to
       *
       * @return True if this object equals this string value
       */
      public final boolean equals(Object obj)
      {
        return ((CharKey)obj).m_char == m_char;
      }
    }


}
