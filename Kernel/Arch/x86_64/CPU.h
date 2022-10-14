/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Atomic.h>
#include <AK/Vector.h>

#include <Kernel/Arch/x86_64/DescriptorTable.h>

#include <AK/Platform.h>
VALIDATE_IS_X86()

/* Map IRQ0-15 @ ISR 0x50-0x5F */
#define IRQ_VECTOR_BASE 0x50
#define GENERIC_INTERRUPT_HANDLERS_COUNT (256 - IRQ_VECTOR_BASE)

namespace Kernel {

struct RegisterState;
class GenericInterruptHandler;

static constexpr u32 safe_eflags_mask = 0xdff;
static constexpr u32 iopl_mask = 3u << 12;

inline u32 get_iopl_from_eflags(u32 eflags)
{
    return (eflags & iopl_mask) >> 12;
}

DescriptorTablePointer const& get_gdtr();
DescriptorTablePointer const& get_idtr();

constexpr FlatPtr page_base_of(FlatPtr address)
{
    return address & PAGE_MASK;
}

inline FlatPtr page_base_of(void const* address)
{
    return page_base_of((FlatPtr)address);
}

constexpr FlatPtr offset_in_page(FlatPtr address)
{
    return address & (~PAGE_MASK);
}

inline FlatPtr offset_in_page(void const* address)
{
    return offset_in_page((FlatPtr)address);
}

}
