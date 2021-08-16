/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Test SourceCodeAnalysis
 * @build KullaTesting TestingInputStream
 * @run testng AnalysisTest
 */

import org.testng.annotations.Test;

@Test
public class AnalysisTest extends KullaTesting {

    public void testSource() {
        assertAnalyze("int x=3//test", "int x=3;//test", "", true);
        assertAnalyze("int x=3 ;//test", "int x=3 ;//test", "", true);
        assertAnalyze("int x=3 //;", "int x=3; //;", "", true);
        assertAnalyze("void m5() {} /// hgjghj", "void m5() {} /// hgjghj", "", true);
        assertAnalyze("int ff; int v // hi", "int ff;", " int v // hi", true);
    }

    public void testSourceSlashStar() {
        assertAnalyze("/*zoo*/int x=3 /*test*/", "/*zoo*/int x=3; /*test*/", "", true);
        assertAnalyze("/*zoo*/int x=3 ;/*test*/", "/*zoo*/int x=3 ;/*test*/", "", true);
        assertAnalyze("int x=3 /*;*/", "int x=3; /*;*/", "", true);
        assertAnalyze("void m5() {} /*hgjghj*/", "void m5() {} /*hgjghj*/", "", true);
        assertAnalyze("int ff; int v /*hgjghj*/", "int ff;", " int v /*hgjghj*/", true);
    }

    public void testIncomplete() {
        assertAnalyze("void m() { //erer", null, "void m() { //erer\n", false);
        assertAnalyze("int m=//", null, "int m=//\n", false);
    }

    public void testExpression() {
        assertAnalyze("45//test", "45//test", "", true);
        assertAnalyze("45;//test", "45;//test", "", true);
        assertAnalyze("45//;", "45//;", "", true);
        assertAnalyze("/*zoo*/45/*test*/", "/*zoo*/45/*test*/", "", true);
        assertAnalyze("/*zoo*/45;/*test*/", "/*zoo*/45;/*test*/", "", true);
        assertAnalyze("45/*;*/", "45/*;*/", "", true);
    }
}
