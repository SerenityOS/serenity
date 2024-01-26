/*
 * Copyright (c) 2023, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/riscv64/pre_init.h>

#include <Kernel/Arch/Processor.h>
#include <Kernel/Arch/riscv64/MMU.h>
#include <Kernel/Arch/riscv64/SBI.h>
#include <Kernel/Sections.h>

namespace Kernel {

UNMAP_AFTER_INIT void dbgln_without_mmu(StringView message)
{
    auto probe_result = SBI::Base::probe_extension(SBI::EID::DebugConsole);
    if (probe_result.is_error() || probe_result.value() == 0) {
        for (auto const ch : message.bytes())
            (void)SBI::Legacy::console_putchar(ch);
        (void)SBI::Legacy::console_putchar('\n');
    } else {
        for (auto const ch : message.bytes())
            (void)SBI::DBCN::debug_console_write_byte(ch);
        (void)SBI::DBCN::debug_console_write_byte('\n');
    }
}

[[noreturn]] UNMAP_AFTER_INIT void panic_without_mmu(StringView message)
{
    dbgln_without_mmu("KERNEL PANIC in pre_init :^("sv);
    dbgln_without_mmu(message);

    // We can't use Processor::halt() here, as that would result in an absolute jump.
    RISCV64::CSR::write(RISCV64::CSR::Address::SIE, 0);
    for (;;)
        asm volatile("wfi");
}

[[gnu::aligned(4)]] [[noreturn]] UNMAP_AFTER_INIT static void early_trap_handler()
{
    panic_without_mmu("Unexpected trap"sv);
}

extern "C" [[noreturn]] UNMAP_AFTER_INIT void pre_init(FlatPtr mhartid, PhysicalPtr fdt_phys_addr)
{

    // Catch traps in pre_init
    RISCV64::CSR::write(RISCV64::CSR::Address::STVEC, bit_cast<FlatPtr>(&early_trap_handler));

    Memory::init_page_tables_and_jump_to_init(mhartid, fdt_phys_addr);
}

}
