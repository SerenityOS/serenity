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

#include <AK/Platform.h>
VALIDATE_IS_X86()

/* Map IRQ0-15 @ ISR 0x50-0x5F */
#define IRQ_VECTOR_BASE 0x50
#define GENERIC_INTERRUPT_HANDLERS_COUNT (256 - IRQ_VECTOR_BASE)
#define PAGE_MASK (~(FlatPtr)0xfffu)

namespace Kernel {

struct RegisterState;
class GenericInterruptHandler;

static constexpr u32 safe_eflags_mask = 0xdff;
static constexpr u32 iopl_mask = 3u << 12;

inline u32 get_iopl_from_eflags(u32 eflags)
{
    return (eflags & iopl_mask) >> 12;
}

const DescriptorTablePointer& get_gdtr();
const DescriptorTablePointer& get_idtr();

void handle_crash(RegisterState const&, char const* description, int signal, bool out_of_memory = false);

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
