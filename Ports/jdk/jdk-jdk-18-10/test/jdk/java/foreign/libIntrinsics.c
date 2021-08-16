/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  version 2 for more details (a copy is included in the LICENSE file that
 *  accompanied this code).
 *
 *  You should have received a copy of the GNU General Public License version
 *  2 along with this work; if not, write to the Free Software Foundation,
 *  Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *   Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 *  or visit www.oracle.com if you need additional information or have any
 *  questions.
 *
 */

#ifdef _WIN64
#define EXPORT __declspec(dllexport)
#else
#define EXPORT
#endif

EXPORT void empty() {
}

EXPORT char identity_char(char x) {
    return x;
}

EXPORT short identity_short(short x) {
    return x;
}

EXPORT int identity_int(int x) {
    return x;
}

EXPORT long long identity_long(long long x) {
    return x;
}

EXPORT float identity_float(float x) {
    return x;
}

EXPORT double identity_double(double x) {
    return x;
}

EXPORT int identity_va(int x, ...) {
    return x;
}

EXPORT int invoke_high_arity0(int x, double d, long long l, float f, char c, short s1, short s2) {
    return x;
}
EXPORT double invoke_high_arity1(int x, double d, long long l, float f, char c, short s1, short s2) {
    return d;
}
EXPORT long long invoke_high_arity2(int x, double d, long long l, float f, char c, short s1, short s2) {
    return l;
}
EXPORT float invoke_high_arity3(int x, double d, long long l, float f, char c, short s1, short s2) {
    return f;
}
EXPORT char invoke_high_arity4(int x, double d, long long l, float f, char c, short s1, short s2) {
    return c;
}
EXPORT short invoke_high_arity5(int x, double d, long long l, float f, char c, short s1, short s2) {
    return s1;
}
EXPORT short invoke_high_arity6(int x, double d, long long l, float f, char c, short s1, short s2) {
    return s2;
}