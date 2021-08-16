/*
 * Copyright (c) 2001, 2016, Oracle and/or its affiliates. All rights reserved.
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

#include <setjmp.h>

#include "util.h"
#include "SDE.h"

#ifdef __APPLE__
/* use setjmp/longjmp versions that do not save/restore the signal mask */
#define setjmp _setjmp
#define longjmp _longjmp
#endif

/**
 * This SourceDebugExtension code does not
 * allow concurrent translation - due to caching method.
 * A separate thread setting the default stratum ID
 * is, however, fine.
 */

#define INIT_SIZE_FILE 10
#define INIT_SIZE_LINE 100
#define INIT_SIZE_STRATUM 3

#define BASE_STRATUM_NAME "Java"

#define null NULL
#define String char *
#define private static

typedef struct {
  int fileId;
  String sourceName;
  String sourcePath; // do not read - use accessor
  int isConverted;
} FileTableRecord;

typedef struct {
    int jplsStart;
    int jplsEnd;
    int jplsLineInc;
    int njplsStart;
    int njplsEnd;
    int fileId;
} LineTableRecord;

typedef struct {
    String id;
    int fileIndex;
    int lineIndex;
} StratumTableRecord;

/* back-end wide value for default stratum */
private String globalDefaultStratumId = null;

/* reference type default */
private String defaultStratumId = null;

private jclass cachedClass = NULL;

private FileTableRecord* fileTable;
private LineTableRecord* lineTable;
private StratumTableRecord* stratumTable;

private int fileTableSize;
private int lineTableSize;
private int stratumTableSize;

private int fileIndex;
private int lineIndex;
private int stratumIndex = 0;
private int currentFileId;

private int defaultStratumIndex;
private int baseStratumIndex;
private char* sdePos;

private char* jplsFilename = null;
private char* NullString = null;

/* mangled in parse, cannot be parsed.  Must be kept. */
private String sourceDebugExtension;

private jboolean sourceMapIsValid;

private jmp_buf jmp_buf_env;

private int stratumTableIndex(String stratumId);
private int stiLineTableIndex(int sti, int jplsLine);
private int stiLineNumber(int sti, int lti, int jplsLine);
private void decode(void);
private void ignoreWhite(void);
private jboolean isValid(void);

    private void
    loadDebugInfo(JNIEnv *env, jclass clazz) {

        if (!isSameObject(env, clazz, cachedClass)) {
            /* Not the same - swap out the info */

            /* Delete existing info */
            if ( cachedClass != null ) {
                tossGlobalRef(env, &cachedClass);
                cachedClass = null;
            }
            if ( sourceDebugExtension!=null ) {
                jvmtiDeallocate(sourceDebugExtension);
            }
            sourceDebugExtension = null;

            /* Init info */
            lineTable = null;
            fileTable = null;
            stratumTable = null;
            lineTableSize = 0;
            fileTableSize = 0;
            stratumTableSize = 0;
            fileIndex = 0;
            lineIndex = 0;
            stratumIndex = 0;
            currentFileId = 0;
            defaultStratumId = null;
            defaultStratumIndex = -1;
            baseStratumIndex = -2; /* so as not to match -1 above */
            sourceMapIsValid = JNI_FALSE;

            if (getSourceDebugExtension(clazz, &sourceDebugExtension) ==
                JVMTI_ERROR_NONE) {
                sdePos = sourceDebugExtension;
                if (setjmp(jmp_buf_env) == 0) {
                    /* this is the initial (non-error) case, do parse */
                    decode();
                }
            }

            cachedClass = null;
            saveGlobalRef(env, clazz, &cachedClass);
        }
    }

    /* Return 1 if match, 0 if no match */
    private int
    patternMatch(char *classname, const char *pattern) {
        int pattLen;
        int compLen;
        char *start;
        int offset;

        if (pattern == NULL || classname == NULL) {
            return 0;
        }
        pattLen = (int)strlen(pattern);

        if ((pattern[0] != '*') && (pattern[pattLen-1] != '*')) {
            return strcmp(pattern, classname) == 0;
        }

        compLen = pattLen - 1;
        offset = (int)strlen(classname) - compLen;
        if (offset < 0) {
            return 0;
        }
        if (pattern[0] == '*') {
            pattern++;
            start = classname + offset;
        }  else {
            start = classname;
        }
        return strncmp(pattern, start, compLen) == 0;
    }

    /**
     * Return 1 if p1 is a SourceName for stratum sti,
     * else, return 0.
     */
    private int
    searchOneSourceName(int sti, char *p1) {
        int fileIndexStart = stratumTable[sti].fileIndex;
        /* one past end */
        int fileIndexEnd = stratumTable[sti+1].fileIndex;
        int ii;
        for (ii = fileIndexStart; ii < fileIndexEnd; ++ii) {
            if (patternMatch(fileTable[ii].sourceName, p1)) {
              return 1;
            }
        }
        return 0;
    }

    /**
     * Return 1 if p1 is a SourceName for any stratum
     * else, return 0.
     */
    int searchAllSourceNames(JNIEnv *env,
                             jclass clazz,
                             char *p1) {
        int ii;
        loadDebugInfo(env, clazz);
        if (!isValid()) {
          return 0; /* no SDE or not SourceMap */
        }

        for (ii = 0; ii < stratumIndex - 1; ++ii) {
            if (searchOneSourceName(ii, p1) == 1) {
                return 1;
            }
        }
        return 0;
    }

    /**
     * Convert a line number table, as returned by the JVMTI
     * function GetLineNumberTable, to one for another stratum.
     * Conversion is by overwrite.
     * Actual line numbers are not returned - just a unique
     * number (file ID in top 16 bits, line number in
     * bottom 16 bits) - this is all stepping needs.
     */
    void
    convertLineNumberTable(JNIEnv *env, jclass clazz,
                           jint *entryCountPtr,
                           jvmtiLineNumberEntry **tablePtr) {
        jvmtiLineNumberEntry *fromEntry = *tablePtr;
        jvmtiLineNumberEntry *toEntry = *tablePtr;
        int cnt = *entryCountPtr;
        int lastLn = 0;
        int sti;

        if (cnt < 0) {
            return;
        }
        loadDebugInfo(env, clazz);
        if (!isValid()) {
            return; /* no SDE or not SourceMap - return unchanged */
        }
        sti = stratumTableIndex(globalDefaultStratumId);
        if (sti == baseStratumIndex || sti < 0) {
            return; /* Java stratum - return unchanged */
        }
        LOG_MISC(("SDE is re-ordering the line table"));
        for (; cnt-- > 0; ++fromEntry) {
            int jplsLine = fromEntry->line_number;
            int lti = stiLineTableIndex(sti, jplsLine);
            if (lti >= 0) {
                int fileId = lineTable[lti].fileId;
                int ln = stiLineNumber(sti, lti, jplsLine);
                ln += (fileId << 16); /* create line hash */
                if (ln != lastLn) {
                    lastLn = ln;
                    toEntry->start_location = fromEntry->start_location;
                    toEntry->line_number = ln;
                    ++toEntry;
                }
            }
        }
        /*LINTED*/
        *entryCountPtr = (int)(toEntry - *tablePtr);
    }

    /**
     * Set back-end wide default stratum ID .
     */
    void
    setGlobalStratumId(char *id) {
        globalDefaultStratumId = id;
    }


    private void syntax(String msg) {
        char buf[200];
        (void)snprintf(buf, sizeof(buf),
                "bad SourceDebugExtension syntax - position %d - %s\n",
                /*LINTED*/
                (int)(sdePos-sourceDebugExtension),
                msg);
        JDI_ASSERT_FAILED(buf);

        longjmp(jmp_buf_env, 1);  /* abort parse */
    }

    private char sdePeek(void) {
        if (*sdePos == 0) {
            syntax("unexpected EOF");
        }
        return *sdePos;
    }

    private char sdeRead(void) {
        if (*sdePos == 0) {
            syntax("unexpected EOF");
        }
        return *sdePos++;
    }

    private void sdeAdvance(void) {
        sdePos++;
    }

    private void assureLineTableSize(void) {
        if (lineIndex >= lineTableSize) {
            size_t allocSize;
            LineTableRecord* new_lineTable;
            int new_lineTableSize;

            new_lineTableSize = lineTableSize == 0?
                                  INIT_SIZE_LINE :
                                  lineTableSize * 2;
            allocSize = new_lineTableSize * (int)sizeof(LineTableRecord);
            new_lineTable = jvmtiAllocate((jint)allocSize);
            if ( new_lineTable == NULL ) {
                EXIT_ERROR(AGENT_ERROR_OUT_OF_MEMORY, "SDE line table");
            }
            if ( lineTable!=NULL ) {
                (void)memcpy(new_lineTable, lineTable,
                        lineTableSize * (int)sizeof(LineTableRecord));
                jvmtiDeallocate(lineTable);
            }
            lineTable     = new_lineTable;
            lineTableSize = new_lineTableSize;
        }
    }

    private void assureFileTableSize(void) {
        if (fileIndex >= fileTableSize) {
            size_t allocSize;
            FileTableRecord* new_fileTable;
            int new_fileTableSize;

            new_fileTableSize = fileTableSize == 0?
                                  INIT_SIZE_FILE :
                                  fileTableSize * 2;
            allocSize = new_fileTableSize * (int)sizeof(FileTableRecord);
            new_fileTable = jvmtiAllocate((jint)allocSize);
            if ( new_fileTable == NULL ) {
                EXIT_ERROR(AGENT_ERROR_OUT_OF_MEMORY, "SDE file table");
            }
            if ( fileTable!=NULL ) {
                (void)memcpy(new_fileTable, fileTable,
                        fileTableSize * (int)sizeof(FileTableRecord));
                jvmtiDeallocate(fileTable);
            }
            fileTable     = new_fileTable;
            fileTableSize = new_fileTableSize;
        }
    }

    private void assureStratumTableSize(void) {
        if (stratumIndex >= stratumTableSize) {
            size_t allocSize;
            StratumTableRecord* new_stratumTable;
            int new_stratumTableSize;

            new_stratumTableSize = stratumTableSize == 0?
                                  INIT_SIZE_STRATUM :
                                  stratumTableSize * 2;
            allocSize = new_stratumTableSize * (int)sizeof(StratumTableRecord);
            new_stratumTable = jvmtiAllocate((jint)allocSize);
            if ( new_stratumTable == NULL ) {
                EXIT_ERROR(AGENT_ERROR_OUT_OF_MEMORY, "SDE stratum table");
            }
            if ( stratumTable!=NULL ) {
                (void)memcpy(new_stratumTable, stratumTable,
                        stratumTableSize * (int)sizeof(StratumTableRecord));
                jvmtiDeallocate(stratumTable);
            }
            stratumTable     = new_stratumTable;
            stratumTableSize = new_stratumTableSize;
        }
    }

    private String readLine(void) {
        char *initialPos;
        char ch;

        ignoreWhite();
        initialPos = sdePos;
        while (((ch = *sdePos) != '\n') && (ch != '\r')) {
            if (ch == 0) {
                syntax("unexpected EOF");
            }
            ++sdePos;
        }
        *sdePos++ = 0; /* null terminate string - mangles SDE */

        /* check for CR LF */
        if ((ch == '\r') && (*sdePos == '\n')) {
            ++sdePos;
        }
        ignoreWhite(); /* leading white */
        return initialPos;
    }

    private int defaultStratumTableIndex(void) {
        if ((defaultStratumIndex == -1) && (defaultStratumId != null)) {
            defaultStratumIndex =
                stratumTableIndex(defaultStratumId);
        }
        return defaultStratumIndex;
    }

    private int stratumTableIndex(String stratumId) {
        int i;

        if (stratumId == null) {
            return defaultStratumTableIndex();
        }
        for (i = 0; i < (stratumIndex-1); ++i) {
            if (strcmp(stratumTable[i].id, stratumId) == 0) {
                return i;
            }
        }
        return defaultStratumTableIndex();
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
 *   syntax(String)
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

    private void ignoreWhite(void) {
        char ch;

        while (((ch = sdePeek()) == ' ') || (ch == '\t')) {
            sdeAdvance();
        }
    }

    private void ignoreLine(void) {
        char ch;

        do {
           ch = sdeRead();
        } while ((ch != '\n') && (ch != '\r'));

        /* check for CR LF */
        if ((ch == '\r') && (sdePeek() == '\n')) {
            sdeAdvance();
        }
        ignoreWhite(); /* leading white */
    }

    private int readNumber(void) {
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

    private void storeFile(int fileId, String sourceName, String sourcePath) {
        assureFileTableSize();
        fileTable[fileIndex].fileId = fileId;
        fileTable[fileIndex].sourceName = sourceName;
        fileTable[fileIndex].sourcePath = sourcePath;
        ++fileIndex;
    }

    private void fileLine(void) {
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

    private void storeLine(int jplsStart, int jplsEnd, int jplsLineInc,
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
    private void lineLine(void) {
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
            syntax("expected ':'");
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
    private void storeStratum(String stratumId) {
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
    private void stratumSection(void) {
        storeStratum(readLine());
    }

    private void fileSection(void) {
        ignoreLine();
        while (sdePeek() != '*') {
            fileLine();
        }
    }

    private void lineSection(void) {
        ignoreLine();
        while (sdePeek() != '*') {
            lineLine();
        }
    }

    /**
     * Ignore a section we don't know about.
     */
    private void ignoreSection(void) {
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
    private void createJavaStratum(void) {
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
    private void decode(void) {
        /* check for "SMAP" - allow EOF if not ours */
        if (strlen(sourceDebugExtension) <= 4 ||
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
        while (1) {
            if (sdeRead() != '*') {
                syntax("expected '*'");
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
                    sourceMapIsValid = JNI_TRUE;
                    return;
                default:
                    ignoreSection();
            }
        }
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

    private jboolean isValid(void) {
        return sourceMapIsValid;
    }
