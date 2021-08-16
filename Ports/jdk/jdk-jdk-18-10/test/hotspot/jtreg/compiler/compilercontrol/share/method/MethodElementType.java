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

package compiler.compilercontrol.share.method;

import java.util.regex.Pattern;

/**
 * Class represents an element of the MethodDescriptor
 * used as pattern for CompilerCommand method strings
 */
public abstract class MethodElementType {
    private static final char[] INVALID_CHARS = { ';', '[', '(', ')', ']',
            '<', '>'};
    protected String element;
    protected String regexp;
    protected MethodDescriptor.Separator separator;

    /**
     * Constructor
     */
    protected MethodElementType(MethodDescriptor.Separator separator) {
        this.separator = separator;
    }

    /**
     * Gets element's separator
     *
     * @return separator instance
     */
    public MethodDescriptor.Separator getSeparator() {
        return separator;
    }

    /**
     * Sets separator for this element
     *
     * @param separator separator type
     */
    public void setSeparator(MethodDescriptor.Separator separator) {
        this.separator = separator;
    }

    /**
     * Gets String representation of the element
     *
     * @return element string
     */
    public String getElement() {
        return element;
    }

    /**
     * Sets String representation of the element
     *
     * @param element custom string to be used as an element
     */
    public void setElement(String element) {
        this.element = element;
        this.regexp = Pattern.quote(element);
    }

    /**
     * Shows that the element is valid according to CompilerControl and JVMS specs
     *
     * @return true, if the element is a valid string
     */
    public boolean isValid() {
        for (char ch : INVALID_CHARS) {
            if (element.indexOf(ch) != -1) {
                return false;
            }
        }
        // Check for * usage
        if (element.equals("**")) {
            return false;
        }
        for (int i = 0; i < element.length(); i++) {
            char c = element.charAt(i);
            if (c == '*' && i > 0 && i < element.length() - 1) {
                // Embedded * isn't allowed
                return false;
            }
        }
        return true;
    }

    /**
     * Creates pattern of a given type
     *
     * @param patternType type of the pattern
     */
    public void setPattern(MethodDescriptor.PatternType patternType) {
        switch (patternType) {
            case EXACT:
                break;
            case PREFIX:
                regexp = ".*" + regexp;
                element = "*" + element;
                break;
            case ANY:
                regexp = ".*";
                element = "*";
                break;
            case SUFFIX:
                regexp = regexp + ".*";
                element = element + "*";
                break;
            case SUBSTRING:
                setPattern(MethodDescriptor.PatternType.PREFIX);
                setPattern(MethodDescriptor.PatternType.SUFFIX);
                break;
            default:
                throw new IllegalArgumentException("ERROR: wrong pattern type"
                        + patternType);
        }
    }

    /**
     * Gets regular expression of this element
     *
     * @return string representation of regexp
     */
    public String getRegexp() {
        return regexp;
    }
}
