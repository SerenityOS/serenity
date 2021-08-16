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

package p1;

public class Holder {
    public Private_PublicClass             c1 = new Private_PublicClass();
    public Private_PublicInterface         c2 = new Private_PublicInterface();
    public Private_PrivateInterface1       c3 = new Private_PrivateInterface1();
    public Private_PrivateInterface2       c4 = new Private_PrivateInterface2();

    public Public_PublicClass              c5 = new Public_PublicClass();
    public Public_PublicInterface          c6 = new Public_PublicInterface();
    public Public_PrivateInterface1        c7 = new Public_PrivateInterface1();
    public Public_PrivateInterface2        c8 = new Public_PrivateInterface2();

    public Private_PublicClass[]          ac1 = new Private_PublicClass[0];
    public Private_PublicInterface[]      ac2 = new Private_PublicInterface[0];
    public Private_PrivateInterface1[]    ac3 = new Private_PrivateInterface1[0];
    public Private_PrivateInterface2[]    ac4 = new Private_PrivateInterface2[0];

    public Public_PublicClass[]           ac5 = new Public_PublicClass[0];
    public Public_PublicInterface[]       ac6 = new Public_PublicInterface[0];
    public Public_PrivateInterface1[]     ac7 = new Public_PrivateInterface1[0];
    public Public_PrivateInterface2[]     ac8 = new Public_PrivateInterface2[0];

    public Private_PublicClass[][]       aac1 = new Private_PublicClass[0][];
    public Private_PublicInterface[][]   aac2 = new Private_PublicInterface[0][];
    public Private_PrivateInterface1[][] aac3 = new Private_PrivateInterface1[0][];
    public Private_PrivateInterface2[][] aac4 = new Private_PrivateInterface2[0][];

    public Public_PublicClass[][]        aac5 = new Public_PublicClass[0][];
    public Public_PublicInterface[][]    aac6 = new Public_PublicInterface[0][];
    public Public_PrivateInterface1[][]  aac7 = new Public_PrivateInterface1[0][];
    public Public_PrivateInterface2[][]  aac8 = new Public_PrivateInterface2[0][];

    public PublicInterface                 i1 = new Private_PublicInterface();
    public PrivateInterface1               i2 = new Private_PrivateInterface1();
    public PrivateInterface2               i3 = new Private_PrivateInterface2();

    public PublicInterface[]              ai1 = new Private_PublicInterface[0];
    public PrivateInterface1[]            ai2 = new Private_PrivateInterface1[0];
    public PrivateInterface2[]            ai3 = new Private_PrivateInterface2[0];

    public PublicInterface[][]           aai1 = new Private_PublicInterface[0][];
    public PrivateInterface1[][]         aai2 = new Private_PrivateInterface1[0][];
    public PrivateInterface2[][]         aai3 = new Private_PrivateInterface2[0][];
}

interface PrivateInterface1 {
}

interface PrivateInterface2 extends PublicInterface {
}

class Private_PublicClass extends PublicClass {
    public String toString() {
        return "passed";
    }
}

class Private_PublicInterface implements PublicInterface {
    public String toString() {
        return "passed";
    }
}

class Private_PrivateInterface1 implements PrivateInterface1 {
    public String toString() {
        return "passed";
    }
}

class Private_PrivateInterface2 implements PrivateInterface2 {
    public String toString() {
        return "passed";
    }
}
