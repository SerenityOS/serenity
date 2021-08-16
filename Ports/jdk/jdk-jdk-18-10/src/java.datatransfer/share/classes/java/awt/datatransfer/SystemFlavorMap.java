/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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

package java.awt.datatransfer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.SoftReference;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import sun.datatransfer.DataFlavorUtil;
import sun.datatransfer.DesktopDatatransferService;

/**
 * The SystemFlavorMap is a configurable map between "natives" (Strings), which
 * correspond to platform-specific data formats, and "flavors" (DataFlavors),
 * which correspond to platform-independent MIME types. This mapping is used by
 * the data transfer subsystem to transfer data between Java and native
 * applications, and between Java applications in separate VMs.
 *
 * @since 1.2
 */
public final class SystemFlavorMap implements FlavorMap, FlavorTable {

    /**
     * Constant prefix used to tag Java types converted to native platform type.
     */
    private static String JavaMIME = "JAVA_DATAFLAVOR:";

    private static final Object FLAVOR_MAP_KEY = new Object();

    /**
     * The list of valid, decoded text flavor representation classes, in order
     * from best to worst.
     */
    private static final String[] UNICODE_TEXT_CLASSES = {
        "java.io.Reader", "java.lang.String", "java.nio.CharBuffer", "\"[C\""
    };

    /**
     * The list of valid, encoded text flavor representation classes, in order
     * from best to worst.
     */
    private static final String[] ENCODED_TEXT_CLASSES = {
        "java.io.InputStream", "java.nio.ByteBuffer", "\"[B\""
    };

    /**
     * A String representing text/plain MIME type.
     */
    private static final String TEXT_PLAIN_BASE_TYPE = "text/plain";

    /**
     * A String representing text/html MIME type.
     */
    private static final String HTML_TEXT_BASE_TYPE = "text/html";

    /**
     * Maps native Strings to Lists of DataFlavors (or base type Strings for
     * text DataFlavors).
     * <p>
     * Do not use the field directly, use {@link #getNativeToFlavor} instead.
     */
    private final Map<String, LinkedHashSet<DataFlavor>> nativeToFlavor = new HashMap<>();

    /**
     * Accessor to nativeToFlavor map. Since we use lazy initialization we must
     * use this accessor instead of direct access to the field which may not be
     * initialized yet. This method will initialize the field if needed.
     *
     * @return nativeToFlavor
     */
    private Map<String, LinkedHashSet<DataFlavor>> getNativeToFlavor() {
        if (!isMapInitialized) {
            initSystemFlavorMap();
        }
        return nativeToFlavor;
    }

    /**
     * Maps DataFlavors (or base type Strings for text DataFlavors) to Lists of
     * native Strings.
     * <p>
     * Do not use the field directly, use {@link #getFlavorToNative} instead.
     */
    private final Map<DataFlavor, LinkedHashSet<String>> flavorToNative = new HashMap<>();

    /**
     * Accessor to flavorToNative map. Since we use lazy initialization we must
     * use this accessor instead of direct access to the field which may not be
     * initialized yet. This method will initialize the field if needed.
     *
     * @return flavorToNative
     */
    private synchronized Map<DataFlavor, LinkedHashSet<String>> getFlavorToNative() {
        if (!isMapInitialized) {
            initSystemFlavorMap();
        }
        return flavorToNative;
    }

    /**
     * Maps a text DataFlavor primary mime-type to the native. Used only to
     * store standard mappings registered in the {@code flavormap.properties}.
     * <p>
     * Do not use this field directly, use {@link #getTextTypeToNative} instead.
     */
    private Map<String, LinkedHashSet<String>> textTypeToNative = new HashMap<>();

    /**
     * Shows if the object has been initialized.
     */
    private boolean isMapInitialized = false;

    /**
     * An accessor to textTypeToNative map. Since we use lazy initialization we
     * must use this accessor instead of direct access to the field which may
     * not be initialized yet. This method will initialize the field if needed.
     *
     * @return textTypeToNative
     */
    private synchronized Map<String, LinkedHashSet<String>> getTextTypeToNative() {
        if (!isMapInitialized) {
            initSystemFlavorMap();
            // From this point the map should not be modified
            textTypeToNative = Collections.unmodifiableMap(textTypeToNative);
        }
        return textTypeToNative;
    }

    /**
     * Caches the result of getNativesForFlavor(). Maps DataFlavors to
     * SoftReferences which reference LinkedHashSet of String natives.
     */
    private final SoftCache<DataFlavor, String> nativesForFlavorCache = new SoftCache<>();

    /**
     * Caches the result getFlavorsForNative(). Maps String natives to
     * SoftReferences which reference LinkedHashSet of DataFlavors.
     */
    private final SoftCache<String, DataFlavor> flavorsForNativeCache = new SoftCache<>();

    /**
     * Dynamic mapping generation used for text mappings should not be applied
     * to the DataFlavors and String natives for which the mappings have been
     * explicitly specified with {@link #setFlavorsForNative} or
     * {@link #setNativesForFlavor}. This keeps all such keys.
     */
    private Set<Object> disabledMappingGenerationKeys = new HashSet<>();

    /**
     * Returns the default FlavorMap for this thread's ClassLoader.
     *
     * @return the default FlavorMap for this thread's ClassLoader
     */
    public static FlavorMap getDefaultFlavorMap() {
        return DataFlavorUtil.getDesktopService().getFlavorMap(SystemFlavorMap::new);
    }

    private SystemFlavorMap() {
    }

    /**
     * Initializes a SystemFlavorMap by reading {@code flavormap.properties}.
     * For thread-safety must be called under lock on {@code this}.
     */
    private void initSystemFlavorMap() {
        if (isMapInitialized) {
            return;
        }
        isMapInitialized = true;

        @SuppressWarnings("removal")
        InputStream is = AccessController.doPrivileged(
            (PrivilegedAction<InputStream>) () -> {
                return SystemFlavorMap.class.getResourceAsStream(
                        "/sun/datatransfer/resources/flavormap.properties");
            });
        if (is == null) {
            throw new InternalError("Default flavor mapping not found");
        }

        try (InputStreamReader isr = new InputStreamReader(is);
             BufferedReader reader = new BufferedReader(isr)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("#") || line.isEmpty()) continue;
                while (line.endsWith("\\")) {
                    line = line.substring(0, line.length() - 1) + reader.readLine().trim();
                }
                int delimiterPosition = line.indexOf('=');
                String key = line.substring(0, delimiterPosition).replace("\\ ", " ");
                String[] values = line.substring(delimiterPosition + 1, line.length()).split(",");
                for (String value : values) {
                    try {
                        value = loadConvert(value);
                        MimeType mime = new MimeType(value);
                        if ("text".equals(mime.getPrimaryType())) {
                            String charset = mime.getParameter("charset");
                            if (DataFlavorUtil.doesSubtypeSupportCharset(mime.getSubType(), charset))
                            {
                                // We need to store the charset and eoln
                                // parameters, if any, so that the
                                // DataTransferer will have this information
                                // for conversion into the native format.
                                DesktopDatatransferService desktopService =
                                        DataFlavorUtil.getDesktopService();
                                if (desktopService.isDesktopPresent()) {
                                    desktopService.registerTextFlavorProperties(
                                            key, charset,
                                            mime.getParameter("eoln"),
                                            mime.getParameter("terminators"));
                                }
                            }

                            // But don't store any of these parameters in the
                            // DataFlavor itself for any text natives (even
                            // non-charset ones). The SystemFlavorMap will
                            // synthesize the appropriate mappings later.
                            mime.removeParameter("charset");
                            mime.removeParameter("class");
                            mime.removeParameter("eoln");
                            mime.removeParameter("terminators");
                            value = mime.toString();
                        }
                    } catch (MimeTypeParseException e) {
                        e.printStackTrace();
                        continue;
                    }

                    DataFlavor flavor;
                    try {
                        flavor = new DataFlavor(value);
                    } catch (Exception e) {
                        try {
                            flavor = new DataFlavor(value, null);
                        } catch (Exception ee) {
                            ee.printStackTrace();
                            continue;
                        }
                    }

                    final LinkedHashSet<DataFlavor> dfs = new LinkedHashSet<>();
                    dfs.add(flavor);

                    if ("text".equals(flavor.getPrimaryType())) {
                        dfs.addAll(convertMimeTypeToDataFlavors(value));
                        store(flavor.mimeType.getBaseType(), key, getTextTypeToNative());
                    }

                    for (DataFlavor df : dfs) {
                        store(df, key, getFlavorToNative());
                        store(key, df, getNativeToFlavor());
                    }
                }
            }
        } catch (IOException e) {
            throw new InternalError("Error reading default flavor mapping", e);
        }
    }

    // Copied from java.util.Properties
    private static String loadConvert(String theString) {
        char aChar;
        int len = theString.length();
        StringBuilder outBuffer = new StringBuilder(len);

        for (int x = 0; x < len; ) {
            aChar = theString.charAt(x++);
            if (aChar == '\\') {
                aChar = theString.charAt(x++);
                if (aChar == 'u') {
                    // Read the xxxx
                    int value = 0;
                    for (int i = 0; i < 4; i++) {
                        aChar = theString.charAt(x++);
                        switch (aChar) {
                            case '0': case '1': case '2': case '3': case '4':
                            case '5': case '6': case '7': case '8': case '9': {
                                value = (value << 4) + aChar - '0';
                                break;
                            }
                            case 'a': case 'b': case 'c':
                            case 'd': case 'e': case 'f': {
                                value = (value << 4) + 10 + aChar - 'a';
                                break;
                            }
                            case 'A': case 'B': case 'C':
                            case 'D': case 'E': case 'F': {
                                value = (value << 4) + 10 + aChar - 'A';
                                break;
                            }
                            default: {
                                throw new IllegalArgumentException(
                                        "Malformed \\uxxxx encoding.");
                            }
                        }
                    }
                    outBuffer.append((char)value);
                } else {
                    if (aChar == 't') {
                        aChar = '\t';
                    } else if (aChar == 'r') {
                        aChar = '\r';
                    } else if (aChar == 'n') {
                        aChar = '\n';
                    } else if (aChar == 'f') {
                        aChar = '\f';
                    }
                    outBuffer.append(aChar);
                }
            } else {
                outBuffer.append(aChar);
            }
        }
        return outBuffer.toString();
    }

    /**
     * Stores the listed object under the specified hash key in map. Unlike a
     * standard map, the listed object will not replace any object already at
     * the appropriate Map location, but rather will be appended to a List
     * stored in that location.
     */
    private <H, L> void store(H hashed, L listed, Map<H, LinkedHashSet<L>> map) {
        LinkedHashSet<L> list = map.get(hashed);
        if (list == null) {
            list = new LinkedHashSet<>(1);
            map.put(hashed, list);
        }
        if (!list.contains(listed)) {
            list.add(listed);
        }
    }

    /**
     * Semantically equivalent to 'nativeToFlavor.get(nat)'. This method handles
     * the case where 'nat' is not found in 'nativeToFlavor'. In that case, a
     * new DataFlavor is synthesized, stored, and returned, if and only if the
     * specified native is encoded as a Java MIME type.
     */
    private LinkedHashSet<DataFlavor> nativeToFlavorLookup(String nat) {
        LinkedHashSet<DataFlavor> flavors = getNativeToFlavor().get(nat);

        if (nat != null && !disabledMappingGenerationKeys.contains(nat)) {
            DesktopDatatransferService desktopService = DataFlavorUtil.getDesktopService();
            if (desktopService.isDesktopPresent()) {
                LinkedHashSet<DataFlavor> platformFlavors =
                        desktopService.getPlatformMappingsForNative(nat);
                if (!platformFlavors.isEmpty()) {
                    if (flavors != null) {
                        // Prepending the platform-specific mappings ensures
                        // that the flavors added with
                        // addFlavorForUnencodedNative() are at the end of
                        // list.
                        platformFlavors.addAll(flavors);
                    }
                    flavors = platformFlavors;
                }
            }
        }

        if (flavors == null && isJavaMIMEType(nat)) {
            String decoded = decodeJavaMIMEType(nat);
            DataFlavor flavor = null;

            try {
                flavor = new DataFlavor(decoded);
            } catch (Exception e) {
                System.err.println("Exception \"" + e.getClass().getName() +
                                   ": " + e.getMessage()  +
                                   "\"while constructing DataFlavor for: " +
                                   decoded);
            }

            if (flavor != null) {
                flavors = new LinkedHashSet<>(1);
                getNativeToFlavor().put(nat, flavors);
                flavors.add(flavor);
                flavorsForNativeCache.remove(nat);

                LinkedHashSet<String> natives = getFlavorToNative().get(flavor);
                if (natives == null) {
                    natives = new LinkedHashSet<>(1);
                    getFlavorToNative().put(flavor, natives);
                }
                natives.add(nat);
                nativesForFlavorCache.remove(flavor);
            }
        }

        return (flavors != null) ? flavors : new LinkedHashSet<>(0);
    }

    /**
     * Semantically equivalent to 'flavorToNative.get(flav)'. This method
     * handles the case where 'flav' is not found in 'flavorToNative' depending
     * on the value of passes 'synthesize' parameter. If 'synthesize' is
     * SYNTHESIZE_IF_NOT_FOUND a native is synthesized, stored, and returned by
     * encoding the DataFlavor's MIME type. Otherwise an empty List is returned
     * and 'flavorToNative' remains unaffected.
     */
    private LinkedHashSet<String> flavorToNativeLookup(final DataFlavor flav,
                                                       final boolean synthesize) {

        LinkedHashSet<String> natives = getFlavorToNative().get(flav);

        if (flav != null && !disabledMappingGenerationKeys.contains(flav)) {
            DesktopDatatransferService desktopService = DataFlavorUtil.getDesktopService();
            if (desktopService.isDesktopPresent()) {
                LinkedHashSet<String> platformNatives =
                        desktopService.getPlatformMappingsForFlavor(flav);
                if (!platformNatives.isEmpty()) {
                    if (natives != null) {
                        // Prepend the platform-specific mappings to ensure
                        // that the natives added with
                        // addUnencodedNativeForFlavor() are at the end of
                        // list.
                        platformNatives.addAll(natives);
                    }
                    natives = platformNatives;
                }
            }
        }

        if (natives == null) {
            if (synthesize) {
                String encoded = encodeDataFlavor(flav);
                natives = new LinkedHashSet<>(1);
                getFlavorToNative().put(flav, natives);
                natives.add(encoded);

                LinkedHashSet<DataFlavor> flavors = getNativeToFlavor().get(encoded);
                if (flavors == null) {
                    flavors = new LinkedHashSet<>(1);
                    getNativeToFlavor().put(encoded, flavors);
                }
                flavors.add(flav);

                nativesForFlavorCache.remove(flav);
                flavorsForNativeCache.remove(encoded);
            } else {
                natives = new LinkedHashSet<>(0);
            }
        }

        return new LinkedHashSet<>(natives);
    }

    /**
     * Returns a {@code List} of {@code String} natives to which the specified
     * {@code DataFlavor} can be translated by the data transfer subsystem. The
     * {@code List} will be sorted from best native to worst. That is, the first
     * native will best reflect data in the specified flavor to the underlying
     * native platform.
     * <p>
     * If the specified {@code DataFlavor} is previously unknown to the data
     * transfer subsystem and the data transfer subsystem is unable to translate
     * this {@code DataFlavor} to any existing native, then invoking this method
     * will establish a mapping in both directions between the specified
     * {@code DataFlavor} and an encoded version of its MIME type as its native.
     *
     * @param  flav the {@code DataFlavor} whose corresponding natives should be
     *         returned. If {@code null} is specified, all natives currently
     *         known to the data transfer subsystem are returned in a
     *         non-deterministic order.
     * @return a {@code java.util.List} of {@code java.lang.String} objects
     *         which are platform-specific representations of platform-specific
     *         data formats
     * @see #encodeDataFlavor
     * @since 1.4
     */
    @Override
    public synchronized List<String> getNativesForFlavor(DataFlavor flav) {
        LinkedHashSet<String> retval = nativesForFlavorCache.check(flav);
        if (retval != null) {
            return new ArrayList<>(retval);
        }

        if (flav == null) {
            retval = new LinkedHashSet<>(getNativeToFlavor().keySet());
        } else if (disabledMappingGenerationKeys.contains(flav)) {
            // In this case we shouldn't synthesize a native for this flavor,
            // since its mappings were explicitly specified.
            retval = flavorToNativeLookup(flav, false);
        } else if (DataFlavorUtil.isFlavorCharsetTextType(flav)) {
            retval = new LinkedHashSet<>(0);

            // For text/* flavors, flavor-to-native mappings specified in
            // flavormap.properties are stored per flavor's base type.
            if ("text".equals(flav.getPrimaryType())) {
                LinkedHashSet<String> textTypeNatives =
                        getTextTypeToNative().get(flav.mimeType.getBaseType());
                if (textTypeNatives != null) {
                    retval.addAll(textTypeNatives);
                }
            }

            // Also include text/plain natives, but don't duplicate Strings
            LinkedHashSet<String> textTypeNatives =
                    getTextTypeToNative().get(TEXT_PLAIN_BASE_TYPE);
            if (textTypeNatives != null) {
                retval.addAll(textTypeNatives);
            }

            if (retval.isEmpty()) {
                retval = flavorToNativeLookup(flav, true);
            } else {
                // In this branch it is guaranteed that natives explicitly
                // listed for flav's MIME type were added with
                // addUnencodedNativeForFlavor(), so they have lower priority.
                retval.addAll(flavorToNativeLookup(flav, false));
            }
        } else if (DataFlavorUtil.isFlavorNoncharsetTextType(flav)) {
            retval = getTextTypeToNative().get(flav.mimeType.getBaseType());

            if (retval == null || retval.isEmpty()) {
                retval = flavorToNativeLookup(flav, true);
            } else {
                // In this branch it is guaranteed that natives explicitly
                // listed for flav's MIME type were added with
                // addUnencodedNativeForFlavor(), so they have lower priority.
                retval.addAll(flavorToNativeLookup(flav, false));
            }
        } else {
            retval = flavorToNativeLookup(flav, true);
        }

        nativesForFlavorCache.put(flav, retval);
        // Create a copy, because client code can modify the returned list.
        return new ArrayList<>(retval);
    }

    /**
     * Returns a {@code List} of {@code DataFlavor}s to which the specified
     * {@code String} native can be translated by the data transfer subsystem.
     * The {@code List} will be sorted from best {@code DataFlavor} to worst.
     * That is, the first {@code DataFlavor} will best reflect data in the
     * specified native to a Java application.
     * <p>
     * If the specified native is previously unknown to the data transfer
     * subsystem, and that native has been properly encoded, then invoking this
     * method will establish a mapping in both directions between the specified
     * native and a {@code DataFlavor} whose MIME type is a decoded version of
     * the native.
     * <p>
     * If the specified native is not a properly encoded native and the mappings
     * for this native have not been altered with {@code setFlavorsForNative},
     * then the contents of the {@code List} is platform dependent, but
     * {@code null} cannot be returned.
     *
     * @param  nat the native whose corresponding {@code DataFlavor}s should be
     *         returned. If {@code null} is specified, all {@code DataFlavor}s
     *         currently known to the data transfer subsystem are returned in a
     *         non-deterministic order.
     * @return a {@code java.util.List} of {@code DataFlavor} objects into which
     *         platform-specific data in the specified, platform-specific native
     *         can be translated
     * @see #encodeJavaMIMEType
     * @since 1.4
     */
    @Override
    public synchronized List<DataFlavor> getFlavorsForNative(String nat) {
        LinkedHashSet<DataFlavor> returnValue = flavorsForNativeCache.check(nat);
        if (returnValue != null) {
            return new ArrayList<>(returnValue);
        } else {
            returnValue = new LinkedHashSet<>();
        }

        if (nat == null) {
            for (String n : getNativesForFlavor(null)) {
                returnValue.addAll(getFlavorsForNative(n));
            }
        } else {
            final LinkedHashSet<DataFlavor> flavors = nativeToFlavorLookup(nat);
            if (disabledMappingGenerationKeys.contains(nat)) {
                return new ArrayList<>(flavors);
            }

            final LinkedHashSet<DataFlavor> flavorsWithSynthesized =
                    nativeToFlavorLookup(nat);

            for (DataFlavor df : flavorsWithSynthesized) {
                returnValue.add(df);
                if ("text".equals(df.getPrimaryType())) {
                    String baseType = df.mimeType.getBaseType();
                    returnValue.addAll(convertMimeTypeToDataFlavors(baseType));
                }
            }
        }
        flavorsForNativeCache.put(nat, returnValue);
        return new ArrayList<>(returnValue);
    }

    @SuppressWarnings("deprecation")
    private static Set<DataFlavor> convertMimeTypeToDataFlavors(
        final String baseType) {

        final Set<DataFlavor> returnValue = new LinkedHashSet<>();

        String subType = null;

        try {
            final MimeType mimeType = new MimeType(baseType);
            subType = mimeType.getSubType();
        } catch (MimeTypeParseException mtpe) {
            // Cannot happen, since we checked all mappings
            // on load from flavormap.properties.
        }

        if (DataFlavorUtil.doesSubtypeSupportCharset(subType, null)) {
            if (TEXT_PLAIN_BASE_TYPE.equals(baseType))
            {
                returnValue.add(DataFlavor.stringFlavor);
            }

            for (String unicodeClassName : UNICODE_TEXT_CLASSES) {
                final String mimeType = baseType + ";charset=Unicode;class=" +
                                            unicodeClassName;

                final LinkedHashSet<String> mimeTypes =
                    handleHtmlMimeTypes(baseType, mimeType);
                for (String mt : mimeTypes) {
                    DataFlavor toAdd = null;
                    try {
                        toAdd = new DataFlavor(mt);
                    } catch (ClassNotFoundException cannotHappen) {
                    }
                    returnValue.add(toAdd);
                }
            }

            for (String charset : DataFlavorUtil.standardEncodings()) {

                for (String encodedTextClass : ENCODED_TEXT_CLASSES) {
                    final String mimeType =
                            baseType + ";charset=" + charset +
                            ";class=" + encodedTextClass;

                    final LinkedHashSet<String> mimeTypes =
                        handleHtmlMimeTypes(baseType, mimeType);

                    for (String mt : mimeTypes) {

                        DataFlavor df = null;

                        try {
                            df = new DataFlavor(mt);
                            // Check for equality to plainTextFlavor so
                            // that we can ensure that the exact charset of
                            // plainTextFlavor, not the canonical charset
                            // or another equivalent charset with a
                            // different name, is used.
                            if (df.equals(DataFlavor.plainTextFlavor)) {
                                df = DataFlavor.plainTextFlavor;
                            }
                        } catch (ClassNotFoundException cannotHappen) {
                        }

                        returnValue.add(df);
                    }
                }
            }

            if (TEXT_PLAIN_BASE_TYPE.equals(baseType))
            {
                returnValue.add(DataFlavor.plainTextFlavor);
            }
        } else {
            // Non-charset text natives should be treated as
            // opaque, 8-bit data in any of its various
            // representations.
            for (String encodedTextClassName : ENCODED_TEXT_CLASSES) {
                DataFlavor toAdd = null;
                try {
                    toAdd = new DataFlavor(baseType +
                         ";class=" + encodedTextClassName);
                } catch (ClassNotFoundException cannotHappen) {
                }
                returnValue.add(toAdd);
            }
        }
        return returnValue;
    }

    private static final String [] htmlDocumentTypes =
            new String [] {"all", "selection", "fragment"};

    private static LinkedHashSet<String> handleHtmlMimeTypes(String baseType,
                                                             String mimeType) {

        LinkedHashSet<String> returnValues = new LinkedHashSet<>();

        if (HTML_TEXT_BASE_TYPE.equals(baseType)) {
            for (String documentType : htmlDocumentTypes) {
                returnValues.add(mimeType + ";document=" + documentType);
            }
        } else {
            returnValues.add(mimeType);
        }

        return returnValues;
    }

    /**
     * Returns a {@code Map} of the specified {@code DataFlavor}s to their most
     * preferred {@code String} native. Each native value will be the same as
     * the first native in the List returned by {@code getNativesForFlavor} for
     * the specified flavor.
     * <p>
     * If a specified {@code DataFlavor} is previously unknown to the data
     * transfer subsystem, then invoking this method will establish a mapping in
     * both directions between the specified {@code DataFlavor} and an encoded
     * version of its MIME type as its native.
     *
     * @param  flavors an array of {@code DataFlavor}s which will be the key set
     *         of the returned {@code Map}. If {@code null} is specified, a
     *         mapping of all {@code DataFlavor}s known to the data transfer
     *         subsystem to their most preferred {@code String} natives will be
     *         returned.
     * @return a {@code java.util.Map} of {@code DataFlavor}s to {@code String}
     *         natives
     * @see #getNativesForFlavor
     * @see #encodeDataFlavor
     */
    @Override
    public synchronized Map<DataFlavor,String> getNativesForFlavors(DataFlavor[] flavors)
    {
        // Use getNativesForFlavor to generate extra natives for text flavors
        // and stringFlavor

        if (flavors == null) {
            List<DataFlavor> flavor_list = getFlavorsForNative(null);
            flavors = new DataFlavor[flavor_list.size()];
            flavor_list.toArray(flavors);
        }

        Map<DataFlavor, String> retval = new HashMap<>(flavors.length, 1.0f);
        for (DataFlavor flavor : flavors) {
            List<String> natives = getNativesForFlavor(flavor);
            String nat = (natives.isEmpty()) ? null : natives.get(0);
            retval.put(flavor, nat);
        }

        return retval;
    }

    /**
     * Returns a {@code Map} of the specified {@code String} natives to their
     * most preferred {@code DataFlavor}. Each {@code DataFlavor} value will be
     * the same as the first {@code DataFlavor} in the List returned by
     * {@code getFlavorsForNative} for the specified native.
     * <p>
     * If a specified native is previously unknown to the data transfer
     * subsystem, and that native has been properly encoded, then invoking this
     * method will establish a mapping in both directions between the specified
     * native and a {@code DataFlavor} whose MIME type is a decoded version of
     * the native.
     *
     * @param  natives an array of {@code String}s which will be the key set of
     *         the returned {@code Map}. If {@code null} is specified, a mapping
     *         of all supported {@code String} natives to their most preferred
     *         {@code DataFlavor}s will be returned.
     * @return a {@code java.util.Map} of {@code String} natives to
     *         {@code DataFlavor}s
     * @see #getFlavorsForNative
     * @see #encodeJavaMIMEType
     */
    @Override
    public synchronized Map<String,DataFlavor> getFlavorsForNatives(String[] natives)
    {
        // Use getFlavorsForNative to generate extra flavors for text natives
        if (natives == null) {
            List<String> nativesList = getNativesForFlavor(null);
            natives = new String[nativesList.size()];
            nativesList.toArray(natives);
        }

        Map<String, DataFlavor> retval = new HashMap<>(natives.length, 1.0f);
        for (String aNative : natives) {
            List<DataFlavor> flavors = getFlavorsForNative(aNative);
            DataFlavor flav = (flavors.isEmpty())? null : flavors.get(0);
            retval.put(aNative, flav);
        }
        return retval;
    }

    /**
     * Adds a mapping from the specified {@code DataFlavor} (and all
     * {@code DataFlavor}s equal to the specified {@code DataFlavor}) to the
     * specified {@code String} native. Unlike {@code getNativesForFlavor}, the
     * mapping will only be established in one direction, and the native will
     * not be encoded. To establish a two-way mapping, call
     * {@code addFlavorForUnencodedNative} as well. The new mapping will be of
     * lower priority than any existing mapping. This method has no effect if a
     * mapping from the specified or equal {@code DataFlavor} to the specified
     * {@code String} native already exists.
     *
     * @param  flav the {@code DataFlavor} key for the mapping
     * @param  nat the {@code String} native value for the mapping
     * @throws NullPointerException if flav or nat is {@code null}
     * @see #addFlavorForUnencodedNative
     * @since 1.4
     */
    public synchronized void addUnencodedNativeForFlavor(DataFlavor flav,
                                                         String nat) {
        Objects.requireNonNull(nat, "Null native not permitted");
        Objects.requireNonNull(flav, "Null flavor not permitted");

        LinkedHashSet<String> natives = getFlavorToNative().get(flav);
        if (natives == null) {
            natives = new LinkedHashSet<>(1);
            getFlavorToNative().put(flav, natives);
        }
        natives.add(nat);
        nativesForFlavorCache.remove(flav);
    }

    /**
     * Discards the current mappings for the specified {@code DataFlavor} and
     * all {@code DataFlavor}s equal to the specified {@code DataFlavor}, and
     * creates new mappings to the specified {@code String} natives. Unlike
     * {@code getNativesForFlavor}, the mappings will only be established in one
     * direction, and the natives will not be encoded. To establish two-way
     * mappings, call {@code setFlavorsForNative} as well. The first native in
     * the array will represent the highest priority mapping. Subsequent natives
     * will represent mappings of decreasing priority.
     * <p>
     * If the array contains several elements that reference equal
     * {@code String} natives, this method will establish new mappings for the
     * first of those elements and ignore the rest of them.
     * <p>
     * It is recommended that client code not reset mappings established by the
     * data transfer subsystem. This method should only be used for
     * application-level mappings.
     *
     * @param  flav the {@code DataFlavor} key for the mappings
     * @param  natives the {@code String} native values for the mappings
     * @throws NullPointerException if flav or natives is {@code null} or if
     *         natives contains {@code null} elements
     * @see #setFlavorsForNative
     * @since 1.4
     */
    public synchronized void setNativesForFlavor(DataFlavor flav,
                                                 String[] natives) {
        Objects.requireNonNull(natives, "Null natives not permitted");
        Objects.requireNonNull(flav, "Null flavors not permitted");

        getFlavorToNative().remove(flav);
        for (String aNative : natives) {
            addUnencodedNativeForFlavor(flav, aNative);
        }
        disabledMappingGenerationKeys.add(flav);
        nativesForFlavorCache.remove(flav);
    }

    /**
     * Adds a mapping from a single {@code String} native to a single
     * {@code DataFlavor}. Unlike {@code getFlavorsForNative}, the mapping will
     * only be established in one direction, and the native will not be encoded.
     * To establish a two-way mapping, call {@code addUnencodedNativeForFlavor}
     * as well. The new mapping will be of lower priority than any existing
     * mapping. This method has no effect if a mapping from the specified
     * {@code String} native to the specified or equal {@code DataFlavor}
     * already exists.
     *
     * @param  nat the {@code String} native key for the mapping
     * @param  flav the {@code DataFlavor} value for the mapping
     * @throws NullPointerException if {@code nat} or {@code flav} is
     *         {@code null}
     * @see #addUnencodedNativeForFlavor
     * @since 1.4
     */
    public synchronized void addFlavorForUnencodedNative(String nat,
                                                         DataFlavor flav) {
        Objects.requireNonNull(nat, "Null native not permitted");
        Objects.requireNonNull(flav, "Null flavor not permitted");

        LinkedHashSet<DataFlavor> flavors = getNativeToFlavor().get(nat);
        if (flavors == null) {
            flavors = new LinkedHashSet<>(1);
            getNativeToFlavor().put(nat, flavors);
        }
        flavors.add(flav);
        flavorsForNativeCache.remove(nat);
    }

    /**
     * Discards the current mappings for the specified {@code String} native,
     * and creates new mappings to the specified {@code DataFlavor}s. Unlike
     * {@code getFlavorsForNative}, the mappings will only be established in one
     * direction, and the natives need not be encoded. To establish two-way
     * mappings, call {@code setNativesForFlavor} as well. The first
     * {@code DataFlavor} in the array will represent the highest priority
     * mapping. Subsequent {@code DataFlavor}s will represent mappings of
     * decreasing priority.
     * <p>
     * If the array contains several elements that reference equal
     * {@code DataFlavor}s, this method will establish new mappings for the
     * first of those elements and ignore the rest of them.
     * <p>
     * It is recommended that client code not reset mappings established by the
     * data transfer subsystem. This method should only be used for
     * application-level mappings.
     *
     * @param  nat the {@code String} native key for the mappings
     * @param  flavors the {@code DataFlavor} values for the mappings
     * @throws NullPointerException if {@code nat} or {@code flavors} is
     *         {@code null} or if {@code flavors} contains {@code null} elements
     * @see #setNativesForFlavor
     * @since 1.4
     */
    public synchronized void setFlavorsForNative(String nat,
                                                 DataFlavor[] flavors) {
        Objects.requireNonNull(nat, "Null native not permitted");
        Objects.requireNonNull(flavors, "Null flavors not permitted");

        getNativeToFlavor().remove(nat);
        for (DataFlavor flavor : flavors) {
            addFlavorForUnencodedNative(nat, flavor);
        }
        disabledMappingGenerationKeys.add(nat);
        flavorsForNativeCache.remove(nat);
    }

    /**
     * Encodes a MIME type for use as a {@code String} native. The format of an
     * encoded representation of a MIME type is implementation-dependent. The
     * only restrictions are:
     * <ul>
     * <li>The encoded representation is {@code null} if and only if the MIME
     *     type {@code String} is {@code null}</li>
     * <li>The encoded representations for two non-{@code null} MIME type
     *     {@code String}s are equal if and only if these {@code String}s are
     *     equal according to {@code String.equals(Object)}</li>
     * </ul>
     * The reference implementation of this method returns the specified MIME
     * type {@code String} prefixed with {@code JAVA_DATAFLAVOR:}.
     *
     * @param  mimeType the MIME type to encode
     * @return the encoded {@code String}, or {@code null} if {@code mimeType}
     *         is {@code null}
     */
    public static String encodeJavaMIMEType(String mimeType) {
        return (mimeType != null)
            ? JavaMIME + mimeType
            : null;
    }

    /**
     * Encodes a {@code DataFlavor} for use as a {@code String} native. The
     * format of an encoded {@code DataFlavor} is implementation-dependent. The
     * only restrictions are:
     * <ul>
     * <li>The encoded representation is {@code null} if and only if the
     *     specified {@code DataFlavor} is {@code null} or its MIME type
     *     {@code String} is {@code null}</li>
     * <li>The encoded representations for two non-{@code null}
     *     {@code DataFlavor}s with non-{@code null} MIME type {@code String}s
     *     are equal if and only if the MIME type {@code String}s of these
     *     {@code DataFlavor}s are equal according to
     *     {@code String.equals(Object)}</li>
     * </ul>
     * The reference implementation of this method returns the MIME type
     * {@code String} of the specified {@code DataFlavor} prefixed with
     * {@code JAVA_DATAFLAVOR:}.
     *
     * @param  flav the {@code DataFlavor} to encode
     * @return the encoded {@code String}, or {@code null} if {@code flav} is
     *         {@code null} or has a {@code null} MIME type
     */
    public static String encodeDataFlavor(DataFlavor flav) {
        return (flav != null)
            ? SystemFlavorMap.encodeJavaMIMEType(flav.getMimeType())
            : null;
    }

    /**
     * Returns whether the specified {@code String} is an encoded Java MIME
     * type.
     *
     * @param  str the {@code String} to test
     * @return {@code true} if the {@code String} is encoded; {@code false}
     *         otherwise
     */
    public static boolean isJavaMIMEType(String str) {
        return (str != null && str.startsWith(JavaMIME, 0));
    }

    /**
     * Decodes a {@code String} native for use as a Java MIME type.
     *
     * @param  nat the {@code String} to decode
     * @return the decoded Java MIME type, or {@code null} if {@code nat} is not
     *         an encoded {@code String} native
     */
    public static String decodeJavaMIMEType(String nat) {
        return (isJavaMIMEType(nat))
            ? nat.substring(JavaMIME.length(), nat.length()).trim()
            : null;
    }

    /**
     * Decodes a {@code String} native for use as a {@code DataFlavor}.
     *
     * @param  nat the {@code String} to decode
     * @return the decoded {@code DataFlavor}, or {@code null} if {@code nat} is
     *         not an encoded {@code String} native
     * @throws ClassNotFoundException if the class of the data flavor is not
     *         loaded
     */
    public static DataFlavor decodeDataFlavor(String nat)
        throws ClassNotFoundException
    {
        String retval_str = SystemFlavorMap.decodeJavaMIMEType(nat);
        return (retval_str != null)
            ? new DataFlavor(retval_str)
            : null;
    }

    private static final class SoftCache<K, V> {
        Map<K, SoftReference<LinkedHashSet<V>>> cache;

        public void put(K key, LinkedHashSet<V> value) {
            if (cache == null) {
                cache = new HashMap<>(1);
            }
            cache.put(key, new SoftReference<>(value));
        }

        public void remove(K key) {
            if (cache == null) return;
            cache.remove(null);
            cache.remove(key);
        }

        public LinkedHashSet<V> check(K key) {
            if (cache == null) return null;
            SoftReference<LinkedHashSet<V>> ref = cache.get(key);
            if (ref != null) {
                return ref.get();
            }
            return null;
        }
    }
}
