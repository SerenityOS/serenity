/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8049632
 * @summary Test ClassFileParser::copy_localvariable_table cases
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @compile DuplicateLVT.jcod DuplicateLVTT.jcod NotFoundLVTT.jcod
 * @compile -g -XDignore.symbol.file TestLVT.java
 * @run main TestLVT
 */

import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;
import java.util.*;

public class TestLVT {
    public static void main(String[] args) throws Exception {
        test();  // Test good LVT in this test

        String jarFile = System.getProperty("test.src") + "/testcase.jar";

        // java -cp $testSrc/testcase.jar DuplicateLVT
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder("DuplicateLVT");
        new OutputAnalyzer(pb.start())
            .shouldContain("Duplicated LocalVariableTable attribute entry for 'by' in class file DuplicateLVT")
            .shouldHaveExitValue(1);

        // java -cp $testclasses/testcase.jar DuplicateLVTT
        pb = ProcessTools.createJavaProcessBuilder("DuplicateLVTT");
        new OutputAnalyzer(pb.start())
            .shouldContain("Duplicated LocalVariableTypeTable attribute entry for 'list' in class file DuplicateLVTT")
            .shouldHaveExitValue(1);

        // java -cp $testclasses/testcase.jar NotFoundLVTT
        pb = ProcessTools.createJavaProcessBuilder("NotFoundLVTT");
        new OutputAnalyzer(pb.start())
            .shouldContain("LVTT entry for 'list' in class file NotFoundLVTT does not match any LVT entry")
            .shouldHaveExitValue(1);
    }

    public static void test() {
        boolean b  = true;
        byte    by = 0x42;
        char    c  = 'X';
        double  d  = 1.1;
        float   f  = (float) 1.2;
        int     i  = 42;
        long    l  = 0xCAFEBABE;
        short   s  = 88;
        ArrayList<String> list = new ArrayList<String>();
        list.add("me");

        System.out.println("b=" + b);
        System.out.println("by=" + by);
        System.out.println("c=" + c);
        System.out.println("d=" + d);
        System.out.println("f=" + f);
        System.out.println("i=" + i);
        System.out.println("l=" + l);
        System.out.println("s=" + s);
        System.out.println("ArrayList<String>=" + list);
    }
}
