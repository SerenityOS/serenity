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

    FlatPtr ip() const { return elr_el1; }
    void set_ip(FlatPtr value) { elr_el1 = value; }

    void set_sp(FlatPtr value) { sp_el0 = value; }

    void set_initial_state(bool, Memory::AddressSpace&, FlatPtr kernel_stack_top)
    {
        set_sp(kernel_stack_top);
    }
};

}
