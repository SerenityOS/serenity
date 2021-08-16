/*
 *  Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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

#include <stdarg.h>

#ifdef _WIN64
#define EXPORT __declspec(dllexport)
#else
#define EXPORT
#endif

// ###### Down calls

EXPORT int sumInts(int argNum, va_list list) {
    int sum = 0;
    for (int i = 0; i < argNum; i++) {
        sum += va_arg(list, int);
    }
    return sum;
}

EXPORT double sumDoubles(int argNum, va_list list) {
    double sum = 0;
    for (int i = 0; i < argNum; i++) {
        sum += va_arg(list, double);
    }
    return sum;
}

EXPORT int getInt(va_list list) {
    int* ptr = va_arg(list, int*);
    return *ptr;
}

typedef struct {
    int x;
    int y;
} Point;

EXPORT int sumStruct(va_list list) {
    Point point = va_arg(list, Point);
    return point.x + point.y;
}

typedef struct {
    long long x;
    long long y;
} BigPoint;

EXPORT long long sumBigStruct(va_list list) {
    BigPoint point = va_arg(list, BigPoint);
    return point.x + point.y;
}

typedef struct {
    long long x;
    long long y;
    long long z;
} HugePoint;

EXPORT long long sumHugeStruct(va_list list) {
    HugePoint point = va_arg(list, HugePoint);
    return point.x + point.y + point.z;
}

typedef struct {
    float x;
    float y;
} FloatPoint;

EXPORT float sumFloatStruct(va_list list) {
    FloatPoint point = va_arg(list, FloatPoint);
    return point.x + point.y;
}

EXPORT void sumStack(long long* longSum, double* doubleSum, va_list list) {
    long long lSum = 0;
    for (int i = 0; i < 16; i++) {
        lSum += va_arg(list, long long);
    }
    *longSum = lSum;
    double dSum = 0.0;
    for (int i = 0; i < 16; i++) {
        dSum += va_arg(list, double);
    }
    *doubleSum = dSum;
}

// ###### Up calls

typedef void CB(va_list);

static void passToUpcall(CB cb, int numArgs, ...) {
    va_list list;
    va_start(list, numArgs);
    cb(list);
    va_end(list);
}

EXPORT void upcallInts(CB cb) {
    passToUpcall(cb, 3, 10, 15, 20);
}

EXPORT void upcallDoubles(CB cb) {
    passToUpcall(cb, 3, 3.0, 4.0, 5.0);
}

EXPORT void upcallStack(CB cb) {
    Point point;
    point.x = 5;
    point.y = 10;

    BigPoint bigPoint;
    bigPoint.x = 15;
    bigPoint.y = 20;

    passToUpcall(cb, 32 + 14,
        1LL, 2LL, 3LL, 4LL, 5LL, 6LL, 7LL, 8LL,
        9LL, 10LL, 11LL, 12LL, 13LL, 14LL, 15LL, 16LL,
        1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0,
        9.0, 10.0, 11.0, 12.0, 13.0, 14.0, 15.0, 16.0,
        // should all be passed on the stack
        1, 'a', 3,  4,  5LL,  6.0f,  7.0,
        8, 'b', 10, 11, 12LL, 13.0f, 14.0,
        point, bigPoint);
}

EXPORT void upcallMemoryAddress(CB cb) {
    int x = 10;
    passToUpcall(cb, 1, &x);
}

EXPORT void upcallStruct(CB cb) {
    Point point;
    point.x = 5;
    point.y = 10;
    passToUpcall(cb, 1, point);
}

EXPORT void upcallFloatStruct(CB cb) {
    FloatPoint point;
    point.x = 1.0f;
    point.y = 2.0f;
    passToUpcall(cb, 1, point);
}

EXPORT void upcallBigStruct(CB cb) {
    BigPoint point;
    point.x = 8;
    point.y = 16;
    passToUpcall(cb, 1, point);
}

EXPORT void upcallBigStructPlusScalar(CB cb) {
    BigPoint point;
    point.x = 8;
    point.y = 16;
    passToUpcall(cb, 2, point, 42);
}

EXPORT void upcallHugeStruct(CB cb) {
    HugePoint point;
    point.x = 1;
    point.y = 2;
    point.z = 3;
    passToUpcall(cb, 1, point);
}
