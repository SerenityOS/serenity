/*
 * Copyright (c) 2022, Timon Kruiper <timonkruiper@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <Kernel/Memory/AddressSpace.h>

namespace Kernel {

struct ThreadRegisters {
    u64 x[31];
    u64 elr_el1;
    u64 sp_el0;
    u64 ttbr0_el1;

    FlatPtr ip() const { return elr_el1; }
    void set_ip(FlatPtr value) { elr_el1 = value; }

    void set_sp(FlatPtr value) { sp_el0 = value; }

    void set_initial_state(bool, Memory::AddressSpace& space, FlatPtr kernel_stack_top)
    {
        set_sp(kernel_stack_top);
        ttbr0_el1 = space.page_directory().ttbr0();
    }

    void set_entry_function(FlatPtr entry_ip, FlatPtr entry_data)
    {
        set_ip(entry_ip);
        x[0] = entry_data;
    }

    void set_exec_state(FlatPtr entry_ip, FlatPtr userspace_sp, Memory::AddressSpace& space)
    {
        (void)entry_ip;
        (void)userspace_sp;
        (void)space;
        TODO_AARCH64();
    }
};

}
