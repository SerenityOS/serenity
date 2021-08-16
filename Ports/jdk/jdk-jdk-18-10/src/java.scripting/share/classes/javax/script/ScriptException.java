/*
 * Copyright (c) 2005, 2011, Oracle and/or its affiliates. All rights reserved.
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

package javax.script;

/**
 * The generic <code>Exception</code> class for the Scripting APIs.  Checked
 * exception types thrown by underlying scripting implementations must be wrapped in instances of
 * <code>ScriptException</code>.  The class has members to store line and column numbers and
 * filenames if this information is available.
 *
 * @author Mike Grogan
 * @since 1.6
 */
public class ScriptException extends Exception {

    private static final long serialVersionUID = 8265071037049225001L;

    private final String fileName;
    private final int lineNumber;
    private final int columnNumber;

    /**
     * Creates a <code>ScriptException</code> with a String to be used in its message.
     * Filename, and line and column numbers are unspecified.
     *
     * @param s The String to use in the message.
     */
    public ScriptException(String s) {
        super(s);
        fileName = null;
        lineNumber = -1;
        columnNumber = -1;
    }

    /**
     * Creates a <code>ScriptException</code> wrapping an <code>Exception</code> thrown by an underlying
     * interpreter.  Line and column numbers and filename are unspecified.
     *
     * @param e The wrapped <code>Exception</code>.
     */
    public ScriptException(Exception e) {
        super(e);
        fileName = null;
        lineNumber = -1;
        columnNumber = -1;
    }

    /**
     * Creates a <code>ScriptException</code> with message, filename and linenumber to
     * be used in error messages.
     *
     * @param message The string to use in the message
     *
     * @param fileName The file or resource name describing the location of a script error
     * causing the <code>ScriptException</code> to be thrown.
     *
     * @param lineNumber A line number describing the location of a script error causing
     * the <code>ScriptException</code> to be thrown.
     */
    public ScriptException(String message, String fileName, int lineNumber) {
        super(message);
        this.fileName = fileName;
        this.lineNumber = lineNumber;
        this.columnNumber = -1;
    }

    /**
     * <code>ScriptException</code> constructor specifying message, filename, line number
     * and column number.
     * @param message The message.
     * @param fileName The filename
     * @param lineNumber the line number.
     * @param columnNumber the column number.
     */
    public ScriptException(String message,
            String fileName,
            int lineNumber,
            int columnNumber) {
        super(message);
        this.fileName = fileName;
        this.lineNumber = lineNumber;
        this.columnNumber = columnNumber;
    }

    /**
     * Returns a message containing the String passed to a constructor as well as
     * line and column numbers and filename if any of these are known.
     * @return The error message.
     */
    public String getMessage() {
        String ret = super.getMessage();
        if (fileName != null) {
            ret += (" in " + fileName);
            if (lineNumber != -1) {
                ret += " at line number " + lineNumber;
            }

            if (columnNumber != -1) {
                ret += " at column number " + columnNumber;
            }
        }

        return ret;
    }

    /**
     * Get the line number on which an error occurred.
     * @return The line number.  Returns -1 if a line number is unavailable.
     */
    public int getLineNumber() {
        return lineNumber;
    }

    /**
     * Get the column number on which an error occurred.
     * @return The column number.  Returns -1 if a column number is unavailable.
     */
    public int getColumnNumber() {
        return columnNumber;
    }

    /**
     * Get the source of the script causing the error.
     * @return The file name of the script or some other string describing the script
     * source.  May return some implementation-defined string such as <i>&lt;unknown&gt;</i>
     * if a description of the source is unavailable.
     */
    public String getFileName() {
        return fileName;
    }
}
