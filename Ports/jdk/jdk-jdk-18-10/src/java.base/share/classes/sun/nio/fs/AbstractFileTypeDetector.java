/*
 * Copyright (c) 2008, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.net.FileNameMap;
import java.net.URLConnection;
import java.nio.file.Path;
import java.nio.file.spi.FileTypeDetector;
import java.util.Locale;
import java.io.IOException;

/**
 * Base implementation of FileTypeDetector
 */

public abstract class AbstractFileTypeDetector
    extends FileTypeDetector
{
    protected AbstractFileTypeDetector() {
        super();
    }

    /**
     * Returns the extension of a file name, specifically the portion of the
     * parameter string after the first dot. If the parameter is {@code null},
     * empty, does not contain a dot, or the dot is the last character, then an
     * empty string is returned, otherwise the characters after the dot are
     * returned.
     *
     * @param name A file name
     * @return The characters after the first dot or an empty string.
     */
    protected final String getExtension(String name) {
        String ext = "";
        if (name != null && !name.isEmpty()) {
            int dot = name.indexOf('.');
            if ((dot >= 0) && (dot < name.length() - 1)) {
                ext = name.substring(dot + 1);
            }
        }
        return ext;
    }

    /**
     * Invokes the appropriate probe method to guess a file's content type,
     * and checks that the content type's syntax is valid.
     */
    @Override
    public final String probeContentType(Path file) throws IOException {
        if (file == null)
            throw new NullPointerException("'file' is null");
        String result = implProbeContentType(file);

        // Fall back to content types property.
        if (result == null) {
            Path fileName = file.getFileName();
            if (fileName != null) {
                FileNameMap fileNameMap = URLConnection.getFileNameMap();
                result = fileNameMap.getContentTypeFor(fileName.toString());
            }
        }

        return (result == null) ? null : parse(result);
    }

    /**
     * Probes the given file to guess its content type.
     */
    protected abstract String implProbeContentType(Path file)
        throws IOException;

    /**
     * Parses a candidate content type into its type and subtype, returning
     * null if either token is invalid.
     */
    private static String parse(String s) {
        int slash = s.indexOf('/');
        int semicolon = s.indexOf(';');
        if (slash < 0)
            return null;  // no subtype
        String type = s.substring(0, slash).trim().toLowerCase(Locale.ENGLISH);
        if (!isValidToken(type))
            return null;  // invalid type
        String subtype = (semicolon < 0) ? s.substring(slash + 1) :
            s.substring(slash + 1, semicolon);
        subtype = subtype.trim().toLowerCase(Locale.ENGLISH);
        if (!isValidToken(subtype))
            return null;  // invalid subtype
        StringBuilder sb = new StringBuilder(type.length() + subtype.length() + 1);
        sb.append(type);
        sb.append('/');
        sb.append(subtype);
        return sb.toString();
    }

    /**
     * Special characters
     */
    private static final String TSPECIALS = "()<>@,;:/[]?=\\\"";

    /**
     * Returns true if the character is a valid token character.
     */
    private static boolean isTokenChar(char c) {
        return (c > 040) && (c < 0177) && (TSPECIALS.indexOf(c) < 0);
    }

    /**
     * Returns true if the given string is a legal type or subtype.
     */
    private static boolean isValidToken(String s) {
        int len = s.length();
        if (len == 0)
            return false;
        for (int i = 0; i < len; i++) {
            if (!isTokenChar(s.charAt(i)))
                return false;
        }
        return true;
    }
}
