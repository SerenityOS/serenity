/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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
package java.lang.constant;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static java.util.Objects.requireNonNull;

/**
 * Helper methods for the implementation of {@code java.lang.constant}.
 */
class ConstantUtils {
    /** an empty constant descriptor */
    public static final ConstantDesc[] EMPTY_CONSTANTDESC = new ConstantDesc[0];
    static final Constable[] EMPTY_CONSTABLE = new Constable[0];
    static final int MAX_ARRAY_TYPE_DESC_DIMENSIONS = 255;

    private static final Set<String> pointyNames = Set.of("<init>", "<clinit>");

    /**
     * Validates the correctness of a binary class name. In particular checks for the presence of
     * invalid characters in the name.
     *
     * @param name the class name
     * @return the class name passed if valid
     * @throws IllegalArgumentException if the class name is invalid
     */
    static String validateBinaryClassName(String name) {
        for (int i=0; i<name.length(); i++) {
            char ch = name.charAt(i);
            if (ch == ';' || ch == '[' || ch == '/')
                throw new IllegalArgumentException("Invalid class name: " + name);
        }
        return name;
    }

    /**
     * Validates a member name
     *
     * @param name the name of the member
     * @return the name passed if valid
     * @throws IllegalArgumentException if the member name is invalid
     */
    public static String validateMemberName(String name, boolean method) {
        requireNonNull(name);
        if (name.length() == 0)
            throw new IllegalArgumentException("zero-length member name");
        for (int i=0; i<name.length(); i++) {
            char ch = name.charAt(i);
            if (ch == '.' || ch == ';' || ch == '[' || ch == '/')
                throw new IllegalArgumentException("Invalid member name: " + name);
            if (method && (ch == '<' || ch == '>')) {
                if (!pointyNames.contains(name))
                    throw new IllegalArgumentException("Invalid member name: " + name);
            }
        }
        return name;
    }

    static void validateClassOrInterface(ClassDesc classDesc) {
        if (!classDesc.isClassOrInterface())
            throw new IllegalArgumentException("not a class or interface type: " + classDesc);
    }

    static int arrayDepth(String descriptorString) {
        int depth = 0;
        while (descriptorString.charAt(depth) == '[')
            depth++;
        return depth;
    }

    static String binaryToInternal(String name) {
        return name.replace('.', '/');
    }

    static String internalToBinary(String name) {
        return name.replace('/', '.');
    }

    static String dropLastChar(String s) {
        return s.substring(0, s.length() - 1);
    }

    static String dropFirstAndLastChar(String s) {
        return s.substring(1, s.length() - 1);
    }

    /**
     * Parses a method descriptor string, and return a list of field descriptor
     * strings, return type first, then parameter types
     *
     * @param descriptor the descriptor string
     * @return the list of types
     * @throws IllegalArgumentException if the descriptor string is not valid
     */
    static List<String> parseMethodDescriptor(String descriptor) {
        int cur = 0, end = descriptor.length();
        ArrayList<String> ptypes = new ArrayList<>();

        if (cur >= end || descriptor.charAt(cur) != '(')
            throw new IllegalArgumentException("Bad method descriptor: " + descriptor);

        ++cur;  // skip '('
        while (cur < end && descriptor.charAt(cur) != ')') {
            int len = skipOverFieldSignature(descriptor, cur, end, false);
            if (len == 0)
                throw new IllegalArgumentException("Bad method descriptor: " + descriptor);
            ptypes.add(descriptor.substring(cur, cur + len));
            cur += len;
        }
        if (cur >= end)
            throw new IllegalArgumentException("Bad method descriptor: " + descriptor);
        ++cur;  // skip ')'

        int rLen = skipOverFieldSignature(descriptor, cur, end, true);
        if (rLen == 0 || cur + rLen != end)
            throw new IllegalArgumentException("Bad method descriptor: " + descriptor);
        ptypes.add(0, descriptor.substring(cur, cur + rLen));
        return ptypes;
    }

    private static final char JVM_SIGNATURE_ARRAY = '[';
    private static final char JVM_SIGNATURE_BYTE = 'B';
    private static final char JVM_SIGNATURE_CHAR = 'C';
    private static final char JVM_SIGNATURE_CLASS = 'L';
    private static final char JVM_SIGNATURE_ENDCLASS = ';';
    private static final char JVM_SIGNATURE_ENUM = 'E';
    private static final char JVM_SIGNATURE_FLOAT = 'F';
    private static final char JVM_SIGNATURE_DOUBLE = 'D';
    private static final char JVM_SIGNATURE_FUNC = '(';
    private static final char JVM_SIGNATURE_ENDFUNC = ')';
    private static final char JVM_SIGNATURE_INT = 'I';
    private static final char JVM_SIGNATURE_LONG = 'J';
    private static final char JVM_SIGNATURE_SHORT = 'S';
    private static final char JVM_SIGNATURE_VOID = 'V';
    private static final char JVM_SIGNATURE_BOOLEAN = 'Z';

    /**
     * Validates that the characters at [start, end) within the provided string
     * describe a valid field type descriptor.
     * @param descriptor the descriptor string
     * @param start the starting index into the string
     * @param end the ending index within the string
     * @param voidOK is void acceptable?
     * @return the length of the descriptor, or 0 if it is not a descriptor
     * @throws IllegalArgumentException if the descriptor string is not valid
     */
    @SuppressWarnings("fallthrough")
    static int skipOverFieldSignature(String descriptor, int start, int end, boolean voidOK) {
        int arrayDim = 0;
        int index = start;
        while (index < end) {
            switch (descriptor.charAt(index)) {
                case JVM_SIGNATURE_VOID: if (!voidOK) { return index; }
                case JVM_SIGNATURE_BOOLEAN:
                case JVM_SIGNATURE_BYTE:
                case JVM_SIGNATURE_CHAR:
                case JVM_SIGNATURE_SHORT:
                case JVM_SIGNATURE_INT:
                case JVM_SIGNATURE_FLOAT:
                case JVM_SIGNATURE_LONG:
                case JVM_SIGNATURE_DOUBLE:
                    return index - start + 1;
                case JVM_SIGNATURE_CLASS:
                    // Skip leading 'L' and ignore first appearance of ';'
                    index++;
                    int indexOfSemi = descriptor.indexOf(';', index);
                    if (indexOfSemi != -1) {
                        String unqualifiedName = descriptor.substring(index, indexOfSemi);
                        boolean legal = verifyUnqualifiedClassName(unqualifiedName);
                        if (!legal) {
                            return 0;
                        }
                        return index - start + unqualifiedName.length() + 1;
                    }
                    return 0;
                case JVM_SIGNATURE_ARRAY:
                    arrayDim++;
                    if (arrayDim > MAX_ARRAY_TYPE_DESC_DIMENSIONS) {
                        throw new IllegalArgumentException(String.format("Cannot create an array type descriptor with more than %d dimensions",
                                ConstantUtils.MAX_ARRAY_TYPE_DESC_DIMENSIONS));
                    }
                    // The rest of what's there better be a legal descriptor
                    index++;
                    voidOK = false;
                    break;
                default:
                    return 0;
            }
        }
        return 0;
    }

    static boolean verifyUnqualifiedClassName(String name) {
        for (int index = 0; index < name.length(); index++) {
            char ch = name.charAt(index);
            if (ch < 128) {
                if (ch == '.' || ch == ';' || ch == '[' ) {
                    return false;   // do not permit '.', ';', or '['
                }
                if (ch == '/') {
                    // check for '//' or leading or trailing '/' which are not legal
                    // unqualified name must not be empty
                    if (index == 0 || index + 1 >= name.length() || name.charAt(index + 1) == '/') {
                        return false;
                    }
                }
            } else {
                index ++;
            }
        }
        return true;
    }
}
