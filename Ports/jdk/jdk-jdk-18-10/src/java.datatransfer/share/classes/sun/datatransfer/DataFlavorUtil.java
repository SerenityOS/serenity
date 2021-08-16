/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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

package sun.datatransfer;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.FlavorMap;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Supplier;

/**
 * Utility class with different datatransfer helper functions.
 *
 * @since 9
 */
public class DataFlavorUtil {

    private DataFlavorUtil() {
        // Avoid instantiation
    }

    private static Comparator<String> getCharsetComparator() {
        return CharsetComparator.INSTANCE;
    }

    public static Comparator<DataFlavor> getDataFlavorComparator() {
        return DataFlavorComparator.INSTANCE;
    }

    public static Comparator<Long> getIndexOrderComparator(Map<Long, Integer> indexMap) {
        return new IndexOrderComparator(indexMap);
    }

    public static Comparator<DataFlavor> getTextFlavorComparator() {
        return TextFlavorComparator.INSTANCE;
    }

    /**
     * Tracks whether a particular text/* MIME type supports the charset
     * parameter. The Map is initialized with all of the standard MIME types
     * listed in the DataFlavor.selectBestTextFlavor method comment. Additional
     * entries may be added during the life of the JRE for text/&lt;other&gt;
     * types.
     */
    private static final Map<String, Boolean> textMIMESubtypeCharsetSupport;

    static {
        Map<String, Boolean> tempMap = new HashMap<>(17);
        tempMap.put("sgml", Boolean.TRUE);
        tempMap.put("xml", Boolean.TRUE);
        tempMap.put("html", Boolean.TRUE);
        tempMap.put("enriched", Boolean.TRUE);
        tempMap.put("richtext", Boolean.TRUE);
        tempMap.put("uri-list", Boolean.TRUE);
        tempMap.put("directory", Boolean.TRUE);
        tempMap.put("css", Boolean.TRUE);
        tempMap.put("calendar", Boolean.TRUE);
        tempMap.put("plain", Boolean.TRUE);
        tempMap.put("rtf", Boolean.FALSE);
        tempMap.put("tab-separated-values", Boolean.FALSE);
        tempMap.put("t140", Boolean.FALSE);
        tempMap.put("rfc822-headers", Boolean.FALSE);
        tempMap.put("parityfec", Boolean.FALSE);
        textMIMESubtypeCharsetSupport = Collections.synchronizedMap(tempMap);
    }

    /**
     * Lazy initialization of Standard Encodings.
     */
    private static class StandardEncodingsHolder {
        private static final SortedSet<String> standardEncodings = load();

        private static SortedSet<String> load() {
            final SortedSet<String> tempSet = new TreeSet<>(getCharsetComparator().reversed());
            tempSet.add("US-ASCII");
            tempSet.add("ISO-8859-1");
            tempSet.add("UTF-8");
            tempSet.add("UTF-16BE");
            tempSet.add("UTF-16LE");
            tempSet.add("UTF-16");
            tempSet.add(Charset.defaultCharset().name());
            return Collections.unmodifiableSortedSet(tempSet);
        }
    }

    /**
     * Returns a {@code SortedSet} of Strings which are a total order of the
     * standard character sets supported by the JRE. The ordering follows the
     * same principles as {@link DataFlavor#selectBestTextFlavor(DataFlavor[])}.
     * So as to avoid loading all available character converters, optional,
     * non-standard, character sets are not included.
     */
    public static Set<String> standardEncodings() {
        return StandardEncodingsHolder.standardEncodings;
    }

    /**
     * Converts an arbitrary text encoding to its canonical name.
     */
    public static String canonicalName(String encoding) {
        if (encoding == null) {
            return null;
        }
        try {
            return Charset.forName(encoding).name();
        } catch (IllegalCharsetNameException icne) {
            return encoding;
        } catch (UnsupportedCharsetException uce) {
            return encoding;
        }
    }

    /**
     * Tests only whether the flavor's MIME type supports the charset parameter.
     * Must only be called for flavors with a primary type of "text".
     */
    public static boolean doesSubtypeSupportCharset(DataFlavor flavor) {
        String subType = flavor.getSubType();
        if (subType == null) {
            return false;
        }

        Boolean support = textMIMESubtypeCharsetSupport.get(subType);

        if (support != null) {
            return support;
        }

        boolean ret_val = (flavor.getParameter("charset") != null);
        textMIMESubtypeCharsetSupport.put(subType, ret_val);
        return ret_val;
    }
    public static boolean doesSubtypeSupportCharset(String subType,
                                                    String charset)
    {
        Boolean support = textMIMESubtypeCharsetSupport.get(subType);

        if (support != null) {
            return support;
        }

        boolean ret_val = (charset != null);
        textMIMESubtypeCharsetSupport.put(subType, ret_val);
        return ret_val;
    }

    /**
     * Returns whether this flavor is a text type which supports the 'charset'
     * parameter.
     */
    public static boolean isFlavorCharsetTextType(DataFlavor flavor) {
        // Although stringFlavor doesn't actually support the charset
        // parameter (because its primary MIME type is not "text"), it should
        // be treated as though it does. stringFlavor is semantically
        // equivalent to "text/plain" data.
        if (DataFlavor.stringFlavor.equals(flavor)) {
            return true;
        }

        if (!"text".equals(flavor.getPrimaryType()) ||
                !doesSubtypeSupportCharset(flavor))
        {
            return false;
        }

        Class<?> rep_class = flavor.getRepresentationClass();

        if (flavor.isRepresentationClassReader() ||
                String.class.equals(rep_class) ||
                flavor.isRepresentationClassCharBuffer() ||
                char[].class.equals(rep_class))
        {
            return true;
        }

        if (!(flavor.isRepresentationClassInputStream() ||
                flavor.isRepresentationClassByteBuffer() ||
                byte[].class.equals(rep_class))) {
            return false;
        }

        String charset = flavor.getParameter("charset");

        // null equals default encoding which is always supported
        return (charset == null) || isEncodingSupported(charset);
    }

    /**
     * Returns whether this flavor is a text type which does not support the
     * 'charset' parameter.
     */
    public static boolean isFlavorNoncharsetTextType(DataFlavor flavor) {
        if (!"text".equals(flavor.getPrimaryType()) || doesSubtypeSupportCharset(flavor)) {
            return false;
        }

        return (flavor.isRepresentationClassInputStream() ||
                flavor.isRepresentationClassByteBuffer() ||
                byte[].class.equals(flavor.getRepresentationClass()));
    }

    /**
     * If the specified flavor is a text flavor which supports the "charset"
     * parameter, then this method returns that parameter, or the default
     * charset if no such parameter was specified at construction. For non-text
     * DataFlavors, and for non-charset text flavors, this method returns
     * {@code null}.
     */
    public static String getTextCharset(DataFlavor flavor) {
        if (!isFlavorCharsetTextType(flavor)) {
            return null;
        }

        String encoding = flavor.getParameter("charset");

        return (encoding != null) ? encoding : Charset.defaultCharset().name();
    }

    /**
     * Determines whether this JRE can both encode and decode text in the
     * specified encoding.
     */
    private static boolean isEncodingSupported(String encoding) {
        if (encoding == null) {
            return false;
        }
        try {
            return Charset.isSupported(encoding);
        } catch (IllegalCharsetNameException icne) {
            return false;
        }
    }

    /**
     * Helper method to compare two objects by their Integer indices in the
     * given map. If the map doesn't contain an entry for either of the objects,
     * the fallback index will be used for the object instead.
     *
     * @param  indexMap the map which maps objects into Integer indexes
     * @param  obj1 the first object to be compared
     * @param  obj2 the second object to be compared
     * @param  fallbackIndex the Integer to be used as a fallback index
     * @return a negative integer, zero, or a positive integer as the first
     *         object is mapped to a less, equal to, or greater index than the
     *         second
     */
    static <T> int compareIndices(Map<T, Integer> indexMap,
                                  T obj1, T obj2,
                                  Integer fallbackIndex) {
        Integer index1 = indexMap.getOrDefault(obj1, fallbackIndex);
        Integer index2 = indexMap.getOrDefault(obj2, fallbackIndex);
        return index1.compareTo(index2);
    }

    /**
     * An IndexedComparator which compares two String charsets. The comparison
     * follows the rules outlined in DataFlavor.selectBestTextFlavor. In order
     * to ensure that non-Unicode, non-ASCII, non-default charsets are sorted
     * in alphabetical order, charsets are not automatically converted to their
     * canonical forms.
     */
    private static class CharsetComparator implements Comparator<String> {
        static final CharsetComparator INSTANCE = new CharsetComparator();

        private static final Map<String, Integer> charsets;

        private static final Integer DEFAULT_CHARSET_INDEX = 2;
        private static final Integer OTHER_CHARSET_INDEX = 1;
        private static final Integer WORST_CHARSET_INDEX = 0;
        private static final Integer UNSUPPORTED_CHARSET_INDEX = Integer.MIN_VALUE;

        private static final String UNSUPPORTED_CHARSET = "UNSUPPORTED";

        static {
            Map<String, Integer> charsetsMap = new HashMap<>(8, 1.0f);

            // we prefer Unicode charsets
            charsetsMap.put(canonicalName("UTF-16LE"), 4);
            charsetsMap.put(canonicalName("UTF-16BE"), 5);
            charsetsMap.put(canonicalName("UTF-8"), 6);
            charsetsMap.put(canonicalName("UTF-16"), 7);

            // US-ASCII is the worst charset supported
            charsetsMap.put(canonicalName("US-ASCII"), WORST_CHARSET_INDEX);

            charsetsMap.putIfAbsent(Charset.defaultCharset().name(), DEFAULT_CHARSET_INDEX);

            charsetsMap.put(UNSUPPORTED_CHARSET, UNSUPPORTED_CHARSET_INDEX);

            charsets = Collections.unmodifiableMap(charsetsMap);
        }

        /**
         * Compares charsets. Returns a negative integer, zero, or a positive
         * integer as the first charset is worse than, equal to, or better than
         * the second.
         * <p>
         * Charsets are ordered according to the following rules:
         * <ul>
         * <li>All unsupported charsets are equal</li>
         * <li>Any unsupported charset is worse than any supported charset.
         * <li>Unicode charsets, such as "UTF-16", "UTF-8", "UTF-16BE" and
         *     "UTF-16LE", are considered best</li>
         * <li>After them, platform default charset is selected</li>
         * <li>"US-ASCII" is the worst of supported charsets</li>
         * <li>For all other supported charsets, the lexicographically less one
         *     is considered the better</li>
         * </ul>
         *
         * @param  charset1 the first charset to be compared
         * @param  charset2 the second charset to be compared
         * @return a negative integer, zero, or a positive integer as the first
         *         argument is worse, equal to, or better than the second
         */
        public int compare(String charset1, String charset2) {
            charset1 = getEncoding(charset1);
            charset2 = getEncoding(charset2);

            int comp = compareIndices(charsets, charset1, charset2, OTHER_CHARSET_INDEX);

            if (comp == 0) {
                return charset2.compareTo(charset1);
            }

            return comp;
        }

        /**
         * Returns encoding for the specified charset according to the following
         * rules:
         * <ul>
         * <li>If the charset is {@code null}, then {@code null} will be
         *     returned</li>
         * <li>Iff the charset specifies an encoding unsupported by this JRE,
         *     {@code UNSUPPORTED_CHARSET} will be returned</li>
         * <li>If the charset specifies an alias name, the corresponding
         *     canonical name will be returned iff the charset is a known
         *     Unicode, ASCII, or default charset</li>
         * </ul>
         *
         * @param  charset the charset
         * @return an encoding for this charset
         */
        static String getEncoding(String charset) {
            if (charset == null) {
                return null;
            } else if (!isEncodingSupported(charset)) {
                return UNSUPPORTED_CHARSET;
            } else {
                // Only convert to canonical form if the charset is one
                // of the charsets explicitly listed in the known charsets
                // map. This will happen only for Unicode, ASCII, or default
                // charsets.
                String canonicalName = canonicalName(charset);
                return (charsets.containsKey(canonicalName))
                        ? canonicalName
                        : charset;
            }
        }
    }

    /**
     * An IndexedComparator which compares two DataFlavors. For text flavors,
     * the comparison follows the rules outlined in
     * {@link DataFlavor#selectBestTextFlavor selectBestTextFlavor}. For
     * non-text flavors, unknown application MIME types are preferred, followed
     * by known application/x-java-* MIME types. Unknown application types are
     * preferred because if the user provides his own data flavor, it will
     * likely be the most descriptive one. For flavors which are otherwise
     * equal, the flavors' string representation are compared in the
     * alphabetical order.
     */
    private static class DataFlavorComparator implements Comparator<DataFlavor> {

        static final DataFlavorComparator INSTANCE = new DataFlavorComparator();

        private static final Map<String, Integer> exactTypes;
        private static final Map<String, Integer> primaryTypes;
        private static final Map<Class<?>, Integer> nonTextRepresentations;
        private static final Map<String, Integer> textTypes;
        private static final Map<Class<?>, Integer> decodedTextRepresentations;
        private static final Map<Class<?>, Integer> encodedTextRepresentations;

        private static final Integer UNKNOWN_OBJECT_LOSES = Integer.MIN_VALUE;
        private static final Integer UNKNOWN_OBJECT_WINS = Integer.MAX_VALUE;

        static {
            {
                Map<String, Integer> exactTypesMap = new HashMap<>(4, 1.0f);

                // application/x-java-* MIME types
                exactTypesMap.put("application/x-java-file-list", 0);
                exactTypesMap.put("application/x-java-serialized-object", 1);
                exactTypesMap.put("application/x-java-jvm-local-objectref", 2);
                exactTypesMap.put("application/x-java-remote-object", 3);

                exactTypes = Collections.unmodifiableMap(exactTypesMap);
            }

            {
                Map<String, Integer> primaryTypesMap = new HashMap<>(1, 1.0f);

                primaryTypesMap.put("application", 0);

                primaryTypes = Collections.unmodifiableMap(primaryTypesMap);
            }

            {
                Map<Class<?>, Integer> nonTextRepresentationsMap = new HashMap<>(3, 1.0f);

                nonTextRepresentationsMap.put(java.io.InputStream.class, 0);
                nonTextRepresentationsMap.put(java.io.Serializable.class, 1);

                nonTextRepresentationsMap.put(RMI.remoteClass(), 2);

                nonTextRepresentations = Collections.unmodifiableMap(nonTextRepresentationsMap);
            }

            {
                Map<String, Integer> textTypesMap = new HashMap<>(16, 1.0f);

                // plain text
                textTypesMap.put("text/plain", 0);

                // stringFlavor
                textTypesMap.put("application/x-java-serialized-object", 1);

                // misc
                textTypesMap.put("text/calendar", 2);
                textTypesMap.put("text/css", 3);
                textTypesMap.put("text/directory", 4);
                textTypesMap.put("text/parityfec", 5);
                textTypesMap.put("text/rfc822-headers", 6);
                textTypesMap.put("text/t140", 7);
                textTypesMap.put("text/tab-separated-values", 8);
                textTypesMap.put("text/uri-list", 9);

                // enriched
                textTypesMap.put("text/richtext", 10);
                textTypesMap.put("text/enriched", 11);
                textTypesMap.put("text/rtf", 12);

                // markup
                textTypesMap.put("text/html", 13);
                textTypesMap.put("text/xml", 14);
                textTypesMap.put("text/sgml", 15);

                textTypes = Collections.unmodifiableMap(textTypesMap);
            }

            {
                Map<Class<?>, Integer> decodedTextRepresentationsMap = new HashMap<>(4, 1.0f);

                decodedTextRepresentationsMap.put(char[].class, 0);
                decodedTextRepresentationsMap.put(CharBuffer.class, 1);
                decodedTextRepresentationsMap.put(String.class, 2);
                decodedTextRepresentationsMap.put(Reader.class, 3);

                decodedTextRepresentations =
                        Collections.unmodifiableMap(decodedTextRepresentationsMap);
            }

            {
                Map<Class<?>, Integer> encodedTextRepresentationsMap = new HashMap<>(3, 1.0f);

                encodedTextRepresentationsMap.put(byte[].class, 0);
                encodedTextRepresentationsMap.put(ByteBuffer.class, 1);
                encodedTextRepresentationsMap.put(InputStream.class, 2);

                encodedTextRepresentations =
                        Collections.unmodifiableMap(encodedTextRepresentationsMap);
            }
        }


        public int compare(DataFlavor flavor1, DataFlavor flavor2) {
            if (flavor1.equals(flavor2)) {
                return 0;
            }

            int comp;

            String primaryType1 = flavor1.getPrimaryType();
            String subType1 = flavor1.getSubType();
            String mimeType1 = primaryType1 + "/" + subType1;
            Class<?> class1 = flavor1.getRepresentationClass();

            String primaryType2 = flavor2.getPrimaryType();
            String subType2 = flavor2.getSubType();
            String mimeType2 = primaryType2 + "/" + subType2;
            Class<?> class2 = flavor2.getRepresentationClass();

            if (flavor1.isFlavorTextType() && flavor2.isFlavorTextType()) {
                // First, compare MIME types
                comp = compareIndices(textTypes, mimeType1, mimeType2, UNKNOWN_OBJECT_LOSES);
                if (comp != 0) {
                    return comp;
                }

                // Only need to test one flavor because they both have the
                // same MIME type. Also don't need to worry about accidentally
                // passing stringFlavor because either
                //   1. Both flavors are stringFlavor, in which case the
                //      equality test at the top of the function succeeded.
                //   2. Only one flavor is stringFlavor, in which case the MIME
                //      type comparison returned a non-zero value.
                if (doesSubtypeSupportCharset(flavor1)) {
                    // Next, prefer the decoded text representations of Reader,
                    // String, CharBuffer, and [C, in that order.
                    comp = compareIndices(decodedTextRepresentations, class1,
                            class2, UNKNOWN_OBJECT_LOSES);
                    if (comp != 0) {
                        return comp;
                    }

                    // Next, compare charsets
                    comp = CharsetComparator.INSTANCE.compare(getTextCharset(flavor1),
                            getTextCharset(flavor2));
                    if (comp != 0) {
                        return comp;
                    }
                }

                // Finally, prefer the encoded text representations of
                // InputStream, ByteBuffer, and [B, in that order.
                comp = compareIndices(encodedTextRepresentations, class1,
                        class2, UNKNOWN_OBJECT_LOSES);
                if (comp != 0) {
                    return comp;
                }
            } else {
                // First, prefer text types
                if (flavor1.isFlavorTextType()) {
                    return 1;
                }

                if (flavor2.isFlavorTextType()) {
                    return -1;
                }

                // Next, prefer application types.
                comp = compareIndices(primaryTypes, primaryType1, primaryType2,
                        UNKNOWN_OBJECT_LOSES);
                if (comp != 0) {
                    return comp;
                }

                // Next, look for application/x-java-* types. Prefer unknown
                // MIME types because if the user provides his own data flavor,
                // it will likely be the most descriptive one.
                comp = compareIndices(exactTypes, mimeType1, mimeType2,
                        UNKNOWN_OBJECT_WINS);
                if (comp != 0) {
                    return comp;
                }

                // Finally, prefer the representation classes of Remote,
                // Serializable, and InputStream, in that order.
                comp = compareIndices(nonTextRepresentations, class1, class2,
                        UNKNOWN_OBJECT_LOSES);
                if (comp != 0) {
                    return comp;
                }
            }

            // The flavours are not equal but still not distinguishable.
            // Compare String representations in alphabetical order
            return flavor1.getMimeType().compareTo(flavor2.getMimeType());
        }
    }

    /**
     * Given the Map that maps objects to Integer indices and a boolean value,
     * this Comparator imposes a direct or reverse order on set of objects.
     * <p>
     * If the specified boolean value is SELECT_BEST, the Comparator imposes the
     * direct index-based order: an object A is greater than an object B if and
     * only if the index of A is greater than the index of B. An object that
     * doesn't have an associated index is less or equal than any other object.
     * <p>
     * If the specified boolean value is SELECT_WORST, the Comparator imposes
     * the reverse index-based order: an object A is greater than an object B if
     * and only if A is less than B with the direct index-based order.
     */
    private static class IndexOrderComparator implements Comparator<Long> {
        private final Map<Long, Integer> indexMap;
        private static final Integer FALLBACK_INDEX = Integer.MIN_VALUE;

        public IndexOrderComparator(Map<Long, Integer> indexMap) {
            this.indexMap = indexMap;
        }

        public int compare(Long obj1, Long obj2) {
            return compareIndices(indexMap, obj1, obj2, FALLBACK_INDEX);
        }
    }

    private static class TextFlavorComparator extends DataFlavorComparator {

        static final TextFlavorComparator INSTANCE = new TextFlavorComparator();

        /**
         * Compares two {@code DataFlavor} objects. Returns a negative integer,
         * zero, or a positive integer as the first {@code DataFlavor} is worse
         * than, equal to, or better than the second.
         * <p>
         * {@code DataFlavor}s are ordered according to the rules outlined for
         * {@link DataFlavor#selectBestTextFlavor selectBestTextFlavor}.
         *
         * @param  flavor1 the first {@code DataFlavor} to be compared
         * @param  flavor2 the second {@code DataFlavor} to be compared
         * @return a negative integer, zero, or a positive integer as the first
         *         argument is worse, equal to, or better than the second
         * @throws ClassCastException if either of the arguments is not an
         *         instance of {@code DataFlavor}
         * @throws NullPointerException if either of the arguments is
         *         {@code null}
         * @see DataFlavor#selectBestTextFlavor
         */
        public int compare(DataFlavor flavor1, DataFlavor flavor2) {
            if (flavor1.isFlavorTextType()) {
                if (flavor2.isFlavorTextType()) {
                    return super.compare(flavor1, flavor2);
                } else {
                    return 1;
                }
            } else if (flavor2.isFlavorTextType()) {
                return -1;
            } else {
                return 0;
            }
        }
    }

    /**
     * A fallback implementation of {@link DesktopDatatransferService} used if
     * there is no desktop.
     */
    private static final class DefaultDesktopDatatransferService implements DesktopDatatransferService {
        static final DesktopDatatransferService INSTANCE = getDesktopService();

        private static DesktopDatatransferService getDesktopService() {
            ServiceLoader<DesktopDatatransferService> loader =
                    ServiceLoader.load(DesktopDatatransferService.class, null);
            Iterator<DesktopDatatransferService> iterator = loader.iterator();
            if (iterator.hasNext()) {
                return iterator.next();
            } else {
                return new DefaultDesktopDatatransferService();
            }
        }

        /**
         * System singleton FlavorTable. Only used if there is no desktop to
         * provide an appropriate FlavorMap.
         */
        private volatile FlavorMap flavorMap;

        @Override
        public void invokeOnEventThread(Runnable r) {
            r.run();
        }

        @Override
        public String getDefaultUnicodeEncoding() {
            return StandardCharsets.UTF_8.name();
        }

        @Override
        public FlavorMap getFlavorMap(Supplier<FlavorMap> supplier) {
            FlavorMap map = flavorMap;
            if (map == null) {
                synchronized (this) {
                    map = flavorMap;
                    if (map == null) {
                        flavorMap = map = supplier.get();
                    }
                }
            }
            return map;
        }

        @Override
        public boolean isDesktopPresent() {
            return false;
        }

        @Override
        public LinkedHashSet<DataFlavor> getPlatformMappingsForNative(String nat) {
            return new LinkedHashSet<>();
        }

        @Override
        public LinkedHashSet<String> getPlatformMappingsForFlavor(DataFlavor df) {
            return new LinkedHashSet<>();
        }

        @Override
        public void registerTextFlavorProperties(String nat, String charset,
                                                 String eoln, String terminators) {
            // Not needed if desktop module is absent
        }
    }

    public static DesktopDatatransferService getDesktopService() {
        return DefaultDesktopDatatransferService.INSTANCE;
    }

    /**
     * A class that provides access to {@code java.rmi.Remote} and
     * {@code java.rmi.MarshalledObject} without creating a static dependency.
     */
    public static class RMI {
        private static final Class<?> remoteClass = getClass("java.rmi.Remote");
        private static final Class<?> marshallObjectClass = getClass("java.rmi.MarshalledObject");
        private static final Constructor<?> marshallCtor = getConstructor(marshallObjectClass, Object.class);
        private static final Method marshallGet = getMethod(marshallObjectClass, "get");

        private static Class<?> getClass(String name) {
            try {
                return Class.forName(name, true, null);
            } catch (ClassNotFoundException e) {
                return null;
            }
        }

        private static Constructor<?> getConstructor(Class<?> c, Class<?>... types) {
            try {
                return (c == null) ? null : c.getDeclaredConstructor(types);
            } catch (NoSuchMethodException x) {
                throw new AssertionError(x);
            }
        }

        private static Method getMethod(Class<?> c, String name, Class<?>... types) {
            try {
                return (c == null) ? null : c.getMethod(name, types);
            } catch (NoSuchMethodException e) {
                throw new AssertionError(e);
            }
        }

        /**
         * Returns {@code java.rmi.Remote.class} if RMI is present; otherwise
         * {@code null}.
         */
        static Class<?> remoteClass() {
            return remoteClass;
        }

        /**
         * Returns {@code true} if the given class is java.rmi.Remote.
         */
        public static boolean isRemote(Class<?> c) {
            return (remoteClass != null) && remoteClass.isAssignableFrom(c);
        }

        /**
         * Returns a new MarshalledObject containing the serialized
         * representation of the given object.
         */
        public static Object newMarshalledObject(Object obj) throws IOException {
            try {
                return marshallCtor == null ? null : marshallCtor.newInstance(obj);
            } catch (InstantiationException | IllegalAccessException x) {
                throw new AssertionError(x);
            } catch (InvocationTargetException x) {
                Throwable cause = x.getCause();
                if (cause instanceof IOException)
                    throw (IOException) cause;
                throw new AssertionError(x);
            }
        }

        /**
         * Returns a new copy of the contained marshalled object.
         */
        public static Object getMarshalledObject(Object obj)
                throws IOException, ClassNotFoundException {
            try {
                return marshallGet == null ? null : marshallGet.invoke(obj);
            } catch (IllegalAccessException x) {
                throw new AssertionError(x);
            } catch (InvocationTargetException x) {
                Throwable cause = x.getCause();
                if (cause instanceof IOException)
                    throw (IOException) cause;
                if (cause instanceof ClassNotFoundException)
                    throw (ClassNotFoundException) cause;
                throw new AssertionError(x);
            }
        }
    }
}
