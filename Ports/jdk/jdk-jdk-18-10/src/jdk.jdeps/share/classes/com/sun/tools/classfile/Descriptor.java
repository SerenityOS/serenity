/*
 * Copyright (c) 2007, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.classfile;

import java.io.IOException;

/**
 * See JVMS, section 4.4.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class Descriptor {
    public static class InvalidDescriptor extends DescriptorException {
        private static final long serialVersionUID = 1L;
        InvalidDescriptor(String desc) {
            this.desc = desc;
            this.index = -1;
        }

        InvalidDescriptor(String desc, int index) {
            this.desc = desc;
            this.index = index;
        }

        @Override
        public String getMessage() {
            // i18n
            if (index == -1)
                return "invalid descriptor \"" + desc + "\"";
            else
                return "descriptor is invalid at offset " + index + " in \"" + desc + "\"";
        }

        public final String desc;
        public final int index;

    }

    public Descriptor(ClassReader cr) throws IOException {
        this(cr.readUnsignedShort());
    }

    public Descriptor(int index) {
        this.index = index;

    }

    public String getValue(ConstantPool constant_pool) throws ConstantPoolException {
        return constant_pool.getUTF8Value(index);
    }

    public int getParameterCount(ConstantPool constant_pool)
            throws ConstantPoolException, InvalidDescriptor {
        String desc = getValue(constant_pool);
        int end = desc.indexOf(")");
        if (end == -1)
            throw new InvalidDescriptor(desc);
        parse(desc, 0, end + 1);
        return count;

    }

    public String getParameterTypes(ConstantPool constant_pool)
            throws ConstantPoolException, InvalidDescriptor {
        String desc = getValue(constant_pool);
        int end = desc.indexOf(")");
        if (end == -1)
            throw new InvalidDescriptor(desc);
        return parse(desc, 0, end + 1);
    }

    public String getReturnType(ConstantPool constant_pool)
            throws ConstantPoolException, InvalidDescriptor {
        String desc = getValue(constant_pool);
        int end = desc.indexOf(")");
        if (end == -1)
            throw new InvalidDescriptor(desc);
        return parse(desc, end + 1, desc.length());
    }

    public String getFieldType(ConstantPool constant_pool)
            throws ConstantPoolException, InvalidDescriptor {
        String desc = getValue(constant_pool);
        return parse(desc, 0, desc.length());
    }

    private String parse(String desc, int start, int end)
            throws InvalidDescriptor {
        int p = start;
        StringBuilder sb = new StringBuilder();
        int dims = 0;
        count = 0;

        while (p < end) {
            String type;
            char ch;
            switch (ch = desc.charAt(p++)) {
                case '(':
                    sb.append('(');
                    continue;

                case ')':
                    sb.append(')');
                    continue;

                case '[':
                    dims++;
                    continue;

                case 'B':
                    type = "byte";
                    break;

                case 'C':
                    type = "char";
                    break;

                case 'D':
                    type = "double";
                    break;

                case 'F':
                    type = "float";
                    break;

                case 'I':
                    type = "int";
                    break;

                case 'J':
                    type = "long";
                    break;

                case 'L':
                    int sep = desc.indexOf(';', p);
                    if (sep == -1)
                        throw new InvalidDescriptor(desc, p - 1);
                    type = desc.substring(p, sep).replace('/', '.');
                    p = sep + 1;
                    break;

                case 'S':
                    type = "short";
                    break;

                case 'Z':
                    type = "boolean";
                    break;

                case 'V':
                    type = "void";
                    break;

                default:
                    throw new InvalidDescriptor(desc, p - 1);
            }

            if (sb.length() > 1 && sb.charAt(0) == '(')
                sb.append(", ");
            sb.append(type);
            for ( ; dims > 0; dims-- )
                sb.append("[]");

            count++;
        }

        return sb.toString();
    }

    public final int index;
    private int count;
}
