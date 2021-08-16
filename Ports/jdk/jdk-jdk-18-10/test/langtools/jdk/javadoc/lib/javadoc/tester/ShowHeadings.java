/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
import java.util.function.Function;

public class ShowHeadings extends HtmlChecker {

    private int currLevel;

    private boolean copyContent;

    ShowHeadings(PrintStream out, Function<Path,String> fileReader) {
        super(out, fileReader);
    }

    @Override
    public void report() {
    }

    @Override
    public void startFile(Path path) {
        out.println("Headings: " + path);
        currLevel = 0;
    }

    @Override
    public void endFile() {
    }

    @Override
    public void docType(String doctype) {
    }

    @Override
    public void startElement(String name, Map<String,String> attrs, boolean selfClosing) {
        switch (name) {
            case "h1": case "h2": case "h3": case "h4": case "h5": case "h6":
                startHeading(name);
                break;
        }
    }

    @Override
    public void endElement(String name) {
        switch (name) {
            case "h1": case "h2": case "h3": case "h4": case "h5": case "h6":
                out.println();
                copyContent = false;
                break;
        }

    }

    private void startHeading(String h) {
        int level = Character.digit(h.charAt(1), 10);
        while (level > currLevel + 1) {
            currLevel++;
            out.println("*  ".repeat(currLevel - 1) + "H" + currLevel + ": *** MISSING ***");
        }
        currLevel = level;
        out.print("*  ".repeat(currLevel - 1) + "H" + currLevel + ": ");
        copyContent = true;
    }

    @Override
    public void content(String s) {
        if (copyContent) {
            out.print(s.replace("&nbsp;", " ")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&amp;", "&")
                .replaceAll("\\s+", " ")
                );
        }
    }
}

