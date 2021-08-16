/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8006582
 * @summary javac should generate method parameters correctly.
 * @modules jdk.jdeps/com.sun.tools.classfile
 * @build MethodParametersTester ClassFileVisitor ReflectionVisitor
 * @compile -parameters UncommonParamNames.java
 * @run main MethodParametersTester UncommonParamNames UncommonParamNames.out
 */

/** Test uncommon parameter names */
class UncommonParamNames {
    public UncommonParamNames(int _x) { }
    public UncommonParamNames(short $1) { }
    public UncommonParamNames(long \u0061) { }
    public UncommonParamNames(char zero\u0000zero\u0000) { }
    public UncommonParamNames(String zero\u0000zero\u0000seven\u0007) { }
    public UncommonParamNames(Object zero\u0000zero\u0000eight\u0008) { }
    public UncommonParamNames(Object aLoooooooooooooooooooooooooooooooooooooooooooooooooooooooooooongName,
                              Object baLoooooooooooooooooooooooooooooooooooooooooooooooooooooooooooongName,
                              Object cbaLoooooooooooooooooooooooooooooooooooooooooooooooooooooooooooongName) { }
    public UncommonParamNames(int a, int ba, int cba, int dcba, int edcba, int fedcba, int gfedcba,
                              int hgfedcba, int ihgfedcba, int jihgfedcba, int kjihgfedcba, int lkjihgfedcba,
                              int mlkjihgfedcba, int nmlkjihgfedcba, int onmlkjihgfedcba, int ponmlkjihgfedcba,
                              int qponmlkjihgfedcba, int rqponmlkjihgfedcba, int srqponmlkjihgfedcba,
                              int tsrqponmlkjihgfedcba, int utsrqponmlkjihgfedcba, int vutsrqponmlkjihgfedcba,
                              int wvutsrqponmlkjihgfedcba, int xwvutsrqponmlkjihgfedcba,
                              int yxwvutsrqponmlkjihgfedcba, int zyxwvutsrqponmlkjihgfedcba) { }

    public void foo(int _x) { }
    public void foo(short $1) { }
    public void foo(long \u0061) { }
    public void foo(char zero\u0000zero\u0000) { }
    public void foo(String zero\u0000zero\u0000seven\u0007) { }
    public void foo(Object zero\u0000zero\u0000eight\u0008) { }
    public void foo(Object aLoooooooooooooooooooooooooooooooooooooooooooooooooooooooooooongName,
                    Object baLoooooooooooooooooooooooooooooooooooooooooooooooooooooooooooongName,
                    Object cbaLoooooooooooooooooooooooooooooooooooooooooooooooooooooooooooongName) { }
    public void foo(int a, int ba, int cba, int dcba, int edcba, int fedcba, int gfedcba,
                    int hgfedcba, int ihgfedcba, int jihgfedcba, int kjihgfedcba, int lkjihgfedcba,
                    int mlkjihgfedcba, int nmlkjihgfedcba, int onmlkjihgfedcba, int ponmlkjihgfedcba,
                    int qponmlkjihgfedcba, int rqponmlkjihgfedcba, int srqponmlkjihgfedcba,
                    int tsrqponmlkjihgfedcba, int utsrqponmlkjihgfedcba, int vutsrqponmlkjihgfedcba,
                    int wvutsrqponmlkjihgfedcba, int xwvutsrqponmlkjihgfedcba,
                    int yxwvutsrqponmlkjihgfedcba, int zyxwvutsrqponmlkjihgfedcba) { }
}
