/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

@TraceResolve(keys={"compiler.err.ref.ambiguous"})
class PrimitiveVsReferenceSamePhase {
    @Candidate(applicable=Phase.BOX, mostSpecific=false)
    static void m_byte(Byte b1, byte b2) {}
    @Candidate(applicable=Phase.BOX, mostSpecific=false)
    static void m_byte(Byte b1, Byte b2) {}

    @Candidate(applicable=Phase.BOX, mostSpecific=false)
    static void m_short(Short s1, short s2) {}
    @Candidate(applicable=Phase.BOX, mostSpecific=false)
    static void m_short(Short s1, Short s2) {}

    @Candidate(applicable=Phase.BOX, mostSpecific=false)
    static void m_int(Integer i1, int i2) {}
    @Candidate(applicable=Phase.BOX, mostSpecific=false)
    static void m_int(Integer i1, Integer i2) {}

    @Candidate(applicable=Phase.BOX, mostSpecific=false)
    static void m_long(Long l1, long l2) {}
    @Candidate(applicable=Phase.BOX, mostSpecific=false)
    static void m_long(Long l1, Long l2) {}

    @Candidate(applicable=Phase.BOX, mostSpecific=false)
    static void m_float(Float f1, float f2) {}
    @Candidate(applicable=Phase.BOX, mostSpecific=false)
    static void m_float(Float f1, Float f2) {}

    @Candidate(applicable=Phase.BOX, mostSpecific=false)
    static void m_double(Double d1, double d2) {}
    @Candidate(applicable=Phase.BOX, mostSpecific=false)
    static void m_double(Double d1, Double d2) {}

    @Candidate(applicable=Phase.BOX, mostSpecific=false)
    static void m_char(Character c1, char c2) {}
    @Candidate(applicable=Phase.BOX, mostSpecific=false)
    static void m_char(Character c1, Character c2) {}

    @Candidate(applicable=Phase.BOX, mostSpecific=false)
    static void m_bool(Boolean z1, boolean z2) {}
    @Candidate(applicable=Phase.BOX, mostSpecific=false)
    static void m_bool(Boolean z1, Boolean z2) {}

    {
        m_byte((byte)0, (byte)0);
        m_short((short)0, (short)0);
        m_int(0, 0);
        m_long(0L, 0L);
        m_float(0.0f, 0.0f);
        m_double(0.0, 0.0);
        m_char('?', '?');
        m_bool(false, false);
    }
}
