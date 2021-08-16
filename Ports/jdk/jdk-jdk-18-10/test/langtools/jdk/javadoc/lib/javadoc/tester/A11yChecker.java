/*
 * Copyright (c) 2002, 2019, Oracle and/or its affiliates. All rights reserved.
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

package javadoc.tester;

import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Map;
import java.util.Stack;
import java.util.function.Function;

/**
 * A class to check various aspects of accessibility in a set of HTML files.
 */
public class A11yChecker extends HtmlChecker {

    private boolean html5;

    private int currRank;
    private int headingErrors;

    private boolean inBody;
    private boolean inNoScript;
    private Stack<String> regions = new Stack<>();
    private int regionErrors;

    A11yChecker(PrintStream out, Function<Path,String> fileReader) {
        super(out, fileReader);
    }

    protected int getErrorCount() {
        return errors;
    }

    @Override
    public void report() {
        if (headingErrors == 0) {
            out.println("All headings OK");
        } else {
            out.println(headingErrors + " bad headings");
        }

        if (regionErrors == 0) {
            out.println("All regions OK");
        } else {
            out.println(regionErrors + " errors in regions");
        }
    }

    @Override
    public void startFile(Path path) {
        html5 = false;
    }

    @Override
    public void endFile() {
    }

    @Override
    public void docType(String doctype) {
        html5 = doctype.matches("(?i)<\\?doctype\\s+html>");
    }

    @Override
    public void startElement(String name, Map<String,String> attrs, boolean selfClosing) {
        switch (name) {
            case "body":
                inBody = true;
                currRank = 0;
                break;

            case "h1": case "h2": case "h3": case "h4": case "h5": case "h6":
                checkHeading(name);
                break;

            case "header": case "footer": case "main": case "nav": case "aside":
                regions.push(name);
                break;

            case "noscript":
                inNoScript = true;
                break;
        }
    }

    @Override
    public void endElement(String name) {
        switch (name) {
            case "body":
                inBody = false;
                break;

            case "header": case "footer": case "main": case "nav": case "aside":
                if (regions.size() > 0 && regions.peek().equals(name)) {
                    regions.pop();
                } else {
                    error(currFile, getLineNumber(), "unmatched tag: " + name);
                    regionErrors++;
                }
                break;

            case "noscript":
                inNoScript = false;
                break;
        }

    }

    private void checkHeading(String h) {
        int rank = Character.digit(h.charAt(1), 10);
        if (rank > currRank + 1) {
            headingErrors++;
            StringBuilder sb = new StringBuilder();
            String sep = "";
            for (int i = currRank + 1; i < rank; i++) {
                sb.append(sep).append("h").append(i);
                sep = ", ";
            }
            error(currFile, getLineNumber(), "missing headings: " + sb);
        }
        currRank = rank;
    }

    @Override
    public void content(String s) {
        if (html5 && inBody && !inNoScript && !s.isBlank() && regions.isEmpty()) {
            error(currFile, getLineNumber(), "content outside of any region");
        }
    }
}
