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

package java.util.regex;

/**
 * Unchecked exception thrown to indicate a syntax error in a
 * regular-expression pattern.
 *
 * @since 1.4
 */

public class PatternSyntaxException
    extends IllegalArgumentException
{
    @java.io.Serial
    private static final long serialVersionUID = -3864639126226059218L;

    private final String desc;
    private final String pattern;
    private final int index;

    /**
     * Constructs a new instance of this class.
     *
     * @param  desc
     *         A description of the error
     *
     * @param  regex
     *         The erroneous pattern
     *
     * @param  index
     *         The approximate index in the pattern of the error,
     *         or {@code -1} if the index is not known
     */
    public PatternSyntaxException(String desc, String regex, int index) {
        this.desc = desc;
        this.pattern = regex;
        this.index = index;
    }

    /**
     * Retrieves the error index.
     *
     * @return  The approximate index in the pattern of the error,
     *         or {@code -1} if the index is not known
     */
    public int getIndex() {
        return index;
    }

    /**
     * Retrieves the description of the error.
     *
     * @return  The description of the error
     */
    public String getDescription() {
        return desc;
    }

    /**
     * Retrieves the erroneous regular-expression pattern.
     *
     * @return  The erroneous pattern
     */
    public String getPattern() {
        return pattern;
    }

    /**
     * Returns a multi-line string containing the description of the syntax
     * error and its index, the erroneous regular-expression pattern, and a
     * visual indication of the error index within the pattern.
     *
     * @return  The full detail message
     */
    public String getMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append(desc);
        if (index >= 0) {
            sb.append(" near index ");
            sb.append(index);
        }
        sb.append(System.lineSeparator());
        sb.append(pattern);
        if (index >= 0 && pattern != null && index < pattern.length()) {
            sb.append(System.lineSeparator());
            for (int i = 0; i < index; i++) {
                sb.append((pattern.charAt(i) == '\t') ? '\t' : ' ');
            }
            sb.append('^');
        }
        return sb.toString();
    }

}
