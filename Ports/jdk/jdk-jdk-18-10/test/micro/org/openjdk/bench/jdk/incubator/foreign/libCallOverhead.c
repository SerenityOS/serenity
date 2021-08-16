/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifdef _WIN64
#define EXPORT __declspec(dllexport)
#else
#define EXPORT
#endif

EXPORT void func() {}

EXPORT int identity(int x) {
  return x;
}

typedef struct {
    long long x;
    long long y;
} Point;

EXPORT Point identity_struct(Point p) {
    return p;
}

EXPORT void* identity_memory_address(void* p) {
    return p;
}

EXPORT void args1(long long a0) {}
EXPORT void args2(long long a0, double a1) {}
EXPORT void args3(long long a0, double a1, long long a2) {}
EXPORT void args4(long long a0, double a1, long long a2, double a3) {}
EXPORT void args5(long long a0, double a1, long long a2, double a3, long long a4) {}
EXPORT void args10(long long a0, double a1, long long a2, double a3, long long a4,
                   double a5, long long a6, double a7, long long a8, double a9) {}
