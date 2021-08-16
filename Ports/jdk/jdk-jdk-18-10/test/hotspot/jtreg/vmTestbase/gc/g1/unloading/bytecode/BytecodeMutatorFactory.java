/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
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
package gc.g1.unloading.bytecode;

import java.io.*;
import java.nio.charset.*;
import java.util.*;


/**
 * This BytecodeFactory produces bytecode that is golden bytecode with className substituted.
 */
public class BytecodeMutatorFactory implements BytecodeFactory {

    private static final String FILLER_CHARACTER = "_";

    /**
     * Utility method in this class
     */
    public static String padName(String s, int length) {
        int difference = length - s.length();
        StringBuilder sb = new StringBuilder(s);
        for (int i = 0; i < difference; i++) {
            sb.append(FILLER_CHARACTER);
        }
        return sb.toString();
    }

    public String padName(String s) {
        return padName(s, getNameLength());
    }

    private final Charset CHARACTER_SET = StandardCharsets.UTF_8;

    private List<Integer> offsets = new LinkedList<>();

    private byte[] templateBytecode;

    private String templateClassName;

    private byte[] templateClassNameAsBytes;

    public BytecodeMutatorFactory() {
        this(DefaultTemplateClass.class.getName());
    }

    public BytecodeMutatorFactory(String templateClassName) {
        this.templateClassName = templateClassName;

        // Read bytecode to array
        InputStream is = ClassLoader.getSystemResourceAsStream(templateClassName.replace('.', '/').concat(".class"));
        try {
            templateBytecode = new byte[is.available()];
            is.read(templateBytecode);
            is.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Save offsets
        templateClassNameAsBytes = templateClassName.replace('.', '/').getBytes(CHARACTER_SET);
        for (int i = 0; i < templateBytecode.length; i++) {
            boolean match = true;
            for (int j = 0; j < templateClassNameAsBytes.length; j++) {
                if (i + j >= templateBytecode.length || templateClassNameAsBytes[j] != templateBytecode[i + j]) {
                    match = false;
                    break;
                }
            }
            if (match) {
                offsets.add(i);
            }
        }
    }

    public byte[] getBytecode(String className) {

        // Check size of name constraint
        byte[] newClassNameAsBytes = className.replace('.', '/').getBytes(CHARACTER_SET);
        if (newClassNameAsBytes.length != templateClassNameAsBytes.length) {
            throw new RuntimeException("Can't produce bytecode with \"" + className + "\" substituted as class name. " +
                    "Length of this name differs from length of \"" + templateClassName + "\" which equals to " + templateClassName.length() +
                    ". Length of \"" + className + "\" is " + className.length() + ".");
        }

        // Prepare bytecode
        byte[] result = Arrays.copyOf(templateBytecode, templateBytecode.length);
        for (int offset : offsets) {
            System.arraycopy(newClassNameAsBytes, 0, result, offset, newClassNameAsBytes.length);
        }
        return result;
    }

    public int getNameLength() {
        return templateClassName.length();
    }

    @Override
    public Bytecode createBytecode(String className) {
        String finalClassName = padName(className);
        byte[] bytecode = getBytecode(finalClassName);
        return new Bytecode(finalClassName, bytecode);
    }

}
