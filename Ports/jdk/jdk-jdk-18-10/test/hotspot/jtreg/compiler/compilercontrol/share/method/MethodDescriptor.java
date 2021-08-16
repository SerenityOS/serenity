/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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

import jdk.test.lib.util.Triple;

import java.lang.reflect.Executable;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Method descriptor for Compiler Control commands.
 * It represents method pattern used for matching in Compiler Control
 * and CompileCommand option
 */
public class MethodDescriptor {
    public final ClassType aClass;         // Represents class and package
    public final MethodType aMethod;       // Represents method
    public final SignatureType aSignature; // Represents signature

    /**
     * Constructor
     *
     * @param method executable to build method descriptor from
     */
    public MethodDescriptor(Executable method) {
        aClass = new ClassType(method);
        aMethod = new MethodType(method);
        aSignature = new SignatureType(method);
    }

    /**
     * Sets signature separators for all elements
     */
    public void setSeparators(
            Triple<Separator, Separator, Separator> separators) {
        aClass.setSeparator(separators.getFirst());
        aMethod.setSeparator(separators.getSecond());
        aSignature.setSeparator(separators.getThird());
    }

    /**
     * Sets custom strings for each element
     */
    public void setStrings(Triple<String, String, String> strings) {
        aClass.setElement(strings.getFirst());
        aMethod.setElement(strings.getSecond());
        aSignature.setElement(strings.getThird());
    }

    /**
     * Sets patterns for all elements
     */
    public void setPatterns(
            Triple<PatternType, PatternType, PatternType> patterns) {
        aClass.setPattern(patterns.getFirst());
        aMethod.setPattern(patterns.getSecond());
        aSignature.setPattern(patterns.getThird());
    }

    /**
     * Separates elements in the MethodDescriptor
     */
    public static enum Separator {
        SLASH("/"),
        DOT("."),
        COMMA(","),
        DOUBLECOLON("::"),
        SPACE(" "),
        NONE("");

        public final String symbol;

        Separator(String symbol) {
            this.symbol = symbol;
        }

        /**
         * Validates method descriptor separators
         *
         * @param md method descriptor to validate
         * @return true if descriptor's separators are valid
         */
        public static boolean isValid(MethodDescriptor md) {
            Separator cls = md.getClassSeparator();
            Separator method = md.getMethodSeparator();
            Separator sign = md.getSignatureSeparator();
            if (sign == SPACE || sign == NONE || sign == COMMA) {
                // if it looks like java/lang/String.indexOf
                if ((cls == SLASH || cls == NONE)
                        // allow space and comma instead of dot
                        && (method == DOT || method == SPACE
                        || method == COMMA)) {
                    return true;
                }
                // if it looks like java.lang.String::indexOf
                if ((cls == DOT || cls == NONE) && method == DOUBLECOLON) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Type of the pattern
     */
    public static enum PatternType {
        PREFIX,
        ANY,
        SUFFIX,
        SUBSTRING,
        EXACT
    }

    public Separator getClassSeparator() {
        return aClass.getSeparator();
    }

    public Separator getMethodSeparator() {
        return aMethod.getSeparator();
    }

    public Separator getSignatureSeparator() {
        return aSignature.getSeparator();
    }

    /**
     * Gets regular expression to match methods
     *
     * @return string representation of the regular expression
     */
    public String getRegexp() {
        // regexp should have a . as a method separator
        // and / as a package/class separator
        return aClass.getRegexp().replaceAll("\\.", "/")
                .replaceAll("/\\*", ".*")
                + Pattern.quote(Separator.DOT.symbol)
                + aMethod.getRegexp() + aSignature.getRegexp();
    }

    /**
     * Gets method descriptor string representation.
     * This string is used as a pattern in CompilerControl and CompileCommand
     */
    public String getString() {
        return aClass.getElement() + getMethodSeparator().symbol
                + aMethod.getElement() + getSignatureSeparator().symbol
                + aSignature.getElement();
    }

    /**
     * Convert method descriptor to be regexp-compatible
     *
     * @return string representation of the method signature
     */
    public String getCanonicalString() {
        return aClass.getElement().replaceAll("\\.", "/") + Separator.DOT.symbol
                + aMethod.getElement() + aSignature.getElement();
    }

    /**
     * Shows if this descriptor is a valid pattern for CompilerControl
     *
     * @return true, if descriptor is valid, false otherwise
     */
    public boolean isValid() {
        return aClass.isValid() && aMethod.isValid() && aSignature.isValid()
                && Separator.isValid(this);
    }

    /**
     * Sets custom string from element mutate function
     * to the appropriate element of method descriptor
     */
    public void applyMutates(Triple<Function<String, String>,
                             Function<String, String>,
                             Function<String, String>> mutators) {
        String elementString = aClass.getElement();
        aClass.setElement(mutators.getFirst().apply(elementString));
        elementString = aMethod.getElement();
        aMethod.setElement(mutators.getSecond().apply(elementString));
        elementString = aSignature.getElement();
        aSignature.setElement(mutators.getThird().apply(elementString));
    }
}
