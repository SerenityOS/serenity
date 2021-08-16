/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.reflect.Method;
import java.util.ArrayList;

public class MethodIdentifierParser {

    private String logString;
    private String className;
    private String methodName;
    private String methodDescriptor;

    /**
     * This is a utility class for parsing the log entries for a method. It supplies
     * a few select methods for reflecting the class and method from that information.
     *
     * Example log entries:
     * "java.util.TreeMap.successor(Ljava/util/TreeMap$Entry;)Ljava/util/TreeMap$Entry;"
     */

    public MethodIdentifierParser(String logString) {
        this.logString = logString;

        int i      = logString.lastIndexOf("."); // find start of method name
        className  = logString.substring(0, i);  // classname is everything before
        int i2     = logString.indexOf("(");     // Signature starts with an '('
        methodName = logString.substring(i+1, i2);
        methodDescriptor  = logString.substring(i2, logString.length());

        // Add sanity check for extracted fields
    }

    public Method getMethod() throws NoSuchMethodException, SecurityException, ClassNotFoundException {
        try {
            return Class.forName(className).getDeclaredMethod(methodName, getParamenterDescriptorArray());
        } catch (UnexpectedTokenException e) {
            throw new RuntimeException("Parse failed");
        }
    }

    public Class<?>[] getParamenterDescriptorArray() throws ClassNotFoundException, UnexpectedTokenException {
        ParameterDecriptorIterator s = new ParameterDecriptorIterator(methodDescriptor);
        Class<?> paramType;
        ArrayList<Class<?>> list = new ArrayList<Class<?>>();
        while ((paramType = s.nextParamType()) != null) {
            list.add(paramType);
        }
        if (list.size() > 0) {
            return list.toArray(new Class<?>[list.size()]);
        } else {
            return null;
        }
    }

    class ParameterDecriptorIterator {

        // This class uses charAt() indexing for startMark and i
        // That is when i points to the last char it can be retrieved with
        // charAt(i). Including the last char for a subString requires
        // substring(startMark, i+1);

        private String methodDescriptor;
        private int startMark;

        public ParameterDecriptorIterator(String signature) {
            this.methodDescriptor = signature;
            this.startMark = 0;
            if (signature.charAt(0) == '(') {
                this.startMark = 1;
            }
        }

        public Class<?> nextParamType() throws UnexpectedTokenException {
            int i = startMark;
            while (methodDescriptor.length() > i) {
                switch (methodDescriptor.charAt(i)) {
                case 'C':
                case 'B':
                case 'I':
                case 'J':
                case 'Z':
                case 'F':
                case 'D':
                case 'S':
                    // Primitive class case, but we may have gotten here with [ as first token
                    break;
                case 'L':
                    // Internal class name suffixed by ';'
                    while (methodDescriptor.charAt(i) != ';') {
                        i++;
                    }
                    break;
                case '[':
                    i++;         // arrays -> do another pass
                    continue;
                case ')':
                    return null; // end found
                case 'V':
                case ';':
                default:
                    throw new UnexpectedTokenException(methodDescriptor, i);
                }
                break;
            }
            if (i == startMark) {
                // Single char -> primitive class case
                startMark++; // Update for next iteration
                switch (methodDescriptor.charAt(i)) {
                case 'C':
                    return char.class;
                case 'B':
                    return byte.class;
                case 'I':
                    return int.class;
                case 'J':
                    return long.class;
                case 'F':
                    return float.class;
                case 'D':
                    return double.class;
                case 'S':
                    return short.class;
                case 'Z':
                    return boolean.class;
                default:
                    throw new UnexpectedTokenException(methodDescriptor, i);
                }
            } else {
                // Multi char case
                String nextParam;
                if (methodDescriptor.charAt(startMark) == 'L') {
                    // When reflecting a class the leading 'L' and trailing';' must be removed.
                    // (When reflecting an array of classes, they must remain...)
                    nextParam = methodDescriptor.substring(startMark+1, i);
                } else {
                    // Any kind of array - simple case, use whole descriptor when reflecting.
                    nextParam = methodDescriptor.substring(startMark, i+1);
                }
                startMark = ++i; // Update for next iteration
                try {
                    // The parameter descriptor uses JVM internal class identifier with '/' as
                    // package separator, but Class.forName expects '.'.
                    nextParam = nextParam.replace('/', '.');
                    return Class.forName(nextParam);
                } catch (ClassNotFoundException e) {
                    System.out.println("Class not Found: " + nextParam);
                    return null;
                }
            }
        }
    }

    class UnexpectedTokenException extends Exception {
        String descriptor;
        int i;
        public UnexpectedTokenException(String descriptor, int i) {
            this.descriptor = descriptor;
            this.i = i;
        }

        @Override
        public String toString() {
            return "Unexpected token at: " + i + " in signature: " + descriptor;
        }

        private static final long serialVersionUID = 1L;
    }

    public void debugPrint() {
        System.out.println("mlf in:               " + logString);
        System.out.println("mlf class:            " + className);
        System.out.println("mlf method:           " + methodName);
        System.out.println("mlf methodDescriptor: " + methodDescriptor);
    }
}
