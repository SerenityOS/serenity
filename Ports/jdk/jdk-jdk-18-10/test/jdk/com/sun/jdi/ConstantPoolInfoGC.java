/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6822627
 * @summary Test that ReferenceType.constantPool does not produce an NPE
 * @author Egor Ushakov
 *
 * @modules jdk.jdi/com.sun.tools.jdi:+open
 *
 * @run build TestScaffold VMConnection
 * @run compile -g ConstantPoolInfoGC.java
 * @run main/othervm ConstantPoolInfoGC
 */

import com.sun.jdi.ReferenceType;
import com.sun.tools.jdi.ReferenceTypeImpl;

import java.lang.ref.Reference;
import java.lang.reflect.Field;
import java.util.Arrays;

    /********** target program **********/

class ConstantPoolGCTarg {
    public static void main(String[] args){
        System.out.println("Anything");
    }
}

    /********** test program **********/

public class ConstantPoolInfoGC extends TestScaffold {
    ReferenceType targetClass;

    ConstantPoolInfoGC(String args[]) {
        super(args);
    }

    public static void main(String[] args)      throws Exception {
        new ConstantPoolInfoGC(args).startTests();
    }

    /********** test core **********/

    protected void runTests() throws Exception {
        targetClass = startToMain("ConstantPoolGCTarg").location().declaringType();

        if (vm().canGetConstantPool()) {
            byte[] cpbytes = targetClass.constantPool();

            // imitate SoftReference cleared
            Field constantPoolBytesRef = ReferenceTypeImpl.class.getDeclaredField("constantPoolBytesRef");
            constantPoolBytesRef.setAccessible(true);
            Reference softRef = (Reference) constantPoolBytesRef.get(targetClass);
            softRef.clear();

            byte[] cpbytes2 = targetClass.constantPool();
            if (!Arrays.equals(cpbytes, cpbytes2)) {
                failure("Consequent constantPool results vary, first was : " + cpbytes + ", now: " + cpbytes2);
            };

        } else {
            System.out.println("can get constant pool version not supported");
        }


        /*
         * resume until end
         */
        listenUntilVMDisconnect();

        /*
         * deal with results of test
         * if anything has called failure("foo") testFailed will be true
         */
        if (!testFailed) {
            println("ConstantPoolInfoGC: passed");
        } else {
            throw new Exception("ConstantPoolInfoGC: failed");
        }
    }
}
