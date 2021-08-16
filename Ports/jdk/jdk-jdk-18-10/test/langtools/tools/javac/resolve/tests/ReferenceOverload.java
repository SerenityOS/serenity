/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
class ReferenceOverload {

    static class A {}
    static class B extends A {}
    static class C extends B {}
    static class D extends C {}
    static class E extends D {}

    @Candidate(applicable=Phase.BASIC, mostSpecific=true)
    static void m_A(A a) {}
    @Candidate
    static void m_A(B a) {}
    @Candidate
    static void m_A(C a) {}
    @Candidate
    static void m_A(D a) {}
    @Candidate
    static void m_A(E a) {}

    @Candidate(applicable=Phase.BASIC, mostSpecific=false)
    static void m_B(A b) {}
    @Candidate(applicable=Phase.BASIC, mostSpecific=true)
    static void m_B(B b) {}
    @Candidate
    static void m_B(C b) {}
    @Candidate
    static void m_B(D b) {}
    @Candidate
    static void m_B(E b) {}

    @Candidate(applicable=Phase.BASIC, mostSpecific=false)
    static void m_C(A c) {}
    @Candidate(applicable=Phase.BASIC, mostSpecific=false)
    static void m_C(B c) {}
    @Candidate(applicable=Phase.BASIC, mostSpecific=true)
    static void m_C(C c) {}
    @Candidate
    static void m_C(D c) {}
    @Candidate
    static void m_C(E c) {}

    @Candidate(applicable=Phase.BASIC, mostSpecific=false)
    static void m_D(A d) {}
    @Candidate(applicable=Phase.BASIC, mostSpecific=false)
    static void m_D(B d) {}
    @Candidate(applicable=Phase.BASIC, mostSpecific=false)
    static void m_D(C d) {}
    @Candidate(applicable=Phase.BASIC, mostSpecific=true)
    static void m_D(D d) {}
    @Candidate
    static void m_D(E d) {}

    @Candidate(applicable=Phase.BASIC, mostSpecific=false)
    static void m_E(A e) {}
    @Candidate(applicable=Phase.BASIC, mostSpecific=false)
    static void m_E(B e) {}
    @Candidate(applicable=Phase.BASIC, mostSpecific=false)
    static void m_E(C e) {}
    @Candidate(applicable=Phase.BASIC, mostSpecific=false)
    static void m_E(D e) {}
    @Candidate(applicable=Phase.BASIC, mostSpecific=true)
    static void m_E(E e) {}

    {
        m_A((A)null);
        m_B((B)null);
        m_C((C)null);
        m_D((D)null);
        m_E((E)null);
    }
}
