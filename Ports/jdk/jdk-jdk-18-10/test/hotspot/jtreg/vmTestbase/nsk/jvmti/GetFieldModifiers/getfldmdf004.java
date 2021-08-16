/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jvmti.GetFieldModifiers;

import java.io.PrintStream;
import java.lang.reflect.Modifier;

public class getfldmdf004 {

    final static int JCK_STATUS_BASE = 95;

    static {
        try {
            System.loadLibrary("getfldmdf004");
        } catch (UnsatisfiedLinkError ule) {
            System.err.println("Could not load getfldmdf004 library");
            System.err.println("java.library.path:"
                + System.getProperty("java.library.path"));
            throw ule;
        }
    }

    native static void check(int i, int mod);
    native static int getRes();

    public static void main(String args[]) {
        args = nsk.share.jvmti.JVMTITest.commonInit(args);

        // produce JCK-like exit status.
        System.exit(run(args, System.out) + JCK_STATUS_BASE);
    }

    int field0 = 0;
    final int field1 = 1;
    volatile int field2 = 2;
    transient int field3 = 3;
    transient final int field4 = 4;
    transient volatile int field5 = 5;

    private int field6 = 6;
    private final int field7 = 7;
    private volatile int field8 = 8;
    private transient int field9 = 9;
    private transient final int field10 = 10;
    private transient volatile int field11 = 11;

    public int field12 = 12;
    public final int field13 = 13;
    public volatile int field14 = 14;
    public transient int field15 = 15;
    public transient final int field16 = 16;
    public transient volatile int field17 = 17;

    protected int field18 = 18;
    protected final int field19 = 19;
    protected volatile int field20 = 20;
    protected transient int field21 = 21;
    protected transient final int field22 = 22;
    protected transient volatile int field23 = 23;

    static int field24 = 24;
    static final int field25 = 25;
    static volatile int field26 = 26;
    static transient int field27 = 27;
    static transient final int field28 = 28;
    static transient volatile int field29 = 29;

    static private int field30 = 30;
    static private final int field31 = 31;
    static private volatile int field32 = 32;
    static private transient int field33 = 33;
    static private transient final int field34 = 34;
    static private transient volatile int field35 = 35;

    static public int field36 = 36;
    static public final int field37 = 37;
    static public volatile int field38 = 38;
    static public transient int field39 = 39;
    static public transient final int field40 = 40;
    static public transient volatile int field41 = 41;

    static protected int field42 = 42;
    static protected final int field43 = 43;
    static protected volatile int field44 = 44;
    static protected transient int field45 = 45;
    static protected transient final int field46 = 46;
    static protected transient volatile int field47 = 47;

    public static int run(String args[], PrintStream out) {
        check(0, 0);
        check(1, Modifier.FINAL);
        check(2, Modifier.VOLATILE);
        check(3, Modifier.TRANSIENT);
        check(4, Modifier.TRANSIENT | Modifier.FINAL);
        check(5, Modifier.TRANSIENT | Modifier.VOLATILE);

        check(6, Modifier.PRIVATE);
        check(7, Modifier.PRIVATE | Modifier.FINAL);
        check(8, Modifier.PRIVATE | Modifier.VOLATILE);
        check(9, Modifier.PRIVATE | Modifier.TRANSIENT);
        check(10, Modifier.PRIVATE | Modifier.TRANSIENT | Modifier.FINAL);
        check(11, Modifier.PRIVATE | Modifier.TRANSIENT | Modifier.VOLATILE);

        check(12, Modifier.PUBLIC);
        check(13, Modifier.PUBLIC | Modifier.FINAL);
        check(14, Modifier.PUBLIC | Modifier.VOLATILE);
        check(15, Modifier.PUBLIC | Modifier.TRANSIENT);
        check(16, Modifier.PUBLIC | Modifier.TRANSIENT | Modifier.FINAL);
        check(17, Modifier.PUBLIC | Modifier.TRANSIENT | Modifier.VOLATILE);

        check(18, Modifier.PROTECTED);
        check(19, Modifier.PROTECTED | Modifier.FINAL);
        check(20, Modifier.PROTECTED | Modifier.VOLATILE);
        check(21, Modifier.PROTECTED | Modifier.TRANSIENT);
        check(22, Modifier.PROTECTED | Modifier.TRANSIENT | Modifier.FINAL);
        check(23, Modifier.PROTECTED | Modifier.TRANSIENT | Modifier.VOLATILE);

        check(24, Modifier.STATIC);
        check(25, Modifier.STATIC | Modifier.FINAL);
        check(26, Modifier.STATIC | Modifier.VOLATILE);
        check(27, Modifier.STATIC | Modifier.TRANSIENT);
        check(28, Modifier.STATIC | Modifier.TRANSIENT | Modifier.FINAL);
        check(29, Modifier.STATIC | Modifier.TRANSIENT | Modifier.VOLATILE);

        check(30, Modifier.STATIC | Modifier.PRIVATE);
        check(31, Modifier.STATIC | Modifier.PRIVATE | Modifier.FINAL);
        check(32, Modifier.STATIC | Modifier.PRIVATE | Modifier.VOLATILE);
        check(33, Modifier.STATIC | Modifier.PRIVATE | Modifier.TRANSIENT);
        check(34, Modifier.STATIC | Modifier.PRIVATE | Modifier.TRANSIENT |
                  Modifier.FINAL);
        check(35, Modifier.STATIC | Modifier.PRIVATE | Modifier.TRANSIENT |
                  Modifier.VOLATILE);

        check(36, Modifier.STATIC | Modifier.PUBLIC);
        check(37, Modifier.STATIC | Modifier.PUBLIC | Modifier.FINAL);
        check(38, Modifier.STATIC | Modifier.PUBLIC | Modifier.VOLATILE);
        check(39, Modifier.STATIC | Modifier.PUBLIC | Modifier.TRANSIENT);
        check(40, Modifier.STATIC | Modifier.PUBLIC | Modifier.TRANSIENT |
                  Modifier.FINAL);
        check(41, Modifier.STATIC | Modifier.PUBLIC | Modifier.TRANSIENT |
                  Modifier.VOLATILE);

        check(42, Modifier.STATIC | Modifier.PROTECTED);
        check(43, Modifier.STATIC | Modifier.PROTECTED | Modifier.FINAL);
        check(44, Modifier.STATIC | Modifier.PROTECTED | Modifier.VOLATILE);
        check(45, Modifier.STATIC | Modifier.PROTECTED | Modifier.TRANSIENT);
        check(46, Modifier.STATIC | Modifier.PROTECTED | Modifier.TRANSIENT |
                  Modifier.FINAL);
        check(47, Modifier.STATIC | Modifier.PROTECTED | Modifier.TRANSIENT |
                  Modifier.VOLATILE);

        return getRes();
    }
}
