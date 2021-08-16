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
 * @bug 8015499
 * @summary javac, Gen is generating extra checkcast instructions in some corner cases
 * @modules jdk.jdeps/com.sun.tools.classfile
 *          jdk.compiler/com.sun.tools.javac.util
 * @run main DoubleCastTest
 */

import java.util.List;
import java.util.ArrayList;
import com.sun.tools.classfile.*;
import com.sun.tools.javac.util.Assert;

public class DoubleCastTest {
    class C {
        Object x;
        Object m() { return null; }
        void m1(byte[] b) {}
        void m2() {
            Object o;
            Object[] os = null;
            m1((byte[])(o = null));
            m1((byte[])o);
            m1((byte[])(o == null ? o : o));
            m1((byte[])m());
            m1((byte[])os[0]);
            m1((byte[])this.x);
            m1((byte[])((byte []) (o = null)));
        }
    }

    public static void main(String... cmdline) throws Exception {

        ClassFile cls = ClassFile.read(DoubleCastTest.class.getResourceAsStream("DoubleCastTest$C.class"));
        for (Method m: cls.methods)
            check(m);
    }

    static void check(Method m) throws Exception {
        boolean last_is_cast = false;
        int last_ref = 0;
        Code_attribute ea = (Code_attribute)m.attributes.get(Attribute.Code);
        for (Instruction i : ea.getInstructions()) {
            if (i.getOpcode() == Opcode.CHECKCAST) {
                Assert.check
                    (!(last_is_cast && last_ref == i.getUnsignedShort(1)),
                     "Double cast found - Test failed");
                last_is_cast = true;
                last_ref = i.getUnsignedShort(1);
            } else {
                last_is_cast = false;
            }
        }
    }
}
