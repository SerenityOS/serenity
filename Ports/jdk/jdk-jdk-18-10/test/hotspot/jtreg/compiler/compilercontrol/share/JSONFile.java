/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

package compiler.compilercontrol.share;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.Objects;
import java.util.Stack;

/**
 * Simple JSON file writer
 */
public class JSONFile implements AutoCloseable {
    private final Stack<Element> stack;
    private final String fileName;
    private final PrintStream out;
    private int spaces;

    /**
     * JSON element
     */
    public enum Element {
        OBJECT,
        ARRAY,
        PAIR,
        VALUE
    }

    /**
     * Constructor. Creates file with default name
     */
    public JSONFile() {
        this("directives_file.json");
    }

    /**
     * Constructor
     *
     * @param fileName file name
     */
    public JSONFile(String fileName) {
        this.spaces = 0;
        this.stack = new Stack<>();
        this.fileName = fileName;
        try {
            out = new PrintStream(fileName);
        } catch (FileNotFoundException e) {
            throw new Error("TESTBUG: can't open/create file " + fileName, e);
        }
    }

    /**
     * Gets file name
     *
     * @return file name string
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Gets current JSON element in the file.
     * The element is a current {@linkplain Element}
     * that was written into a file.
     *
     * @return the type of the current element,
     * or null if there are nothing written
     */
    public Element getElement() {
        if (stack.empty()) {
            return null;
        }
        return stack.peek();
    }

    /**
     * Writes given type with a value to file.
     * Note that only PAIR and VALUE types accept a single value parameter.
     * OBJECT and ARRAY do not have a value
     *
     * @param element  JSON element type
     * @param value element's value
     * @return this file instance
     */
    public JSONFile write(Element element, String... value) {
        if (value.length > 1) {
            throw new Error("TESTBUG: Unexpected value length: "
                    + value.length);
        }
        if (!stack.empty()) {
            if (stack.peek() == Element.VALUE) {
                out.print(", ");
                stack.pop();
            }
        }
        switch (element) {
            case OBJECT:
                out.print("{");
                spaces++;
                stack.push(Element.VALUE);
                break;
            case ARRAY:
                out.print("[");
                stack.push(Element.VALUE);
                break;
            case PAIR:
                fillSpaces();
                Objects.requireNonNull(value, "TESTBUG: " + element
                        + "requires a value to be set");
                out.print(value[0] + ": ");
                break;
            case VALUE:
                Objects.requireNonNull(value, "TESTBUG: " + element
                        + "requires a value to be set");
                out.print(value[0]);
                break;
        }
        stack.push(element);
        return this;
    }

    private void fillSpaces() {
        out.println();
        for (int i = 0; i < spaces; i++) {
            // Fill with spaces to be more readable
            out.print("  ");
        }
    }

    /**
     * Ends current object or array of {@linkplain Element}
     *
     * @return this file instance
     */
    public JSONFile end() {
        if (!stack.empty()) {
            Element prev = stack.pop();
            while (prev != Element.OBJECT && prev != Element.ARRAY
                    && !stack.empty()) {
                prev = stack.pop();
            }
            switch (prev) {
                case OBJECT:
                    spaces--;
                    fillSpaces();
                    out.print("}");
                    break;
                case ARRAY:
                    out.print("]");
                    break;
                default:
                    throw new Error("TESTBUG: Incorrect end. " +
                            "Wrong type found: " + prev);
            }
        } else {
            throw new Error("TESTBUG: Incorrect end. Empty stack");
        }
        return this;
    }

    /**
     * Closes this file
     */
    @Override
    public void close() {
        out.close();
    }
}
