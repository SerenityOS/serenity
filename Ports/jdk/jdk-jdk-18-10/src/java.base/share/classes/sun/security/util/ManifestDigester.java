/*
 * Copyright (c) 1997, 2019, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.util;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * This class is used to compute digests on sections of the Manifest.
 * Please note that multiple sections might have the same name, and they
 * all belong to a single Entry.
 */
public class ManifestDigester {

    /**
     * The part "{@code Manifest-Main-Attributes}" of the main attributes
     * digest header name in a signature file as described in the jar
     * specification:
     * <blockquote>{@code x-Digest-Manifest-Main-Attributes}
     * (where x is the standard name of a {@link MessageDigest} algorithm):
     * The value of this attribute is the digest value of the main attributes
     * of the manifest.</blockquote>
     * @see <a href="{@docRoot}/../specs/jar/jar.html#signature-file">
     * JAR File Specification, section Signature File</a>
     * @see #getMainAttsEntry
     */
    public static final String MF_MAIN_ATTRS = "Manifest-Main-Attributes";

    /** the raw bytes of the manifest */
    private final byte[] rawBytes;

    private final Entry mainAttsEntry;

    /** individual sections by their names */
    private final HashMap<String, Entry> entries = new HashMap<>();

    /** state returned by findSection */
    static class Position {
        int endOfFirstLine; // not including newline character

        int endOfSection; // end of section, not including the blank line
                          // between sections
        int startOfNext;  // the start of the next section
    }

    /**
     * find a section in the manifest.
     *
     * @param offset should point to the starting offset with in the
     * raw bytes of the next section.
     *
     * @pos set by
     *
     * @return false if end of bytes has been reached, otherwise returns
     *          true
     */
    @SuppressWarnings("fallthrough")
    private boolean findSection(int offset, Position pos)
    {
        int i = offset, len = rawBytes.length;
        int last = offset - 1;
        int next;
        boolean allBlank = true;

        /* denotes that a position is not yet assigned.
         * As a primitive type int it cannot be null
         * and -1 would be confused with (i - 1) when i == 0 */
        final int UNASSIGNED = Integer.MIN_VALUE;

        pos.endOfFirstLine = UNASSIGNED;

        while (i < len) {
            byte b = rawBytes[i];
            switch(b) {
            case '\r':
                if (pos.endOfFirstLine == UNASSIGNED)
                    pos.endOfFirstLine = i-1;
                if (i < len - 1 && rawBytes[i + 1] == '\n')
                    i++;
                /* fall through */
            case '\n':
                if (pos.endOfFirstLine == UNASSIGNED)
                    pos.endOfFirstLine = i-1;
                if (allBlank || (i == len-1)) {
                    pos.endOfSection = allBlank ? last : i;
                    pos.startOfNext = i+1;
                    return true;
                }
                else {
                    // start of a new line
                    last = i;
                    allBlank = true;
                }
                break;
            default:
                allBlank = false;
                break;
            }
            i++;
        }
        return false;
    }

    public ManifestDigester(byte[] bytes)
    {
        rawBytes = bytes;

        Position pos = new Position();

        if (!findSection(0, pos)) {
            mainAttsEntry = null;
            return; // XXX: exception?
        }

        // create an entry for main attributes
        mainAttsEntry = new Entry().addSection(new Section(
                0, pos.endOfSection + 1, pos.startOfNext, rawBytes));

        int start = pos.startOfNext;
        while(findSection(start, pos)) {
            int len = pos.endOfFirstLine-start+1;
            int sectionLen = pos.endOfSection-start+1;
            int sectionLenWithBlank = pos.startOfNext-start;

            if (len >= 6) { // 6 == "Name: ".length()
                if (isNameAttr(bytes, start)) {
                    ByteArrayOutputStream nameBuf = new ByteArrayOutputStream();
                    nameBuf.write(bytes, start+6, len-6);

                    int i = start + len;
                    if ((i-start) < sectionLen) {
                        if (bytes[i] == '\r'
                                && i + 1 - start < sectionLen
                                && bytes[i + 1] == '\n') {
                            i += 2;
                        } else {
                            i += 1;
                        }
                    }

                    while ((i-start) < sectionLen) {
                        if (bytes[i++] == ' ') {
                            // name is wrapped
                            int wrapStart = i;
                            while (((i-start) < sectionLen)
                                    && (bytes[i] != '\r')
                                    && (bytes[i] != '\n')) i++;
                            int wrapLen = i - wrapStart;
                            if (i - start < sectionLen) {
                                i++;
                                if (bytes[i - 1] == '\r'
                                    && i - start < sectionLen
                                    && bytes[i] == '\n')
                                        i++;
                            }

                            nameBuf.write(bytes, wrapStart, wrapLen);
                        } else {
                            break;
                        }
                    }

                    entries.computeIfAbsent(nameBuf.toString(UTF_8),
                                            dummy -> new Entry())
                            .addSection(new Section(start, sectionLen,
                                    sectionLenWithBlank, rawBytes));
                }
            }
            start = pos.startOfNext;
        }
    }

    private boolean isNameAttr(byte[] bytes, int start)
    {
        return ((bytes[start] == 'N') || (bytes[start] == 'n')) &&
               ((bytes[start+1] == 'a') || (bytes[start+1] == 'A')) &&
               ((bytes[start+2] == 'm') || (bytes[start+2] == 'M')) &&
               ((bytes[start+3] == 'e') || (bytes[start+3] == 'E')) &&
               (bytes[start+4] == ':') &&
               (bytes[start+5] == ' ');
    }

    public static class Entry {

        // One Entry for one name, and one name can have multiple sections.
        // According to the JAR File Specification: "If there are multiple
        // individual sections for the same file entry, the attributes in
        // these sections are merged."
        private List<Section> sections = new ArrayList<>();
        boolean oldStyle;

        private Entry addSection(Section sec)
        {
            sections.add(sec);
            return this;
        }

        /**
         * Check if the sections (particularly the last one of usually only one)
         * are properly delimited with a trailing blank line so that another
         * section can be correctly appended and return {@code true} or return
         * {@code false} to indicate that reproduction is not advised and should
         * be carried out with a clean "normalized" newly-written manifest.
         *
         * @see #reproduceRaw
         */
        public boolean isProperlyDelimited() {
            return sections.stream().allMatch(
                    Section::isProperlySectionDelimited);
        }

        public void reproduceRaw(OutputStream out) throws IOException {
            for (Section sec : sections) {
                out.write(sec.rawBytes, sec.offset, sec.lengthWithBlankLine);
            }
        }

        public byte[] digest(MessageDigest md)
        {
            md.reset();
            for (Section sec : sections) {
                if (oldStyle) {
                    Section.doOldStyle(md, sec.rawBytes, sec.offset, sec.lengthWithBlankLine);
                } else {
                    md.update(sec.rawBytes, sec.offset, sec.lengthWithBlankLine);
                }
            }
            return md.digest();
        }

        /** Netscape doesn't include the new line. Intel and JavaSoft do */

        public byte[] digestWorkaround(MessageDigest md)
        {
            md.reset();
            for (Section sec : sections) {
                md.update(sec.rawBytes, sec.offset, sec.length);
            }
            return md.digest();
        }
    }

    private static class Section {
        int offset;
        int length;
        int lengthWithBlankLine;
        byte[] rawBytes;

        public Section(int offset, int length,
                     int lengthWithBlankLine, byte[] rawBytes)
        {
            this.offset = offset;
            this.length = length;
            this.lengthWithBlankLine = lengthWithBlankLine;
            this.rawBytes = rawBytes;
        }

        /**
         * Returns {@code true} if the raw section is terminated with a blank
         * line so that another section can possibly be appended resulting in a
         * valid manifest and {@code false} otherwise.
         */
        private boolean isProperlySectionDelimited() {
            return lengthWithBlankLine > length;
        }

        private static void doOldStyle(MessageDigest md,
                                byte[] bytes,
                                int offset,
                                int length)
        {
            // this is too gross to even document, but here goes
            // the 1.1 jar verification code ignored spaces at the
            // end of lines when calculating digests, so that is
            // what this code does. It only gets called if we
            // are parsing a 1.1 signed signature file
            int i = offset;
            int start = offset;
            int max = offset + length;
            int prev = -1;
            while(i <max) {
                if ((bytes[i] == '\r') && (prev == ' ')) {
                    md.update(bytes, start, i-start-1);
                    start = i;
                }
                prev = bytes[i];
                i++;
            }
            md.update(bytes, start, i-start);
        }
    }

    /**
     * @see #MF_MAIN_ATTRS
     */
    public Entry getMainAttsEntry() {
        return mainAttsEntry;
    }

    /**
     * @see #MF_MAIN_ATTRS
     */
    public Entry getMainAttsEntry(boolean oldStyle) {
        mainAttsEntry.oldStyle = oldStyle;
        return mainAttsEntry;
    }

    public Entry get(String name) {
        return entries.get(name);
    }

    public Entry get(String name, boolean oldStyle) {
        Entry e = get(name);
        if (e == null && MF_MAIN_ATTRS.equals(name)) {
            e = getMainAttsEntry();
        }
        if (e != null) {
            e.oldStyle = oldStyle;
        }
        return e;
    }

    public byte[] manifestDigest(MessageDigest md) {
        md.reset();
        md.update(rawBytes, 0, rawBytes.length);
        return md.digest();
    }

}
