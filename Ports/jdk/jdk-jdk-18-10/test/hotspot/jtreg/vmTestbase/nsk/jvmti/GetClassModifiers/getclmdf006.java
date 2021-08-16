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

package nsk.jvmti.GetClassModifiers;

import java.io.PrintStream;
import java.lang.reflect.Modifier;

public class getclmdf006 {

    final static int JCK_STATUS_BASE = 95;

    static {
        try {
            System.loadLibrary("getclmdf006");
        } catch (UnsatisfiedLinkError ule) {
            System.err.println("Could not load getclmdf006 library");
            System.err.println("java.library.path:"
                + System.getProperty("java.library.path"));
            throw ule;
        }
    }

    native static void check(Class cls, int mod);
    native static int getRes();

    public static void main(String args[]) {
        args = nsk.share.jvmti.JVMTITest.commonInit(args);

        // produce JCK-like exit status.
        System.exit(run(args, System.out) + JCK_STATUS_BASE);
    }

    public static int run(String args[], PrintStream out) {
        check(getclmdf006a.class, 0);
        check(getclmdf006b.class, Modifier.FINAL);
        check(getclmdf006c.class, Modifier.ABSTRACT);
        check(getclmdf006d.class, Modifier.INTERFACE | Modifier.ABSTRACT);
        check(getclmdf006e.class, Modifier.PUBLIC | Modifier.INTERFACE |
                                  Modifier.ABSTRACT);
        check(Inner01.class, Modifier.PUBLIC);
        check(Inner02.class, Modifier.PUBLIC | Modifier.FINAL);
        check(Inner03.class, Modifier.PUBLIC | Modifier.ABSTRACT);
        check(Inner04.class, Modifier.PRIVATE);
        check(Inner05.class, Modifier.PRIVATE | Modifier.FINAL);
        check(Inner06.class, Modifier.PRIVATE | Modifier.ABSTRACT);
        check(Inner07.class, Modifier.PROTECTED);
        check(Inner08.class, Modifier.PROTECTED | Modifier.FINAL);
        check(Inner09.class, Modifier.PROTECTED | Modifier.ABSTRACT);
        check(Inner10.class, Modifier.STATIC);
        check(Inner11.class, Modifier.STATIC | Modifier.FINAL);
        check(Inner12.class, Modifier.STATIC | Modifier.ABSTRACT);
        check(Inner13.class, Modifier.PUBLIC | Modifier.STATIC);
        check(Inner14.class, Modifier.PUBLIC | Modifier.STATIC |
                             Modifier.FINAL);
        check(Inner15.class, Modifier.PUBLIC | Modifier.STATIC |
                             Modifier.ABSTRACT);
        check(Inner16.class, Modifier.PRIVATE | Modifier.STATIC);
        check(Inner17.class, Modifier.PRIVATE | Modifier.STATIC |
                             Modifier.FINAL);
        check(Inner18.class, Modifier.PRIVATE | Modifier.STATIC |
                             Modifier.ABSTRACT);
        check(Inner19.class, Modifier.PROTECTED | Modifier.STATIC);
        check(Inner20.class, Modifier.PROTECTED | Modifier.STATIC |
                             Modifier.FINAL);
        check(Inner21.class, Modifier.PROTECTED | Modifier.STATIC |
                             Modifier.ABSTRACT);
        check(Inner22.class, Modifier.STATIC | Modifier.INTERFACE |
                             Modifier.ABSTRACT);
        check(Inner23.class, Modifier.PUBLIC | Modifier.STATIC |
                             Modifier.INTERFACE | Modifier.ABSTRACT);
        check(Inner24.class, Modifier.PRIVATE | Modifier.STATIC |
                             Modifier.INTERFACE | Modifier.ABSTRACT);
        check(Inner25.class, Modifier.PROTECTED | Modifier.STATIC |
                             Modifier.INTERFACE | Modifier.ABSTRACT);
        return getRes();
    }

    public class Inner01 {}
    public final class Inner02 {}
    public abstract class Inner03 {}
    private class Inner04 {}
    private final class Inner05 {}
    private abstract class Inner06 {}
    protected class Inner07 {}
    protected final class Inner08 {}
    protected abstract class Inner09 {}
    static class Inner10 {}
    static final class Inner11 {}
    static abstract class Inner12 {}
    public static class Inner13 {}
    public static final class Inner14 {}
    public static abstract class Inner15 {}
    private static class Inner16 {}
    private static final class Inner17 {}
    private static abstract class Inner18 {}
    protected static class Inner19 {}
    protected static final class Inner20 {}
    protected static abstract class Inner21 {}

    interface Inner22 {}
    public interface Inner23 {}
    private interface Inner24 {}
    protected interface Inner25 {}
}

class getclmdf006a {}
final class getclmdf006b {}
abstract class getclmdf006c {}
interface getclmdf006d {}
