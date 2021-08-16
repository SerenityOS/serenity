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

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;

/**
 * Represents a method in CompileControl method signature
 */
public class MethodType extends MethodElementType {
    private static final char[] INVALID_CHARS = { '.', '/' };

    public MethodType(Executable method) {
        // Use pack/subpack/Class::method separators style
        super(MethodDescriptor.Separator.DOT);
        if (method instanceof Constructor) {
            element = "<init>";
        } else {
            element = method.getName();
        }
        regexp = element;
    }

    @Override
    public boolean isValid() {
        for (char ch : INVALID_CHARS) {
            if (element.indexOf(ch) != -1) {
                return false;
            }
        }
        if (element.isEmpty()) {
            // Shouldn't be empty
            return false;
        }
        if (element.contains("<") || element.contains(">")) {
            return element.matches("(\\*)?<(cl)?init>(\\*)?");
        }
        return super.isValid();
    }

    @Override
    public void setPattern(MethodDescriptor.PatternType patternType) {
        switch (patternType) {
            case EXACT:
                break;
            case PREFIX:
                regexp = ".*" + regexp;
                element = "*" + element;
                break;
            case ANY:
                regexp = "[^(]*";
                element = "*";
                break;
            case SUFFIX:
                regexp = regexp + "[^(]*";
                element = element + "*";
                break;
            case SUBSTRING:
                setPattern(MethodDescriptor.PatternType.PREFIX);
                setPattern(MethodDescriptor.PatternType.SUFFIX);
                break;
            default:
                throw new IllegalArgumentException("ERROR: wrong pattern type "
                        + patternType);
        }
    }
}
