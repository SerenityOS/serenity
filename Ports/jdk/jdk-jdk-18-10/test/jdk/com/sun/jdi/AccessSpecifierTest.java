/*
 * Copyright (c) 2001, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4359628
 * @summary Test fix for JDI: methods Accessible.is...() lie about array types
 * @author Tim Bell
 *
 * @run build TestScaffold VMConnection TargetListener TargetAdapter
 * @run compile -g AccessSpecifierTest.java
 * @run driver AccessSpecifierTest
 */
import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;

import java.util.*;

    /********** target program **********/


/** Sample package-private interface. */
interface AccessSpecifierPackagePrivateInterface {}

/** Sample package-private class. */
class AccessSpecifierPackagePrivateClass {}

/** Sample package-private class. */
class AccessSpecifierPackagePrivateClassTwo implements
    AccessSpecifierPackagePrivateInterface {}

class AccessSpecifierTarg {
    private boolean z0;
    boolean z1[]={z0}, z2[][]={z1};

    public byte b0;
    byte b1[]={b0}, b2[][]={b1};

    protected short s0;
    short s1[]={s0}, s2[][]={s1};

    int i0;
    int i1[]={i0}, i2[][]={i1};

    private long l0;
    long l1[]={l0}, l2[][]={l1};

    public char c0;
    char c1[]={c0}, c2[][]={c1};

    protected float f0;
    float f1[]={f0}, f2[][]={f1};

    double d0;
    double d1[]={d0}, d2[][]={d1};

    Boolean   Z0 = Boolean.TRUE;
    Boolean   Z1[]={Z0}, Z2[][]={Z1};
    Byte      B0 = new Byte ((byte)0x1f);
    Byte      B1[]={B0}, B2[][]={B1};
    Character C0 = new Character ('a');
    Character C1[]={C0}, C2[][]={C1};
    Double    D0 = new Double (1.0d);
    Double    D1[]={D0}, D2[][]={D1};
    Float     F0 = new Float (2.0f);
    Float     F1[]={F0}, F2[][]={F1};
    Integer   I0 = new Integer (8675309);
    Integer   I1[]={I0}, I2[][]={I1};
    Long      L0 = new Long (973230999L);
    Long      L1[]={L0}, L2[][]={L1};
    String    S0 = "A String";
    String    S1[]={S0}, S2[][]={S1};
    Object    O0 = new Object();
    Object    O1[]={O0}, O2[][]={O1};

    private   static class  U {}
    protected static class  V {}
    public    static class  W {}
    static class  P {} // package private

    U u0=new U(), u1[]={u0}, u2[][]={u1};
    V v0=new V(), v1[]={v0}, v2[][]={v1};
    W w0=new W(), w1[]={w0}, w2[][]={w1};
    P p0=new P(), p1[]={p0}, p2[][]={p1};

    private static interface StaticInterface {}
    private static class ClassUsingStaticInterface
        implements StaticInterface {}

    StaticInterface staticInterface_0 = new ClassUsingStaticInterface();
    StaticInterface staticInterface_1[]={staticInterface_0};
    StaticInterface staticInterface_2[][]={staticInterface_1};

    AccessSpecifierTarg a0, a1[]={a0}, a2[][]={a1};

    AccessSpecifierPackagePrivateClass ppc0=new AccessSpecifierPackagePrivateClass();
    AccessSpecifierPackagePrivateClass ppc1[]={ppc0};
    AccessSpecifierPackagePrivateClass ppc2[][]={ppc1};

    AccessSpecifierPackagePrivateInterface ppi0 =
        new AccessSpecifierPackagePrivateClassTwo ();
    AccessSpecifierPackagePrivateInterface ppi1[]={ppi0};
    AccessSpecifierPackagePrivateInterface ppi2[][]={ppi1};

    public AccessSpecifierTarg() {
        super();
    }

    public void ready(){
        System.out.println("Ready!");
    }

    public static void main(String[] args){
        System.out.println("Howdy!");
        AccessSpecifierTarg my = new AccessSpecifierTarg();
        my.ready();
        System.out.println("Goodbye from AccessSpecifierTarg!");
    }
}

    /********** test program **********/

public class AccessSpecifierTest extends TestScaffold {

    private final static String debugeeName = "AccessSpecifierTarg";

    /** Known Accessible Information about the Debugee. */
    private static final int NAME = 0;
    private static final int ACCESS = 1;
    private final static String primitives[][] = {
        {"z", "private", "public", "public"},
        {"b", "public", "public", "public"},
        {"s", "protected", "public", "public"},
        {"i", "package private", "public", "public"},
        {"l", "private", "public", "public"},
        {"c", "public", "public", "public"},
        {"f", "protected", "public", "public"},
        {"d", "package private", "public", "public"},
    };
    private final static String references[][] = {
        {"java.lang.Boolean"  , "public"},
        {"java.lang.Character", "public"},
        {"java.lang.Class"    , "public"},
        {"java.lang.Double"   , "public"},
        {"java.lang.Float"    , "public"},
        {"java.lang.Integer"  , "public"},
        {"java.lang.Long"     , "public"},
        {"java.lang.String"   , "public"},
        {"java.lang.Object"   , "public"},

        {"AccessSpecifierTarg", "package private"},
        {"AccessSpecifierPackagePrivateClass", "package private"},
        {"AccessSpecifierPackagePrivateInterface", "package private"},

        {"AccessSpecifierTarg$StaticInterface", "private"},

        {"AccessSpecifierTarg$U", "private"},
        {"AccessSpecifierTarg$V", "protected"},
        {"AccessSpecifierTarg$W", "public"},
        {"AccessSpecifierTarg$P", "package private"}
    };

    AccessSpecifierTest (String args[]) {
        super(args);
    }

    public static void main(String[] args)      throws Exception {
        new AccessSpecifierTest (args).startTests();
    }

    /********** test core **********/

    private void testAccessible (String name, Accessible a,
                                 boolean isPublic, boolean isProtected,
                                 boolean isPrivate, boolean isPackagePrivate) {
        System.out.println ("  Testing: " + name + " modifiers = " +
                            Integer.toBinaryString(a.modifiers()));
        if (a.isPublic() != isPublic) {
            failure("**Name = " + name + " expecting: " + isPublic +
                    " isPublic() was: " + a.isPublic());
        }
        if (a.isPrivate() != isPrivate) {
            failure("**Name = " + name + " expecting: " + isPrivate +
                    " isPrivate() was: " + a.isPrivate());
        }
        if (a.isProtected() != isProtected) {
            failure("**Name = " + name + " expecting: " + isProtected +
                    " isProtected() is: " + a.isProtected());
        }
        if (a.isPackagePrivate() != isPackagePrivate) {
            failure("**Name = " + name + " expecting: " + isPackagePrivate +
                    " isPackagePrivate() is: " + a.isPackagePrivate());
        }
    }

    protected void runTests() throws Exception {
        /*
         * Get to the top of ready()
         */
        startTo(debugeeName, "ready", "()V");

        ReferenceType rt = findReferenceType(debugeeName);
        if (rt == null) {
            throw new Exception ("ReferenceType not found for: " + debugeeName);
        }
        for (int i = 0; i < primitives.length; i++) {
            for (int j = 0; j < 3; j++) {
                String suffix = Integer.toString(j);
                String fieldName = primitives[i][NAME] + suffix;
                Field field = rt.fieldByName(fieldName);
                if (field == null) {
                    throw new Exception ("Field not found for: " + fieldName);
                }

                Type t = field.type();
                if (t instanceof ReferenceType) {
                    ReferenceType reft = (ReferenceType)t;
                    if (primitives[i][ACCESS + j].equals("public")) {
                        testAccessible(reft.name(), reft,
                                       true, false, false, false);
                    } else if (primitives[i][ACCESS + j].equals("protected")) {
                        testAccessible(reft.name(), reft,
                                       false, true, false, false);
                    } else if (primitives[i][ACCESS + j].equals("private")) {
                        testAccessible(reft.name(), reft,
                                       false, false, true, false);
                    } else if (primitives[i][ACCESS + j].equals("package private")) {
                        testAccessible(reft.name(), reft,
                                       false, false, false, true);
                    }
                } else {
                    System.out.println ("  Skipping " + t +
                                        " (primitive scalar type)");
                }
            }
        }

        String brackets[] = {"[][]", "[]", ""};

        for (int i = 0; i < references.length; i++) {
            for (int j = 0; j < 3; j++) {
                String suffix = brackets[j];
                String referenceName = references[i][NAME] + suffix;
                ReferenceType refType = findReferenceType(referenceName);
                if (refType == null) {
                    System.out.println ("Skipping " + referenceName +
                                        " (not found)");
                } else {
                    if (references[i][ACCESS].equals("public")) {
                        testAccessible(refType.name(), refType, true, false, false, false);
                    } else if  (references[i][ACCESS].equals("protected")) {
                        testAccessible(refType.name(), refType, false, true, false, false);
                    } else if  (references[i][ACCESS].equals("private")) {
                        testAccessible(refType.name(), refType, false, false, true, false);
                    } else if  (references[i][ACCESS].equals("package private")) {
                        testAccessible(refType.name(), refType, false, false, false, true);
                    }
                }
            }
        }

        /*
         * resume the target listening for events
         */
        listenUntilVMDisconnect();

        /*
         * deal with results of test
         * if anything has called failure("foo") testFailed will be true
         */
        if (!testFailed) {
            println("AccessSpecifierTest: passed");
        } else {
            throw new Exception("AccessSpecifierTest: failed");
        }
    }
}
