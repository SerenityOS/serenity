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

package sun.awt.datatransfer;

import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.FlavorMap;
import java.awt.datatransfer.FlavorTable;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ImageObserver;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FilePermission;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Reader;
import java.io.SequenceInputStream;
import java.io.StringReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.ProtectionDomain;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.Stack;
import java.util.TreeMap;
import java.util.stream.Stream;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;
import javax.imageio.spi.ImageWriterSpi;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;

import sun.awt.AppContext;
import sun.awt.ComponentFactory;
import sun.awt.SunToolkit;
import sun.awt.image.ImageRepresentation;
import sun.awt.image.ToolkitImage;
import sun.datatransfer.DataFlavorUtil;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Provides a set of functions to be shared among the DataFlavor class and
 * platform-specific data transfer implementations.
 *
 * The concept of "flavors" and "natives" is extended to include "formats",
 * which are the numeric values Win32 and X11 use to express particular data
 * types. Like FlavorMap, which provides getNativesForFlavors(DataFlavor[]) and
 * getFlavorsForNatives(String[]) functions, DataTransferer provides a set
 * of getFormatsFor(Transferable|Flavor|Flavors) and
 * getFlavorsFor(Format|Formats) functions.
 *
 * Also provided are functions for translating a Transferable into a byte
 * array, given a source DataFlavor and a target format, and for translating
 * a byte array or InputStream into an Object, given a source format and
 * a target DataFlavor.
 *
 * @author David Mendenhall
 * @author Danila Sinopalnikov
 *
 * @since 1.3.1
 */
public abstract class DataTransferer {
    /**
     * The {@code DataFlavor} representing a Java text encoding String
     * encoded in UTF-8, where
     * <pre>
     *     representationClass = [B
     *     mimeType            = "application/x-java-text-encoding"
     * </pre>
     */
    public static final DataFlavor javaTextEncodingFlavor;

    /**
     * A collection of all natives listed in flavormap.properties with
     * a primary MIME type of "text".
     */
    private static final Set<Long> textNatives =
            Collections.synchronizedSet(new HashSet<>());

    /**
     * The native encodings/charsets for the Set of textNatives.
     */
    private static final Map<Long, String> nativeCharsets =
            Collections.synchronizedMap(new HashMap<>());

    /**
     * The end-of-line markers for the Set of textNatives.
     */
    private static final Map<Long, String> nativeEOLNs =
            Collections.synchronizedMap(new HashMap<>());

    /**
     * The number of terminating NUL bytes for the Set of textNatives.
     */
    private static final Map<Long, Integer> nativeTerminators =
            Collections.synchronizedMap(new HashMap<>());

    /**
     * The key used to store pending data conversion requests for an AppContext.
     */
    private static final String DATA_CONVERTER_KEY = "DATA_CONVERTER_KEY";

    static {
        DataFlavor tJavaTextEncodingFlavor = null;
        try {
            tJavaTextEncodingFlavor = new DataFlavor("application/x-java-text-encoding;class=\"[B\"");
        } catch (ClassNotFoundException cannotHappen) {
        }
        javaTextEncodingFlavor = tJavaTextEncodingFlavor;
    }

    /**
     * The accessor method for the singleton DataTransferer instance. Note
     * that in a headless environment, there may be no DataTransferer instance;
     * instead, null will be returned.
     */
    public static synchronized DataTransferer getInstance() {
        return ((ComponentFactory) Toolkit.getDefaultToolkit()).getDataTransferer();
    }

    /**
     * Converts a FlavorMap to a FlavorTable.
     */
    public static FlavorTable adaptFlavorMap(final FlavorMap map) {
        if (map instanceof FlavorTable) {
            return (FlavorTable)map;
        }

        return new FlavorTable() {
            @Override
            public Map<DataFlavor, String> getNativesForFlavors(DataFlavor[] flavors) {
                return map.getNativesForFlavors(flavors);
            }
            @Override
            public Map<String, DataFlavor> getFlavorsForNatives(String[] natives) {
                return map.getFlavorsForNatives(natives);
            }
            @Override
            public List<String> getNativesForFlavor(DataFlavor flav) {
                Map<DataFlavor, String> natives = getNativesForFlavors(new DataFlavor[]{flav});
                String nat = natives.get(flav);
                if (nat != null) {
                    return Collections.singletonList(nat);
                } else {
                    return Collections.emptyList();
                }
            }
            @Override
            public List<DataFlavor> getFlavorsForNative(String nat) {
                Map<String, DataFlavor> flavors = getFlavorsForNatives(new String[]{nat});
                DataFlavor flavor = flavors.get(nat);
                if (flavor != null) {
                    return Collections.singletonList(flavor);
                } else {
                    return Collections.emptyList();
                }
            }
        };
    }

    /**
     * Returns the default Unicode encoding for the platform. The encoding
     * need not be canonical. This method is only used by the archaic function
     * DataFlavor.getTextPlainUnicodeFlavor().
     */
    public abstract String getDefaultUnicodeEncoding();

    /**
     * This method is called for text flavor mappings established while parsing
     * the flavormap.properties file. It stores the "eoln" and "terminators"
     * parameters which are not officially part of the MIME type. They are
     * MIME parameters specific to the flavormap.properties file format.
     */
    public void registerTextFlavorProperties(String nat, String charset,
                                             String eoln, String terminators) {
        Long format = getFormatForNativeAsLong(nat);

        textNatives.add(format);
        nativeCharsets.put(format, (charset != null && charset.length() != 0)
                ? charset : Charset.defaultCharset().name());
        if (eoln != null && eoln.length() != 0 && !eoln.equals("\n")) {
            nativeEOLNs.put(format, eoln);
        }
        if (terminators != null && terminators.length() != 0) {
            Integer iTerminators = Integer.valueOf(terminators);
            if (iTerminators > 0) {
                nativeTerminators.put(format, iTerminators);
            }
        }
    }

    /**
     * Determines whether the native corresponding to the specified long format
     * was listed in the flavormap.properties file.
     */
    protected boolean isTextFormat(long format) {
        return textNatives.contains(Long.valueOf(format));
    }

    protected String getCharsetForTextFormat(Long lFormat) {
        return nativeCharsets.get(lFormat);
    }

    /**
     * Specifies whether text imported from the native system in the specified
     * format is locale-dependent. If so, when decoding such text,
     * 'nativeCharsets' should be ignored, and instead, the Transferable should
     * be queried for its javaTextEncodingFlavor data for the correct encoding.
     */
    public abstract boolean isLocaleDependentTextFormat(long format);

    /**
     * Determines whether the DataFlavor corresponding to the specified long
     * format is DataFlavor.javaFileListFlavor.
     */
    public abstract boolean isFileFormat(long format);

    /**
     * Determines whether the DataFlavor corresponding to the specified long
     * format is DataFlavor.imageFlavor.
     */
    public abstract boolean isImageFormat(long format);

    /**
     * Determines whether the format is a URI list we can convert to
     * a DataFlavor.javaFileListFlavor.
     */
    protected boolean isURIListFormat(long format) {
        return false;
    }

    /**
     * Returns a Map whose keys are all of the possible formats into which the
     * Transferable's transfer data flavors can be translated. The value of
     * each key is the DataFlavor in which the Transferable's data should be
     * requested when converting to the format.
     * <p>
     * The map keys are sorted according to the native formats preference
     * order.
     */
    public SortedMap<Long,DataFlavor> getFormatsForTransferable(Transferable contents,
                                                                FlavorTable map)
    {
        DataFlavor[] flavors = contents.getTransferDataFlavors();
        if (flavors == null) {
            return Collections.emptySortedMap();
        }
        return getFormatsForFlavors(flavors, map);
    }

    /**
     * Returns a Map whose keys are all of the possible formats into which data
     * in the specified DataFlavors can be translated. The value of each key
     * is the DataFlavor in which the Transferable's data should be requested
     * when converting to the format.
     * <p>
     * The map keys are sorted according to the native formats preference
     * order.
     *
     * @param flavors the data flavors
     * @param map the FlavorTable which contains mappings between
     *            DataFlavors and data formats
     * @throws NullPointerException if flavors or map is {@code null}
     */
    public SortedMap<Long, DataFlavor> getFormatsForFlavors(DataFlavor[] flavors,
                                                            FlavorTable map)
    {
        Map<Long,DataFlavor> formatMap = new HashMap<>(flavors.length);
        Map<Long,DataFlavor> textPlainMap = new HashMap<>(flavors.length);
        // Maps formats to indices that will be used to sort the formats
        // according to the preference order.
        // Larger index value corresponds to the more preferable format.
        Map<Long, Integer> indexMap = new HashMap<>(flavors.length);
        Map<Long, Integer> textPlainIndexMap = new HashMap<>(flavors.length);

        int currentIndex = 0;

        // Iterate backwards so that preferred DataFlavors are used over
        // other DataFlavors. (See javadoc for
        // Transferable.getTransferDataFlavors.)
        for (int i = flavors.length - 1; i >= 0; i--) {
            DataFlavor flavor = flavors[i];
            if (flavor == null) continue;

            // Don't explicitly test for String, since it is just a special
            // case of Serializable
            if (flavor.isFlavorTextType() ||
                flavor.isFlavorJavaFileListType() ||
                DataFlavor.imageFlavor.equals(flavor) ||
                flavor.isRepresentationClassSerializable() ||
                flavor.isRepresentationClassInputStream() ||
                flavor.isRepresentationClassRemote())
            {
                List<String> natives = map.getNativesForFlavor(flavor);

                currentIndex += natives.size();

                for (String aNative : natives) {
                    Long lFormat = getFormatForNativeAsLong(aNative);
                    Integer index = currentIndex--;

                    formatMap.put(lFormat, flavor);
                    indexMap.put(lFormat, index);

                    // SystemFlavorMap.getNativesForFlavor will return
                    // text/plain natives for all text/*. While this is good
                    // for a single text/* flavor, we would prefer that
                    // text/plain native data come from a text/plain flavor.
                    if (("text".equals(flavor.getPrimaryType()) &&
                            "plain".equals(flavor.getSubType())) ||
                            flavor.equals(DataFlavor.stringFlavor)) {
                        textPlainMap.put(lFormat, flavor);
                        textPlainIndexMap.put(lFormat, index);
                    }
                }

                currentIndex += natives.size();
            }
        }

        formatMap.putAll(textPlainMap);
        indexMap.putAll(textPlainIndexMap);

        // Sort the map keys according to the formats preference order.
        Comparator<Long> comparator = DataFlavorUtil.getIndexOrderComparator(indexMap).reversed();
        SortedMap<Long, DataFlavor> sortedMap = new TreeMap<>(comparator);
        sortedMap.putAll(formatMap);

        return sortedMap;
    }

    /**
     * Reduces the Map output for the root function to an array of the
     * Map's keys.
     */
    public long[] getFormatsForTransferableAsArray(Transferable contents,
                                                   FlavorTable map) {
        return keysToLongArray(getFormatsForTransferable(contents, map));
    }

    /**
     * Returns a Map whose keys are all of the possible DataFlavors into which
     * data in the specified formats can be translated. The value of each key
     * is the format in which the Clipboard or dropped data should be requested
     * when converting to the DataFlavor.
     */
    public Map<DataFlavor, Long> getFlavorsForFormats(long[] formats, FlavorTable map) {
        Map<DataFlavor, Long> flavorMap = new HashMap<>(formats.length);
        Set<AbstractMap.SimpleEntry<Long, DataFlavor>> mappingSet = new HashSet<>(formats.length);
        Set<DataFlavor> flavorSet = new HashSet<>(formats.length);

        // First step: build flavorSet, mappingSet and initial flavorMap
        // flavorSet  - the set of all the DataFlavors into which
        //              data in the specified formats can be translated;
        // mappingSet - the set of all the mappings from the specified formats
        //              into any DataFlavor;
        // flavorMap  - after this step, this map maps each of the DataFlavors
        //              from flavorSet to any of the specified formats.
        for (long format : formats) {
            String nat = getNativeForFormat(format);
            List<DataFlavor> flavors = map.getFlavorsForNative(nat);
            for (DataFlavor flavor : flavors) {
                // Don't explicitly test for String, since it is just a special
                // case of Serializable
                if (flavor.isFlavorTextType() ||
                        flavor.isFlavorJavaFileListType() ||
                        DataFlavor.imageFlavor.equals(flavor) ||
                        flavor.isRepresentationClassSerializable() ||
                        flavor.isRepresentationClassInputStream() ||
                        flavor.isRepresentationClassRemote()) {

                    AbstractMap.SimpleEntry<Long, DataFlavor> mapping =
                            new AbstractMap.SimpleEntry<>(format, flavor);
                    flavorMap.put(flavor, format);
                    mappingSet.add(mapping);
                    flavorSet.add(flavor);
                }
            }
        }

        // Second step: for each DataFlavor try to figure out which of the
        // specified formats is the best to translate to this flavor.
        // Then map each flavor to the best format.
        // For the given flavor, FlavorTable indicates which native will
        // best reflect data in the specified flavor to the underlying native
        // platform. We assume that this native is the best to translate
        // to this flavor.
        // Note: FlavorTable allows one-way mappings, so we can occasionally
        // map a flavor to the format for which the corresponding
        // format-to-flavor mapping doesn't exist. For this reason we have built
        // a mappingSet of all format-to-flavor mappings for the specified formats
        // and check if the format-to-flavor mapping exists for the
        // (flavor,format) pair being added.
        for (DataFlavor flavor : flavorSet) {
            List<String> natives = map.getNativesForFlavor(flavor);
            for (String aNative : natives) {
                Long lFormat = getFormatForNativeAsLong(aNative);
                if (mappingSet.contains(new AbstractMap.SimpleEntry<>(lFormat, flavor))) {
                    flavorMap.put(flavor, lFormat);
                    break;
                }
            }
        }

        return flavorMap;
    }

    /**
     * Returns a Set of all DataFlavors for which
     * 1) a mapping from at least one of the specified formats exists in the
     * specified map and
     * 2) the data translation for this mapping can be performed by the data
     * transfer subsystem.
     *
     * @param formats the data formats
     * @param map the FlavorTable which contains mappings between
     *            DataFlavors and data formats
     * @throws NullPointerException if formats or map is {@code null}
     */
    public Set<DataFlavor> getFlavorsForFormatsAsSet(long[] formats, FlavorTable map) {
        Set<DataFlavor> flavorSet = new HashSet<>(formats.length);

        for (long format : formats) {
            List<DataFlavor> flavors = map.getFlavorsForNative(getNativeForFormat(format));
            for (DataFlavor flavor : flavors) {
                // Don't explicitly test for String, since it is just a special
                // case of Serializable
                if (flavor.isFlavorTextType() ||
                        flavor.isFlavorJavaFileListType() ||
                        DataFlavor.imageFlavor.equals(flavor) ||
                        flavor.isRepresentationClassSerializable() ||
                        flavor.isRepresentationClassInputStream() ||
                        flavor.isRepresentationClassRemote()) {
                    flavorSet.add(flavor);
                }
            }
        }

        return flavorSet;
    }

    /**
     * Returns an array of all DataFlavors for which
     * 1) a mapping from at least one of the specified formats exists in the
     * specified map and
     * 2) the data translation for this mapping can be performed by the data
     * transfer subsystem.
     * The array will be sorted according to a
     * {@code DataFlavorComparator} created with the specified
     * map as an argument.
     *
     * @param formats the data formats
     * @param map the FlavorTable which contains mappings between
     *            DataFlavors and data formats
     * @throws NullPointerException if formats or map is {@code null}
     */
    public DataFlavor[] getFlavorsForFormatsAsArray(long[] formats,
                                                    FlavorTable map) {
        // getFlavorsForFormatsAsSet() is less expensive than
        // getFlavorsForFormats().
        return setToSortedDataFlavorArray(getFlavorsForFormatsAsSet(formats, map));
    }

    /**
     * Looks-up or registers the String native with the native data transfer
     * system and returns a long format corresponding to that native.
     */
    protected abstract Long getFormatForNativeAsLong(String str);

    /**
     * Looks-up the String native corresponding to the specified long format in
     * the native data transfer system.
     */
    protected abstract String getNativeForFormat(long format);

    /* Contains common code for finding the best charset for
     * clipboard string encoding/decoding, basing on clipboard
     * format and localeTransferable(on decoding, if available)
     */
    protected String getBestCharsetForTextFormat(Long lFormat,
        Transferable localeTransferable) throws IOException
    {
        String charset = null;
        if (localeTransferable != null &&
            isLocaleDependentTextFormat(lFormat) &&
            localeTransferable.isDataFlavorSupported(javaTextEncodingFlavor)) {
            try {
                byte[] charsetNameBytes = (byte[])localeTransferable
                        .getTransferData(javaTextEncodingFlavor);
                charset = new String(charsetNameBytes, UTF_8);
            } catch (UnsupportedFlavorException cannotHappen) {
            }
        } else {
            charset = getCharsetForTextFormat(lFormat);
        }
        if (charset == null) {
            // Only happens when we have a custom text type.
            charset = Charset.defaultCharset().name();
        }
        return charset;
    }

    /**
     *  Translation function for converting string into
     *  a byte array. Search-and-replace EOLN. Encode into the
     *  target format. Append terminating NUL bytes.
     *
     *  Java to Native string conversion
     */
    private byte[] translateTransferableString(String str,
                                               long format) throws IOException
    {
        Long lFormat = format;
        String charset = getBestCharsetForTextFormat(lFormat, null);
        // Search and replace EOLN. Note that if EOLN is "\n", then we
        // never added an entry to nativeEOLNs anyway, so we'll skip this
        // code altogether.
        // windows: "abc\nde"->"abc\r\nde"
        String eoln = nativeEOLNs.get(lFormat);
        if (eoln != null) {
            int length = str.length();
            StringBuilder buffer = new StringBuilder(length * 2); // 2 is a heuristic
            for (int i = 0; i < length; i++) {
                // Fix for 4914613 - skip native EOLN
                if (str.startsWith(eoln, i)) {
                    buffer.append(eoln);
                    i += eoln.length() - 1;
                    continue;
                }
                char c = str.charAt(i);
                if (c == '\n') {
                    buffer.append(eoln);
                } else {
                    buffer.append(c);
                }
            }
            str = buffer.toString();
        }

        // Encode text in target format.
        byte[] bytes = str.getBytes(charset);

        // Append terminating NUL bytes. Note that if terminators is 0,
        // the we never added an entry to nativeTerminators anyway, so
        // we'll skip code altogether.
        // "abcde" -> "abcde\0"
        Integer terminators = nativeTerminators.get(lFormat);
        if (terminators != null) {
            int numTerminators = terminators;
            byte[] terminatedBytes =
                new byte[bytes.length + numTerminators];
            System.arraycopy(bytes, 0, terminatedBytes, 0, bytes.length);
            for (int i = bytes.length; i < terminatedBytes.length; i++) {
                terminatedBytes[i] = 0x0;
            }
            bytes = terminatedBytes;
        }
        return bytes;
    }

    /**
     * Translating either a byte array or an InputStream into an String.
     * Strip terminators and search-and-replace EOLN.
     *
     * Native to Java string conversion
     */
    private String translateBytesToString(byte[] bytes, long format,
                                          Transferable localeTransferable)
            throws IOException
    {

        Long lFormat = format;
        String charset = getBestCharsetForTextFormat(lFormat, localeTransferable);

        // Locate terminating NUL bytes. Note that if terminators is 0,
        // the we never added an entry to nativeTerminators anyway, so
        // we'll skip code altogether.

        // In other words: we are doing char alignment here basing on suggestion
        // that count of zero-'terminators' is a number of bytes in one symbol
        // for selected charset (clipboard format). It is not complitly true for
        // multibyte coding like UTF-8, but helps understand the procedure.
        // "abcde\0" -> "abcde"

        String eoln = nativeEOLNs.get(lFormat);
        Integer terminators = nativeTerminators.get(lFormat);
        int count;
        if (terminators != null) {
            int numTerminators = terminators;
search:
            for (count = 0; count < (bytes.length - numTerminators + 1); count += numTerminators) {
                for (int i = count; i < count + numTerminators; i++) {
                    if (bytes[i] != 0x0) {
                        continue search;
                    }
                }
                // found terminators
                break search;
            }
        } else {
            count = bytes.length;
        }

        // Decode text to chars. Don't include any terminators.
        String converted = new String(bytes, 0, count, charset);

        // Search and replace EOLN. Note that if EOLN is "\n", then we
        // never added an entry to nativeEOLNs anyway, so we'll skip this
        // code altogether.
        // Count of NUL-terminators and EOLN coding are platform-specific and
        // loaded from flavormap.properties file
        // windows: "abc\r\nde" -> "abc\nde"

        if (eoln != null) {

            /* Fix for 4463560: replace EOLNs symbol-by-symbol instead
             * of using buf.replace()
             */

            char[] buf = converted.toCharArray();
            char[] eoln_arr = eoln.toCharArray();
            int j = 0;
            boolean match;

            for (int i = 0; i < buf.length; ) {
                // Catch last few bytes
                if (i + eoln_arr.length > buf.length) {
                    buf[j++] = buf[i++];
                    continue;
                }

                match = true;
                for (int k = 0, l = i; k < eoln_arr.length; k++, l++) {
                    if (eoln_arr[k] != buf[l]) {
                        match = false;
                        break;
                    }
                }
                if (match) {
                    buf[j++] = '\n';
                    i += eoln_arr.length;
                } else {
                    buf[j++] = buf[i++];
                }
            }
            converted = new String(buf, 0, j);
        }

        return converted;
    }


    /**
     * Primary translation function for translating a Transferable into
     * a byte array, given a source DataFlavor and target format.
     */
    @SuppressWarnings("deprecation")
    public byte[] translateTransferable(Transferable contents,
                                        DataFlavor flavor,
                                        long format) throws IOException
    {
        // Obtain the transfer data in the source DataFlavor.
        //
        // Note that we special case DataFlavor.plainTextFlavor because
        // StringSelection supports this flavor incorrectly -- instead of
        // returning an InputStream as the DataFlavor representation class
        // states, it returns a Reader. Instead of using this broken
        // functionality, we request the data in stringFlavor (the other
        // DataFlavor which StringSelection supports) and use the String
        // translator.
        Object obj;
        boolean stringSelectionHack;
        try {
            obj = contents.getTransferData(flavor);
            if (obj == null) {
                return null;
            }
            if (flavor.equals(DataFlavor.plainTextFlavor) &&
                !(obj instanceof InputStream))
            {
                obj = contents.getTransferData(DataFlavor.stringFlavor);
                if (obj == null) {
                    return null;
                }
                stringSelectionHack = true;
            } else {
                stringSelectionHack = false;
            }
        } catch (UnsupportedFlavorException e) {
            throw new IOException(e.getMessage());
        }

        // Source data is a String. Search-and-replace EOLN. Encode into the
        // target format. Append terminating NUL bytes.
        if (stringSelectionHack ||
            (String.class.equals(flavor.getRepresentationClass()) &&
             DataFlavorUtil.isFlavorCharsetTextType(flavor) && isTextFormat(format))) {

            String str = removeSuspectedData(flavor, contents, (String)obj);

            return translateTransferableString(
                str,
                format);

        // Source data is a Reader. Convert to a String and recur. In the
        // future, we may want to rewrite this so that we encode on demand.
        } else if (flavor.isRepresentationClassReader()) {
            if (!(DataFlavorUtil.isFlavorCharsetTextType(flavor) && isTextFormat(format))) {
                throw new IOException
                    ("cannot transfer non-text data as Reader");
            }

            StringBuilder buf = new StringBuilder();
            try (Reader r = (Reader)obj) {
                int c;
                while ((c = r.read()) != -1) {
                    buf.append((char)c);
                }
            }

            return translateTransferableString(
                buf.toString(),
                format);

        // Source data is a CharBuffer. Convert to a String and recur.
        } else if (flavor.isRepresentationClassCharBuffer()) {
            if (!(DataFlavorUtil.isFlavorCharsetTextType(flavor) && isTextFormat(format))) {
                throw new IOException
                    ("cannot transfer non-text data as CharBuffer");
            }

            CharBuffer buffer = (CharBuffer)obj;
            int size = buffer.remaining();
            char[] chars = new char[size];
            buffer.get(chars, 0, size);

            return translateTransferableString(
                new String(chars),
                format);

        // Source data is a char array. Convert to a String and recur.
        } else if (char[].class.equals(flavor.getRepresentationClass())) {
            if (!(DataFlavorUtil.isFlavorCharsetTextType(flavor) && isTextFormat(format))) {
                throw new IOException
                    ("cannot transfer non-text data as char array");
            }

            return translateTransferableString(
                new String((char[])obj),
                format);

        // Source data is a ByteBuffer. For arbitrary flavors, simply return
        // the array. For text flavors, decode back to a String and recur to
        // reencode according to the requested format.
        } else if (flavor.isRepresentationClassByteBuffer()) {
            ByteBuffer buffer = (ByteBuffer)obj;
            int size = buffer.remaining();
            byte[] bytes = new byte[size];
            buffer.get(bytes, 0, size);

            if (DataFlavorUtil.isFlavorCharsetTextType(flavor) && isTextFormat(format)) {
                String sourceEncoding = DataFlavorUtil.getTextCharset(flavor);
                return translateTransferableString(
                    new String(bytes, sourceEncoding),
                    format);
            } else {
                return bytes;
            }

        // Source data is a byte array. For arbitrary flavors, simply return
        // the array. For text flavors, decode back to a String and recur to
        // reencode according to the requested format.
        } else if (byte[].class.equals(flavor.getRepresentationClass())) {
            byte[] bytes = (byte[])obj;

            if (DataFlavorUtil.isFlavorCharsetTextType(flavor) && isTextFormat(format)) {
                String sourceEncoding = DataFlavorUtil.getTextCharset(flavor);
                return translateTransferableString(
                    new String(bytes, sourceEncoding),
                    format);
            } else {
                return bytes;
            }
        // Source data is Image
        } else if (DataFlavor.imageFlavor.equals(flavor)) {
            if (!isImageFormat(format)) {
                throw new IOException("Data translation failed: " +
                                      "not an image format");
            }

            Image image = (Image)obj;
            byte[] bytes = imageToPlatformBytes(image, format);

            if (bytes == null) {
                throw new IOException("Data translation failed: " +
                    "cannot convert java image to native format");
            }
            return bytes;
        }

        byte[] theByteArray = null;

        // Target data is a file list. Source data must be a
        // java.util.List which contains java.io.File or String instances.
        if (isFileFormat(format)) {
            if (!DataFlavor.javaFileListFlavor.equals(flavor)) {
                throw new IOException("data translation failed");
            }

            final List<?> list = (List<?>)obj;

            final ProtectionDomain userProtectionDomain = getUserProtectionDomain(contents);

            final ArrayList<String> fileList = castToFiles(list, userProtectionDomain);

            try (ByteArrayOutputStream bos = convertFileListToBytes(fileList)) {
                theByteArray = bos.toByteArray();
            }

        // Target data is a URI list. Source data must be a
        // java.util.List which contains java.io.File or String instances.
        } else if (isURIListFormat(format)) {
            if (!DataFlavor.javaFileListFlavor.equals(flavor)) {
                throw new IOException("data translation failed");
            }
            String nat = getNativeForFormat(format);
            String targetCharset = null;
            if (nat != null) {
                try {
                    targetCharset = new DataFlavor(nat).getParameter("charset");
                } catch (ClassNotFoundException cnfe) {
                    throw new IOException(cnfe);
                }
            }
            if (targetCharset == null) {
                targetCharset = "UTF-8";
            }
            final List<?> list = (List<?>)obj;
            final ProtectionDomain userProtectionDomain = getUserProtectionDomain(contents);
            final ArrayList<String> fileList = castToFiles(list, userProtectionDomain);
            final ArrayList<String> uriList = new ArrayList<>(fileList.size());
            for (String fileObject : fileList) {
                final URI uri = new File(fileObject).toURI();
                // Some implementations are fussy about the number of slashes (file:///path/to/file is best)
                try {
                    uriList.add(new URI(uri.getScheme(), "", uri.getPath(), uri.getFragment()).toString());
                } catch (URISyntaxException uriSyntaxException) {
                    throw new IOException(uriSyntaxException);
                  }
              }

            byte[] eoln = "\r\n".getBytes(targetCharset);

            try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                for (String uri : uriList) {
                    byte[] bytes = uri.getBytes(targetCharset);
                    bos.write(bytes, 0, bytes.length);
                    bos.write(eoln, 0, eoln.length);
                }
                theByteArray = bos.toByteArray();
            }

        // Source data is an InputStream. For arbitrary flavors, just grab the
        // bytes and dump them into a byte array. For text flavors, decode back
        // to a String and recur to reencode according to the requested format.
        } else if (flavor.isRepresentationClassInputStream()) {

            // Workaround to JDK-8024061: Exception thrown when drag and drop
            //      between two components is executed quickly.
            // and JDK-8065098:  JColorChooser no longer supports drag and drop
            //      between two JVM instances
            if (!(obj instanceof InputStream)) {
                return new byte[0];
            }

            try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                try (InputStream is = (InputStream)obj) {
                    is.mark(Integer.MAX_VALUE);
                    is.transferTo(bos);
                    is.reset();
                }

                if (DataFlavorUtil.isFlavorCharsetTextType(flavor) && isTextFormat(format)) {
                    byte[] bytes = bos.toByteArray();
                    String sourceEncoding = DataFlavorUtil.getTextCharset(flavor);
                    return translateTransferableString(
                               new String(bytes, sourceEncoding),
                               format);
                }
                theByteArray = bos.toByteArray();
            }


        // Source data is an RMI object
        } else if (flavor.isRepresentationClassRemote()) {
            theByteArray = convertObjectToBytes(DataFlavorUtil.RMI.newMarshalledObject(obj));

        // Source data is Serializable
        } else if (flavor.isRepresentationClassSerializable()) {

            theByteArray = convertObjectToBytes(obj);

        } else {
            throw new IOException("data translation failed");
        }



        return theByteArray;
    }

    private static byte[] convertObjectToBytes(Object object) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos))
        {
            oos.writeObject(object);
            return bos.toByteArray();
        }
    }

    protected abstract ByteArrayOutputStream convertFileListToBytes(ArrayList<String> fileList) throws IOException;

    @SuppressWarnings("removal")
    private String removeSuspectedData(DataFlavor flavor, final Transferable contents, final String str)
            throws IOException
    {
        if (null == System.getSecurityManager()
            || !flavor.isMimeTypeEqual("text/uri-list"))
        {
            return str;
        }

        final ProtectionDomain userProtectionDomain = getUserProtectionDomain(contents);

        try {
            return AccessController.doPrivileged((PrivilegedExceptionAction<String>) () -> {

                StringBuilder allowedFiles = new StringBuilder(str.length());
                String [] uriArray = str.split("(\\s)+");

                for (String fileName : uriArray)
                {
                    File file = new File(fileName);
                    if (file.exists() &&
                        !(isFileInWebstartedCache(file) ||
                        isForbiddenToRead(file, userProtectionDomain)))
                    {
                        if (0 != allowedFiles.length())
                        {
                            allowedFiles.append("\\r\\n");
                        }

                        allowedFiles.append(fileName);
                    }
                }

                return allowedFiles.toString();
            });
        } catch (PrivilegedActionException pae) {
            throw new IOException(pae.getMessage(), pae);
        }
    }

    private static ProtectionDomain getUserProtectionDomain(Transferable contents) {
        return contents.getClass().getProtectionDomain();
    }

    private boolean isForbiddenToRead (File file, ProtectionDomain protectionDomain)
    {
        if (null == protectionDomain) {
            return false;
        }
        try {
            FilePermission filePermission =
                    new FilePermission(file.getCanonicalPath(), "read, delete");
            if (protectionDomain.implies(filePermission)) {
                return false;
            }
        } catch (IOException e) {}

        return true;
    }

    @SuppressWarnings("removal")
    private ArrayList<String> castToFiles(final List<?> files,
                                          final ProtectionDomain userProtectionDomain) throws IOException {
        try {
            return AccessController.doPrivileged((PrivilegedExceptionAction<ArrayList<String>>) () -> {
                ArrayList<String> fileList = new ArrayList<>();
                for (Object fileObject : files)
                {
                    File file = castToFile(fileObject);
                    if (file != null &&
                        (null == System.getSecurityManager() ||
                        !(isFileInWebstartedCache(file) ||
                        isForbiddenToRead(file, userProtectionDomain))))
                    {
                        fileList.add(file.getCanonicalPath());
                    }
                }
                return fileList;
            });
        } catch (PrivilegedActionException pae) {
            throw new IOException(pae.getMessage());
        }
    }

    // It is important do not use user's successors
    // of File class.
    private File castToFile(Object fileObject) throws IOException {
        String filePath = null;
        if (fileObject instanceof File) {
            filePath = ((File)fileObject).getCanonicalPath();
        } else if (fileObject instanceof String) {
           filePath = (String) fileObject;
        } else {
           return null;
        }
        return new File(filePath);
    }

    private static final String[] DEPLOYMENT_CACHE_PROPERTIES = {
        "deployment.system.cachedir",
        "deployment.user.cachedir",
        "deployment.javaws.cachedir",
        "deployment.javapi.cachedir"
    };

    private static final ArrayList <File> deploymentCacheDirectoryList = new ArrayList<>();

    private static boolean isFileInWebstartedCache(File f) {

        if (deploymentCacheDirectoryList.isEmpty()) {
            for (String cacheDirectoryProperty : DEPLOYMENT_CACHE_PROPERTIES) {
                String cacheDirectoryPath = System.getProperty(cacheDirectoryProperty);
                if (cacheDirectoryPath != null) {
                    try {
                        File cacheDirectory = (new File(cacheDirectoryPath)).getCanonicalFile();
                        if (cacheDirectory != null) {
                            deploymentCacheDirectoryList.add(cacheDirectory);
                        }
                    } catch (IOException ioe) {}
                }
            }
        }

        for (File deploymentCacheDirectory : deploymentCacheDirectoryList) {
            for (File dir = f; dir != null; dir = dir.getParentFile()) {
                if (dir.equals(deploymentCacheDirectory)) {
                    return true;
                }
            }
        }

        return false;
    }


    public Object translateBytes(byte[] bytes, DataFlavor flavor,
                                 long format, Transferable localeTransferable)
        throws IOException
    {

        Object theObject = null;

        // Source data is a file list. Use the dragQueryFile native function to
        // do most of the decoding. Then wrap File objects around the String
        // filenames and return a List.
        if (isFileFormat(format)) {
            if (!DataFlavor.javaFileListFlavor.equals(flavor)) {
                throw new IOException("data translation failed");
            }
            String[] filenames = dragQueryFile(bytes);
            if (filenames == null) {
                return null;
            }

            // Convert the strings to File objects
            File[] files = new File[filenames.length];
            for (int i = 0; i < filenames.length; i++) {
                files[i] = new File(filenames[i]);
            }

            // Turn the list of Files into a List and return
            theObject = Arrays.asList(files);

            // Source data is a URI list. Convert to DataFlavor.javaFileListFlavor
            // where possible.
        } else if (isURIListFormat(format)
                    && DataFlavor.javaFileListFlavor.equals(flavor)) {

            try (ByteArrayInputStream str = new ByteArrayInputStream(bytes))  {

                URI[] uris = dragQueryURIs(str, format, localeTransferable);
                if (uris == null) {
                    return null;
                }
                List<File> files = new ArrayList<>();
                for (URI uri : uris) {
                    try {
                        files.add(new File(uri));
                    } catch (IllegalArgumentException illegalArg) {
                        // When converting from URIs to less generic files,
                        // common practice (Wine, SWT) seems to be to
                        // silently drop the URIs that aren't local files.
                    }
                }
                theObject = files;
            }

            // Target data is a String. Strip terminating NUL bytes. Decode bytes
            // into characters. Search-and-replace EOLN.
        } else if (String.class.equals(flavor.getRepresentationClass()) &&
                   DataFlavorUtil.isFlavorCharsetTextType(flavor) && isTextFormat(format)) {

            theObject = translateBytesToString(bytes, format, localeTransferable);

            // Target data is a Reader. Obtain data in InputStream format, encoded
            // as "Unicode" (utf-16be). Then use an InputStreamReader to decode
            // back to chars on demand.
        } else if (flavor.isRepresentationClassReader()) {
            try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes)) {
                theObject = translateStream(bais,
                        flavor, format, localeTransferable);
            }
            // Target data is a CharBuffer. Recur to obtain String and wrap.
        } else if (flavor.isRepresentationClassCharBuffer()) {
            if (!(DataFlavorUtil.isFlavorCharsetTextType(flavor) && isTextFormat(format))) {
                throw new IOException("cannot transfer non-text data as CharBuffer");
            }

            CharBuffer buffer = CharBuffer.wrap(
                translateBytesToString(bytes,format, localeTransferable));

            theObject = constructFlavoredObject(buffer, flavor, CharBuffer.class);

            // Target data is a char array. Recur to obtain String and convert to
            // char array.
        } else if (char[].class.equals(flavor.getRepresentationClass())) {
            if (!(DataFlavorUtil.isFlavorCharsetTextType(flavor) && isTextFormat(format))) {
                throw new IOException
                          ("cannot transfer non-text data as char array");
            }

            theObject = translateBytesToString(
                bytes, format, localeTransferable).toCharArray();

            // Target data is a ByteBuffer. For arbitrary flavors, just return
            // the raw bytes. For text flavors, convert to a String to strip
            // terminators and search-and-replace EOLN, then reencode according to
            // the requested flavor.
        } else if (flavor.isRepresentationClassByteBuffer()) {
            if (DataFlavorUtil.isFlavorCharsetTextType(flavor) && isTextFormat(format)) {
                bytes = translateBytesToString(
                    bytes, format, localeTransferable).getBytes(
                        DataFlavorUtil.getTextCharset(flavor)
                    );
            }

            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            theObject = constructFlavoredObject(buffer, flavor, ByteBuffer.class);

            // Target data is a byte array. For arbitrary flavors, just return
            // the raw bytes. For text flavors, convert to a String to strip
            // terminators and search-and-replace EOLN, then reencode according to
            // the requested flavor.
        } else if (byte[].class.equals(flavor.getRepresentationClass())) {
            if (DataFlavorUtil.isFlavorCharsetTextType(flavor) && isTextFormat(format)) {
                theObject = translateBytesToString(
                    bytes, format, localeTransferable
                ).getBytes(DataFlavorUtil.getTextCharset(flavor));
            } else {
                theObject = bytes;
            }

            // Target data is an InputStream. For arbitrary flavors, just return
            // the raw bytes. For text flavors, decode to strip terminators and
            // search-and-replace EOLN, then reencode according to the requested
            // flavor.
        } else if (flavor.isRepresentationClassInputStream()) {

            try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes)) {
                theObject = translateStream(bais, flavor, format, localeTransferable);
            }

        } else if (flavor.isRepresentationClassRemote()) {
            try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
                 ObjectInputStream ois = new ObjectInputStream(bais)) {

                theObject = DataFlavorUtil.RMI.getMarshalledObject(ois.readObject());
            } catch (Exception e) {
                throw new IOException(e.getMessage());
            }

            // Target data is Serializable
        } else if (flavor.isRepresentationClassSerializable()) {

            try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes)) {
                theObject = translateStream(bais, flavor, format, localeTransferable);
            }

            // Target data is Image
        } else if (DataFlavor.imageFlavor.equals(flavor)) {
            if (!isImageFormat(format)) {
                throw new IOException("data translation failed");
            }

            theObject = platformImageBytesToImage(bytes, format);
        }

        if (theObject == null) {
            throw new IOException("data translation failed");
        }

        return theObject;

    }

    /**
     * Primary translation function for translating
     * an InputStream into an Object, given a source format and a target
     * DataFlavor.
     */
    @SuppressWarnings("deprecation")
    public Object translateStream(InputStream str, DataFlavor flavor,
                                  long format, Transferable localeTransferable)
        throws IOException
    {

        Object theObject = null;
        // Source data is a URI list. Convert to DataFlavor.javaFileListFlavor
        // where possible.
        if (isURIListFormat(format)
                && DataFlavor.javaFileListFlavor.equals(flavor))
        {

            URI[] uris = dragQueryURIs(str, format, localeTransferable);
            if (uris == null) {
                return null;
            }
            List<File> files = new ArrayList<>();
            for (URI uri : uris) {
                try {
                    files.add(new File(uri));
                } catch (IllegalArgumentException illegalArg) {
                    // When converting from URIs to less generic files,
                    // common practice (Wine, SWT) seems to be to
                    // silently drop the URIs that aren't local files.
                }
            }
            theObject = files;

        // Target data is a String. Strip terminating NUL bytes. Decode bytes
        // into characters. Search-and-replace EOLN.
        } else if (String.class.equals(flavor.getRepresentationClass()) &&
                   DataFlavorUtil.isFlavorCharsetTextType(flavor) && isTextFormat(format)) {

            return translateBytesToString(inputStreamToByteArray(str),
                format, localeTransferable);

            // Special hack to maintain backwards-compatibility with the brokenness
            // of StringSelection. Return a StringReader instead of an InputStream.
            // Recur to obtain String and encapsulate.
        } else if (DataFlavor.plainTextFlavor.equals(flavor)) {
            theObject = new StringReader(translateBytesToString(
                inputStreamToByteArray(str),
                format, localeTransferable));

            // Target data is an InputStream. For arbitrary flavors, just return
            // the raw bytes. For text flavors, decode to strip terminators and
            // search-and-replace EOLN, then reencode according to the requested
            // flavor.
        } else if (flavor.isRepresentationClassInputStream()) {
            theObject = translateStreamToInputStream(str, flavor, format,
                                                               localeTransferable);

            // Target data is a Reader. Obtain data in InputStream format, encoded
            // as "Unicode" (utf-16be). Then use an InputStreamReader to decode
            // back to chars on demand.
        } else if (flavor.isRepresentationClassReader()) {
            if (!(DataFlavorUtil.isFlavorCharsetTextType(flavor) && isTextFormat(format))) {
                throw new IOException
                          ("cannot transfer non-text data as Reader");
            }

            InputStream is = (InputStream)translateStreamToInputStream(
                    str, DataFlavor.plainTextFlavor,
                    format, localeTransferable);

            String unicode = DataFlavorUtil.getTextCharset(DataFlavor.plainTextFlavor);

            Reader reader = new InputStreamReader(is, unicode);

            theObject = constructFlavoredObject(reader, flavor, Reader.class);
            // Target data is a byte array
        } else if (byte[].class.equals(flavor.getRepresentationClass())) {
            if (DataFlavorUtil.isFlavorCharsetTextType(flavor) && isTextFormat(format)) {
                theObject = translateBytesToString(inputStreamToByteArray(str), format, localeTransferable)
                        .getBytes(DataFlavorUtil.getTextCharset(flavor));
            } else {
                theObject = inputStreamToByteArray(str);
            }
            // Target data is an RMI object
        } else if (flavor.isRepresentationClassRemote()) {
            try (ObjectInputStream ois = new ObjectInputStream(str)) {
                theObject = DataFlavorUtil.RMI.getMarshalledObject(ois.readObject());
            } catch (Exception e) {
                throw new IOException(e.getMessage());
            }

            // Target data is Serializable
        } else if (flavor.isRepresentationClassSerializable()) {
            try (ObjectInputStream ois =
                     new ObjectInputStream(str))
            {
                theObject = ois.readObject();
            } catch (Exception e) {
                throw new IOException(e.getMessage());
            }
            // Target data is Image
        } else if (DataFlavor.imageFlavor.equals(flavor)) {
            if (!isImageFormat(format)) {
                throw new IOException("data translation failed");
            }
            theObject = platformImageBytesToImage(inputStreamToByteArray(str), format);
        }

        if (theObject == null) {
            throw new IOException("data translation failed");
        }

        return theObject;

    }

    /**
     * For arbitrary flavors, just use the raw InputStream. For text flavors,
     * ReencodingInputStream will decode and reencode the InputStream on demand
     * so that we can strip terminators and search-and-replace EOLN.
     */
    private Object translateStreamToInputStream
        (InputStream str, DataFlavor flavor, long format,
         Transferable localeTransferable) throws IOException
    {
        if (DataFlavorUtil.isFlavorCharsetTextType(flavor) && isTextFormat(format)) {
            str = new ReencodingInputStream
                (str, format, DataFlavorUtil.getTextCharset(flavor),
                 localeTransferable);
        }

        return constructFlavoredObject(str, flavor, InputStream.class);
    }

    /**
     * We support representations which are exactly of the specified Class,
     * and also arbitrary Objects which have a constructor which takes an
     * instance of the Class as its sole parameter.
     */
    @SuppressWarnings("removal")
    private Object constructFlavoredObject(Object arg, DataFlavor flavor,
                                           Class<?> clazz)
        throws IOException
    {
        final Class<?> dfrc = flavor.getRepresentationClass();

        if (clazz.equals(dfrc)) {
            return arg; // simple case
        } else {
            Constructor<?>[] constructors;

            try {
                constructors = AccessController.doPrivileged(
                        (PrivilegedAction<Constructor<?>[]>) dfrc::getConstructors);
            } catch (SecurityException se) {
                throw new IOException(se.getMessage());
            }

            Constructor<?> constructor = Stream.of(constructors)
                    .filter(c -> Modifier.isPublic(c.getModifiers()))
                    .filter(c -> {
                        Class<?>[] ptypes = c.getParameterTypes();
                        return ptypes != null
                                && ptypes.length == 1
                                && clazz.equals(ptypes[0]);
                    })
                    .findFirst()
                    .orElseThrow(() ->
                            new IOException("can't find <init>(L"+ clazz + ";)V for class: " + dfrc.getName()));

            try {
                return constructor.newInstance(arg);
            } catch (Exception e) {
                throw new IOException(e.getMessage());
            }
        }
    }

    /**
     * Used for decoding and reencoding an InputStream on demand so that we
     * can strip NUL terminators and perform EOLN search-and-replace.
     */
    public class ReencodingInputStream extends InputStream {
        BufferedReader wrapped;
        final char[] in = new char[2];
        byte[] out;

        CharsetEncoder encoder;
        CharBuffer inBuf;
        ByteBuffer outBuf;

        char[] eoln;
        int numTerminators;

        boolean eos;
        int index, limit;

        public ReencodingInputStream(InputStream bytestream, long format,
                                     String targetEncoding,
                                     Transferable localeTransferable)
            throws IOException
        {
            Long lFormat = format;

            String sourceEncoding = getBestCharsetForTextFormat(format, localeTransferable);
            wrapped = new BufferedReader(new InputStreamReader(bytestream, sourceEncoding));

            if (targetEncoding == null) {
                // Throw NullPointerException for compatibility with the former
                // call to sun.io.CharToByteConverter.getConverter(null)
                // (Charset.forName(null) throws unspecified IllegalArgumentException
                // now; see 6228568)
                throw new NullPointerException("null target encoding");
            }

            try {
                encoder = Charset.forName(targetEncoding).newEncoder();
                out = new byte[(int)(encoder.maxBytesPerChar() * 2 + 0.5)];
                inBuf = CharBuffer.wrap(in);
                outBuf = ByteBuffer.wrap(out);
            } catch (IllegalCharsetNameException
                    | UnsupportedCharsetException
                    | UnsupportedOperationException e) {
                throw new IOException(e.toString());
            }

            String sEoln = nativeEOLNs.get(lFormat);
            if (sEoln != null) {
                eoln = sEoln.toCharArray();
            }

            // A hope and a prayer that this works generically. This will
            // definitely work on Win32.
            Integer terminators = nativeTerminators.get(lFormat);
            if (terminators != null) {
                numTerminators = terminators;
            }
        }

        private int readChar() throws IOException {
            int c = wrapped.read();

            if (c == -1) { // -1 is EOS
                eos = true;
                return -1;
            }

            // "c == 0" is not quite correct, but good enough on Windows.
            if (numTerminators > 0 && c == 0) {
                eos = true;
                return -1;
            } else if (eoln != null && matchCharArray(eoln, c)) {
                c = '\n' & 0xFFFF;
            }

            return c;
        }

        public int read() throws IOException {
            if (eos) {
                return -1;
            }

            if (index >= limit) {
                // deal with supplementary characters
                int c = readChar();
                if (c == -1) {
                    return -1;
                }

                in[0] = (char) c;
                in[1] = 0;
                inBuf.limit(1);
                if (Character.isHighSurrogate((char) c)) {
                    c = readChar();
                    if (c != -1) {
                        in[1] = (char) c;
                        inBuf.limit(2);
                    }
                }

                inBuf.rewind();
                outBuf.limit(out.length).rewind();
                encoder.encode(inBuf, outBuf, false);
                outBuf.flip();
                limit = outBuf.limit();

                index = 0;

                return read();
            } else {
                return out[index++] & 0xFF;
            }
        }

        public int available() throws IOException {
            return ((eos) ? 0 : (limit - index));
        }

        public void close() throws IOException {
            wrapped.close();
        }

        /**
         * Checks to see if the next array.length characters in wrapped
         * match array. The first character is provided as c. Subsequent
         * characters are read from wrapped itself. When this method returns,
         * the wrapped index may be different from what it was when this
         * method was called.
         */
        private boolean matchCharArray(char[] array, int c)
            throws IOException
        {
            wrapped.mark(array.length);  // BufferedReader supports mark

            int count = 0;
            if ((char)c == array[0]) {
                for (count = 1; count < array.length; count++) {
                    c = wrapped.read();
                    if (c == -1 || ((char)c) != array[count]) {
                        break;
                    }
                }
            }

            if (count == array.length) {
                return true;
            } else {
                wrapped.reset();
                return false;
            }
        }
    }

    /**
     * Decodes a byte array into a set of String filenames.
     */
    protected abstract String[] dragQueryFile(byte[] bytes);

    /**
     * Decodes URIs from either a byte array or a stream.
     */
    protected URI[] dragQueryURIs(InputStream stream,
                                  long format,
                                  Transferable localeTransferable)
      throws IOException
    {
        throw new IOException(
            new UnsupportedOperationException("not implemented on this platform"));
    }

    /**
     * Translates either a byte array or an input stream which contain
     * platform-specific image data in the given format into an Image.
     */


    protected abstract Image platformImageBytesToImage(
        byte[] bytes,long format) throws IOException;

    /**
     * Translates either a byte array or an input stream which contain
     * an image data in the given standard format into an Image.
     *
     * @param mimeType image MIME type, such as: image/png, image/jpeg, image/gif
     */
    protected Image standardImageBytesToImage(
        byte[] bytes, String mimeType) throws IOException
    {

        Iterator<ImageReader> readerIterator = ImageIO.getImageReadersByMIMEType(mimeType);

        if (!readerIterator.hasNext()) {
            throw new IOException("No registered service provider can decode " +
                                  " an image from " + mimeType);
        }

        IOException ioe = null;

        while (readerIterator.hasNext()) {
            ImageReader imageReader = readerIterator.next();
            try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes)) {
                try (ImageInputStream imageInputStream = ImageIO.createImageInputStream(bais)) {
                    ImageReadParam param = imageReader.getDefaultReadParam();
                    imageReader.setInput(imageInputStream, true, true);
                    BufferedImage bufferedImage = imageReader.read(imageReader.getMinIndex(), param);
                    if (bufferedImage != null) {
                        return bufferedImage;
                    }
                } finally {
                    imageReader.dispose();
                }
            } catch (IOException e) {
                ioe = e;
                continue;
            }
        }

        if (ioe == null) {
            ioe = new IOException("Registered service providers failed to decode"
                                  + " an image from " + mimeType);
        }

        throw ioe;
    }

    /**
     * Translates a Java Image into a byte array which contains platform-
     * specific image data in the given format.
     */
    protected abstract byte[] imageToPlatformBytes(Image image, long format)
      throws IOException;

    /**
     * Translates a Java Image into a byte array which contains
     * an image data in the given standard format.
     *
     * @param mimeType image MIME type, such as: image/png, image/jpeg
     */
    protected byte[] imageToStandardBytes(Image image, String mimeType)
      throws IOException {
        IOException originalIOE = null;

        Iterator<ImageWriter> writerIterator = ImageIO.getImageWritersByMIMEType(mimeType);

        if (!writerIterator.hasNext()) {
            throw new IOException("No registered service provider can encode " +
                                  " an image to " + mimeType);
        }

        if (image instanceof RenderedImage) {
            // Try to encode the original image.
            try {
                return imageToStandardBytesImpl((RenderedImage)image, mimeType);
            } catch (IOException ioe) {
                originalIOE = ioe;
            }
        }

        // Retry with a BufferedImage.
        int width = 0;
        int height = 0;
        if (image instanceof ToolkitImage) {
            ImageRepresentation ir = ((ToolkitImage)image).getImageRep();
            ir.reconstruct(ImageObserver.ALLBITS);
            width = ir.getWidth();
            height = ir.getHeight();
        } else {
            width = image.getWidth(null);
            height = image.getHeight(null);
        }

        ColorModel model = ColorModel.getRGBdefault();
        WritableRaster raster =
            model.createCompatibleWritableRaster(width, height);

        BufferedImage bufferedImage =
            new BufferedImage(model, raster, model.isAlphaPremultiplied(),
                              null);

        Graphics g = bufferedImage.getGraphics();
        try {
            g.drawImage(image, 0, 0, width, height, null);
        } finally {
            g.dispose();
        }

        try {
            return imageToStandardBytesImpl(bufferedImage, mimeType);
        } catch (IOException ioe) {
            if (originalIOE != null) {
                throw originalIOE;
            } else {
                throw ioe;
            }
        }
    }

    byte[] imageToStandardBytesImpl(RenderedImage renderedImage,
                                              String mimeType)
        throws IOException {

        Iterator<ImageWriter> writerIterator = ImageIO.getImageWritersByMIMEType(mimeType);

        ImageTypeSpecifier typeSpecifier =
            new ImageTypeSpecifier(renderedImage);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        IOException ioe = null;

        while (writerIterator.hasNext()) {
            ImageWriter imageWriter = writerIterator.next();
            ImageWriterSpi writerSpi = imageWriter.getOriginatingProvider();

            if (!writerSpi.canEncodeImage(typeSpecifier)) {
                continue;
            }

            try {
                try (ImageOutputStream imageOutputStream = ImageIO.createImageOutputStream(baos)) {
                    imageWriter.setOutput(imageOutputStream);
                    imageWriter.write(renderedImage);
                    imageOutputStream.flush();
                }
            } catch (IOException e) {
                imageWriter.dispose();
                baos.reset();
                ioe = e;
                continue;
            }

            imageWriter.dispose();
            baos.close();
            return baos.toByteArray();
        }

        baos.close();

        if (ioe == null) {
            ioe = new IOException("Registered service providers failed to encode "
                                  + renderedImage + " to " + mimeType);
        }

        throw ioe;
    }

    /**
     * Concatenates the data represented by two objects. Objects can be either
     * byte arrays or instances of {@code InputStream}. If both arguments
     * are byte arrays byte array will be returned. Otherwise an
     * {@code InputStream} will be returned.
     * <p>
     * Currently is only called from native code to prepend palette data to
     * platform-specific image data during image transfer on Win32.
     *
     * @param obj1 the first object to be concatenated.
     * @param obj2 the second object to be concatenated.
     * @return a byte array or an {@code InputStream} which represents
     *         a logical concatenation of the two arguments.
     * @throws NullPointerException is either of the arguments is
     *         {@code null}
     * @throws ClassCastException is either of the arguments is
     *         neither byte array nor an instance of {@code InputStream}.
     */
    private Object concatData(Object obj1, Object obj2) {
        InputStream str1 = null;
        InputStream str2 = null;

        if (obj1 instanceof byte[]) {
            byte[] arr1 = (byte[])obj1;
            if (obj2 instanceof byte[]) {
                byte[] arr2 = (byte[])obj2;
                byte[] ret = new byte[arr1.length + arr2.length];
                System.arraycopy(arr1, 0, ret, 0, arr1.length);
                System.arraycopy(arr2, 0, ret, arr1.length, arr2.length);
                return ret;
            } else {
                str1 = new ByteArrayInputStream(arr1);
                str2 = (InputStream)obj2;
            }
        } else {
            str1 = (InputStream)obj1;
            if (obj2 instanceof byte[]) {
                str2 = new ByteArrayInputStream((byte[])obj2);
            } else {
                str2 = (InputStream)obj2;
            }
        }

        return new SequenceInputStream(str1, str2);
    }

    public byte[] convertData(final Object source,
                              final Transferable contents,
                              final long format,
                              final Map<Long, DataFlavor> formatMap,
                              final boolean isToolkitThread)
        throws IOException
    {
        byte[] ret = null;

        /*
         * If the current thread is the Toolkit thread we should post a
         * Runnable to the event dispatch thread associated with source Object,
         * since translateTransferable() calls Transferable.getTransferData()
         * that may contain client code.
         */
        if (isToolkitThread) try {
            final Stack<byte[]> stack = new Stack<>();
            final Runnable dataConverter = new Runnable() {
                // Guard against multiple executions.
                private boolean done = false;
                public void run() {
                    if (done) {
                        return;
                    }
                    byte[] data = null;
                    try {
                        DataFlavor flavor = formatMap.get(format);
                        if (flavor != null) {
                            data = translateTransferable(contents, flavor, format);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        data = null;
                    }
                    try {
                        getToolkitThreadBlockedHandler().lock();
                        stack.push(data);
                        getToolkitThreadBlockedHandler().exit();
                    } finally {
                        getToolkitThreadBlockedHandler().unlock();
                        done = true;
                    }
                }
            };

            final AppContext appContext = SunToolkit.targetToAppContext(source);

            getToolkitThreadBlockedHandler().lock();

            if (appContext != null) {
                appContext.put(DATA_CONVERTER_KEY, dataConverter);
            }

            SunToolkit.executeOnEventHandlerThread(source, dataConverter);

            while (stack.empty()) {
                getToolkitThreadBlockedHandler().enter();
            }

            if (appContext != null) {
                appContext.remove(DATA_CONVERTER_KEY);
            }

            ret = stack.pop();
        } finally {
            getToolkitThreadBlockedHandler().unlock();
        } else {
            DataFlavor flavor = formatMap.get(format);
            if (flavor != null) {
                ret = translateTransferable(contents, flavor, format);
            }
        }

        return ret;
    }

    public void processDataConversionRequests() {
        if (EventQueue.isDispatchThread()) {
            AppContext appContext = AppContext.getAppContext();
            getToolkitThreadBlockedHandler().lock();
            try {
                Runnable dataConverter =
                    (Runnable)appContext.get(DATA_CONVERTER_KEY);
                if (dataConverter != null) {
                    dataConverter.run();
                    appContext.remove(DATA_CONVERTER_KEY);
                }
            } finally {
                getToolkitThreadBlockedHandler().unlock();
            }
        }
    }

    public abstract ToolkitThreadBlockedHandler getToolkitThreadBlockedHandler();

    /**
     * Helper function to reduce a Map with Long keys to a long array.
     * <p>
     * The map keys are sorted according to the native formats preference
     * order.
     */
    public static long[] keysToLongArray(SortedMap<Long, ?> map) {
        Set<Long> keySet = map.keySet();
        long[] retval = new long[keySet.size()];
        int i = 0;
        for (Iterator<Long> iter = keySet.iterator(); iter.hasNext(); i++) {
            retval[i] = iter.next();
        }
        return retval;
    }

    /**
     * Helper function to convert a Set of DataFlavors to a sorted array.
     * The array will be sorted according to {@code DataFlavorComparator}.
     */
    public static DataFlavor[] setToSortedDataFlavorArray(Set<DataFlavor> flavorsSet) {
        DataFlavor[] flavors = new DataFlavor[flavorsSet.size()];
        flavorsSet.toArray(flavors);
        final Comparator<DataFlavor> comparator = DataFlavorUtil.getDataFlavorComparator().reversed();
        Arrays.sort(flavors, comparator);
        return flavors;
    }

    /**
     * Helper function to convert an InputStream to a byte[] array.
     */
    protected static byte[] inputStreamToByteArray(InputStream str)
        throws IOException
    {
        return str.readAllBytes();
    }

    /**
     * Returns platform-specific mappings for the specified native.
     * If there are no platform-specific mappings for this native, the method
     * returns an empty {@code List}.
     */
    public LinkedHashSet<DataFlavor> getPlatformMappingsForNative(String nat) {
        return new LinkedHashSet<>();
    }

    /**
     * Returns platform-specific mappings for the specified flavor.
     * If there are no platform-specific mappings for this flavor, the method
     * returns an empty {@code List}.
     */
    public LinkedHashSet<String> getPlatformMappingsForFlavor(DataFlavor df) {
        return new LinkedHashSet<>();
    }
}
