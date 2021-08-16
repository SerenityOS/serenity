/*
 * Copyright (c) 2001, 2017, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.jdi;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

class SDE {
    private static final int INIT_SIZE_FILE = 3;
    private static final int INIT_SIZE_LINE = 100;
    private static final int INIT_SIZE_STRATUM = 3;

    static final String BASE_STRATUM_NAME = "Java";

    /* for C capatibility */
    static final String NullString = null;

    private class FileTableRecord {
        int fileId;
        String sourceName;
        String sourcePath; // do not read - use accessor
        boolean isConverted = false;

        /**
         * Return the sourcePath, computing it if not set.
         * If set, convert '/' in the sourcePath to the
         * local file separator.
         */
        String getSourcePath(ReferenceTypeImpl refType) {
            if (!isConverted) {
                if (sourcePath == null) {
                    sourcePath = refType.baseSourceDir() + sourceName;
                } else {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < sourcePath.length(); ++i) {
                        char ch = sourcePath.charAt(i);
                        if (ch == '/') {
                            sb.append(File.separatorChar);
                        } else {
                            sb.append(ch);
                        }
                    }
                    sourcePath = sb.toString();
                }
                isConverted = true;
            }
            return sourcePath;
        }
    }

    private class LineTableRecord {
        int jplsStart;
        int jplsEnd;
        int jplsLineInc;
        int njplsStart;
        @SuppressWarnings("unused")
        int njplsEnd;
        int fileId;
    }

    private class StratumTableRecord {
        String id;
        int fileIndex;
        int lineIndex;
    }

    class Stratum {
        private final int sti; /* stratum index */

        private Stratum(int sti) {
            this.sti = sti;
        }

        String id() {
            return stratumTable[sti].id;
        }

        boolean isJava() {
            return sti == baseStratumIndex;
        }

        /**
         * Return all the sourceNames for this stratum.
         * Look from our starting fileIndex upto the starting
         * fileIndex of next stratum - can do this since there
         * is always a terminator stratum.
         * Default sourceName (the first one) must be first.
         */
        List<String> sourceNames(ReferenceTypeImpl refType) {
            int i;
            int fileIndexStart = stratumTable[sti].fileIndex;
            /* one past end */
            int fileIndexEnd = stratumTable[sti+1].fileIndex;
            List<String> result = new ArrayList<>(fileIndexEnd - fileIndexStart);
            for (i = fileIndexStart; i < fileIndexEnd; ++i) {
                result.add(fileTable[i].sourceName);
            }
            return result;
        }

        /**
         * Return all the sourcePaths for this stratum.
         * Look from our starting fileIndex upto the starting
         * fileIndex of next stratum - can do this since there
         * is always a terminator stratum.
         * Default sourcePath (the first one) must be first.
         */
        List<String> sourcePaths(ReferenceTypeImpl refType) {
            int i;
            int fileIndexStart = stratumTable[sti].fileIndex;
            /* one past end */
            int fileIndexEnd = stratumTable[sti+1].fileIndex;
            List<String> result = new ArrayList<>(fileIndexEnd - fileIndexStart);
            for (i = fileIndexStart; i < fileIndexEnd; ++i) {
                result.add(fileTable[i].getSourcePath(refType));
            }
            return result;
        }

        LineStratum lineStratum(ReferenceTypeImpl refType,
                                int jplsLine) {
            int lti = stiLineTableIndex(sti, jplsLine);
            if (lti < 0) {
                return null;
            } else {
                return new LineStratum(sti, lti, refType,
                                       jplsLine);
            }
        }
    }

    class LineStratum {
        private final int sti; /* stratum index */
        private final int lti; /* line table index */
        private final ReferenceTypeImpl refType;
        private final int jplsLine;
        private String sourceName = null;
        private String sourcePath = null;

        private LineStratum(int sti, int lti,
                            ReferenceTypeImpl refType,
                            int jplsLine) {
            this.sti = sti;
            this.lti = lti;
            this.refType = refType;
            this.jplsLine = jplsLine;
        }

        public boolean equals(Object obj) {
            if (obj instanceof LineStratum) {
                LineStratum other = (LineStratum)obj;
                return (lti == other.lti) &&
                       (sti == other.sti) &&
                       (lineNumber() == other.lineNumber()) &&
                       (refType.equals(other.refType));
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return (lineNumber() * 17) ^ refType.hashCode();
        }

        int lineNumber() {
            return stiLineNumber(sti, lti, jplsLine);
        }

        /**
         * Fetch the source name and source path for
         * this line, converting or constructing
         * the source path if needed.
         */
        void getSourceInfo() {
            if (sourceName != null) {
                // already done
                return;
            }
            int fti = stiFileTableIndex(sti, lti);
            if (fti == -1) {
                throw new InternalError(
              "Bad SourceDebugExtension, no matching source id " +
              lineTable[lti].fileId + " jplsLine: " + jplsLine);
            }
            FileTableRecord ftr = fileTable[fti];
            sourceName = ftr.sourceName;
            sourcePath = ftr.getSourcePath(refType);
        }

        String sourceName() {
            getSourceInfo();
            return sourceName;
        }

        String sourcePath() {
            getSourceInfo();
            return sourcePath;
        }
    }

    private FileTableRecord[] fileTable = null;
    private LineTableRecord[] lineTable = null;
    private StratumTableRecord[] stratumTable = null;

    private int fileIndex = 0;
    private int lineIndex = 0;
    private int stratumIndex = 0;
    private int currentFileId = 0;

    private int defaultStratumIndex = -1;
    private int baseStratumIndex = -2; /* so as not to match -1 above */
    private int sdePos = 0;

    final String sourceDebugExtension;
    String jplsFilename = null;
    String defaultStratumId = null;
    boolean isValid = false;

    SDE(String sourceDebugExtension) {
        this.sourceDebugExtension = sourceDebugExtension;
        decode();
    }

    SDE() {
        this.sourceDebugExtension = null;
        createProxyForAbsentSDE();
    }

    char sdePeek() {
        if (sdePos >= sourceDebugExtension.length()) {
            syntax();
        }
        return sourceDebugExtension.charAt(sdePos);
    }

    char sdeRead() {
        if (sdePos >= sourceDebugExtension.length()) {
            syntax();
        }
        return sourceDebugExtension.charAt(sdePos++);
    }

    void sdeAdvance() {
        sdePos++;
    }

    void syntax() {
        throw new InternalError("bad SourceDebugExtension syntax - position " +
                                sdePos);
    }

    void syntax(String msg) {
        throw new InternalError("bad SourceDebugExtension syntax: " + msg);
    }

    void assureLineTableSize() {
        int len = lineTable == null? 0 : lineTable.length;
        if (lineIndex >= len) {
            int i;
            int newLen = len == 0? INIT_SIZE_LINE : len * 2;
            LineTableRecord[] newTable = new LineTableRecord[newLen];
            for (i = 0; i < len; ++i) {
                newTable[i] = lineTable[i];
            }
            for (; i < newLen; ++i) {
                newTable[i] = new LineTableRecord();
            }
            lineTable = newTable;
        }
    }

    void assureFileTableSize() {
        int len = fileTable == null? 0 : fileTable.length;
        if (fileIndex >= len) {
            int i;
            int newLen = len == 0? INIT_SIZE_FILE : len * 2;
            FileTableRecord[] newTable = new FileTableRecord[newLen];
            for (i = 0; i < len; ++i) {
                newTable[i] = fileTable[i];
            }
            for (; i < newLen; ++i) {
                newTable[i] = new FileTableRecord();
            }
            fileTable = newTable;
        }
    }

    void assureStratumTableSize() {
        int len = stratumTable == null? 0 : stratumTable.length;
        if (stratumIndex >= len) {
            int i;
            int newLen = len == 0? INIT_SIZE_STRATUM : len * 2;
            StratumTableRecord[] newTable = new StratumTableRecord[newLen];
            for (i = 0; i < len; ++i) {
                newTable[i] = stratumTable[i];
            }
            for (; i < newLen; ++i) {
                newTable[i] = new StratumTableRecord();
            }
            stratumTable = newTable;
        }
    }

    String readLine() {
        StringBuilder sb = new StringBuilder();
        char ch;

        ignoreWhite();
        while (((ch = sdeRead()) != '\n') && (ch != '\r')) {
            sb.append(ch);
        }
        // check for CR LF
        if ((ch == '\r') && (sdePeek() == '\n')) {
            sdeRead();
        }
        ignoreWhite(); // leading white
        return sb.toString();
    }

    private int defaultStratumTableIndex() {
        if ((defaultStratumIndex == -1) && (defaultStratumId != null)) {
            defaultStratumIndex =
                stratumTableIndex(defaultStratumId);
        }
        return defaultStratumIndex;
    }

    int stratumTableIndex(String stratumId) {
        int i;

        if (stratumId == null) {
            return defaultStratumTableIndex();
        }
        for (i = 0; i < (stratumIndex-1); ++i) {
            if (stratumTable[i].id.equals(stratumId)) {
                return i;
            }
        }
        return defaultStratumTableIndex();
    }

    Stratum stratum(String stratumID) {
        int sti = stratumTableIndex(stratumID);
        return new Stratum(sti);
    }

    List<String> availableStrata() {
        List<String> strata = new ArrayList<>();

        for (int i = 0; i < (stratumIndex-1); ++i) {
            StratumTableRecord rec = stratumTable[i];
            strata.add(rec.id);
        }
        return strata;
    }

/*****************************
 * below functions/methods are written to compile under either Java or C
 *
 * Needed support functions:
 *   sdePeek()
 *   sdeRead()
 *   sdeAdvance()
 *   readLine()
 *   assureLineTableSize()
 *   assureFileTableSize()
 *   assureStratumTableSize()
 *   syntax()
 *
 *   stratumTableIndex(String)
 *
 * Needed support variables:
 *   lineTable
 *   lineIndex
 *   fileTable
 *   fileIndex
 *   currentFileId
 *
 * Needed types:
 *   String
 *
 * Needed constants:
 *   NullString
 */

    void ignoreWhite() {
        char ch;

        while (((ch = sdePeek()) == ' ') || (ch == '\t')) {
            sdeAdvance();
        }
    }

    void ignoreLine() {
        char ch;

        while (((ch = sdeRead()) != '\n') && (ch != '\r')) {
        }
        /* check for CR LF */
        if ((ch == '\r') && (sdePeek() == '\n')) {
            sdeAdvance();
        }
        ignoreWhite(); /* leading white */
    }

    int readNumber() {
        int value = 0;
        char ch;

        ignoreWhite();
        while (((ch = sdePeek()) >= '0') && (ch <= '9')) {
            sdeAdvance();
            value = (value * 10) + ch - '0';
        }
        ignoreWhite();
        return value;
    }

    void storeFile(int fileId, String sourceName, String sourcePath) {
        assureFileTableSize();
        fileTable[fileIndex].fileId = fileId;
        fileTable[fileIndex].sourceName = sourceName;
        fileTable[fileIndex].sourcePath = sourcePath;
        ++fileIndex;
    }

    void fileLine() {
        int hasAbsolute = 0; /* acts as boolean */
        int fileId;
        String sourceName;
        String sourcePath = null;

        /* is there an absolute filename? */
        if (sdePeek() == '+') {
            sdeAdvance();
            hasAbsolute = 1;
        }
        fileId = readNumber();
        sourceName = readLine();
        if (hasAbsolute == 1) {
            sourcePath = readLine();
        }

        storeFile(fileId, sourceName, sourcePath);
    }

    void storeLine(int jplsStart, int jplsEnd, int jplsLineInc,
                   int njplsStart, int njplsEnd, int fileId) {
        assureLineTableSize();
        lineTable[lineIndex].jplsStart = jplsStart;
        lineTable[lineIndex].jplsEnd = jplsEnd;
        lineTable[lineIndex].jplsLineInc = jplsLineInc;
        lineTable[lineIndex].njplsStart = njplsStart;
        lineTable[lineIndex].njplsEnd = njplsEnd;
        lineTable[lineIndex].fileId = fileId;
        ++lineIndex;
    }

    /**
     * Parse line translation info.  Syntax is
     *     <NJ-start-line> [ # <file-id> ] [ , <line-count> ] :
     *                 <J-start-line> [ , <line-increment> ] CR
     */
    void lineLine() {
        int lineCount = 1;
        int lineIncrement = 1;
        int njplsStart;
        int jplsStart;

        njplsStart = readNumber();

        /* is there a fileID? */
        if (sdePeek() == '#') {
            sdeAdvance();
            currentFileId = readNumber();
        }

        /* is there a line count? */
        if (sdePeek() == ',') {
            sdeAdvance();
            lineCount = readNumber();
        }

        if (sdeRead() != ':') {
            syntax();
        }
        jplsStart = readNumber();
        if (sdePeek() == ',') {
            sdeAdvance();
            lineIncrement = readNumber();
        }
        ignoreLine(); /* flush the rest */

        storeLine(jplsStart,
                  jplsStart + (lineCount * lineIncrement) -1,
                  lineIncrement,
                  njplsStart,
                  njplsStart + lineCount -1,
                  currentFileId);
    }

    /**
     * Until the next stratum section, everything after this
     * is in stratumId - so, store the current indicies.
     */
    void storeStratum(String stratumId) {
        /* remove redundant strata */
        if (stratumIndex > 0) {
            if ((stratumTable[stratumIndex-1].fileIndex
                                            == fileIndex) &&
                (stratumTable[stratumIndex-1].lineIndex
                                            == lineIndex)) {
                /* nothing changed overwrite it */
                --stratumIndex;
            }
        }
        /* store the results */
        assureStratumTableSize();
        stratumTable[stratumIndex].id = stratumId;
        stratumTable[stratumIndex].fileIndex = fileIndex;
        stratumTable[stratumIndex].lineIndex = lineIndex;
        ++stratumIndex;
        currentFileId = 0;
    }

    /**
     * The beginning of a stratum's info
     */
    void stratumSection() {
        storeStratum(readLine());
    }

    void fileSection() {
        ignoreLine();
        while (sdePeek() != '*') {
            fileLine();
        }
    }

    void lineSection() {
        ignoreLine();
        while (sdePeek() != '*') {
            lineLine();
        }
    }

    /**
     * Ignore a section we don't know about.
     */
    void ignoreSection() {
        ignoreLine();
        while (sdePeek() != '*') {
            ignoreLine();
        }
    }

    /**
     * A base "Java" stratum is always available, though
     * it is not in the SourceDebugExtension.
     * Create the base stratum.
     */
    void createJavaStratum() {
        baseStratumIndex = stratumIndex;
        storeStratum(BASE_STRATUM_NAME);
        storeFile(1, jplsFilename, NullString);
        /* JPL line numbers cannot exceed 65535 */
        storeLine(1, 65536, 1, 1, 65536, 1);
        storeStratum("Aux"); /* in case they don't declare */
    }

    /**
     * Decode a SourceDebugExtension which is in SourceMap format.
     * This is the entry point into the recursive descent parser.
     */
    void decode() {
        /* check for "SMAP" - allow EOF if not ours */
        if ((sourceDebugExtension.length() < 4) ||
            (sdeRead() != 'S') ||
            (sdeRead() != 'M') ||
            (sdeRead() != 'A') ||
            (sdeRead() != 'P')) {
            return; /* not our info */
        }
        ignoreLine(); /* flush the rest */
        jplsFilename = readLine();
        defaultStratumId = readLine();
        createJavaStratum();
        while (true) {
            if (sdeRead() != '*') {
                syntax();
            }
            switch (sdeRead()) {
                case 'S':
                    stratumSection();
                    break;
                case 'F':
                    fileSection();
                    break;
                case 'L':
                    lineSection();
                    break;
                case 'E':
                    /* set end points */
                    storeStratum("*terminator*");
                    isValid = true;
                    return;
                default:
                    ignoreSection();
            }
        }
    }

    void createProxyForAbsentSDE() {
        jplsFilename = null;
        defaultStratumId = BASE_STRATUM_NAME;
        defaultStratumIndex = stratumIndex;
        createJavaStratum();
        storeStratum("*terminator*");
    }

    /***************** query functions ***********************/

    private int stiLineTableIndex(int sti, int jplsLine) {
        int i;
        int lineIndexStart;
        int lineIndexEnd;

        lineIndexStart = stratumTable[sti].lineIndex;
        /* one past end */
        lineIndexEnd = stratumTable[sti+1].lineIndex;
        for (i = lineIndexStart; i < lineIndexEnd; ++i) {
            if ((jplsLine >= lineTable[i].jplsStart) &&
                            (jplsLine <= lineTable[i].jplsEnd)) {
                return i;
            }
        }
        return -1;
    }

    private int stiLineNumber(int sti, int lti, int jplsLine) {
        return lineTable[lti].njplsStart +
                (((jplsLine - lineTable[lti].jplsStart) /
                                   lineTable[lti].jplsLineInc));
    }

    private int fileTableIndex(int sti, int fileId) {
        int i;
        int fileIndexStart = stratumTable[sti].fileIndex;
        /* one past end */
        int fileIndexEnd = stratumTable[sti+1].fileIndex;
        for (i = fileIndexStart; i < fileIndexEnd; ++i) {
            if (fileTable[i].fileId == fileId) {
                return i;
            }
        }
        return -1;
    }

    private int stiFileTableIndex(int sti, int lti) {
        return fileTableIndex(sti, lineTable[lti].fileId);
    }

    boolean isValid() {
        return isValid;
    }
}
