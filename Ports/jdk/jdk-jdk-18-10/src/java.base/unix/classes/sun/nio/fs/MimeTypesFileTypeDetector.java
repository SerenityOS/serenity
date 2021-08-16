/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.nio.fs;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * File type detector that uses a file extension to look up its MIME type
 * based on a mime.types file.
 */

class MimeTypesFileTypeDetector extends AbstractFileTypeDetector {

    // path to mime.types file
    private final Path mimeTypesFile;

    // map of extension to MIME type
    private Map<String,String> mimeTypeMap;

    // set to true when file loaded
    private volatile boolean loaded;

    public MimeTypesFileTypeDetector(Path filePath) {
        mimeTypesFile = filePath;
    }

    @Override
    protected String implProbeContentType(Path path) {
        Path fn = path.getFileName();
        if (fn == null)
            return null;  // no file name

        String ext = getExtension(fn.toString());
        if (ext.isEmpty())
            return null;  // no extension

        loadMimeTypes();
        if (mimeTypeMap == null || mimeTypeMap.isEmpty())
            return null;

        // Case-sensitive search
        String mimeType;
        do {
            mimeType = mimeTypeMap.get(ext);
            if (mimeType == null)
                ext = getExtension(ext);
        } while (mimeType == null && !ext.isEmpty());

        return mimeType;
    }

    /**
     * Parse the mime types file, and store the type-extension mappings into
     * mimeTypeMap. The mime types file is not loaded until the first probe
     * to achieve the lazy initialization. It adopts double-checked locking
     * optimization to reduce the locking overhead.
     */
    private void loadMimeTypes() {
        if (!loaded) {
            synchronized (this) {
                if (!loaded) {
                    @SuppressWarnings("removal")
                    List<String> lines = AccessController.doPrivileged(
                        new PrivilegedAction<>() {
                            @Override
                            public List<String> run() {
                                try {
                                    return Files.readAllLines(mimeTypesFile,
                                                              Charset.defaultCharset());
                                } catch (IOException ignore) {
                                    return Collections.emptyList();
                                }
                            }
                        });

                    mimeTypeMap = new HashMap<>(lines.size());
                    String entry = "";
                    for (String line : lines) {
                        entry += line;
                        if (entry.endsWith("\\")) {
                            entry = entry.substring(0, entry.length() - 1);
                            continue;
                        }
                        parseMimeEntry(entry);
                        entry = "";
                    }
                    if (!entry.isEmpty()) {
                        parseMimeEntry(entry);
                    }
                    loaded = true;
                }
            }
        }
    }

    /**
     * Parse a mime-types entry, which can have the following formats.
     * 1) Simple space-delimited format
     * image/jpeg   jpeg jpg jpe JPG
     *
     * 2) Netscape key-value pair format
     * type=application/x-java-jnlp-file desc="Java Web Start" exts="jnlp"
     * or
     * type=text/html exts=htm,html
     */
    private void parseMimeEntry(String entry) {
        entry = entry.trim();
        if (entry.isEmpty() || entry.charAt(0) == '#')
            return;

        entry = entry.replaceAll("\\s*#.*", "");
        int equalIdx = entry.indexOf('=');
        if (equalIdx > 0) {
            // Parse a mime-types command having the key-value pair format
            final String TYPEEQUAL = "type=";
            String typeRegex = "\\b" + TYPEEQUAL +
                    "(\"\\p{Graph}+?/\\p{Graph}+?\"|\\p{Graph}+/\\p{Graph}+\\b)";
            Pattern typePattern = Pattern.compile(typeRegex);
            Matcher typeMatcher = typePattern.matcher(entry);

            if (typeMatcher.find()) {
                String type = typeMatcher.group().substring(TYPEEQUAL.length());
                if (type.charAt(0) == '"') {
                    type = type.substring(1, type.length() - 1);
                }

                final String EXTEQUAL = "exts=";
                String extRegex = "\\b" + EXTEQUAL +
                        "(\"[\\p{Graph}\\p{Blank}]+?\"|\\p{Graph}+\\b)";
                Pattern extPattern = Pattern.compile(extRegex);
                Matcher extMatcher = extPattern.matcher(entry);

                if (extMatcher.find()) {
                    String exts =
                            extMatcher.group().substring(EXTEQUAL.length());
                    if (exts.charAt(0) == '"') {
                        exts = exts.substring(1, exts.length() - 1);
                    }
                    String[] extList = exts.split("[\\p{Blank}\\p{Punct}]+");
                    for (String ext : extList) {
                        putIfAbsent(ext, type);
                    }
                }
            }
        } else {
            // Parse a mime-types command having the space-delimited format
            String[] elements = entry.split("\\s+");
            int i = 1;
            while (i < elements.length) {
                putIfAbsent(elements[i++], elements[0]);
            }
        }
    }

    private void putIfAbsent(String key, String value) {
        if (key != null && !key.isEmpty() &&
            value != null && !value.isEmpty() &&
            !mimeTypeMap.containsKey(key))
        {
            mimeTypeMap.put(key, value);
        }
    }
}
