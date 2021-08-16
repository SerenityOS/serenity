/*
 * Copyright (c) 2011, 2015, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 8003280
 * @summary Add lambda tests
 *   This test is for identifying SAM types 2 and 3, see Helper.java for SAM types
 * @modules java.sql
 * @compile LambdaTest2_SAM1.java Helper.java
 * @run main LambdaTest2_SAM1
 */

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.io.*;

public class LambdaTest2_SAM1 {
    private static List<String> strs = new ArrayList<String>();
    private static List<File> files = new ArrayList<File>();

    public static void main(String[] args) {
        strs.add("copy");
        strs.add("paste");
        strs.add("delete");
        strs.add("rename");

        files.add(new File("a.txt"));
        files.add(new File("c.txt"));
        files.add(new File("b.txt"));

        //type #2: Comparator<T>
        Collections.sort(files, (File f1, File f2) -> f1.getName().compareTo(f2.getName()));
        for(File f : files)
            System.out.println(f.getName());
        System.out.println();
        Collections.sort(files, (File f1, File f2) -> (int)(f1.length() - f2.length()));
        for(File f : files)
            System.out.println(f.getName() + " " + f.length());
        System.out.println();

        LambdaTest2_SAM1 test = new LambdaTest2_SAM1();

        //type #2:
        test.methodMars((File f) -> {
            System.out.println("implementing Mars<File>.getAge(File f)...");
            return (int)f.length();
        });
        test.methodJupiter((int n) -> n+1);

        //type #3:
        test.methodXY((List<String> strList) -> strList.size() );
        test.methodXYZ((List<String> strList) -> 20 );
    }

    //type #2:
    void methodMars(Mars<File> m) {
        System.out.println("methodMars(): SAM type interface Mars object instantiated: " + m);
        System.out.println(m.getAge(new File("a.txt")));
    }

    //type #2:
    void methodJupiter(Jupiter j) {
        System.out.println("methodJupiter(): SAM type interface Jupiter object instantiated: " + j);
        System.out.println(j.increment(33));
    }

    //type #3:
    void methodXY(XY xy) {
        System.out.println("methodXY(): SAM type interface XY object instantiated: " + xy);
        System.out.println(xy.getTotal(strs));
    }

    //type #3:
    void methodXYZ(XYZ xyz) {
        System.out.println("methodXYZ(): SAM type interface XYZ object instantiated: " + xyz);
        System.out.println(xyz.getTotal(strs));
    }
}
