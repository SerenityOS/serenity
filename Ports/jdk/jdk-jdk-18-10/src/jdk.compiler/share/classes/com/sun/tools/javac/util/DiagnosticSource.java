/*
 * Copyright (c) 1999, 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.javac.util;

import java.io.IOException;
import java.lang.ref.SoftReference;
import java.nio.CharBuffer;
import javax.tools.JavaFileObject;

import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.tree.EndPosTable;

import static com.sun.tools.javac.util.LayoutCharacters.*;

/**
 * A simple abstraction of a source file, as needed for use in a diagnostic message.
 * Provides access to the line and position in a line for any given character offset.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class DiagnosticSource {

    /* constant DiagnosticSource to be used when sourcefile is missing */
    public static final DiagnosticSource NO_SOURCE = new DiagnosticSource() {
        @Override
        protected boolean findLine(int pos) {
            return false;
        }
    };

    public DiagnosticSource(JavaFileObject fo, AbstractLog log) {
        this.fileObject = fo;
        this.log = log;
    }

    private DiagnosticSource() {}

    /** Return the underlying file object handled by this
     *  DiagnosticSource object.
     */
    public JavaFileObject getFile() {
        return fileObject;
    }

    /** Return the one-based line number associated with a given pos
     * for the current source file.  Zero is returned if no line exists
     * for the given position.
     */
    public int getLineNumber(int pos) {
        try {
            if (findLine(pos)) {
                return line;
            }
            return 0;
        } finally {
            buf = null;
        }
    }

    /** Return the one-based column number associated with a given pos
     * for the current source file.  Zero is returned if no column exists
     * for the given position.
     */
    public int getColumnNumber(int pos, boolean expandTabs) {
        try {
            if (findLine(pos)) {
                int column = 0;
                for (int bp = lineStart; bp < pos; bp++) {
                    if (bp >= bufLen) {
                        return 0;
                    }
                    if (buf[bp] == '\t' && expandTabs) {
                        column = tabulate(column);
                    } else {
                        column++;
                    }
                }
                return column + 1; // positions are one-based
            }
            return 0;
        } finally {
            buf = null;
        }
    }

    /** Return the content of the line containing a given pos.
     */
    public String getLine(int pos) {
        try {
            if (!findLine(pos))
                return null;

            int lineEnd = lineStart;
            while (lineEnd < bufLen && buf[lineEnd] != CR && buf[lineEnd] != LF)
                lineEnd++;
            if (lineEnd - lineStart == 0)
                return null;
            return new String(buf, lineStart, lineEnd - lineStart);
        } finally {
            buf = null;
        }
    }

    public EndPosTable getEndPosTable() {
        return endPosTable;
    }

    public void setEndPosTable(EndPosTable t) {
        if (endPosTable != null && endPosTable != t)
            throw new IllegalStateException("endPosTable already set");
        endPosTable = t;
    }

    /** Find the line in the buffer that contains the current position
     * @param pos      Character offset into the buffer
     */
    protected boolean findLine(int pos) {
        if (pos == Position.NOPOS)
            return false;

        try {
            // try and recover buffer from soft reference cache
            if (buf == null && refBuf != null)
                buf = refBuf.get();

            if (buf == null) {
                buf = initBuf(fileObject);
                lineStart = 0;
                line = 1;
            } else if (lineStart > pos) { // messages don't come in order
                lineStart = 0;
                line = 1;
            }

            int bp = lineStart;
            while (bp < bufLen && bp < pos) {
                switch (buf[bp++]) {
                case CR:
                    if (bp < bufLen && buf[bp] == LF) bp++;
                    line++;
                    lineStart = bp;
                    break;
                case LF:
                    line++;
                    lineStart = bp;
                    break;
                }
            }
            return bp <= bufLen;
        } catch (IOException e) {
            log.directError("source.unavailable");
            buf = new char[0];
            return false;
        }
    }

    protected char[] initBuf(JavaFileObject fileObject) throws IOException {
        char[] buf;
        CharSequence cs = fileObject.getCharContent(true);
        if (cs instanceof CharBuffer charBuffer) {
            buf = JavacFileManager.toArray(charBuffer);
            bufLen = charBuffer.limit();
        } else {
            buf = cs.toString().toCharArray();
            bufLen = buf.length;
        }
        refBuf = new SoftReference<>(buf);
        return buf;
    }

    /** The underlying file object. */
    protected JavaFileObject fileObject;

    protected EndPosTable endPosTable;

    /** A soft reference to the content of the file object. */
    protected SoftReference<char[]> refBuf;

    /** A temporary hard reference to the content of the file object. */
    protected char[] buf;

    /** The length of the content. */
    protected int bufLen;

    /** The start of a line found by findLine. */
    protected int lineStart;

    /** The line number of a line found by findLine. */
    protected int line;

    /** A log for reporting errors, such as errors accessing the content. */
    protected AbstractLog log;
}
