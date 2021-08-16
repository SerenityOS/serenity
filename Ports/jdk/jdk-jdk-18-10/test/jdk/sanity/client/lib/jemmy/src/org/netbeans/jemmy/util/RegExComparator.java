/*
 * Copyright (c) 1997, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Oracle designates this
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
package org.netbeans.jemmy.util;

import java.lang.reflect.InvocationTargetException;

import org.netbeans.jemmy.ClassReference;
import org.netbeans.jemmy.JemmyException;
import org.netbeans.jemmy.operators.Operator.StringComparator;

/**
 * Be executed under 1.4 uses {@code java.util.regex.Pattern}
 * functionality. Otherwise understands only "." and "*" simbols, i.e. regexprs
 * like ".*Ques.ion.*".
 */
public class RegExComparator implements StringComparator {

    private static final int ANY_SIMBOL = -1;
    private static final int IGNORE_SIMBOL = -999;

    @Override
    public boolean equals(String caption, String match) {
        if (match == null) {
            return true;
        }
        if (caption == null) {
            return false;
        }
        if (System.getProperty("java.specification.version").compareTo("1.3") > 0) {
            try {
                Object result = new ClassReference("java.util.regex.Pattern").
                        invokeMethod("matches",
                                new Object[]{match, caption},
                                new Class<?>[]{String.class, Class.forName("java.lang.CharSequence")});
                return ((Boolean) result).booleanValue();
            } catch (InvocationTargetException e) {
                throw (new JemmyException("Exception during regexpr using",
                        e));
            } catch (ClassNotFoundException e) {
                throw (new JemmyException("Exception during regexpr using",
                        e));
            } catch (NoSuchMethodException e) {
                throw (new JemmyException("Exception during regexpr using",
                        e));
            } catch (IllegalAccessException e) {
                throw (new JemmyException("Exception during regexpr using",
                        e));
            }
        } else {
            return parse(caption, match);
        }
    }

    /**
     * Checks that caption matshes the pattern. Understands only "." (any
     * symbol) and "*" (repeat symbol). Used for 1.3 and earclier javas,
     * starting from 1.4 {@code java.util.regex.Pattern} class is used.
     *
     * @param caption a caption to compare with the pattern.
     * @param match a pattern
     * @return true if the caption matches the pattern.
     */
    public boolean parse(String caption, String match) {
        if (match.length() == 0
                && caption.length() == 0) {
            return true;
        } else if (match.length() == 0) {
            return false;
        }
        int c0 = match.charAt(0);
        int c1 = IGNORE_SIMBOL;
        if (match.length() > 1) {
            c1 = match.charAt(1);
        }
        int shift = 1;
        switch (c0) {
            case '\\':
                if (match.length() == 1) {
                    throw (new RegExParsingException("\\ is not appropriate"));
                }
                c0 = match.charAt(1);
                if (match.length() > 2) {
                    c1 = match.charAt(2);
                } else {
                    c1 = IGNORE_SIMBOL;
                }
                shift = 2;
                break;
            case '.':
                c0 = ANY_SIMBOL;
                break;
            case '*':
                throw (new RegExParsingException("* is not appropriate"));
        }
        if (c1 == '*') {
            shift = shift + 1;
            int i = 0;
            while (i <= caption.length()) {
                if (i == 0
                        || checkOne(caption.substring(i - 1), c0)) {
                    if (parse(caption.substring(i), match.substring(shift))) {
                        return true;
                    }
                } else {
                    return false;
                }
                i++;
            }
            return false;
        } else {
            if (caption.length() == 0) {
                return false;
            }
            if (checkOne(caption, c0)) {
                return parse(caption.substring(1), match.substring(shift));
            } else {
                return false;
            }
        }
    }

    private boolean checkOne(String caption, int simbol) {
        return (simbol == ANY_SIMBOL
                || simbol == caption.charAt(0));
    }

    /**
     * Thrown in case of parsing error.
     */
    public static class RegExParsingException extends JemmyException {

        private static final long serialVersionUID = 42L;

        /**
         * Constructs a RegExComparator$RegExParsingException object.
         *
         * @param message an error message
         */
        public RegExParsingException(String message) {
            super(message);
        }

        /**
         * Constructs a RegExComparator$RegExParsingException object.
         *
         * @param message an error message
         * @param innerException a parsing exception.
         */
        public RegExParsingException(String message, Exception innerException) {
            super(message, innerException);
        }
    }
}
