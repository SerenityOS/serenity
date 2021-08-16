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
class PrimitiveOverInferred {
    @Candidate(applicable=Phase.BOX, mostSpecific=true)
    static void m_byte(Byte b) {}
    @Candidate(applicable=Phase.BOX, sig="(java.lang.Byte)void")
    static <B> void m_byte(B b) {}

    @Candidate(applicable=Phase.BOX, mostSpecific=true)
    static void m_short(Short s) {}
    @Candidate(applicable=Phase.BOX, sig="(java.lang.Short)void")
    static <S> void m_short(S s) {}

    @Candidate(applicable=Phase.BOX, mostSpecific=true)
    static void m_int(Integer i) {}
    @Candidate(applicable=Phase.BOX, sig="(java.lang.Integer)void")
    static <I> void m_int(I i) {}

    @Candidate(applicable=Phase.BOX, mostSpecific=true)
    static void m_long(Long l) {}
    @Candidate(applicable=Phase.BOX, sig="(java.lang.Long)void")
    static <L> void m_long(L l) {}

    @Candidate(applicable=Phase.BOX, mostSpecific=true)
    static void m_float(Float f) {}
    @Candidate(applicable=Phase.BOX, sig="(java.lang.Float)void")
    static <F> void m_float(F f) {}

    @Candidate(applicable=Phase.BOX, mostSpecific=true)
    static void m_double(Double d) {}
    @Candidate(applicable=Phase.BOX, sig="(java.lang.Double)void")
    static <D> void m_double(D d) {}

    @Candidate(applicable=Phase.BOX, mostSpecific=true)
    static void m_char(Character c) {}
    @Candidate(applicable=Phase.BOX, sig="(java.lang.Character)void")
    static <C> void m_char(C c) {}

    @Candidate(applicable=Phase.BOX, mostSpecific=true)
    static void m_bool(Boolean z) {}
    @Candidate(applicable=Phase.BOX, sig="(java.lang.Boolean)void")
    static <Z> void m_bool(Z z) {}

    {
        m_byte((byte)0);
        m_short((short)0);
        m_int(0);
        m_long(0L);
        m_float(0.0f);
        m_double(0.0);
        m_char('?');
        m_bool(false);
    }
}
