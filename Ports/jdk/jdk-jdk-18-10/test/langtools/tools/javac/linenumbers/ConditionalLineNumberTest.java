/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 8034091
 * @summary Add LineNumberTable attributes for conditional operator (?:) split across several lines.
 * @modules jdk.jdeps/com.sun.tools.classfile
 */

import com.sun.tools.classfile.ClassFile;
import com.sun.tools.classfile.ConstantPoolException;
import com.sun.tools.classfile.Method;
import com.sun.tools.classfile.Attribute;
import com.sun.tools.classfile.Code_attribute;
import com.sun.tools.classfile.LineNumberTable_attribute;
import com.sun.tools.classfile.LineNumberTable_attribute.Entry;

import java.io.File;
import java.io.IOException;

public class ConditionalLineNumberTest {
    public static void main(String[] args) throws Exception {
        // check that we have 5 consecutive entries for method()
        Entry[] lines = findEntries();
        if (lines == null || lines.length != 5)
            throw new Exception("conditional line number table incorrect");

        int current = lines[0].line_number;
        for (Entry e : lines) {
            if (e.line_number != current)
                throw new Exception("conditional line number table incorrect");
            current++;
        }
   }

    static Entry[] findEntries() throws IOException, ConstantPoolException {
        ClassFile self = ClassFile.read(ConditionalLineNumberTest.class.getResourceAsStream("ConditionalLineNumberTest.class"));
        for (Method m : self.methods) {
            if ("method".equals(m.getName(self.constant_pool))) {
                Code_attribute code_attribute = (Code_attribute)m.attributes.get(Attribute.Code);
                for (Attribute at : code_attribute.attributes) {
                    if (Attribute.LineNumberTable.equals(at.getName(self.constant_pool))) {
                        return ((LineNumberTable_attribute)at).line_number_table;
                    }
                }
            }
        }
        return null;
    }

    // This method should get one LineNumberTable entry per line
    // in the method body.
    public static String method(int field) {
        String s = field % 2 == 0 ?
            (field == 0 ? "false"
             : "true" + field) : //Breakpoint
            "false" + field; //Breakpoint
        return s;
    }
}
