/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

@TraceResolve
class AbstractMerge {

    interface A {
        @Candidate(applicable=Phase.BASIC)
        java.io.Serializable m1();
        @Candidate(applicable=Phase.BASIC)
        java.io.Serializable m2();
        @Candidate(applicable=Phase.BASIC)
        java.io.Serializable m3();
        @Candidate(applicable=Phase.BASIC)
        java.io.Serializable m4();
        @Candidate(applicable=Phase.BASIC)
        java.io.Serializable m5();
        @Candidate(applicable=Phase.BASIC)
        java.io.Serializable m6();
    }

    interface B {
        @Candidate(applicable=Phase.BASIC)
        Cloneable m1();
        @Candidate(applicable=Phase.BASIC)
        Cloneable m2();
        @Candidate(applicable=Phase.BASIC)
        Cloneable m3();
        @Candidate(applicable=Phase.BASIC)
        Cloneable m4();
        @Candidate(applicable=Phase.BASIC)
        Cloneable m5();
        @Candidate(applicable=Phase.BASIC)
        Cloneable m6();
    }

    interface C {
        @Candidate(applicable=Phase.BASIC, mostSpecific=true)
        Object[] m1();
        @Candidate(applicable=Phase.BASIC, mostSpecific=true)
        Object[] m2();
        @Candidate(applicable=Phase.BASIC, mostSpecific=true)
        Object[] m3();
        @Candidate(applicable=Phase.BASIC, mostSpecific=true)
        Object[] m4();
        @Candidate(applicable=Phase.BASIC, mostSpecific=true)
        Object[] m5();
        @Candidate(applicable=Phase.BASIC, mostSpecific=true)
        Object[] m6();
    }

    interface ABC extends A, B, C { }
    interface ACB extends A, C, B { }
    interface BAC extends B, A, C { }
    interface BCA extends B, C, A { }
    interface CAB extends C, A, B { }
    interface CBA extends C, B, A { }

    {
        ABC abc = null;
        abc.m1();
    }

    {
        ACB acb = null;
        acb.m2();
    }

    {
        BAC bac = null;
        bac.m3();
    }

    {
        BCA bca = null;
        bca.m4();
    }

    {
        CAB cab = null;
        cab.m5();
    }

    {
        CBA cba = null;
        cba.m6();
    }
}
