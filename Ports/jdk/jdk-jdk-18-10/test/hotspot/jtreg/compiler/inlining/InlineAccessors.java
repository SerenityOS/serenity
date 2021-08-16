/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8140650
 * @summary Method::is_accessor should cover getters and setters for all types
 * @library /test/lib
 * @requires vm.flagless
 *
 * @run driver compiler.inlining.InlineAccessors
 */

package compiler.inlining;

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

public class InlineAccessors {
    public static void main(String[] args) throws Exception {
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(
                "-XX:+IgnoreUnrecognizedVMOptions", "-showversion",
                "-server", "-XX:-TieredCompilation", "-Xbatch",
                "-XX:+PrintCompilation", "-XX:+UnlockDiagnosticVMOptions", "-XX:+PrintInlining",
                 Launcher.class.getName());

        OutputAnalyzer analyzer = new OutputAnalyzer(pb.start());

        analyzer.shouldHaveExitValue(0);

        // The test is applicable only to C2 (present in Server VM).
        if (analyzer.getStderr().contains("Server VM")) {
            analyzer.shouldContain("InlineAccessors::setBool (6 bytes)   accessor");
            analyzer.shouldContain("InlineAccessors::setByte (6 bytes)   accessor");
            analyzer.shouldContain("InlineAccessors::setChar (6 bytes)   accessor");
            analyzer.shouldContain("InlineAccessors::setShort (6 bytes)   accessor");
            analyzer.shouldContain("InlineAccessors::setInt (6 bytes)   accessor");
            analyzer.shouldContain("InlineAccessors::setFloat (6 bytes)   accessor");
            analyzer.shouldContain("InlineAccessors::setLong (6 bytes)   accessor");
            analyzer.shouldContain("InlineAccessors::setDouble (6 bytes)   accessor");
            analyzer.shouldContain("InlineAccessors::setObject (6 bytes)   accessor");
            analyzer.shouldContain("InlineAccessors::setArray (6 bytes)   accessor");

            analyzer.shouldContain("InlineAccessors::getBool (5 bytes)   accessor");
            analyzer.shouldContain("InlineAccessors::getByte (5 bytes)   accessor");
            analyzer.shouldContain("InlineAccessors::getChar (5 bytes)   accessor");
            analyzer.shouldContain("InlineAccessors::getShort (5 bytes)   accessor");
            analyzer.shouldContain("InlineAccessors::getInt (5 bytes)   accessor");
            analyzer.shouldContain("InlineAccessors::getFloat (5 bytes)   accessor");
            analyzer.shouldContain("InlineAccessors::getLong (5 bytes)   accessor");
            analyzer.shouldContain("InlineAccessors::getDouble (5 bytes)   accessor");
            analyzer.shouldContain("InlineAccessors::getObject (5 bytes)   accessor");
            analyzer.shouldContain("InlineAccessors::getArray (5 bytes)   accessor");
        }
    }

    boolean bool;
    byte b;
    char c;
    short s;
    int i;
    float f;
    long l;
    double d;
    Object o;
    Object[] a;

    public void setBool(boolean v)   { bool = v; }
    public void setByte(byte v)      { b = v; }
    public void setChar(char v)      { c = v; }
    public void setShort(short v)    { s = v; }
    public void setInt(int v)        { i = v; }
    public void setFloat(float v)    { f = v; }
    public void setLong(long v)      { l = v; }
    public void setDouble(double v)  { d = v; }
    public void setObject(Object v)  { o = v; }
    public void setArray(Object[] v) { a = v; }

    public boolean  getBool()        { return bool; }
    public byte     getByte()        { return b; }
    public char     getChar()        { return c; }
    public short    getShort()       { return s; }
    public int      getInt()         { return i; }
    public float    getFloat()       { return f; }
    public long     getLong()        { return l; }
    public double   getDouble()      { return d; }
    public Object   getObject()      { return o; }
    public Object[] getArray()       { return a; }

    static void doTest() {
        InlineAccessors o = new InlineAccessors();
        o.setBool(false);
        o.setByte((byte)0);
        o.setChar('a');
        o.setShort((short)0);
        o.setInt(0);
        o.setFloat(0F);
        o.setLong(0L);
        o.setDouble(0D);
        o.setObject(new Object());
        o.setArray(new Object[1]);

        o.getBool();
        o.getByte();
        o.getChar();
        o.getShort();
        o.getInt();
        o.getFloat();
        o.getLong();
        o.getDouble();
        o.getObject();
        o.getArray();
    }

    static class Launcher {
        public static void main(String[] args) throws Exception {
            for (int c = 0; c < 20_000; c++) {
              doTest();
            }
        }
    }
}
