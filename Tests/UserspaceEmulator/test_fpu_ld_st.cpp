/*
 * Copyright (c) 2021, Leon Albrecht <leon2002.la@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <AK/Types.h>
#include <stdio.h>
#include <string.h>

void test_ld_st();
void test_env();

void test_ld_st()
{
    // floats
    outln("float");
    float f1 = 0;
    float f2 = 3498934.237823f;

    asm volatile(
        "flds %1 \n"
        "fstps %0"
        : "=m"(f1)
        : "m"(f2)
        : "memory");
    if (f1 != f2) {
        outln("Fail f1 {} != f2 {}, diff(f1-f2) {}", f1, f2, f1 - f2);
        exit(1);
    }
    // doubles
    outln("doubles");
    double d1 = 0;
    double d2 = 20348.78324;

    asm volatile(
        "fldl %1 \n"
        "fstpl %0"
        : "=m"(d1)
        : "m"(d2)
        : "memory");

    if (d1 != d2) {
        outln("Fail l1 {} != l2 {}, diff(l1-l2) {}", d1, d2, d1 - d2);
        exit(1);
    }

    // long doubles
    outln("long doubles");
    long double l1 = 0;
    long double l2 = 237402.394875938745983l;

    asm volatile(
        "fldt %1 \n"
        "fstpt %0"
        : "+m"(l1)
        : "m"(l2)
        : "memory");

    if (l1 != l2) {
        outln("Fail l1 {} != l2 {}, diff(l1-l2) {}", l1, l2, l1 - l2);
        exit(1);
    }
}

int main()
{
    test_ld_st();
    outln("pass");
}
