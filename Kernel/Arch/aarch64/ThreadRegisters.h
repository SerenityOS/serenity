/*
 * Copyright (c) 2022, Timon Kruiper <timonkruiper@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <Kernel/Memory/AddressSpace.h>
#include <Kernel/StdLib.h>

namespace Kernel {

struct ThreadRegisters {
    u64 x[31];
    u64 spsr_el1;
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
        set_spsr_el1();
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

    void set_spsr_el1()
    {
        Aarch64::SPSR_EL1 saved_program_status_register_el1 = {};

        // Don't mask any interrupts, so all interrupts are enabled when transfering into the new context
        saved_program_status_register_el1.D = 0;
        saved_program_status_register_el1.A = 0;
        saved_program_status_register_el1.I = 0;
        saved_program_status_register_el1.F = 0;

        // Set exception origin mode to EL1h, so when the context is restored, we'll be executing in EL1 with SP_EL1
        // FIXME: This must be EL0t when aarch64 supports userspace applications.
        saved_program_status_register_el1.M = Aarch64::SPSR_EL1::Mode::EL1h;
        memcpy(&spsr_el1, &saved_program_status_register_el1, sizeof(u64));
    }
};

}
