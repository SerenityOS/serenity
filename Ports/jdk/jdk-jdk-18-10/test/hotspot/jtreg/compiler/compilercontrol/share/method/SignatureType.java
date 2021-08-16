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

import jdk.test.lib.Utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.regex.Pattern;

/**
 * This class represents a signature of the method
 */
public class SignatureType extends MethodElementType {
    public SignatureType(Executable method) {
        super(MethodDescriptor.Separator.NONE);
        // Get parameters
        Class<?>[] types = method.getParameterTypes();
        String[] parameterTypes = new String[types.length];
        for (int i = 0; i < types.length; i++) {
            parameterTypes[i] = Utils.toJVMTypeSignature(types[i]);
        }
        // Get return value
        String returnType;
        if (method instanceof Method) {
            returnType = Utils.toJVMTypeSignature(((Method) method)
                    .getReturnType());
        } else if (method instanceof Constructor) {
            // Constructor returns void in VM
            returnType = Utils.toJVMTypeSignature(void.class);
        } else {
            throw new Error(String.format("TESTBUG: wrong type of executable "
                    + "%s of class %s", method, method.getClass()));
        }
        // Create signature
        setElement("(" + String.join("", parameterTypes)+ ")" + returnType);
        regexp = element;
        setPattern(MethodDescriptor.PatternType.EXACT);
        separator = MethodDescriptor.Separator.NONE;
    }

    @Override
    public void setElement(String element) {
        if (element.isEmpty()) {
            setPattern(MethodDescriptor.PatternType.ANY);
        } else {
            this.element = element;
            this.regexp = element;
        }
    }

    @Override
    public boolean isValid() {
        if (element.isEmpty()) {
            return true;
        }
        // Allowed primitive types
        char[] baseTypes = {'B', 'C', 'D', 'F', 'I', 'J', 'S', 'Z'};  // sorted
        // Parsing states
        boolean isArray = false;
        boolean isInsideSig = false;
        boolean isClass = false;

        for (char ch : element.toCharArray()) {
            if (ch == '(') {
                if (isInsideSig) {
                    // Met another ( inside
                    return false;
                }
                isInsideSig = true;
            } else if (ch == ')') {
                if (!isInsideSig) {
                    // met another ) outside
                    return false;
                }
                isInsideSig = false;
            } else if (ch == 'V') {
                if (isInsideSig) {
                    // void type is allowed only as a return value
                    return false;
                }
            } else if (ch == 'L') {
                // this is a beginning of class/interface
                isClass = true;
                // met actual type of array
                isArray = false;
            } else if (ch == '[') {
                isArray = true;
            } else if (isClass) {
                if (!Character.isJavaIdentifierPart(ch)) {
                    if (ch == '/' || ch == '.') {
                        // separator met
                    } else if (ch == ';') {
                        // end of class/interface
                        isClass = false;
                    } else {
                        return false;
                    }
                }
            } else if (Arrays.binarySearch(baseTypes, ch) < 0) {
                // if it doesn't belong to base types
                return false;
            } else {
                // array of a base type
                isArray = false;
            }
        }
        return !(isArray || isInsideSig || isClass);
    }

    @Override
    public void setPattern(MethodDescriptor.PatternType patternType) {
        switch (patternType) {
            case PREFIX:
            case SUFFIX:
            case SUBSTRING:
                // These patterns are not supported in Compiler Control
                // Just use ANY pattern instead
            case ANY:
                regexp = "\\(.*\\).*";
                element = "";
                break;
            case EXACT:
                break;
            default:
                throw new IllegalArgumentException("ERROR: wrong pattern type "
                        + patternType);
        }
    }

    @Override
    public String getRegexp() {
        if ("\\(.*\\).*".equals(regexp)) {
            return regexp;
        }
        return Pattern.quote(regexp);
    }
}
