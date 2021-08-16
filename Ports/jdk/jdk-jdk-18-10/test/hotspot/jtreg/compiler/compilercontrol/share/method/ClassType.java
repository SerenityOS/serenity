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

import java.lang.reflect.Executable;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Element represents class in method descriptor
 * that consist from class package and class itself
 */
public class ClassType extends MethodElementType {
    private final String[] packageDirs;
    private final Class<?> aClass;
    private boolean setPackage;

    public ClassType(Executable method) {
        // Use pack/subpack/Class::method separators style
        super(MethodDescriptor.Separator.SLASH);
        // Get package
        aClass = method.getDeclaringClass();
        Package aPackage = method.getDeclaringClass().getPackage();
        if (aPackage != null) {
            // split into directories
            packageDirs = aPackage.getName().split("\\.");
        } else {
            packageDirs = null;
        }
        setPackage = true;
        buildElement(setPackage);
    }

    @Override
    public boolean isValid() {
        if (element.isEmpty()) {
            return false;
        }
        boolean separatorMet = false;
        char separatorChar = 0;
        char[] charArray = element.toCharArray();
        for (int i = 0; i < charArray.length; i++) {
            char ch = charArray[i];
            switch (ch) {
                case '/':
                case '.':
                    if (separatorMet) {
                        if (ch != separatorChar) {
                            // there are two different separators
                            return false;
                        }
                    } else {
                        separatorChar = ch;
                        separatorMet = true;
                    }
                    break;
                case ':':
                    if (++i != charArray.length) {
                        if (charArray[i] == ':') {
                            // :: is invalid separator
                            separator = MethodDescriptor.Separator.DOUBLECOLON;
                            return false;
                        }
                    }
                    break;
                // Invalid separators
                case ',':
                case ' ':
                    return false;
            }
        }
        // set correct separator
        switch (separatorChar) {
            case '.':
                separator = MethodDescriptor.Separator.DOT;
                break;
            case '/':
                separator = MethodDescriptor.Separator.SLASH;
                break;
            default:
                separator = MethodDescriptor.Separator.NONE;
                break;
        }
        return super.isValid();
    }

    @Override
    public void setSeparator(MethodDescriptor.Separator separator) {
        this.separator = separator;
        buildElement(setPackage);
    }

    @Override
    public void setPattern(MethodDescriptor.PatternType patternType) {
        switch (patternType) {
            case EXACT:
                break;
            case PREFIX:
                // For prefix pattern use only class name without package
                buildElement(false);
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
                throw new IllegalArgumentException("ERROR: wrong pattern type "
                        + patternType);
        }
    }

    /**
     * Builds element string and regexp.
     *
     * @param setPackage shows that element should have a package name
     */
    private void buildElement(boolean setPackage) {
        this.setPackage = setPackage;
        StringBuilder elementBuilder = new StringBuilder();
        if (packageDirs != null && setPackage) {
            elementBuilder.append(Arrays.stream(packageDirs)
                    .collect(Collectors.joining(separator.symbol)));
            elementBuilder.append(separator.symbol);
        }
        String className = aClass.getSimpleName();
        if (setPackage) {
            // Add outer classes if any
            Class<?> enclosingClass = aClass.getEnclosingClass();
            while (enclosingClass != null) {
                className = enclosingClass.getSimpleName() + "$" + className;
                enclosingClass = enclosingClass.getEnclosingClass();
            }
        }
        elementBuilder.append(className);
        element = elementBuilder.toString();
        if (separator == MethodDescriptor.Separator.DOT) {
            // Replace . with / to make regexp look like CommandSignature
            regexp = element.replace(".", "/");
        } else {
            regexp = element;
        }
        regexp = regexp.replace("$", "\\$");
    }
}
