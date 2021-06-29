/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Atomic.h>
#include <AK/Concepts.h>
#include <AK/Vector.h>

#include <Kernel/Arch/x86/DescriptorTable.h>

/* Map IRQ0-15 @ ISR 0x50-0x5F */
#define IRQ_VECTOR_BASE 0x50
#define GENERIC_INTERRUPT_HANDLERS_COUNT (256 - IRQ_VECTOR_BASE)
#define PAGE_MASK ((FlatPtr)0xfffff000u)

namespace Kernel {

struct RegisterState;
class GenericInterruptHandler;

static constexpr u32 safe_eflags_mask = 0xdff;
static constexpr u32 iopl_mask = 3u << 12;

inline u32 get_iopl_from_eflags(u32 eflags)
{
    return (eflags & iopl_mask) >> 12;
}

template<Integral T>
void read_possibly_unaligned_data(u8* where, T& data) requires(sizeof(T) == 8 || sizeof(T) == 4 || sizeof(T) == 2)
{
    if (((FlatPtr)where % alignof(T)) == 0) {
        data = *(T*)where;
        return;
    }
    if constexpr (sizeof(T) == 2) {
        data = *where | ((u16)(*(where + 1)) << 8);
        return;
    }
    if constexpr (sizeof(T) == 4) {
        data = *where | (((u32) * (where + 1)) << 8) | (((u32) * (where + 2)) << 16) | (((u32) * (where + 3)) << 24);
        return;
    }
    if constexpr (sizeof(T) == 8) {
        data = *where | (((u32) * (where + 1)) << 8) | (((u64) * (where + 2)) << 16) | (((u64) * (where + 3)) << 24)
            | (((u64) * (where + 4)) << 32) | (((u64) * (where + 5)) << 40) | (((u64) * (where + 6)) << 48) | (((u64) * (where + 7)) << 56);
        return;
    }
    VERIFY_NOT_REACHED();
}

template<Integral T>
void write_possibly_unaligned_data(u8* where, T data) requires(sizeof(T) == 8 || sizeof(T) == 4 || sizeof(T) == 2)
{
    if (((FlatPtr)where % alignof(T)) == 0) {
        *(T*)where = data;
        return;
    }
    if constexpr (sizeof(T) == 2) {
        where[0] = (u8)(data & 0xFF);
        where[1] = (u8)((data >> 8) & 0xFF);
        return;
    }
    if constexpr (sizeof(T) == 4) {
        where[0] = (u8)(data & 0xFF);
        where[1] = (u8)((data >> 8) & 0xFF);
        where[2] = (u8)((data >> 16) & 0xFF);
        where[3] = (u8)((data >> 24) & 0xFF);
        return;
    }
    if constexpr (sizeof(T) == 8) {
        where[0] = (u8)(data & 0xFF);
        where[1] = (u8)((data >> 8) & 0xFF);
        where[2] = (u8)((data >> 16) & 0xFF);
        where[3] = (u8)((data >> 24) & 0xFF);
        where[5] = (u8)(data >> 32 & 0xFF);
        where[5] = (u8)((data >> 40) & 0xFF);
        where[6] = (u8)((data >> 48) & 0xFF);
        where[7] = (u8)((data >> 52) & 0xFF);
        return;
    }
    VERIFY_NOT_REACHED();
}

const DescriptorTablePointer& get_gdtr();
const DescriptorTablePointer& get_idtr();

[[noreturn]] void handle_crash(RegisterState&, const char* description, int signal, bool out_of_memory = false);

#define LSW(x) ((u32)(x)&0xFFFF)
#define MSW(x) (((u32)(x) >> 16) & 0xFFFF)
#define LSB(x) ((x)&0xFF)
#define MSB(x) (((x) >> 8) & 0xFF)

constexpr FlatPtr page_base_of(FlatPtr address)
{
    return address & PAGE_MASK;
}

inline FlatPtr page_base_of(const void* address)
{
    return page_base_of((FlatPtr)address);
}

constexpr FlatPtr offset_in_page(FlatPtr address)
{
    return address & (~PAGE_MASK);
}

inline FlatPtr offset_in_page(const void* address)
{
    return offset_in_page((FlatPtr)address);
}

}
