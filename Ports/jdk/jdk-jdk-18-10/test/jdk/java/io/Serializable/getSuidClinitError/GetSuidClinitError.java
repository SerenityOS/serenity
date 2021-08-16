/*
 * Copyright (c) 2001, 2019, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 4482966
 * @summary Verify that ObjectStreamClass.getSerialVersionUID() will not mask
 *          Errors (other than NoSuchMethodError) triggered by JNI query for
 *          static initializer method.
 */

import java.io.*;

@SuppressWarnings("serial") /* Incorrect declarations are being tested. */
class A implements Serializable {
    static {
        // compiler prohibits direct throw
        throwMe(new RuntimeException("blargh"));
    }

    static void throwMe(RuntimeException ex) throws RuntimeException {
        throw ex;
    }
}

@SuppressWarnings("serial") /* Incorrect declarations are being tested. */
class B implements Serializable {
}

@SuppressWarnings("serial") /* Incorrect declarations are being tested. */
class C implements Serializable {
    static { System.out.println("C.<clinit>"); }
}

@SuppressWarnings("serial") /* Incorrect declarations are being tested. */
class B1 extends B {
}

@SuppressWarnings("serial") /* Incorrect declarations are being tested. */
class B2 extends B {
    static { System.out.println("B2.<clinit>"); }
}

@SuppressWarnings("serial") /* Incorrect declarations are being tested. */
class C1 extends C {
}

@SuppressWarnings("serial") /* Incorrect declarations are being tested. */
class C2 extends C {
    static { System.out.println("C2.<clinit>"); }
}

public class GetSuidClinitError {
    public static void main(String[] args) throws Exception {
        Class<?> cl = Class.forName(
            "A", false, GetSuidClinitError.class.getClassLoader());
        for (int i = 0; i < 2; i++) {
            try {
                ObjectStreamClass.lookup(cl).getSerialVersionUID();
                throw new Error();
            } catch (ExceptionInInitializerError er) {
            } catch (NoClassDefFoundError er) {
                /*
                 * er _should_ be an ExceptionInInitializerError; however,
                 * hotspot currently throws a NoClassDefFoundError in this
                 * case, which runs against the JNI spec.  For the purposes of
                 * testing this fix, however, permit either.
                 */
                System.out.println("warning: caught " + er +
                    " instead of ExceptionInInitializerError");
            }
        }

        Class<?>[] cls = {
            B.class, B1.class, B2.class,
            C.class, C1.class, C2.class
        };
        long[] suids = new long[] {     // 1.3.1 default serialVersionUIDs
            369445310364440919L, 7585771686008346939L, -8952923334200087495L,
            3145821251853463625L, 327577314910517070L, -92102021266426451L
        };
        for (int i = 0; i < cls.length; i++) {
            if (ObjectStreamClass.lookup(cls[i]).getSerialVersionUID() !=
                suids[i])
            {
                throw new Error();
            }
        }
    }
}
