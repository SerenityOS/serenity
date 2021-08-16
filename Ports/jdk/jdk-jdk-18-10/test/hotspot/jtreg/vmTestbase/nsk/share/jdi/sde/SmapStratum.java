/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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
package nsk.share.jdi.sde;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the line and file mappings associated with a JSR-045
 * "stratum".
 */
public class SmapStratum
{

    //*********************************************************************
    // Class for storing LineInfo data

    /**
     * Represents a single LineSection in an SMAP, associated with
     * a particular stratum.
     */
    public static class LineInfo {
        private int inputStartLine = -1;
        private int outputStartLine = -1;
        private int lineFileID = 0;
        private int inputLineCount = 1;
        private int outputLineIncrement = 1;
        private boolean lineFileIDSet = false;

        /** Sets InputStartLine. */
        public void setInputStartLine(int inputStartLine) {
            if (inputStartLine < 0)
                throw new IllegalArgumentException("" + inputStartLine);
            this.inputStartLine = inputStartLine;
        }

        /** Sets OutputStartLine. */
        public void setOutputStartLine(int outputStartLine) {
            if (outputStartLine < 0)
                throw new IllegalArgumentException("" + outputStartLine);
            this.outputStartLine = outputStartLine;
        }

        /**
         * Sets lineFileID.  Should be called only when different from
         * that of prior LineInfo object (in any given context) or 0
         * if the current LineInfo has no (logical) predecessor.
         * <tt>LineInfo</tt> will print this file number no matter what.
         */
        public void setLineFileID(int lineFileID) {
            if (lineFileID < 0)
                throw new IllegalArgumentException("" + lineFileID);
            this.lineFileID = lineFileID;
            this.lineFileIDSet = true;
        }

        /** Sets InputLineCount. */
        public void setInputLineCount(int inputLineCount) {
            if (inputLineCount < 0)
                throw new IllegalArgumentException("" + inputLineCount);
            this.inputLineCount = inputLineCount;
        }

        /** Sets OutputLineIncrement. */
        public void setOutputLineIncrement(int outputLineIncrement) {
            if (outputLineIncrement < 0)
                throw new IllegalArgumentException("" + outputLineIncrement);
            this.outputLineIncrement = outputLineIncrement;
        }

        /**
         * Retrieves the current LineInfo as a String, print all values
         * only when appropriate (but LineInfoID if and only if it's been
         * specified, as its necessity is sensitive to context).
         */
        public String getString() {
            if (inputStartLine == -1 || outputStartLine == -1)
                throw new IllegalStateException();
            StringBuffer out = new StringBuffer();
            out.append(inputStartLine);
            if (lineFileIDSet)
                out.append("#" + lineFileID);
            if (inputLineCount != 1)
                out.append("," + inputLineCount);
            out.append(":" + outputStartLine);
            if (outputLineIncrement != 1)
                out.append("," + outputLineIncrement);
            out.append('\n');
            return out.toString();
        }

        public String toString() {
            return getString();
        }
    }

    //*********************************************************************
    // Private state

    private String stratumName;
    private List<String> fileNameList;
    private List<String> filePathList;
    private List<LineInfo> lineData;
    private int lastFileID;

    //*********************************************************************
    // Constructor

    /**
     * Constructs a new SmapStratum object for the given stratum name
     * (e.g., JSP).
     *
     * @param stratumName the name of the stratum (e.g., JSP)
     */
    public SmapStratum(String stratumName) {
        this.stratumName = stratumName;
        fileNameList = new ArrayList<String>();
        filePathList = new ArrayList<String>();
        lineData = new ArrayList<LineInfo>();
        lastFileID = 0;
    }

    //*********************************************************************
    // Methods to add mapping information

    /**
     * Adds record of a new file, by filename.
     *
     * @param filename the filename to add, unqualified by path.
     */
    public void addFile(String filename) {
        addFile(filename, filename);
    }

    /**
     * Adds record of a new file, by filename and path.  The path
     * may be relative to a source compilation path.
     *
     * @param filename the filename to add, unqualified by path
     * @param filePath the path for the filename, potentially relative
     *                 to a source compilation path
     */
    public void addFile(String filename, String filePath) {
        int pathIndex = filePathList.indexOf(filePath);
        if (pathIndex == -1) {
            fileNameList.add(filename);
            filePathList.add(filePath);
        }
    }

    /**
     * Adds complete information about a simple line mapping.  Specify
     * all the fields in this method; the back-end machinery takes care
     * of printing only those that are necessary in the final SMAP.
     * (My view is that fields are optional primarily for spatial efficiency,
     * not for programmer convenience.  Could always add utility methods
     * later.)
     *
     * @param inputStartLine starting line in the source file
     *        (SMAP <tt>InputStartLine</tt>)
     * @param inputFileName the filepath (or name) from which the input comes
     *        (yields SMAP <tt>LineFileID</tt>)  Use unqualified names
     *        carefully, and only when they uniquely identify a file.
     * @param inputLineCount the number of lines in the input to map
     *        (SMAP <tt>LineFileCount</tt>)
     * @param outputStartLine starting line in the output file
     *        (SMAP <tt>OutputStartLine</tt>)
     * @param outputLineIncrement number of output lines to map to each
     *        input line (SMAP <tt>OutputLineIncrement</tt>).  <i>Given the
     *        fact that the name starts with "output", I continuously have
     *        the subconscious urge to call this field
     *        <tt>OutputLineExcrement</tt>.</i>
     */
    public void addLineData(
            int inputStartLine,
            String inputFileName,
            int inputLineCount,
            int outputStartLine,
            int outputLineIncrement) {
        // check the input - what are you doing here??
        int fileIndex = fileNameList.indexOf(inputFileName);
        if (fileIndex == -1) // still
            throw new IllegalArgumentException(
                    "inputFileName: " + inputFileName);

        //Jasper incorrectly SMAPs certain Nodes, giving them an
        //outputStartLine of 0.  This can cause a fatal error in
        //optimizeLineSection, making it impossible for Jasper to
        //compile the JSP.  Until we can fix the underlying
        //SMAPping problem, we simply ignore the flawed SMAP entries.
        if (outputStartLine == 0)
            return;

        // build the LineInfo
        LineInfo li = new LineInfo();
        li.setInputStartLine(inputStartLine);
        li.setInputLineCount(inputLineCount);
        li.setOutputStartLine(outputStartLine);
        li.setOutputLineIncrement(outputLineIncrement);
        if (fileIndex != lastFileID)
            li.setLineFileID(fileIndex);
        lastFileID = fileIndex;

        // save it
        lineData.add(li);
    }

    //*********************************************************************
    // Methods to retrieve information

    /**
     * Returns the name of the stratum.
     */
    public String getStratumName() {
        return stratumName;
    }

    /**
     * Returns the given stratum as a String:  a StratumSection,
     * followed by at least one FileSection and at least one LineSection.
     */
    public String getString() {
        // check state and initialize buffer
        if (fileNameList.size() == 0 || lineData.size() == 0)
            return null;

        StringBuffer out = new StringBuffer();

        // print StratumSection
        out.append("*S " + stratumName + "\n");

        // print FileSection
        out.append("*F\n");
        int bound = fileNameList.size();
        for (int i = 0; i < bound; i++) {
            if (filePathList.get(i) != null) {
                out.append("+ " + i + " " + fileNameList.get(i) + "\n");
                // Source paths must be relative, not absolute, so we
                // remove the leading "/", if one exists.
                String filePath = (String)filePathList.get(i);
                if (filePath.startsWith("/")) {
                    filePath = filePath.substring(1);
                }
                out.append(filePath + "\n");
            } else {
                out.append(i + " " + fileNameList.get(i) + "\n");
            }
        }

        // print LineSection
        out.append("*L\n");
        bound = lineData.size();
        for (int i = 0; i < bound; i++) {
            LineInfo li = (LineInfo)lineData.get(i);
            out.append(li.getString());
        }

        return out.toString();
    }

    public String toString() {
        return getString();
    }

}
