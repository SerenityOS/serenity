/*
 * Copyright (c) 1998, 2020, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.jdi;

import java.util.ArrayList;
import java.util.List;

public class JNITypeParser {

    static final char SIGNATURE_ENDCLASS = ';';
    static final char SIGNATURE_FUNC = '(';
    static final char SIGNATURE_ENDFUNC = ')';

    private String signature;
    private List<String> typeNameList;
    private List<String> signatureList;
    private int currentIndex;

    JNITypeParser(String signature) {
        this.signature = signature;
    }

    static String typeNameToSignature(String typeName) {
        StringBuilder sb = new StringBuilder();
        int firstIndex = typeName.indexOf('[');
        int index = firstIndex;
        while (index != -1) {
            sb.append('[');
            index = typeName.indexOf('[', index + 1);
        }

        if (firstIndex != -1) {
            typeName = typeName.substring(0, firstIndex);
        }

        if (typeName.equals("boolean")) {
            sb.append('Z');
        } else if (typeName.equals("byte")) {
            sb.append('B');
        } else if (typeName.equals("char")) {
            sb.append('C');
        } else if (typeName.equals("short")) {
            sb.append('S');
        } else if (typeName.equals("int")) {
            sb.append('I');
        } else if (typeName.equals("long")) {
            sb.append('J');
        } else if (typeName.equals("float")) {
            sb.append('F');
        } else if (typeName.equals("double")) {
            sb.append('D');
        } else {
            sb.append('L');
            index = typeName.indexOf("/");   // check if it's a hidden class
            if (index < 0) {
                sb.append(typeName.replace('.', '/'));
            } else {
                sb.append(typeName.substring(0, index).replace('.', '/'));
                sb.append(".");
                sb.append(typeName.substring(index + 1));
            }
            sb.append(';');
        }

        return sb.toString();
    }

    String typeName() {
        return typeNameList().get(typeNameList().size()-1);
    }

    List<String> argumentTypeNames() {
        return typeNameList().subList(0, typeNameList().size() - 1);
    }

    String signature() {
        return signatureList().get(signatureList().size()-1);
    }

    List<String> argumentSignatures() {
        return signatureList().subList(0, signatureList().size() - 1);
    }

    int dimensionCount() {
        int count = 0;
        String signature = signature();
        while (signature.charAt(count) == '[') {
            count++;
        }
        return count;
    }

    byte jdwpTag() {
        return (byte) signature().charAt(0);
    }

    String componentSignature(int level) {
        assert level <= dimensionCount();
        return signature().substring(level);
    }

    String componentSignature() {
        assert isArray();
        return componentSignature(1);
    }

    boolean isArray() {
        return jdwpTag() == JDWP.Tag.ARRAY;
    }

    boolean isVoid() {
        return jdwpTag() == JDWP.Tag.VOID;
    }

    boolean isBoolean() {
        return jdwpTag() == JDWP.Tag.BOOLEAN;
    }

    boolean isReference() {
        byte tag = jdwpTag();
        return tag == JDWP.Tag.ARRAY ||
                tag == JDWP.Tag.OBJECT;
    }

    boolean isPrimitive() {
        switch (jdwpTag()) {
            case (JDWP.Tag.BOOLEAN):
            case (JDWP.Tag.BYTE):
            case (JDWP.Tag.CHAR):
            case (JDWP.Tag.SHORT):
            case (JDWP.Tag.INT):
            case (JDWP.Tag.LONG):
            case (JDWP.Tag.FLOAT):
            case (JDWP.Tag.DOUBLE):
                return true;
        }
        return false;
    }

    static String convertSignatureToClassname(String classSignature) {
        assert classSignature.startsWith("L") && classSignature.endsWith(";");

        // trim leading "L" and trailing ";"
        String name = classSignature.substring(1, classSignature.length() - 1);
        int index = name.indexOf(".");  // check if it is a hidden class
        if (index < 0) {
            return name.replace('/', '.');
        } else {
            // map the type descriptor from: "L" + N + "." + <suffix> + ";"
            // to class name: N.replace('/', '.') + "/" + <suffix>
            return name.substring(0, index).replace('/', '.')
                    + "/" + name.substring(index + 1);
        }
    }

    private synchronized List<String> signatureList() {
        if (signatureList == null) {
            signatureList = new ArrayList<>(10);
            String elem;

            currentIndex = 0;

            while(currentIndex < signature.length()) {
                elem = nextSignature();
                signatureList.add(elem);
            }
            if (signatureList.size() == 0) {
                throw new IllegalArgumentException("Invalid JNI signature '" +
                                                   signature + "'");
            }
        }
        return signatureList;
    }

    private synchronized List<String> typeNameList() {
        if (typeNameList == null) {
            typeNameList = new ArrayList<>(10);
            String elem;

            currentIndex = 0;

            while(currentIndex < signature.length()) {
                elem = nextTypeName();
                typeNameList.add(elem);
            }
            if (typeNameList.size() == 0) {
                throw new IllegalArgumentException("Invalid JNI signature '" +
                                                   signature + "'");
            }
        }
        return typeNameList;
    }

    private String nextSignature() {
        char key = signature.charAt(currentIndex++);

        switch(key) {
            case (JDWP.Tag.ARRAY):
                return  key + nextSignature();

            case (JDWP.Tag.OBJECT):
                int endClass = signature.indexOf(SIGNATURE_ENDCLASS,
                                                 currentIndex);
                String retVal = signature.substring(currentIndex - 1,
                                                    endClass + 1);
                currentIndex = endClass + 1;
                return retVal;

            case (JDWP.Tag.VOID):
            case (JDWP.Tag.BOOLEAN):
            case (JDWP.Tag.BYTE):
            case (JDWP.Tag.CHAR):
            case (JDWP.Tag.SHORT):
            case (JDWP.Tag.INT):
            case (JDWP.Tag.LONG):
            case (JDWP.Tag.FLOAT):
            case (JDWP.Tag.DOUBLE):
                return String.valueOf(key);

            case SIGNATURE_ENDFUNC:
            case SIGNATURE_FUNC:
                return nextSignature();

            default:
                throw new IllegalArgumentException(
                    "Invalid JNI signature character '" + key + "'");

        }
    }

    private String nextTypeName() {
        char key = signature.charAt(currentIndex++);

        switch(key) {
            case (JDWP.Tag.ARRAY):
                return  nextTypeName() + "[]";

            case (JDWP.Tag.BYTE):
                return "byte";

            case (JDWP.Tag.CHAR):
                return "char";

            case (JDWP.Tag.OBJECT):
                int endClass = signature.indexOf(SIGNATURE_ENDCLASS,
                                                 currentIndex);
                String retVal = signature.substring(currentIndex,
                                                    endClass);
                int index = retVal.indexOf(".");
                if (index < 0) {
                    retVal = retVal.replace('/', '.');
                } else {
                    // hidden class
                    retVal = retVal.substring(0, index).replace('/', '.')
                                + "/" + retVal.substring(index + 1);
                }
                currentIndex = endClass + 1;
                return retVal;

            case (JDWP.Tag.FLOAT):
                return "float";

            case (JDWP.Tag.DOUBLE):
                return "double";

            case (JDWP.Tag.INT):
                return "int";

            case (JDWP.Tag.LONG):
                return "long";

            case (JDWP.Tag.SHORT):
                return "short";

            case (JDWP.Tag.VOID):
                return "void";

            case (JDWP.Tag.BOOLEAN):
                return "boolean";

            case SIGNATURE_ENDFUNC:
            case SIGNATURE_FUNC:
                return nextTypeName();

            default:
                throw new IllegalArgumentException(
                    "Invalid JNI signature character '" + key + "'");
        }
    }
}
