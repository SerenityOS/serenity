/*
 * Copyright (c) 2021, Leon Albrecht <leon2002.la@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <AK/Types.h>
#include <stdlib.h>
#include <time.h>

void test_add();
void test_sub();

void test_add()
{
    u64 res;
    for (u8 _it = 0; _it < 128; ++_it) {
        u32 rand_val = static_cast<u32>(rand());
        u64 packed_val = (u64)explode_byte(rand_val) << 32 | explode_byte(rand_val);
        asm volatile(
            "movq %1, %0\n"
            "paddb %0, %0"
            : "=y"(res)
            : "y"(packed_val));

        for (u8 i = 0; i < 8; ++i) {
            if (reinterpret_cast<u8*>(&res)[i] != static_cast<u8>(reinterpret_cast<u8*>(&packed_val)[i] * 2)) {
                outln("paddb-{}: expected {}, got {}", _it, static_cast<u8>(reinterpret_cast<u8*>(&packed_val)[i] * 2), reinterpret_cast<u8*>(&res)[i]);
                VERIFY_NOT_REACHED();
            }
        }
        asm volatile(
            "movq %1, %0\n"
            "paddw %0, %0"
            : "=y"(res)
            : "y"(packed_val));

        for (u8 i = 0; i < 4; ++i) {
            if (reinterpret_cast<u16*>(&res)[i] != static_cast<u16>(reinterpret_cast<u16*>(&packed_val)[i] * 2)) {
                outln("paddw-{}: expected {}, got {}", _it, static_cast<u16>(reinterpret_cast<u16*>(&packed_val)[i] * 2), reinterpret_cast<u16*>(&res)[i]);
                VERIFY_NOT_REACHED();
            }
        }
        asm volatile(
            "movq %1, %0\n"
            "paddd %0, %0"
            : "=y"(res)
            : "y"(packed_val));

        for (u8 i = 0; i < 2; ++i) {
            if (reinterpret_cast<u32*>(&res)[i] != static_cast<u32>(reinterpret_cast<u32*>(&packed_val)[i] * 2)) {
                outln("paddd-{}: expected {}, got {}", _it, static_cast<u32>(reinterpret_cast<u32*>(&packed_val)[i] * 2), reinterpret_cast<u32*>(&res)[i]);
                VERIFY_NOT_REACHED();
            }
        }
    }
}
void test_sub()
{
    u64 res;
    for (u8 _it = 0; _it < 128; ++_it) {
        u32 rand_val = static_cast<u32>(rand());
        u64 packed_val = (u64)explode_byte(rand_val) << 32 | explode_byte(rand_val);
        asm volatile(
            "movq %1, %0\n"
            "psubb %0, %0"
            : "=y"(res)
            : "y"(packed_val));

        for (u8 i = 0; i < 8; ++i) {
            if (reinterpret_cast<u8*>(&res)[i] != 0) {
                outln("psubb-{}: expected {}, got {}", _it, 0, reinterpret_cast<u8*>(&res)[i]);
                VERIFY_NOT_REACHED();
            }
        }
        asm volatile(
            "movq %1, %0\n"
            "psubw %0, %0"
            : "=y"(res)
            : "y"(packed_val));

        for (u8 i = 0; i < 4; ++i) {
            if (reinterpret_cast<u16*>(&res)[i] != 0) {
                outln("psubw-{}: expected {}, got {}", _it, 0, reinterpret_cast<u16*>(&res)[i]);
                VERIFY_NOT_REACHED();
            }
        }
        asm volatile(
            "movq %1, %0\n"
            "psubd %0, %0"
            : "=y"(res)
            : "y"(packed_val));

        for (u8 i = 0; i < 2; ++i) {
            if (reinterpret_cast<u32*>(&res)[i] != 0) {
                outln("psubd-{}: expected {}, got {}", _it, 0, reinterpret_cast<u32*>(&res)[i]);
                VERIFY_NOT_REACHED();
            }
        }
    }
}

int main()
{
    srand(time(0));
    test_add();
    test_sub();
}
