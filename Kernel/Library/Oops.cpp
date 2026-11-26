/*
 * Copyright (c) 2026, bogdan-glitchm <github.34373732+copyright@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Oops.hpp"

[[noreturn]] void Kernel::oops(Kernel::OopsCode code) {
    dbgln("----- KERNEL OOPS OCCURED -----");
    dbgln("Code: {} ({})", static_cast<u32>(code), _get_oopscode_name(code));
    dbgln("(No Message)");
    dbgln("----- -----");

    Kernel::Integrity::taint.fetch_or(
        static_cast<u64>(TaintFlags::Oops),
        AK::memory_order_seq_cst
    );

    if (Kernel::Process::has_current()) {
        if (!Processor::are_interrupts_enabled())
            PANIC("Kernel oops issued while interrupts are disabled, deemed unrecoverable.");

        auto* proc = &Kernel::Process::current();
        proc->crash(SIGKILL, {});
    } else {
        PANIC("Kernel oops issued outside schedulable context, deemed unrecoverable.");
    }
}
