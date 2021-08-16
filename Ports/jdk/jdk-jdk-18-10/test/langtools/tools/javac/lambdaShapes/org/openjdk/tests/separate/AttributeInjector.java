/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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

package org.openjdk.tests.separate;

import java.io.*;

public class AttributeInjector implements ClassFilePreprocessor {

    private String attributeName;
    private byte[] attributeData;

    public AttributeInjector(String attributeName, byte[] attributeData) {
        this.attributeName = attributeName;
        this.attributeData = attributeData;
    }

    public byte[] preprocess(String name, byte[] cf) {
        ClassFile classfile = new ClassFile(cf);

        short cpIndex = (short)classfile.constant_pool.size();

        ClassFile.CpUtf8 entry = new ClassFile.CpUtf8();
        entry.bytes = new byte[attributeName.length()];
        for (int i = 0; i < attributeName.length(); ++i) {
            entry.bytes[i] = (byte)attributeName.charAt(i);
        }

        classfile.constant_pool.add(entry);

        ClassFile.Attribute attr = new ClassFile.Attribute();
        attr.attribute_name_index = cpIndex;
        attr.info = attributeData;

        classfile.attributes.add(attr);
        return classfile.toByteArray();
    }

/*
    public static void main(String argv[]) throws Exception {
        File input = new File(argv[0]);
        byte[] buffer = new byte[(int)input.length()];
        new FileInputStream(input).read(buffer);

        ClassFilePreprocessor cfp =
            new AttributeInjector("RequiresBridges", new byte[0]);
        byte[] cf = cfp.preprocess(argv[0], buffer);
        new FileOutputStream(argv[0] + ".mod").write(cf);
    }
*/
}
