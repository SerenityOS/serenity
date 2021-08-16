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
class PrimitiveOverload {

    @Candidate(applicable=Phase.BASIC, mostSpecific=true)
    static void m_byte(byte b) {}
    @Candidate(applicable=Phase.BASIC, mostSpecific=false)
    static void m_byte(short b) {}
    @Candidate(applicable=Phase.BASIC, mostSpecific=false)
    static void m_byte(int b) {}
    @Candidate(applicable=Phase.BASIC, mostSpecific=false)
    static void m_byte(long b) {}
    @Candidate(applicable=Phase.BASIC, mostSpecific=false)
    static void m_byte(float b) {}
    @Candidate(applicable=Phase.BASIC, mostSpecific=false)
    static void m_byte(double b) {}

    @Candidate
    static void m_short(byte b) {}
    @Candidate(applicable=Phase.BASIC, mostSpecific=true)
    static void m_short(short b) {}
    @Candidate(applicable=Phase.BASIC, mostSpecific=false)
    static void m_short(int b) {}
    @Candidate(applicable=Phase.BASIC, mostSpecific=false)
    static void m_short(long b) {}
    @Candidate(applicable=Phase.BASIC, mostSpecific=false)
    static void m_short(float b) {}
    @Candidate(applicable=Phase.BASIC, mostSpecific=false)
    static void m_short(double b) {}

    @Candidate
    static void m_int(byte b) {}
    @Candidate
    static void m_int(short b) {}
    @Candidate(applicable=Phase.BASIC, mostSpecific=true)
    static void m_int(int b) {}
    @Candidate(applicable=Phase.BASIC, mostSpecific=false)
    static void m_int(long b) {}
    @Candidate(applicable=Phase.BASIC, mostSpecific=false)
    static void m_int(float b) {}
    @Candidate(applicable=Phase.BASIC, mostSpecific=false)
    static void m_int(double b) {}

    @Candidate
    static void m_long(byte b) {}
    @Candidate
    static void m_long(short b) {}
    @Candidate
    static void m_long(int b) {}
    @Candidate(applicable=Phase.BASIC, mostSpecific=true)
    static void m_long(long b) {}
    @Candidate(applicable=Phase.BASIC, mostSpecific=false)
    static void m_long(float b) {}
    @Candidate(applicable=Phase.BASIC, mostSpecific=false)
    static void m_long(double b) {}

    @Candidate
    static void m_float(byte b) {}
    @Candidate
    static void m_float(short b) {}
    @Candidate
    static void m_float(int b) {}
    @Candidate
    static void m_float(long b) {}
    @Candidate(applicable=Phase.BASIC, mostSpecific=true)
    static void m_float(float b) {}
    @Candidate(applicable=Phase.BASIC, mostSpecific=false)
    static void m_float(double b) {}

    @Candidate
    static void m_double(byte b) {}
    @Candidate
    static void m_double(short b) {}
    @Candidate
    static void m_double(int b) {}
    @Candidate
    static void m_double(long b) {}
    @Candidate
    static void m_double(float b) {}
    @Candidate(applicable=Phase.BASIC, mostSpecific=true)
    static void m_double(double b) {}

    {
        m_byte((byte)0);
        m_short((short)0);
        m_int(0);
        m_long(0L);
        m_float(0.0f);
        m_double(0.0);
    }
}
